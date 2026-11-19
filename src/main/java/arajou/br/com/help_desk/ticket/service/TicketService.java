package arajou.br.com.help_desk.ticket.service;

import arajou.br.com.help_desk.auditlog.model.enums.AuditAction;
import arajou.br.com.help_desk.auditlog.model.enums.AuditEntityType;
import arajou.br.com.help_desk.auditlog.service.AuditLogService;
import arajou.br.com.help_desk.security.SecurityService;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.ticket.repository.TicketRepository;
import arajou.br.com.help_desk.ticket.repository.specs.TicketSpecs;
import arajou.br.com.help_desk.ticket.validator.TicketValidator;
import arajou.br.com.help_desk.user.model.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orquestra o ciclo de vida do chamado: busca o {@link Ticket}, chama o
 * {@link TicketValidator} certo pra cada ação (ele decide "pode ou não pode"), e só então
 * altera o estado e salva.
 */
@Service // Registra a classe como bean de camada de serviço, gerenciado pelo Spring
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketValidator ticketValidator;
    private final SecurityService securityService;
    private final AuditLogService auditLogService;

    /**
     * Busca um chamado pelo id.
     *
     * @param id id do chamado
     * @return o chamado, se existir
     */
    public Optional<Ticket> findById(Long id){
        return ticketRepository.findById(id);
    }

    /**
     * Cria um novo chamado. O cliente autenticado é sempre o dono do chamado - nunca
     * confiamos em um customerId vindo do corpo da requisição, senão a API daria pra um
     * cliente abrir chamado "em nome" de outro.
     *
     * @param ticket chamado a criar (ainda sem customer)
     * @return o chamado já salvo, com id gerado
     */
    public Ticket save(Ticket ticket) {
        ticket.setCustomer(securityService.getAuthenticatedUser());
        Ticket saved = ticketRepository.save(ticket);
        auditLogService.record(AuditAction.TICKET_CREATED, AuditEntityType.TICKET, saved.getId(), null);
        return saved;
    }

    /**
     * Edita a descrição de um chamado (só permitido pelo dono, enquanto {@code PENDING}
     * - ver {@link TicketValidator#validateEdit}).
     *
     * @param ticket chamado a editar
     * @param newDescription nova descrição
     */
    public void updateDescription(Ticket ticket, String newDescription) {
        ticketValidator.validateEdit(ticket, securityService.getAuthenticatedUser());
        ticket.setDescription(newDescription);
        ticketRepository.save(ticket);
        auditLogService.record(AuditAction.TICKET_EDITED, AuditEntityType.TICKET, ticket.getId(), null);
    }

    /**
     * Técnico assume o chamado: {@code PENDING} -&gt; {@code IN_PROGRESS}.
     *
     * @param ticket chamado a ser assumido pelo técnico autenticado
     */
    public void assume(Ticket ticket) {
        ticketValidator.validateAssume(ticket);
        ticket.setTechnician(securityService.getAuthenticatedUser());
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);
        auditLogService.record(AuditAction.TICKET_ASSUMED, AuditEntityType.TICKET, ticket.getId(), null);
    }

    /**
     * Técnico responsável resolve: {@code IN_PROGRESS} -&gt; {@code RESOLVED}.
     *
     * @param ticket chamado a ser resolvido
     */
    public void resolve(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateResolve(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        auditLogService.record(AuditAction.TICKET_RESOLVED, AuditEntityType.TICKET, ticket.getId(), null);
    }

    /**
     * Cliente confirma: {@code RESOLVED} -&gt; {@code CLOSED}.
     *
     * @param ticket chamado a ser fechado
     */
    public void close(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateClose(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        auditLogService.record(AuditAction.TICKET_CLOSED, AuditEntityType.TICKET, ticket.getId(), null);
    }

    /**
     * Cliente cancela: {@code PENDING} -&gt; {@code CANCELLED}.
     *
     * @param ticket chamado a ser cancelado
     */
    public void cancel(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateCancel(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
        auditLogService.record(AuditAction.TICKET_CANCELLED, AuditEntityType.TICKET, ticket.getId(), null);
    }

    /**
     * Cliente reabre: volta pra {@code IN_PROGRESS} (não pra {@code PENDING}) porque o
     * técnico responsável já tem contexto do problema - não faz sentido devolver pra fila
     * geral. Limpa as datas de resolução/fechamento, já que o chamado está aberto de novo.
     *
     * @param ticket chamado a ser reaberto
     */
    public void reopen(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateReopen(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        ticketRepository.save(ticket);
        auditLogService.record(AuditAction.TICKET_REOPENED, AuditEntityType.TICKET, ticket.getId(), null);
    }

    /**
     * Busca paginada de chamados. O escopo da busca é automático conforme o papel do
     * usuário autenticado: cliente só vê os próprios chamados, técnico vê os
     * {@code PENDING} (pra escolher o que assumir) mais os que ele mesmo já assumiu, e
     * gerente (permissão {@code VIEW_REPORTS}) vê todos, sem restrição de dono.
     * Status e tema são filtros opcionais aplicados por cima desse escopo.
     *
     * @param status filtro opcional por status
     * @param theme filtro opcional por tema
     * @param page número da página (começando em 0)
     * @param pageSize tamanho da página
     * @return a página de resultados já restrita ao escopo do usuário autenticado
     */
    public Page<Ticket> search(TicketStatus status, TicketTheme theme, Integer page, Integer pageSize) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();

        Specification<Ticket> specs = Specification.where((root, query, cb) -> cb.conjunction());

        if (!hasAuthority(authenticatedUser, "VIEW_REPORTS")) {
            if (hasAuthority(authenticatedUser, "VIEW_PENDING_TICKETS")) {
                specs = specs.and(TicketSpecs.pendingOrAssignedTo(authenticatedUser.getId()));
            } else {
                specs = specs.and(TicketSpecs.customerIdEquals(authenticatedUser.getId()));
            }
        }

        if (status != null) {
            specs = specs.and(TicketSpecs.statusEquals(status));
        }

        if (theme != null) {
            specs = specs.and(TicketSpecs.themeEquals(theme));
        }

        Pageable pageable = PageRequest.of(page, pageSize);
        return ticketRepository.findAll(specs, pageable);
    }

    // Checa se o usuário tem uma determinada authority (role ou permissão) entre as suas.
    private boolean hasAuthority(UserModel user, String authority) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}

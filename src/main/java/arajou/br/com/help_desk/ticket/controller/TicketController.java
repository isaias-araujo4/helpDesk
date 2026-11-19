package arajou.br.com.help_desk.ticket.controller;

import arajou.br.com.help_desk.common.GenericController;
import arajou.br.com.help_desk.security.SecurityService;
import arajou.br.com.help_desk.ticket.mapper.TicketMapper;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.dto.TicketCreatedDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketResponseDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketUpdatedDTO;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.ticket.service.TicketService;
import arajou.br.com.help_desk.ticket.validator.TicketValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Endpoints do ciclo de vida do chamado: criar, visualizar, editar, assumir, resolver,
 * fechar, cancelar, reabrir e listar. Cada ação exige uma permissão específica
 * ({@code @PreAuthorize}) e, nas ações sobre um chamado já existente, também passa pela
 * checagem de dono/técnico responsável feita pelo {@link TicketValidator}.
 */
@RestController // Combina @Controller + @ResponseBody: cada método devolve diretamente o corpo da resposta (JSON), não uma view
@RequestMapping("tickets") // Prefixo comum de rota para todos os endpoints desta classe
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class TicketController implements GenericController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;
    private final TicketValidator ticketValidator;
    private final SecurityService securityService;

    /**
     * Abre um novo chamado. O cliente autenticado é sempre o dono.
     *
     * @param ticketCreatedDTO dados do chamado (título, descrição, tema)
     * @return 201 com o header {@code Location} apontando para o chamado criado
     */
    @PostMapping // Mapeia POST /tickets para este método
    @PreAuthorize("hasAuthority('CREATE_TICKET')") // Só executa se o usuário autenticado tiver essa permissão; senão, 403
    public ResponseEntity<Void> create(@RequestBody @Valid TicketCreatedDTO ticketCreatedDTO){
        Ticket ticket = ticketMapper.toEntity(ticketCreatedDTO);
        Ticket saved = ticketService.save(ticket);
        URI location = generateLocationHeader(saved.getId());
        return ResponseEntity.created(location).build();
    }

    /**
     * Busca um chamado por id. Só VIEW_OWN_TICKETS/VIEW_PENDING_TICKETS/VIEW_REPORTS
     * passam da porta de entrada ({@code @PreAuthorize}) - o
     * {@link TicketValidator#validateView} decide se ESSE chamado específico pode ser visto.
     *
     * @param id id do chamado
     * @return 200 com os dados do chamado, ou 404 se não existir
     */
    @GetMapping("{id}") // Mapeia GET /tickets/{id} para este método
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_TICKETS','VIEW_PENDING_TICKETS','VIEW_REPORTS')")
    public ResponseEntity<TicketResponseDTO> findById(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketValidator.validateView(ticket, securityService.getAuthenticatedUser());
                    return ResponseEntity.ok(ticketMapper.toTicketResponseDTO(ticket));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Edita a descrição de um chamado. Reaproveita {@code CREATE_TICKET} (é uma permissão
     * de cliente) - o {@link TicketValidator#validateEdit} é quem garante que só o DONO
     * desse chamado específico pode editá-lo.
     *
     * @param id id do chamado
     * @param ticketUpdatedDTO nova descrição
     * @return 204 em caso de sucesso, ou 404 se o chamado não existir
     */
    @PutMapping("{id}") // Mapeia PUT /tickets/{id} para este método
    @PreAuthorize("hasAuthority('CREATE_TICKET')")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody @Valid TicketUpdatedDTO ticketUpdatedDTO) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.updateDescription(ticket, ticketUpdatedDTO.description());
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Técnico assume o chamado ({@code PENDING} -&gt; {@code IN_PROGRESS}).
     *
     * @param id id do chamado
     * @return 204 em caso de sucesso, ou 404 se o chamado não existir
     */
    @PostMapping("{id}/assume") // Mapeia POST /tickets/{id}/assume para este método
    @PreAuthorize("hasAuthority('ASSUME_TICKET')")
    public ResponseEntity<Void> assume(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.assume(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Técnico responsável resolve o chamado ({@code IN_PROGRESS} -&gt; {@code RESOLVED}).
     *
     * @param id id do chamado
     * @return 204 em caso de sucesso, ou 404 se o chamado não existir
     */
    @PostMapping("{id}/resolve") // Mapeia POST /tickets/{id}/resolve para este método
    @PreAuthorize("hasAuthority('RESOLVE_TICKET')")
    public ResponseEntity<Void> resolve(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.resolve(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cliente confirma e fecha o chamado ({@code RESOLVED} -&gt; {@code CLOSED}).
     *
     * @param id id do chamado
     * @return 204 em caso de sucesso, ou 404 se o chamado não existir
     */
    @PostMapping("{id}/close") // Mapeia POST /tickets/{id}/close para este método
    @PreAuthorize("hasAuthority('CLOSE_TICKET')")
    public ResponseEntity<Void> close(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.close(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cliente cancela o chamado (só a partir de {@code PENDING}).
     *
     * @param id id do chamado
     * @return 204 em caso de sucesso, ou 404 se o chamado não existir
     */
    @PostMapping("{id}/cancel") // Mapeia POST /tickets/{id}/cancel para este método
    @PreAuthorize("hasAuthority('CANCEL_TICKET')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.cancel(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cliente reabre o chamado ({@code RESOLVED}/{@code CLOSED} -&gt; {@code IN_PROGRESS}).
     *
     * @param id id do chamado
     * @return 204 em caso de sucesso, ou 404 se o chamado não existir
     */
    @PostMapping("{id}/reopen") // Mapeia POST /tickets/{id}/reopen para este método
    @PreAuthorize("hasAuthority('REOPEN_TICKET')")
    public ResponseEntity<Void> reopen(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.reopen(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Busca paginada de chamados, com escopo automático por papel (ver
     * {@code TicketService.search}) e filtros opcionais por status/tema.
     *
     * @param status filtro opcional por status
     * @param theme filtro opcional por tema
     * @param page número da página (começando em 0)
     * @param pageSize tamanho da página
     * @return 200 com a página de chamados encontrados
     */
    @GetMapping // Mapeia GET /tickets para este método
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_TICKETS','VIEW_PENDING_TICKETS','VIEW_REPORTS')")
    public ResponseEntity<Page<TicketResponseDTO>> search(
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "theme", required = false) TicketTheme theme,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        Page<Ticket> result = ticketService.search(status, theme, page, pageSize);
        Page<TicketResponseDTO> dtoPage = result.map(ticketMapper::toTicketResponseDTO);

        return ResponseEntity.ok(dtoPage);
    }
}

package arajou.br.com.help_desk.ticket.validator;

import arajou.br.com.help_desk.exception.OperationNotAllowedException;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.user.model.UserModel;
import org.springframework.stereotype.Component;

/**
 * Concentra as regras de negócio do ciclo de vida do chamado: em que estado ele precisa
 * estar pra cada ação, e quem (cliente dono / técnico responsável) tem permissão de
 * executar aquela ação. O {@code TicketService} chama esses métodos ANTES de alterar o
 * {@link Ticket} - se alguma regra for violada, uma {@link OperationNotAllowedException}
 * interrompe a operação.
 * <p>
 * Diferente do {@code @PreAuthorize} dos controllers (que só checa se o usuário TEM a
 * permissão), aqui a checagem é sobre o registro específico: é o dono/técnico DESTE
 * chamado em particular.
 */
@Component // Registra a classe como bean gerenciado pelo Spring
public class TicketValidator {

    /**
     * Só um chamado {@code PENDING} pode ser assumido por um técnico (ainda não tem técnico).
     *
     * @param ticket chamado a validar
     */
    public void validateAssume(Ticket ticket){
        if (ticket.getStatus() != TicketStatus.PENDING){
            throw new OperationNotAllowedException("Only pending tickets can be assumed.");
        }
    }

    /**
     * Só quem assumiu o chamado (o técnico responsável) pode resolvê-lo, e só a partir
     * de {@code IN_PROGRESS} - não dá pra "pular" de {@code PENDING} direto pra {@code RESOLVED}.
     *
     * @param ticket chamado a validar
     * @param authemticatedUser usuário autenticado tentando resolver o chamado
     */
    public void validateResolve(Ticket ticket, UserModel authemticatedUser){
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS){
            throw new OperationNotAllowedException("Only tickets in progress can be resolved");
        }

        if (!ticket.getTechnician().getId().equals(authemticatedUser.getId())){
            throw new  OperationNotAllowedException("Only the assigned technician can resolve this ticket");
        }
    }

    /**
     * Fechar é a confirmação final do CLIENTE de que o problema foi resolvido - só ele
     * pode fazer isso, e só depois que o técnico marcou como {@code RESOLVED}.
     *
     * @param ticket chamado a validar
     * @param authenticatedUser usuário autenticado tentando fechar o chamado
     */
    public void validateClose(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new OperationNotAllowedException("Only resolved tickets can be closed.");
        }
        validateOwnership(ticket, authenticatedUser);
    }

    /**
     * Cancelar só é permitido enquanto ainda está {@code PENDING} (antes de qualquer
     * técnico investir tempo nele), e só o dono pode cancelar o próprio chamado.
     *
     * @param ticket chamado a validar
     * @param authenticatedUser usuário autenticado tentando cancelar o chamado
     */
    public void validateCancel(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending tickets can be cancelled.");
        }
        validateOwnership(ticket, authenticatedUser);
    }

    /**
     * Reabrir é o cliente dizendo "isso não foi resolvido de verdade" - só faz sentido
     * a partir de {@code RESOLVED} ou {@code CLOSED}, e só o dono do chamado pode reabrir.
     *
     * @param ticket chamado a validar
     * @param authenticatedUser usuário autenticado tentando reabrir o chamado
     */
    public void validateReopen(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new OperationNotAllowedException("Only resolved or closed tickets can be reopened.");
        }
        validateOwnership(ticket, authenticatedUser);
    }

    /**
     * Editar a descrição só é permitido enquanto o chamado está {@code PENDING} - depois
     * que um técnico assume e começa a trabalhar com base na descrição original, ela não
     * pode mais ser alterada pelo cliente.
     *
     * @param ticket chamado a validar
     * @param authenticatedUser usuário autenticado tentando editar o chamado
     */
    public void validateEdit(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.PENDING){
            throw new OperationNotAllowedException("Only pending tickets can be edited.");
        }

        validateOwnership(ticket, authenticatedUser);
    }

    // Regra repetida em várias ações acima: o usuário autenticado precisa ser o CLIENTE
    // dono do chamado (não o técnico, não outro cliente qualquer).
    private void validateOwnership(Ticket ticket, UserModel authenticatedUser) {
        if (!ticket.getCustomer().getId().equals(authenticatedUser.getId())) {
            throw new OperationNotAllowedException("You are not allowed to perform this action on this ticket.");
        }
    }

    /**
     * Visualizar um chamado é permitido pro dono (cliente), pro técnico responsável, ou
     * pra quem tem {@code VIEW_REPORTS} (gerente vê tudo). Sem essa checagem, ter a
     * permissão {@code VIEW_OWN_TICKETS} deixaria qualquer cliente ver o chamado de
     * QUALQUER outro cliente - a permissão sozinha não diferencia "meus chamados" de
     * "chamados de qualquer um".
     *
     * @param ticket chamado a validar
     * @param authenticatedUser usuário autenticado tentando visualizar o chamado
     */
    public void validateView(Ticket ticket, UserModel authenticatedUser) {
        boolean isOwner = ticket.getCustomer().getId().equals(authenticatedUser.getId());
        boolean isAssignedTechnician = ticket.getTechnician() != null
                && ticket.getTechnician().getId().equals(authenticatedUser.getId());
        boolean isManager = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("VIEW_REPORTS"));

        if (!isOwner && !isAssignedTechnician && !isManager) {
            throw new OperationNotAllowedException("You are not allowed to view this ticket.");
        }
    }
}

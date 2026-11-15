package arajou.br.com.help_desk.ticket.validator;

import arajou.br.com.help_desk.exception.OperationNotAllowedException;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.user.model.UserModel;
import org.springframework.stereotype.Component;

@Component
public class TicketValidador {

    public void validateAssume(Ticket ticket){
        if (ticket.getStatus() != TicketStatus.PENDING){
            throw new OperationNotAllowedException("Only pending tickets can be assumed.");
        }
    }

    public void validateResolve(Ticket ticket, UserModel authemticatedUser){
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS){
            throw new OperationNotAllowedException("Only tickets in progress can be resolved");
        }

        if (!ticket.getTechnician().getId().equals(authemticatedUser.getId())){
            throw new  OperationNotAllowedException("Only the assigned technician can resolve this ticket");
        }
    }

    public void validateClose(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new OperationNotAllowedException("Only resolved tickets can be closed.");
        }
        validateOwnership(ticket, authenticatedUser);
    }

    public void validateCancel(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending tickets can be cancelled.");
        }
        validateOwnership(ticket, authenticatedUser);
    }

    public void validateReopen(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new OperationNotAllowedException("Only resolved or closed tickets can be reopened.");
        }
        validateOwnership(ticket, authenticatedUser);
    }

    public void validateEdit(Ticket ticket, UserModel authenticatedUser) {
        if (ticket.getStatus() != TicketStatus.PENDING){
            throw new OperationNotAllowedException("Only pending tickets can be edited.");
        }

        validateOwnership(ticket, authenticatedUser);
    }

    private void validateOwnership(Ticket ticket, UserModel authenticatedUser) {
        if (!ticket.getCustomer().getId().equals(authenticatedUser.getId())) {
            throw new OperationNotAllowedException("You are not allowed to perform this action on this ticket.");
        }
    }
}

package arajou.br.com.help_desk.ticket.service;

import arajou.br.com.help_desk.security.SecurityService;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.repository.TicketRepository;
import arajou.br.com.help_desk.ticket.validator.TicketValidator;
import arajou.br.com.help_desk.user.model.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketValidator ticketValidator;
    private final SecurityService securityService;

    public Optional<Ticket> findById(Long id){
        return ticketRepository.findById(id);
    }

    public Ticket save(Ticket ticket) {
        ticket.setCustomer(securityService.getAuthenticatedUser());
        return ticketRepository.save(ticket);
    }

    public void updateDescription(Ticket ticket, String newDescription) {
        ticketValidator.validateEdit(ticket, securityService.getAuthenticatedUser());
        ticket.setDescription(newDescription);
        ticketRepository.save(ticket);
    }

    public void assume(Ticket ticket) {
        ticketValidator.validateAssume(ticket);
        ticket.setTechnician(securityService.getAuthenticatedUser());
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);
    }

    public void resolve(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateResolve(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    public void close(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateClose(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    public void cancel(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateCancel(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
    }

    public void reopen(Ticket ticket) {
        UserModel authenticatedUser = securityService.getAuthenticatedUser();
        ticketValidator.validateReopen(ticket, authenticatedUser);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        ticketRepository.save(ticket);
    }
}

package arajou.br.com.help_desk.ticket.validator;

import arajou.br.com.help_desk.exception.OperationNotAllowedException;
import arajou.br.com.help_desk.security.Permission;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.user.model.UserModel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários da máquina de estados do chamado - sem Spring, sem banco, só a lógica
 * pura do {@link TicketValidator}. Cada ação é testada no caminho feliz e em pelo menos
 * uma violação (estado errado e/ou usuário errado).
 */
class TicketValidatorTest {

    private final TicketValidator validator = new TicketValidator();

    private UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    private Ticket ticket(TicketStatus status, UserModel customer, UserModel technician) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status);
        ticket.setCustomer(customer);
        ticket.setTechnician(technician);
        return ticket;
    }

    @Test
    void validateAssume_allowsWhenPending() {
        Ticket ticket = ticket(TicketStatus.PENDING, user(1L), null);

        assertDoesNotThrow(() -> validator.validateAssume(ticket));
    }

    @Test
    void validateAssume_rejectsWhenNotPending() {
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L), user(2L));

        assertThrows(OperationNotAllowedException.class, () -> validator.validateAssume(ticket));
    }

    @Test
    void validateResolve_allowsAssignedTechnicianWhenInProgress() {
        UserModel technician = user(2L);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L), technician);

        assertDoesNotThrow(() -> validator.validateResolve(ticket, technician));
    }

    @Test
    void validateResolve_rejectsWhenNotInProgress() {
        UserModel technician = user(2L);
        Ticket ticket = ticket(TicketStatus.PENDING, user(1L), technician);

        assertThrows(OperationNotAllowedException.class, () -> validator.validateResolve(ticket, technician));
    }

    @Test
    void validateResolve_rejectsTechnicianNotAssignedToThisTicket() {
        UserModel assignedTechnician = user(2L);
        UserModel anotherTechnician = user(3L);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L), assignedTechnician);

        assertThrows(OperationNotAllowedException.class, () -> validator.validateResolve(ticket, anotherTechnician));
    }

    @Test
    void validateClose_allowsOwnerWhenResolved() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.RESOLVED, customer, user(2L));

        assertDoesNotThrow(() -> validator.validateClose(ticket, customer));
    }

    @Test
    void validateClose_rejectsWhenNotResolved() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, customer, user(2L));

        assertThrows(OperationNotAllowedException.class, () -> validator.validateClose(ticket, customer));
    }

    @Test
    void validateClose_rejectsNonOwner() {
        UserModel customer = user(1L);
        UserModel stranger = user(3L);
        Ticket ticket = ticket(TicketStatus.RESOLVED, customer, user(2L));

        assertThrows(OperationNotAllowedException.class, () -> validator.validateClose(ticket, stranger));
    }

    @Test
    void validateCancel_allowsOwnerWhenPending() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.PENDING, customer, null);

        assertDoesNotThrow(() -> validator.validateCancel(ticket, customer));
    }

    @Test
    void validateCancel_rejectsWhenNotPending() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, customer, user(2L));

        assertThrows(OperationNotAllowedException.class, () -> validator.validateCancel(ticket, customer));
    }

    @Test
    void validateReopen_allowsOwnerWhenResolvedOrClosed() {
        UserModel customer = user(1L);
        Ticket resolved = ticket(TicketStatus.RESOLVED, customer, user(2L));
        Ticket closed = ticket(TicketStatus.CLOSED, customer, user(2L));

        assertDoesNotThrow(() -> validator.validateReopen(resolved, customer));
        assertDoesNotThrow(() -> validator.validateReopen(closed, customer));
    }

    @Test
    void validateReopen_rejectsWhenPending() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.PENDING, customer, null);

        assertThrows(OperationNotAllowedException.class, () -> validator.validateReopen(ticket, customer));
    }

    @Test
    void validateEdit_allowsOwnerWhenPending() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.PENDING, customer, null);

        assertDoesNotThrow(() -> validator.validateEdit(ticket, customer));
    }

    @Test
    void validateEdit_rejectsWhenNotPending() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, customer, user(2L));

        assertThrows(OperationNotAllowedException.class, () -> validator.validateEdit(ticket, customer));
    }

    @Test
    void validateView_allowsOwner() {
        UserModel customer = user(1L);
        Ticket ticket = ticket(TicketStatus.PENDING, customer, null);

        assertDoesNotThrow(() -> validator.validateView(ticket, customer));
    }

    @Test
    void validateView_allowsAssignedTechnician() {
        UserModel technician = user(2L);
        Ticket ticket = ticket(TicketStatus.IN_PROGRESS, user(1L), technician);

        assertDoesNotThrow(() -> validator.validateView(ticket, technician));
    }

    @Test
    void validateView_allowsUserWithViewReportsAuthority() {
        UserModel manager = user(4L);
        manager.setExtraPermissions(Set.of(Permission.VIEW_REPORTS));
        Ticket ticket = ticket(TicketStatus.PENDING, user(1L), null);

        assertDoesNotThrow(() -> validator.validateView(ticket, manager));
    }

    @Test
    void validateView_rejectsUnrelatedUser() {
        UserModel stranger = user(5L);
        Ticket ticket = ticket(TicketStatus.PENDING, user(1L), user(2L));

        assertThrows(OperationNotAllowedException.class, () -> validator.validateView(ticket, stranger));
    }
}

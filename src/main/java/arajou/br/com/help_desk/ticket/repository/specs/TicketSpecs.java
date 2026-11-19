package arajou.br.com.help_desk.ticket.repository.specs;

import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TicketSpecs {
    public static Specification<Ticket> statusEquals(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> themeEquals(TicketTheme theme) {
        return (root, query, cb) -> cb.equal(root.get("theme"), theme);
    }

    public static Specification<Ticket> customerIdEquals(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    // Usado pro técnico: vê chamados ainda sem dono (PENDING, pra escolher o que assumir)
    // OU os que ele mesmo já assumiu (pra acompanhar o próprio trabalho).
    public static Specification<Ticket> pendingOrAssignedTo(Long technicianId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("status"), TicketStatus.PENDING),
                cb.equal(root.get("technician").get("id"), technicianId)
        );
    }

    public static  Specification<Ticket> createdOnBetween(LocalDateTime start, LocalDateTime end){
        return (root, query, cb) -> cb.between(root.get("createdOn"), start, end);
    }

    public static Specification<Ticket> customerDepartmentEquals(Department department){
        return (root, query, cb) -> cb.equal(root.get("customer").get("department"), department);
    }
}

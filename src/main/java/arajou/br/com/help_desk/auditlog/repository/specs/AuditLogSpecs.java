package arajou.br.com.help_desk.auditlog.repository.specs;

import arajou.br.com.help_desk.auditlog.model.AuditLog;
import arajou.br.com.help_desk.auditlog.model.enums.AuditAction;
import arajou.br.com.help_desk.auditlog.model.enums.AuditEntityType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Filtros opcionais e independentes para a busca do log de auditoria - mesmo padrão usado
 * em {@code TicketSpecs}/{@code UserSpecs}.
 */
public class AuditLogSpecs {

    public static Specification<AuditLog> entityTypeEquals(AuditEntityType entityType) {
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> actionEquals(AuditAction action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> performedAtAfterOrEqual(LocalDateTime start) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("performedAt"), start);
    }

    public static Specification<AuditLog> performedAtBeforeOrEqual(LocalDateTime end) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("performedAt"), end);
    }
}

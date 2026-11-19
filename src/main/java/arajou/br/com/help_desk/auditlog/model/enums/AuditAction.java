package arajou.br.com.help_desk.auditlog.model.enums;

/**
 * Ações relevantes registradas no log de auditoria. Cada valor corresponde a um ponto
 * específico do código (TicketService ou UserService) onde {@code AuditLogService.record}
 * é chamado.
 */
public enum AuditAction {
    TICKET_CREATED,
    TICKET_EDITED,
    TICKET_ASSUMED,
    TICKET_RESOLVED,
    TICKET_CLOSED,
    TICKET_CANCELLED,
    TICKET_REOPENED,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED
}

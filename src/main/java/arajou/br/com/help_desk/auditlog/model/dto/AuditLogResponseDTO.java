package arajou.br.com.help_desk.auditlog.model.dto;

import arajou.br.com.help_desk.auditlog.model.enums.AuditAction;
import arajou.br.com.help_desk.auditlog.model.enums.AuditEntityType;

import java.time.LocalDateTime;

/**
 * Representação de uma entrada do log de auditoria devolvida pela API.
 */
public record AuditLogResponseDTO(
        Long id,
        AuditAction action,
        AuditEntityType entityType,
        Long entityId,
        String performedBy,
        LocalDateTime performedAt,
        String details
) {
}

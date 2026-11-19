package arajou.br.com.help_desk.auditlog.repository;

import arajou.br.com.help_desk.auditlog.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acesso a dados do log de auditoria. {@link JpaSpecificationExecutor} permite montar a
 * busca filtrada dinamicamente, ver {@code AuditLogSpecs}.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}

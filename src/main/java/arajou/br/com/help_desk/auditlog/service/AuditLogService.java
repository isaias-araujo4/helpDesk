package arajou.br.com.help_desk.auditlog.service;

import arajou.br.com.help_desk.auditlog.model.AuditLog;
import arajou.br.com.help_desk.auditlog.model.enums.AuditAction;
import arajou.br.com.help_desk.auditlog.model.enums.AuditEntityType;
import arajou.br.com.help_desk.auditlog.repository.AuditLogRepository;
import arajou.br.com.help_desk.auditlog.repository.specs.AuditLogSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Grava e consulta o log de auditoria. Quem chama {@link #record} não precisa saber quem
 * está autenticado - esse detalhe vem do mesmo {@link AuditorAware} já usado para preencher
 * {@code createdBy}/{@code updatedBy} nas outras entidades.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditorAware<String> auditorAware;

    /**
     * Registra uma nova entrada no log de auditoria. Nunca lança exception - uma falha ao
     * gravar o log não pode derrubar a ação de negócio que está sendo auditada.
     *
     * @param action ação realizada
     * @param entityType tipo da entidade afetada
     * @param entityId id da entidade afetada
     * @param details contexto extra opcional (pode ser {@code null})
     */
    public void record(AuditAction action, AuditEntityType entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setPerformedBy(auditorAware.getCurrentAuditor().orElse("system"));
        auditLog.setPerformedAt(LocalDateTime.now());
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    /**
     * Busca paginada do log de auditoria, com filtros opcionais e independentes entre si.
     *
     * @param entityType filtro opcional por tipo de entidade
     * @param action filtro opcional por ação
     * @param startDate filtro opcional: só entradas a partir desta data (inclusive)
     * @param endDate filtro opcional: só entradas até esta data (inclusive)
     * @param page número da página (começando em 0)
     * @param pageSize tamanho da página
     * @return a página de resultados, mais recentes primeiro
     */
    public Page<AuditLog> search(
            AuditEntityType entityType,
            AuditAction action,
            LocalDate startDate,
            LocalDate endDate,
            Integer page,
            Integer pageSize
    ) {
        Specification<AuditLog> specs = Specification.where((root, query, cb) -> cb.conjunction());

        if (entityType != null) {
            specs = specs.and(AuditLogSpecs.entityTypeEquals(entityType));
        }
        if (action != null) {
            specs = specs.and(AuditLogSpecs.actionEquals(action));
        }
        if (startDate != null) {
            specs = specs.and(AuditLogSpecs.performedAtAfterOrEqual(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            specs = specs.and(AuditLogSpecs.performedAtBeforeOrEqual(endDate.atTime(LocalTime.MAX)));
        }

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("performedAt").descending());

        return auditLogRepository.findAll(specs, pageable);
    }
}

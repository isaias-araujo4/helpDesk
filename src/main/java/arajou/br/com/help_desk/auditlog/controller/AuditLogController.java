package arajou.br.com.help_desk.auditlog.controller;

import arajou.br.com.help_desk.auditlog.mapper.AuditLogMapper;
import arajou.br.com.help_desk.auditlog.model.AuditLog;
import arajou.br.com.help_desk.auditlog.model.dto.AuditLogResponseDTO;
import arajou.br.com.help_desk.auditlog.model.enums.AuditAction;
import arajou.br.com.help_desk.auditlog.model.enums.AuditEntityType;
import arajou.br.com.help_desk.auditlog.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Consulta do log de auditoria - só leitura, nenhuma entrada é criada por aqui (são
 * gravadas internamente pelo {@code TicketService}/{@code UserService} via
 * {@link AuditLogService#record}).
 */
@RestController
@RequestMapping("audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    // Limite máximo de itens por página - evita que um cliente peça pageSize=999999 e
    // force a aplicação a carregar a tabela inteira de uma vez.
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

    /**
     * Busca paginada do log de auditoria, com filtros opcionais por tipo de entidade,
     * ação e período.
     *
     * @param entityType filtro opcional por tipo de entidade (TICKET/USER)
     * @param action filtro opcional por ação
     * @param startDate filtro opcional: só entradas a partir desta data
     * @param endDate filtro opcional: só entradas até esta data
     * @param page número da página (começando em 0)
     * @param pageSize tamanho da página
     * @return 200 com a página de entradas encontradas, mais recentes primeiro
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')") // Só MANAGER tem essa permissão por padrão
    public ResponseEntity<Page<AuditLogResponseDTO>> search(
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<AuditLog> result = auditLogService.search(
                entityType, action, startDate, endDate, page, Math.min(pageSize, MAX_PAGE_SIZE));

        return ResponseEntity.ok(result.map(auditLogMapper::toResponseDTO));
    }
}

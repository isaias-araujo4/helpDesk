package arajou.br.com.help_desk.auditlog.mapper;

import arajou.br.com.help_desk.auditlog.model.AuditLog;
import arajou.br.com.help_desk.auditlog.model.dto.AuditLogResponseDTO;
import org.mapstruct.Mapper;

/**
 * Mapeamento direto entidade -&gt; DTO - todos os campos têm o mesmo nome dos dois lados,
 * então o MapStruct resolve sozinho, sem precisar de nenhum {@code @Mapping}.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponseDTO toResponseDTO(AuditLog auditLog);
}

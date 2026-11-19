package arajou.br.com.help_desk.auditlog.model;

import arajou.br.com.help_desk.auditlog.model.enums.AuditAction;
import arajou.br.com.help_desk.auditlog.model.enums.AuditEntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Uma entrada imutável do log de auditoria: registra QUEM fez O QUÊ, QUANDO, sobre qual
 * registro. Diferente de {@code createdBy}/{@code updatedBy} (que só guardam a ÚLTIMA pessoa
 * a mexer num registro), isso é um histórico completo - cada ação relevante gera uma linha
 * nova, nenhuma é sobrescrita. Gravado explicitamente via {@code AuditLogService.record},
 * chamado nos pontos importantes do {@code TicketService} e do {@code UserService}.
 */
@Entity // Marca a classe como uma entidade JPA, mapeada para uma tabela do banco
@Table(name = "audit_logs") // Define o nome da tabela no banco de dados
@Data // Lombok: gera getters, setters, equals/hashCode e toString
@NoArgsConstructor // Lombok: gera um construtor sem argumentos (exigido pelo JPA)
@AllArgsConstructor // Lombok: gera um construtor com todos os campos
public class AuditLog {

    @Id // Marca o campo como chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Geração delegada ao banco (auto-incremento)
    private Long id;

    @Enumerated(EnumType.STRING) // Persiste o enum pelo nome ("TICKET_CREATED"), não pelo índice numérico
    @Column(length = 30, nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 20, nullable = false)
    private AuditEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // E-mail de quem executou a ação (ou "system" quando não há usuário autenticado).
    @Column(name = "performed_by", length = 50, nullable = false)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    // Texto livre opcional com contexto extra (ex: qual permissão foi concedida/revogada).
    @Column(columnDefinition = "TEXT")
    private String details;
}

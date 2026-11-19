package arajou.br.com.help_desk.ticket.model;


import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.user.model.UserModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidade que representa um chamado de suporte técnico aberto por um cliente.
 * Ver {@link TicketStatus} para o ciclo de vida completo dos estados.
 */
@Entity // Marca a classe como uma entidade JPA, mapeada para uma tabela do banco
@Table(name = "tickets") // Define o nome da tabela
@Data // Lombok: gera getters, setters, equals/hashCode, toString e um construtor sem argumentos
@AllArgsConstructor // Lombok: gera um construtor com todos os campos
@NoArgsConstructor // Lombok: gera um construtor sem argumentos (exigido pelo JPA para instanciar a entidade)
@EntityListeners(AuditingEntityListener.class) // Habilita o preenchimento automático do campo @CreatedDate
public class Ticket {

    @Id // Chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Geração delegada ao banco (auto-incremento)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    // TEXT em vez de VARCHAR: a descrição do problema pode ser longa, diferente de
    // campos curtos como o título.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING) // Persiste o enum pelo nome (ex: "HARDWARE"), não pelo índice numérico - mais legível e seguro contra reordenação do enum
    @Column(length = 30, nullable = false)
    private TicketTheme theme;

    // Já nasce PENDING - reflete a regra de negócio de que todo chamado recém-criado
    // aguarda um técnico assumir.
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    // Dono do chamado - quem criou. Só ele pode editar a descrição, cancelar ou reabrir
    // (ver TicketValidator).
    @ManyToOne // Relacionamento N:1 - vários chamados podem ter o mesmo cliente
    @JoinColumn(name = "customer_id", nullable = false) // Nome da coluna de chave estrangeira; obrigatória (todo chamado tem um dono)
    private UserModel customer;

    // Quem assumiu o atendimento - fica null até algum TECHNICIAN assumir o chamado.
    @ManyToOne
    @JoinColumn(name = "technician_id") // Nullable (sem "nullable = false"): o chamado pode não ter técnico ainda
    private UserModel technician;

    // Quando o técnico marcou o chamado como RESOLVED.
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Quando o cliente confirmou e fechou o chamado (CLOSED).
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreatedDate // Spring Data Auditing: preenche automaticamente com a data/hora no momento da criação
    @Column(name = "created_on")
    private LocalDateTime createdOn;
}

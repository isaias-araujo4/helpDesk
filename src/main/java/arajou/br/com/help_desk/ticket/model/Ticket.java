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

import java.time.LocalDate;

@Entity
@Table(name = "tickets")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TicketTheme theme;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private UserModel customer;

    @ManyToOne
    @JoinColumn(name = "technican_id")
    private UserModel technician;

    @Column(name = "resolved_at")
    private LocalDate resolvedAt;

    @Column(name = "closed_at")
    private LocalDate closedAt;

    @CreatedDate
    @Column(name = "created_on")
    private LocalDate createdOn;
}

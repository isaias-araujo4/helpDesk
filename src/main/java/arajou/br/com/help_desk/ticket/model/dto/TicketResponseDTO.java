package arajou.br.com.help_desk.ticket.model.dto;

import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;

import java.time.LocalDateTime;

public record TicketResponseDTO(
        Long id,
        String title,
        String description,
        TicketTheme theme,
        TicketStatus status,
        Long customerId,
        String customerName,
        Long technicianId,
        String technicianName,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        LocalDateTime createdOn
) {
}

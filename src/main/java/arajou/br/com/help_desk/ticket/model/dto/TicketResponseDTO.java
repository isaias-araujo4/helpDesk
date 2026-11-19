package arajou.br.com.help_desk.ticket.model.dto;

import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;

import java.time.LocalDateTime;

/**
 * Representação de um chamado devolvida pela API. Inclui o id e o nome do cliente e do
 * técnico (quando houver) já resolvidos, evitando que o cliente da API precise fazer
 * outra chamada só para exibir esses nomes.
 */
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

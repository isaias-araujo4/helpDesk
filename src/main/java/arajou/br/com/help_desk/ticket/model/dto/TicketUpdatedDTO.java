package arajou.br.com.help_desk.ticket.model.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketUpdatedDTO(
        @NotBlank(message = "Required field")
        String description
) {
}

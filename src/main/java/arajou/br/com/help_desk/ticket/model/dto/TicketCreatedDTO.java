package arajou.br.com.help_desk.ticket.model.dto;

import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketCreatedDTO(
        @NotBlank(message = "Required field")
        String title,

        @NotBlank(message = "Required field")
        String description,

        @NotNull(message = "Required field")
        TicketTheme theme
) {
}

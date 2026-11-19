package arajou.br.com.help_desk.ticket.model.dto;

import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisição de abertura de chamado ({@code POST /tickets}). O cliente só
 * informa título, descrição e tema - o status nasce {@code PENDING} e o dono do chamado
 * é sempre o usuário autenticado, nunca vindo do corpo da requisição.
 */
public record TicketCreatedDTO(
        @NotBlank(message = "Required field")
        String title,

        @NotBlank(message = "Required field")
        String description,

        @NotNull(message = "Required field")
        TicketTheme theme
) {
}

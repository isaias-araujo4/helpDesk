package arajou.br.com.help_desk.ticket.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da requisição de edição de chamado ({@code PUT /tickets/{id}}). Só a descrição é
 * editável - título e tema são definidos na abertura e não mudam depois. Só é aceito
 * enquanto o chamado está {@code PENDING} (ver {@code TicketValidator.validateEdit}).
 */
public record TicketUpdatedDTO(
        @NotBlank(message = "Required field")
        String description
) {
}

package arajou.br.com.help_desk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da requisição de login ({@code POST /auth/login}).
 */
public record LoginRequestDTO(
        @NotBlank(message = "Required field") // Bean Validation: não pode ser nulo/vazio/só espaços
        @Email(message = "Invalid email") // Bean Validation: precisa ter formato de e-mail válido
        String email,

        @NotBlank(message = "Required field")
        String password
) {
}

package arajou.br.com.help_desk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Required field")
        @Email(message = "Invalid email")
        String email,

        @NotBlank(message = "Required field")
        String password
) {
}

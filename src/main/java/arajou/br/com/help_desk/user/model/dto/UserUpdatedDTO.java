package arajou.br.com.help_desk.user.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdatedDTO(
        @NotBlank(message = "required field")
        String firstName,

        @NotBlank(message = "required field")
        String lastName,

        @Email(message = "Invalid email" ) @NotBlank(message = "required field")
        String email

        ) {
}

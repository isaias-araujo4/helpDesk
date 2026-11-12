package arajou.br.com.help_desk.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDTO(
        @NotBlank(message = "Required field")
        @Size(min = 8, message = "Must be at least 8 characters")
        String newPassword
) {
}

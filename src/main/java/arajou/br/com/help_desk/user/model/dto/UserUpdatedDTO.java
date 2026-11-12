package arajou.br.com.help_desk.user.model.dto;

import arajou.br.com.help_desk.user.model.enums.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserUpdatedDTO(
        @NotBlank(message = "Required field")
        String firstName,

        @NotBlank(message = "Required field")
        String lastName,

        @NotNull(message = "Required field")
        Department department,

        @NotNull(message = "Required field")
        Long roleId,

        @Email(message = "Invalid email" ) @NotBlank(message = "Required field")
        String email

        ) {
}

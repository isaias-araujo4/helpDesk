package arajou.br.com.help_desk.user.model.dto;


import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreatedDTO(
        @NotBlank(message = "Required field")
        String firstName,

        @NotBlank(message = "Required field")
        String lastName,

        @NotNull(message = "Required field")
        Department department,

        @Email(message = "Invalid email" ) @NotBlank(message = "Required field")
        String email,

        @NotBlank(message = "Required field")
        @Size(min = 8, message = "Must be at least 8 characters")
        String password,

        @NotNull(message = "Required field")
        UserRole role
) {
}

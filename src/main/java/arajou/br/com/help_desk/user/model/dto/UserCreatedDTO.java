package arajou.br.com.help_desk.user.model.dto;


import arajou.br.com.help_desk.user.model.enums.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de cadastro de usuário ({@code POST /users}, feito pelo MANAGER).
 * Propositalmente não tem campo de senha - o sistema gera uma temporária sozinho
 * (ver {@link arajou.br.com.help_desk.user.service.UserService#save(arajou.br.com.help_desk.user.model.UserModel)}).
 */
public record UserCreatedDTO(
        @NotBlank(message = "Required field")
        String firstName,

        @NotBlank(message = "Required field")
        String lastName,

        @NotNull(message = "Required field")
        Department department,

        @Email(message = "Invalid email" ) @NotBlank(message = "Required field")
        String email,

        @NotNull(message = "Required field")
        Long roleId
) {
}

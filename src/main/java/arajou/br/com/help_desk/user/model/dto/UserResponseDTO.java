package arajou.br.com.help_desk.user.model.dto;

import arajou.br.com.help_desk.security.Permission;
import arajou.br.com.help_desk.user.model.enums.Department;

import java.time.LocalDate;
import java.util.Set;

/**
 * Representação de um usuário devolvida pela API. Nunca inclui a senha (nem em hash) -
 * o {@code UserMapper} simplesmente não copia esse campo da entidade.
 */
public record UserResponseDTO(
    Long id,
    String firstName,
    String lastName,
    Department department,
    String email,
    Long roleId,
    String roleName,
    Set<Permission> extraPermissions,
    Boolean active,
    String createdBy,
    LocalDate createdOn,
    String updatedBy,
    LocalDate updatedOn
) {
}

package arajou.br.com.help_desk.user.model.dto;

import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.model.enums.UserRole;

import java.time.LocalDate;

public record UserResponseDTO(
    Long id,
    String firstName,
    String lastName,
    Department department,
    String email,
    UserRole role,
    Boolean active,
    String createdBy,
    LocalDate createdOn,
    String updatedBy,
    LocalDate updatedOn
) {
}

package arajou.br.com.help_desk.user.model.dto;

import java.time.LocalDate;

public record UserResponseDTO(
    Long id,
    String firstName,
    String lastName,
    String email,
    Boolean active,
    String createdBy,
    LocalDate createdOn,
    String updatedBy,
    LocalDate updatedOn
) {
}

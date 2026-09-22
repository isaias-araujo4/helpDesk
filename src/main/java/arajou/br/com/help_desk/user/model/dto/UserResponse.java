package arajou.br.com.help_desk.user.model.dto;

import java.time.LocalDate;

public record UserResponse(
    Long id,
    String firstName,
    String LastName,
    String email,
    Boolean active,
    String createdBy,
    LocalDate createdOn,
    String updatedBy,
    LocalDate updatedOn
) {
}

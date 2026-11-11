package arajou.br.com.help_desk.auth.dto;

public record LoginResponseDTO(
        String token, boolean mustChangePassword
) {
}

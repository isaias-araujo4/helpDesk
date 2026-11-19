package arajou.br.com.help_desk.auth.dto;

/**
 * Resposta de um login bem-sucedido.
 *
 * @param token JWT a ser enviado no header {@code Authorization: Bearer <token>} nas próximas requisições
 * @param mustChangePassword se {@code true}, o cliente deve chamar {@code PUT /auth/change-password}
 *                           antes de usar qualquer outro endpoint - todos os demais retornarão 403
 */
public record LoginResponseDTO(
        String token, boolean mustChangePassword
) {
}

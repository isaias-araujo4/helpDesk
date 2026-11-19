package arajou.br.com.help_desk.exception;

/**
 * Lançada quando um e-mail acumulou tentativas de login erradas demais em sequência -
 * ver {@link arajou.br.com.help_desk.security.LoginAttemptService}. Bloqueia novas
 * tentativas por um tempo, mesmo com a senha certa, até o bloqueio expirar.
 * <p>
 * Tratada por {@link GlobalExceptionHandler}, que a converte numa resposta HTTP 429.
 */
public class TooManyLoginAttemptsException extends RuntimeException {

    /**
     * @param message mensagem informando o motivo do bloqueio
     */
    public TooManyLoginAttemptsException(String message) {
        super(message);
    }
}

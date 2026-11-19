package arajou.br.com.help_desk.exception;

// Lançada quando um e-mail acumulou tentativas de login erradas demais em sequência -
// ver LoginAttemptService. Bloqueia novas tentativas por um tempo, mesmo com a senha certa,
// até o bloqueio expirar.
public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException(String message) {
        super(message);
    }
}

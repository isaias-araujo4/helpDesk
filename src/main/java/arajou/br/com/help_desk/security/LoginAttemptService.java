package arajou.br.com.help_desk.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Proteção contra força bruta no login: depois de MAX_ATTEMPTS tentativas erradas seguidas
// pro mesmo e-mail, bloqueia novas tentativas (mesmo com a senha certa) por LOCKOUT_DURATION.
// Guarda o contador em memória (ConcurrentHashMap) - suficiente pra uma instância única da
// aplicação. Se um dia rodar várias instâncias ao mesmo tempo, cada uma teria seu próprio
// contador isolado, e isso precisaria virar um contador compartilhado (ex: Redis).
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final Map<String, Attempt> attemptsByEmail = new ConcurrentHashMap<>();

    public void recordFailure(String email) {
        attemptsByEmail.compute(normalize(email), (key, previous) ->
                new Attempt(previous == null ? 1 : previous.count() + 1, Instant.now()));
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    public boolean isBlocked(String email) {
        Attempt attempt = attemptsByEmail.get(normalize(email));
        if (attempt == null || attempt.count() < MAX_ATTEMPTS) {
            return false;
        }

        boolean stillLocked = Instant.now().isBefore(attempt.lastFailureAt().plus(LOCKOUT_DURATION));
        if (!stillLocked) {
            // Bloqueio expirou - limpa pra não segurar memória com entradas antigas.
            attemptsByEmail.remove(normalize(email));
        }
        return stillLocked;
    }

    private String normalize(String email) {
        return email == null ? "" : email.toLowerCase();
    }

    private record Attempt(int count, Instant lastFailureAt) {
    }
}

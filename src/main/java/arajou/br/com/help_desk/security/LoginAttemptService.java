package arajou.br.com.help_desk.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra força bruta no login: depois de {@value #MAX_ATTEMPTS} tentativas erradas
 * seguidas pro mesmo e-mail, bloqueia novas tentativas (mesmo com a senha certa) por
 * {@link #LOCKOUT_DURATION}.
 * <p>
 * Guarda o contador em memória ({@link ConcurrentHashMap}) - suficiente pra uma instância
 * única da aplicação. Se um dia rodar várias instâncias ao mesmo tempo, cada uma teria seu
 * próprio contador isolado, e isso precisaria virar um contador compartilhado (ex: Redis).
 */
@Component // Registra a classe como bean gerenciado pelo Spring
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    // ConcurrentHashMap: a API web atende requisições em paralelo (várias threads), então a
    // estrutura que guarda as tentativas precisa ser segura para acesso concorrente.
    private final Map<String, Attempt> attemptsByEmail = new ConcurrentHashMap<>();

    /**
     * Registra mais uma tentativa de login falha para o e-mail informado.
     *
     * @param email e-mail usado na tentativa de login
     */
    public void recordFailure(String email) {
        // compute: operação atômica de ler+atualizar o valor associado à chave - evita
        // condição de corrida entre duas tentativas falhas simultâneas para o mesmo e-mail.
        attemptsByEmail.compute(normalize(email), (key, previous) ->
                new Attempt(previous == null ? 1 : previous.count() + 1, Instant.now()));
    }

    /**
     * Login bem-sucedido: zera o contador de tentativas falhas desse e-mail.
     *
     * @param email e-mail que acabou de autenticar com sucesso
     */
    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    /**
     * Verifica se o e-mail informado está bloqueado por excesso de tentativas falhas.
     *
     * @param email e-mail a checar
     * @return {@code true} se o e-mail atingiu o limite de tentativas e ainda está dentro
     *         da janela de bloqueio
     */
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

    // E-mail em minúsculas para que "User@x.com" e "user@x.com" compartilhem o mesmo contador.
    private String normalize(String email) {
        return email == null ? "" : email.toLowerCase();
    }

    // Record interno: guarda quantas falhas seguidas houve e quando foi a última, para
    // calcular se a janela de bloqueio ainda está ativa.
    private record Attempt(int count, Instant lastFailureAt) {
    }
}

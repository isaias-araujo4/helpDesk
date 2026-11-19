package arajou.br.com.help_desk.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Informa ao auditing do Spring Data JPA (@CreatedBy/@LastModifiedBy) quem é o "usuário atual".
 * Registrado como bean para que {@code @EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")}
 * (ver {@link arajou.br.com.help_desk.HelpDeskApplication}) o encontre pelo nome.
 */
@Component // Registra a classe como bean gerenciado pelo Spring, injetável em outros componentes
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    /**
     * Retorna o identificador (e-mail) do usuário autenticado no momento, usado para
     * preencher automaticamente os campos createdBy/updatedBy das entidades.
     *
     * @return o e-mail do usuário logado, ou "system" quando não há usuário autenticado
     */
    @Override // Sobrescreve o método da interface AuditorAware<String>
    public Optional<String> getCurrentAuditor() {
        // Lê quem o Spring Security tem autenticado no momento para essa requisição/thread.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Sem autenticação (requisição anônima, ex: chamadas antes do login, ou contexto
        // de testes) cai no fallback "system" em vez de lançar erro ou deixar nulo.
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of("system");
        }

        // Usado como valor de createdBy/updatedBy nas entidades.
        return Optional.of(authentication.getName());
    }
}


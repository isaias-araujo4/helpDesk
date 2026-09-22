package arajou.br.com.help_desk.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Informa ao auditing do Spring Data JPA (@CreatedBy/@LastModifiedBy) quem é o "usuário atual".
// Registrado como bean para que @EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware") o encontre.
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // Lê quem o Spring Security tem autenticado no momento para essa requisição/thread.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Ainda não existe login configurado, então nunca há um usuário autenticado de verdade aqui -
        // por enquanto sempre cai no fallback "system". Quando a Security for configurada (JWT/login),
        // authentication vai conter o usuário logado real e esse mesmo código passa a devolver o
        // nome dele automaticamente, sem precisar mudar nada aqui.
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of("system");
        }

        // Usado como valor de createdBy/updatedBy nas entidades.
        return Optional.of(authentication.getName());
    }
}


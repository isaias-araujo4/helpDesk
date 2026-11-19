package arajou.br.com.help_desk.security;

import arajou.br.com.help_desk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementação do {@link UserDetailsService} do Spring Security: ensina o framework a
 * buscar um usuário a partir do e-mail (usado tanto no login quanto na validação do JWT
 * em cada requisição autenticada).
 */
@Service // Registra a classe como bean de camada de serviço, gerenciado pelo Spring
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class CustomUserDetailsService implements UserDetailsService {

    private  final UserRepository userRepository;

    /**
     * Busca o usuário pelo e-mail (usado como "username" neste sistema).
     * {@link arajou.br.com.help_desk.user.model.UserModel} implementa {@link UserDetails}
     * diretamente, então o próprio usuário retornado já serve como principal da autenticação.
     *
     * @param email e-mail do usuário a autenticar
     * @return os dados de autenticação/autorização do usuário
     * @throws UsernameNotFoundException se não existir usuário com esse e-mail
     */
    @Override // Sobrescreve o método da interface UserDetailsService
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}

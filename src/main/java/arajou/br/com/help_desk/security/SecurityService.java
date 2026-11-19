package arajou.br.com.help_desk.security;

import arajou.br.com.help_desk.user.model.UserModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Centraliza o acesso ao usuário autenticado na requisição atual. Sem isso, cada Service
 * que precisasse checar "o usuário logado é o dono do registro?" teria que repetir essa
 * leitura do {@link SecurityContextHolder} - aqui fica num único lugar.
 */
@Component // Registra a classe como bean gerenciado pelo Spring
public class SecurityService {

    /**
     * Retorna o usuário autenticado na requisição/thread atual.
     * <p>
     * O cast para {@link UserModel} é seguro porque o "principal" colocado no contexto de
     * segurança é sempre o {@code UserDetails} retornado por
     * {@link arajou.br.com.help_desk.security.CustomUserDetailsService} - que é o próprio
     * {@code UserModel} (ele implementa {@code UserDetails} diretamente).
     *
     * @return o usuário autenticado
     */
    public UserModel getAuthenticatedUser(){
        return (UserModel) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

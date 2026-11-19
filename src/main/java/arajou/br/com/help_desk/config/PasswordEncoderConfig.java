package arajou.br.com.help_desk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Disponibiliza o {@link PasswordEncoder} usado em toda a aplicação para criptografar
 * e validar senhas (cadastro de usuário, login, troca de senha).
 */
@Configuration // Marca a classe como fonte de definições de beans (métodos @Bean)
public class PasswordEncoderConfig {

    /**
     * Cria o encoder de senhas com o algoritmo BCrypt (hash unidirecional com salt
     * embutido, resistente a ataques de força bruta/rainbow table) - padrão de mercado
     * para armazenamento de senha no Spring Security.
     *
     * @return instância de {@link BCryptPasswordEncoder}
     */
    @Bean // Registra o retorno do método como um bean gerenciado pelo Spring
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}

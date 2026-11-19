package arajou.br.com.help_desk.config;

import arajou.br.com.help_desk.security.CustomUserDetailsService;
import arajou.br.com.help_desk.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração central do Spring Security: define quais rotas exigem autenticação,
 * como a autenticação é feita (via JWT, sem sessão), e os beans de CORS/AuthenticationProvider
 * usados pelo restante da aplicação.
 */
@Configuration // Marca a classe como fonte de definições de beans (métodos @Bean)
@EnableWebSecurity // Ativa a configuração de segurança web do Spring Security (filtros HTTP)
@EnableMethodSecurity // Habilita anotações de autorização em métodos, como @PreAuthorize nos controllers
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class SecurityConfiguration {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Injeta o valor da propriedade app.cors.allowed-origins (vinda do .env via
    // CORS_ALLOWED_ORIGINS) direto no campo, sem precisar de construtor pra isso.
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Monta a cadeia de filtros de segurança HTTP da aplicação: desliga CSRF (desnecessário
     * numa API stateless autenticada por token, não por cookie/sessão), ativa CORS, define
     * a política de sessão como stateless, declara quais rotas são públicas, e insere o
     * filtro de JWT antes do filtro padrão de login por usuário/senha do Spring Security.
     *
     * @param httpSecurity builder de configuração de segurança HTTP fornecido pelo Spring
     * @return a cadeia de filtros de segurança já construída
     * @throws Exception caso a configuração do builder falhe
     */
    @Bean // Registra o retorno do método como um bean gerenciado pelo Spring
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
                // CSRF protege contra ataques que abusam de sessão/cookie - não se aplica aqui,
                // já que a autenticação é via token JWT enviado manualmente em cada requisição.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // STATELESS: o Spring Security nunca cria nem usa HttpSession - cada requisição
                // precisa trazer o próprio token, nada fica guardado em memória no servidor.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    // Únicas rotas acessíveis sem autenticação: login, e a documentação OpenAPI/Swagger.
                    authorize.requestMatchers(
                            "/auth/login",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll();
                    // Qualquer outra rota exige um usuário autenticado (token JWT válido).
                    authorize.anyRequest().authenticated();
                })
                // Insere o filtro de validação do JWT antes do filtro padrão do Spring Security
                // que processaria login por formulário - assim o JWT é checado primeiro.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Define quais origens (domínios de frontend) podem chamar a API via navegador.
     * Lista de origens permitidas vem do .env (CORS_ALLOWED_ORIGINS) - nunca "*", porque a
     * API lida com dados sensíveis (usuários, chamados) e o token vai no header Authorization.
     *
     * @return a configuração de CORS aplicada a todas as rotas ("/**")
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Provedor de autenticação que busca o usuário via {@link CustomUserDetailsService}
     * (por e-mail) e confere a senha usando o {@link PasswordEncoder} (BCrypt).
     *
     * @return o provedor de autenticação usado pelo {@link AuthenticationManager}
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        // Construtor recebendo o UserDetailsService diretamente - necessário porque o
        // Spring Security do Boot 4 removeu o construtor sem argumentos desta classe.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Expõe o {@link AuthenticationManager} padrão do Spring Security como bean, para que
     * o {@code AuthController} possa injetá-lo e autenticar o login manualmente.
     *
     * @param config configuração de autenticação já montada pelo Spring a partir dos
     *               AuthenticationProvider disponíveis (aqui, o {@link #authenticationProvider()})
     * @return o gerenciador de autenticação da aplicação
     * @throws Exception caso o Spring não consiga montar o AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }
}

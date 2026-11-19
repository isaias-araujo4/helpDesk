package arajou.br.com.help_desk.security;

import arajou.br.com.help_desk.exception.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro HTTP executado uma vez por requisição: lê o header {@code Authorization: Bearer <token>},
 * valida o JWT e, se válido, autentica a requisição no {@link SecurityContextHolder} do
 * Spring Security - sem isso, nenhum endpoint protegido (@PreAuthorize) reconheceria o usuário.
 * <p>
 * Também é responsável por bloquear o acesso a qualquer rota (exceto a própria troca de
 * senha) enquanto o usuário estiver com a senha temporária.
 */
@Component // Registra a classe como bean gerenciado pelo Spring (necessário para ser injetada em SecurityConfiguration)
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class JwtAuthenticationFilter extends OncePerRequestFilter { // OncePerRequestFilter: garante que o filtro roda só uma vez por requisição, mesmo com forward/include internos

    private static final String CHANGE_PASSWORD_PATH = "/api/v1/auth/change-password";

    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    /**
     * Executado para cada requisição HTTP recebida. Extrai e valida o JWT (se houver),
     * autentica a requisição e, caso o usuário ainda precise trocar a senha, bloqueia
     * o acesso a qualquer rota que não seja a de troca de senha.
     *
     * @param request requisição HTTP recebida
     * @param response resposta HTTP a ser escrita (usada para retornar erro 403 quando bloqueado)
     * @param filterChain cadeia de filtros do Spring Security - precisa ser continuada
     *                    ({@code doFilter}) para a requisição seguir seu curso normal
     * @throws ServletException erro genérico de servlet
     * @throws IOException erro ao escrever a resposta de bloqueio
     */
    @Override // Sobrescreve o método abstrato de OncePerRequestFilter
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException{
        String authHeader = request.getHeader("Authorization");

        // Sem header Authorization, ou num formato diferente de "Bearer <token>": deixa a
        // requisição seguir sem autenticação (rotas públicas) ou ela será barrada mais
        // adiante pelo SecurityConfiguration (anyRequest().authenticated()).
        if (authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        // Remove o prefixo "Bearer " (7 caracteres) para extrair só o token em si.
        String token = authHeader.substring(7);
        String email = tokenService.extractEmail(token);

        // Só processa se ainda não houver autenticação registrada nessa requisição
        // (evita reprocessar à toa caso o filtro seja chamado mais de uma vez).
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (tokenService.isTokenValid(token, userDetails)){
                // Monta o objeto de autenticação do Spring Security a partir do usuário e
                // suas authorities (roles + permissões, ver UserModel.getAuthorities()) -
                // sem senha (null), já que o JWT em si já prova a identidade.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                // Anexa metadados da requisição (IP, sessionId) ao token de autenticação.
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // A partir daqui, @PreAuthorize e @AuthenticationPrincipal enxergam esse usuário
                // como autenticado para o restante do processamento desta requisição.
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Bloqueia qualquer endpoint (exceto o proprio change-password) enquanto o
                // usuario ainda estiver na senha temporaria gerada no cadastro.
                boolean mustChangePassword = tokenService.extractMustChangePassword(token);
                boolean isChangePasswordRequest = request.getRequestURI().equals(CHANGE_PASSWORD_PATH);

                if (mustChangePassword && !isChangePasswordRequest){
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    ErrorResponse errorResponse = new ErrorResponse(
                            HttpServletResponse.SC_FORBIDDEN,
                            "You must change your temporary password before continuing.",
                            List.of());
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

package arajou.br.com.help_desk.security;

import arajou.br.com.help_desk.user.model.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Gera e valida os tokens JWT usados para autenticar as requisições. O token carrega
 * o e-mail do usuário (subject), a role (só informativa) e se ele precisa trocar a senha.
 */
@Component // Registra a classe como bean gerenciado pelo Spring
public class TokenService {

    // Chave secreta usada para assinar/validar o token - vem do .env (JWT_SECRET), nunca
    // deve ser hardcoded nem reaproveitada entre projetos.
    @Value("${JWT_SECRET}")
    private String secret;

    // Tempo de vida do token em milissegundos - vem do .env (JWT_EXPIRATION).
    @Value("${JWT_EXPIRATION}")
    private Long expirationMillis;

    /**
     * Converte a chave secreta (string) numa {@link SecretKey} no formato exigido pela
     * biblioteca jjwt para assinatura/verificação HMAC.
     *
     * @return a chave usada para assinar e validar os tokens
     */
    private SecretKey getSigKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um novo JWT assinado para o usuário informado, válido por
     * {@link #expirationMillis} milissegundos a partir de agora.
     *
     * @param userModel usuário autenticado para quem o token será emitido
     * @return o token JWT compactado (formato {@code header.payload.signature})
     */
    public String generateToken(UserModel userModel){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        // getRole() pode ser null agora (usuário sem role, só com permissões isoladas),
        // por isso o null-check - essa claim é só informativa, não é usada pra autorizar nada.
        String roleName = userModel.getRole() != null ? userModel.getRole().getName() : null;

        return Jwts.builder()
                .subject(userModel.getEmail())
                .claim("role", roleName)
                .claim("mustChangePassword", userModel.isMustChangePassword())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigKey())
                .compact();
    }

    /**
     * Lê o e-mail (subject) contido no token.
     *
     * @param token JWT já assinado
     * @return o e-mail do usuário dono do token
     */
    public String extractEmail(String token){
        return parseClaims(token).getSubject();
    }

    /**
     * Confere se o token pertence ao usuário informado e ainda não expirou.
     *
     * @param token JWT a validar
     * @param userDetails usuário carregado do banco para comparação
     * @return {@code true} se o token é válido para esse usuário
     */
    public boolean isTokenValid(String token, UserDetails userDetails){
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token){
        return parseClaims(token).getExpiration().before(new Date());
    }

    /**
     * Valida a assinatura do token com a chave secreta e extrai suas claims (payload).
     * Lança exception automaticamente (biblioteca jjwt) se a assinatura não bater ou o
     * token estiver malformado.
     *
     * @param token JWT a decodificar/validar
     * @return as claims (dados) contidas no token
     */
    private Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Lê a flag que indica se o usuário ainda está na senha temporária e precisa trocá-la
     * antes de usar o restante da API (ver {@link arajou.br.com.help_desk.security.JwtAuthenticationFilter}).
     *
     * @param token JWT já assinado
     * @return {@code true} se o usuário ainda precisa trocar a senha
     */
    public boolean extractMustChangePassword(String token){
        return parseClaims(token).get("mustChangePassword", Boolean.class);
    }
}

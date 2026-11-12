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

@Component
public class TokenService {

    @Value("${JWT_SECRET}")
    private String secret;

    @Value("${JWT_EXPIRATION}")
    private Long expirationMillis;

    private SecretKey getSigKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

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

    public String extractEmail(String token){
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token){
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean extractMustChangePassword(String token){
        return parseClaims(token).get("mustChangePassword", Boolean.class);
    }
}

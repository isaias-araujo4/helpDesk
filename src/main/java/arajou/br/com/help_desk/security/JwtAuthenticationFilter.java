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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_PATH = "/auth/change-password";

    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException{
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String email = tokenService.extractEmail(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (tokenService.isTokenValid(token, userDetails)){
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
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

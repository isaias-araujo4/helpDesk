package arajou.br.com.help_desk.auth.controller;

import arajou.br.com.help_desk.auth.dto.ChangePasswordDTO;
import arajou.br.com.help_desk.auth.dto.LoginRequestDTO;
import arajou.br.com.help_desk.auth.dto.LoginResponseDTO;
import arajou.br.com.help_desk.exception.TooManyLoginAttemptsException;
import arajou.br.com.help_desk.security.LoginAttemptService;
import arajou.br.com.help_desk.security.TokenService;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de autenticação: login (emite o JWT) e troca de senha.
 * Único controller com rota pública ({@code /auth/login}) - todo o resto da API
 * exige um token válido (ver {@link arajou.br.com.help_desk.config.SecurityConfiguration}).
 */
@RestController // Combina @Controller + @ResponseBody: cada método devolve diretamente o corpo da resposta (JSON), não uma view
@RequestMapping("auth") // Prefixo comum de rota para todos os endpoints desta classe
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    /**
     * Autentica o usuário por e-mail/senha e devolve um JWT. Bloqueado temporariamente
     * (429) após várias tentativas erradas seguidas para o mesmo e-mail - ver
     * {@link LoginAttemptService}.
     *
     * @param loginRequestDTO e-mail e senha informados
     * @return 200 com o token e a flag mustChangePassword
     */
    @PostMapping("login") // Mapeia POST /auth/login para este método
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequestDTO){
        if (loginAttemptService.isBlocked(loginRequestDTO.email())){
            throw new TooManyLoginAttemptsException(
                    "Too many failed login attempts. Try again in a few minutes.");
        }

        try {
            // Delega a verificação de e-mail/senha ao AuthenticationManager, que por sua vez
            // usa o DaoAuthenticationProvider (ver SecurityConfiguration) para buscar o
            // usuário e comparar a senha com BCrypt. Lança BadCredentialsException se falhar.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.email(), loginRequestDTO.password()));
            loginAttemptService.recordSuccess(loginRequestDTO.email());

            // O "principal" devolvido é o próprio UserModel (ele implementa UserDetails).
            UserModel userModel = (UserModel) authentication.getPrincipal();
            String token = tokenService.generateToken(userModel);

            return ResponseEntity.ok(new LoginResponseDTO(token, userModel.isMustChangePassword()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(loginRequestDTO.email());
            // Repassa a exception original - quem trata a resposta HTTP é o GlobalExceptionHandler.
            throw e;
        }
    }

    /**
     * Troca a senha do usuário autenticado (sua própria senha - não dá pra trocar a de
     * outro usuário por aqui). Usado tanto para sair da senha temporária no primeiro
     * login quanto para trocar a senha normalmente depois.
     *
     * @param userModel usuário autenticado, injetado automaticamente a partir do JWT
     * @param changePasswordDTO nova senha desejada
     * @return 204 (sem corpo) em caso de sucesso
     */
    @PutMapping("change-password") // Mapeia PUT /auth/change-password para este método
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserModel userModel, // Injeta o usuário autenticado atual (vindo do SecurityContextHolder)
            @RequestBody @Valid ChangePasswordDTO changePasswordDTO){
        userService.changePassword(userModel, changePasswordDTO.newPassword());
        return ResponseEntity.noContent().build();
    }
}

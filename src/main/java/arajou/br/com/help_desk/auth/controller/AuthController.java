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

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequestDTO){
        if (loginAttemptService.isBlocked(loginRequestDTO.email())){
            throw new TooManyLoginAttemptsException(
                    "Too many failed login attempts. Try again in a few minutes.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.email(), loginRequestDTO.password()));
            loginAttemptService.recordSuccess(loginRequestDTO.email());

            UserModel userModel = (UserModel) authentication.getPrincipal();
            String token = tokenService.generateToken(userModel);

            return ResponseEntity.ok(new LoginResponseDTO(token, userModel.isMustChangePassword()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(loginRequestDTO.email());
            throw e;
        }
    }

    @PutMapping("change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserModel userModel, @RequestBody @Valid ChangePasswordDTO changePasswordDTO){
        userService.changePassword(userModel, changePasswordDTO.newPassword());
        return ResponseEntity.noContent().build();
    }
}

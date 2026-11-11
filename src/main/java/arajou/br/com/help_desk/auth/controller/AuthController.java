package arajou.br.com.help_desk.auth.controller;

import arajou.br.com.help_desk.auth.dto.LoginRequestDTO;
import arajou.br.com.help_desk.auth.dto.LoginResponseDTO;
import arajou.br.com.help_desk.security.TokenService;
import arajou.br.com.help_desk.user.model.UserModel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping("login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequestDTO){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDTO.email(), loginRequestDTO.password()));
        UserModel userModel = (UserModel) authentication.getPrincipal();
        String token = tokenService.generateToken(userModel);

        return ResponseEntity.ok(new LoginResponseDTO(token, userModel.isMustChangePassword()));
    }
}

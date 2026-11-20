package arajou.br.com.help_desk.auth.controller;

import arajou.br.com.help_desk.AbstractIntegrationTest;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Testes de integração do fluxo de login: credenciais corretas/erradas e o bloqueio por
 * força bruta ({@code LoginAttemptService}).
 */
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        UserModel user = createActiveUser("CUSTOMER", Department.IT);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), FIXTURE_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        UserModel user = createActiveUser("CUSTOMER", Department.IT);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_after5FailedAttempts_blocksEvenWithCorrectPassword() throws Exception {
        UserModel user = createActiveUser("CUSTOMER", Department.IT);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(loginBody(user.getEmail(), "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }

        // 6ª tentativa, agora com a senha CERTA - ainda assim bloqueado por 429.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), FIXTURE_PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }
}

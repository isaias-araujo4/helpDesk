package arajou.br.com.help_desk.user.controller;

import arajou.br.com.help_desk.AbstractIntegrationTest;
import arajou.br.com.help_desk.role.model.Role;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração de gestão de usuários: criação, conflito de duplicidade, 404
 * consistente, autorização por permissão, e o ciclo completo de desativar/reativar.
 */
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private Long managerRoleId() {
        return roleRepository.findAll().stream()
                .filter(r -> r.getName().equals("MANAGER"))
                .findFirst()
                .map(Role::getId)
                .orElseThrow();
    }

    @Test
    void create_asManager_returnsCreated() throws Exception {
        UserModel manager = createActiveUser("MANAGER", Department.IT);
        String token = loginAndGetToken(manager.getEmail(), FIXTURE_PASSWORD);

        String body = """
                {"firstName":"Novo","lastName":"Usuario","department":"HR","email":"novo.usuario@helpdesk.com","roleId":%d}
                """.formatted(managerRoleId());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void create_withoutManageUsersPermission_returnsForbidden() throws Exception {
        UserModel customer = createActiveUser("CUSTOMER", Department.IT);
        String token = loginAndGetToken(customer.getEmail(), FIXTURE_PASSWORD);

        String body = """
                {"firstName":"Novo","lastName":"Usuario","department":"HR","email":"outro.usuario@helpdesk.com","roleId":%d}
                """.formatted(managerRoleId());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withDuplicateEmail_returnsConflict() throws Exception {
        UserModel manager = createActiveUser("MANAGER", Department.IT);
        UserModel existing = createActiveUser("CUSTOMER", Department.HR);
        String token = loginAndGetToken(manager.getEmail(), FIXTURE_PASSWORD);

        String body = """
                {"firstName":"Duplicado","lastName":"Usuario","department":"MARKETING","email":"%s","roleId":%d}
                """.formatted(existing.getEmail(), managerRoleId());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void findById_nonExistentUser_returnsNotFoundWithConsistentBody() throws Exception {
        UserModel manager = createActiveUser("MANAGER", Department.IT);
        String token = loginAndGetToken(manager.getEmail(), FIXTURE_PASSWORD);

        mockMvc.perform(get("/api/v1/users/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found."));
    }

    @Test
    void deactivateThenReactivate_fullCycle() throws Exception {
        UserModel manager = createActiveUser("MANAGER", Department.IT);
        UserModel target = createActiveUser("CUSTOMER", Department.HR);
        String token = loginAndGetToken(manager.getEmail(), FIXTURE_PASSWORD);

        // Desativa.
        mockMvc.perform(delete("/api/v1/users/{id}", target.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", target.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.active").value(false));

        // Reativar quem já desativamos: funciona.
        mockMvc.perform(post("/api/v1/users/{id}/reactivate", target.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", target.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.active").value(true));

        // Reativar quem já está ativo: 409.
        mockMvc.perform(post("/api/v1/users/{id}/reactivate", target.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }
}

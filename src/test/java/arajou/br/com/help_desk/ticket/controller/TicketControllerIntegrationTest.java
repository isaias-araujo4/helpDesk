package arajou.br.com.help_desk.ticket.controller;

import arajou.br.com.help_desk.AbstractIntegrationTest;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do ciclo de vida do chamado: fluxo feliz completo (criar → assumir
 * → resolver → fechar) e as duas camadas de autorização (permissão e dono/técnico).
 */
class TicketControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String TICKET_BODY = """
            {"title":"Impressora não liga","description":"A impressora do setor parou de responder.","theme":"HARDWARE"}
            """;

    private Long createTicketAndGetId(String customerToken) throws Exception {
        String location = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(TICKET_BODY))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void fullLifecycle_createAssumeResolveClose() throws Exception {
        UserModel customer = createActiveUser("CUSTOMER", Department.IT);
        UserModel technician = createActiveUser("TECHNICIAN", Department.IT);
        String customerToken = loginAndGetToken(customer.getEmail(), FIXTURE_PASSWORD);
        String technicianToken = loginAndGetToken(technician.getEmail(), FIXTURE_PASSWORD);

        Long ticketId = createTicketAndGetId(customerToken);

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/v1/tickets/{id}/assume", ticketId)
                        .header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(post("/api/v1/tickets/{id}/close", ticketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void assume_byCustomer_returnsForbidden() throws Exception {
        UserModel customer = createActiveUser("CUSTOMER", Department.IT);
        String customerToken = loginAndGetToken(customer.getEmail(), FIXTURE_PASSWORD);

        Long ticketId = createTicketAndGetId(customerToken);

        // Cliente não tem a permissão ASSUME_TICKET - bloqueado antes mesmo de checar dono.
        mockMvc.perform(post("/api/v1/tickets/{id}/assume", ticketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolve_byUnassignedTechnician_returnsConflict() throws Exception {
        UserModel customer = createActiveUser("CUSTOMER", Department.IT);
        UserModel assignedTechnician = createActiveUser("TECHNICIAN", Department.IT);
        UserModel anotherTechnician = createActiveUser("TECHNICIAN", Department.IT);
        String customerToken = loginAndGetToken(customer.getEmail(), FIXTURE_PASSWORD);
        String assignedTechnicianToken = loginAndGetToken(assignedTechnician.getEmail(), FIXTURE_PASSWORD);
        String anotherTechnicianToken = loginAndGetToken(anotherTechnician.getEmail(), FIXTURE_PASSWORD);

        Long ticketId = createTicketAndGetId(customerToken);

        mockMvc.perform(post("/api/v1/tickets/{id}/assume", ticketId)
                        .header("Authorization", "Bearer " + assignedTechnicianToken))
                .andExpect(status().isNoContent());

        // Tem a permissão RESOLVE_TICKET, mas não é O técnico responsável por ESTE chamado.
        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .header("Authorization", "Bearer " + anotherTechnicianToken))
                .andExpect(status().isConflict());
    }

    @Test
    void findById_byUnrelatedCustomer_returnsConflict() throws Exception {
        UserModel owner = createActiveUser("CUSTOMER", Department.IT);
        UserModel stranger = createActiveUser("CUSTOMER", Department.HR);
        String ownerToken = loginAndGetToken(owner.getEmail(), FIXTURE_PASSWORD);
        String strangerToken = loginAndGetToken(stranger.getEmail(), FIXTURE_PASSWORD);

        Long ticketId = createTicketAndGetId(ownerToken);

        // Tem a permissão VIEW_OWN_TICKETS, mas o chamado não é dele - TicketValidator.validateView
        // é quem barra isso, não o @PreAuthorize.
        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isConflict());
    }
}

package arajou.br.com.help_desk;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: confirma que o contexto do Spring sobe por completo (todas as beans, o
 * Flyway, o DataSeeder) sem lançar exception - gerado pelo Spring Initializr, mantido
 * porque já herda o Testcontainers via {@link AbstractIntegrationTest}.
 */
class HelpDeskApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

}

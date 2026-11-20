package arajou.br.com.help_desk;

import arajou.br.com.help_desk.role.model.Role;
import arajou.br.com.help_desk.role.repository.RoleRepository;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para os testes de integração: sobe o contexto completo do Spring (com
 * {@link MockMvc} pra simular requisições HTTP de verdade) contra um MySQL real,
 * rodando num container Docker descartável via Testcontainers - as migrations do Flyway
 * e o {@code DataSeeder} rodam normalmente contra ele, igual rodariam em produção.
 * <p>
 * O container é {@code static} e iniciado manualmente num bloco {@code static} (sem a
 * anotação {@code @Container}) de propósito: com {@code @Container}, o Testcontainers
 * para o container automaticamente ao final de CADA classe de teste - mesmo sendo um
 * campo estático herdado, ele não sobrevive pra próxima classe. Iniciando manualmente,
 * ele só é derrubado pelo "Ryuk" (reaper do Testcontainers) quando a JVM do Maven termina,
 * virando de fato um "singleton container" reaproveitado por toda a suíte de testes.
 * <p>
 * As variáveis exigidas pela aplicação (ver {@code DotenvEnvironmentPostProcessor} e
 * {@code application.properties}) não vêm de um `.env` aqui - são injetadas diretamente
 * via {@link DynamicPropertySource}, com valores reais pro banco (vindos do container) e
 * valores "dummy" pro resto (e-mail/JWT/CORS), suficientes pra aplicação subir sem
 * depender de nenhuma credencial real durante os testes.
 * <p>
 * {@code webEnvironment} fica no padrão (MOCK, sem porta real) de propósito: {@link MockMvc}
 * não precisa de um servidor de verdade, e isso permite o {@code @Transactional} abaixo
 * desfazer sozinho, ao final de cada teste, qualquer dado que ele tenha gravado no banco -
 * com uma porta real (RANDOM_PORT), a requisição rodaria numa thread separada da do teste,
 * e o rollback automático não funcionaria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("help_desk");

    static {
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("DATASOURCE_USERNAME", MYSQL_CONTAINER::getUsername);
        registry.add("DATASOURCE_PASSWORD", MYSQL_CONTAINER::getPassword);

        // "localhost:2525" não tem nada escutando de propósito - o envio de e-mail falha
        // rápido (em vez de tentar de verdade contra o Mailtrap), e o UserService já trata
        // essa falha sem derrubar o cadastro (ver UserService.save).
        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_PORT", () -> "2525");
        registry.add("MAIL_USERNAME", () -> "test_dummy_user");
        registry.add("MAIL_PASSWORD", () -> "test_dummy_password");
        registry.add("JWT_SECRET", () -> "test_dummy_secret_at_least_32_characters_long");
        registry.add("JWT_EXPIRATION", () -> "3600000");
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:3000");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** Senha em texto puro de todo usuário criado via {@link #createActiveUser}. */
    protected static final String FIXTURE_PASSWORD = "Test@12345";

    private static final AtomicLong FIXTURE_COUNTER = new AtomicLong();

    /**
     * Cria e salva diretamente no banco (sem passar pela API) um usuário já ATIVO, com
     * senha definitiva conhecida ({@link #FIXTURE_PASSWORD}) e sem exigir troca de senha -
     * pronto pra logar e usar nos testes, sem o fluxo de senha temporária no meio do
     * caminho. Nome/sobrenome/e-mail são gerados com um contador, pra nunca colidir com a
     * constraint de unicidade mesmo chamando este método várias vezes no mesmo teste.
     *
     * @param roleName nome da role já seedada pelo {@code DataSeeder} (CUSTOMER/TECHNICIAN/MANAGER)
     * @param department setor do usuário
     * @return o usuário já salvo, pronto pra logar com {@link #FIXTURE_PASSWORD}
     */
    protected UserModel createActiveUser(String roleName, Department department) {
        long n = FIXTURE_COUNTER.incrementAndGet();
        Role role = roleRepository.findAll().stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));

        UserModel user = new UserModel();
        user.setFirstName("Test" + n);
        user.setLastName("User" + n);
        user.setDepartment(department);
        user.setEmail("test" + n + "@helpdesk.com");
        user.setPassword(passwordEncoder.encode(FIXTURE_PASSWORD));
        user.setMustChangePassword(false);
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    /**
     * Loga via {@code POST /api/v1/auth/login} e devolve o JWT da resposta - usado pelos
     * testes pra autenticar as próximas requisições sem repetir esse boilerplate.
     *
     * @param email e-mail do usuário (normalmente criado com {@link #createActiveUser})
     * @param password senha em texto puro (geralmente {@link #FIXTURE_PASSWORD})
     * @return o JWT, pronto pra usar no header {@code Authorization: Bearer <token>}
     */
    protected String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Autowired
    protected ObjectMapper objectMapper;
}

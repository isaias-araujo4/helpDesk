package arajou.br.com.help_desk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura o esquema de segurança do Swagger UI: adiciona o botão "Authorize", onde se
 * cola um JWT (sem o prefixo "Bearer ", o springdoc já adiciona) e ele passa a ser enviado
 * automaticamente em toda chamada testada na tela, nos endpoints que exigem autenticação.
 * <p>
 * O springdoc-openapi escaneia os {@code @RestController}/DTOs automaticamente para gerar
 * a especificação OpenAPI - esta classe só adiciona metadados (título, descrição) e o
 * esquema de autenticação, que não dá pra inferir sozinho a partir do código.
 */
@Configuration // Marca a classe como fonte de definições de beans (métodos @Bean)
public class OpenApiConfig {

    // Nome interno do esquema de segurança, referenciado tanto na declaração quanto no uso.
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Monta o objeto de configuração da especificação OpenAPI: título/descrição da API
     * exibidos no Swagger UI, e o esquema de autenticação Bearer (JWT) usado pelo botão
     * "Authorize".
     *
     * @return a configuração de OpenAPI lida pelo springdoc ao gerar a documentação
     */
    @Bean // Registra o retorno do método como um bean gerenciado pelo Spring
    public OpenAPI helpDeskOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("Help Desk API")
                        .description("API REST para gerenciamento de chamados de suporte técnico, "
                                + "com RBAC dinâmico (roles + permissões), autenticação JWT e fluxo "
                                + "de senha temporária no cadastro de usuários.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

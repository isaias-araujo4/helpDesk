package arajou.br.com.help_desk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Aplica o prefixo {@code /api/v1} em todos os controllers da aplicação, sem precisar
 * repetir isso no {@code @RequestMapping} de cada um. Versionar a API assim permite, no
 * futuro, introduzir um {@code /api/v2} com mudanças incompatíveis sem quebrar quem ainda
 * consome o v1.
 * <p>
 * O filtro é por pacote (só classes dentro de {@code arajou.br.com.help_desk}), de propósito:
 * se fosse só "tem @RestController", o prefixo vazaria pros endpoints internos de
 * bibliotecas de terceiros (ex: o springdoc usa @RestController por baixo dos panos para
 * servir {@code /v3/api-docs}), quebrando o Swagger.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer){
        configurer.addPathPrefix("/api/v1", clazz ->
                clazz.getPackageName().startsWith("arajou.br.com.help_desk"));
    }
}

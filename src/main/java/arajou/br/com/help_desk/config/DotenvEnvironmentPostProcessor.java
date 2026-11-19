package arajou.br.com.help_desk.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carrega o arquivo .env da raiz do projeto e registra seus valores como uma
 * PropertySource do Spring, ANTES de qualquer outra configuracao ser processada -
 * é por isso que precisa ser um {@link EnvironmentPostProcessor}, nao um @Component comum
 * (esta classe roda cedo demais no startup pra o container de beans existir ainda).
 * <p>
 * Escrito na mao porque a lib "spring-dotenv" nao estava funcionando com o Spring
 * Boot 4.1.1 (os valores do .env nunca chegavam no Environment, causando erro de
 * "Could not resolve placeholder").
 * <p>
 * Registrado via {@code META-INF/spring.factories} (não há anotação equivalente a
 * @Component para esse tipo de extensão - o Spring Boot descobre a classe por esse
 * arquivo de configuração, lido antes mesmo do container de beans existir).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /**
     * Lê o arquivo .env (se existir) e registra cada linha KEY=VALUE como uma propriedade
     * do Environment do Spring, para que {@code ${VAR}} nas properties/application.properties
     * seja resolvido a partir dele.
     *
     * @param environment ambiente do Spring onde a nova PropertySource será registrada
     * @param application referência à aplicação Spring Boot sendo iniciada (não usada aqui)
     */
    @Override // Sobrescreve o método da interface EnvironmentPostProcessor
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = Path.of(".env");

        if (!Files.exists(envFile)) {
            return;
        }

        Map<String, Object> values = new LinkedHashMap<>();

        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separatorIndex = trimmed.indexOf('=');

                if (separatorIndex == -1) {
                    continue;
                }

                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env file", e);
        }

        // addLast(): adiciona com a MENOR prioridade entre as fontes de propriedade -
        // uma variável de ambiente real do sistema operacional, se existir com o mesmo
        // nome, sempre vence o valor vindo do .env.
        environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
    }
}

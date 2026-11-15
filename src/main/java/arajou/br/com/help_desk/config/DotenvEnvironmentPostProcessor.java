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

// Carrega o arquivo .env da raiz do projeto e registra seus valores como uma
// PropertySource do Spring, ANTES de qualquer outra configuracao ser processada -
// é por isso que precisa ser um EnvironmentPostProcessor, nao um @Component comum
// (esta classe roda cedo demais no startup pra o container de beans existir ainda).
//
// Escrito na mao porque a lib "spring-dotenv" nao estava funcionando com o Spring
// Boot 4.1.1 (os valores do .env nunca chegavam no Environment, causando erro de
// "Could not resolve placeholder"). addLast(): valores reais de variavel de ambiente
// do sistema operacional continuam tendo prioridade sobre o .env, se existirem.
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
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

        environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
    }
}

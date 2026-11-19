package arajou.br.com.help_desk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Classe principal da aplicação - ponto de entrada que inicializa o Spring Boot.
 */
// Habilita a auditoria automática do Spring Data JPA (@CreatedBy/@CreatedDate/
// @LastModifiedBy/@LastModifiedDate nas entidades), usando o bean "springSecurityAuditorAware"
// (SpringSecurityAuditorAware) para descobrir quem é o usuário autenticado no momento.
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
// Anotação de conveniência que combina @Configuration, @EnableAutoConfiguration e
// @ComponentScan - faz o Spring configurar automaticamente a aplicação e escanear
// todos os componentes (@Component, @Service, @Repository, @RestController, etc.)
// a partir deste pacote para baixo.
@SpringBootApplication
public class HelpDeskApplication {

	/**
	 * Sobe o contexto do Spring e inicia o servidor embutido (Tomcat).
	 *
	 * @param args argumentos de linha de comando (não usados por este projeto)
	 */
	public static void main(String[] args) {
		SpringApplication.run(HelpDeskApplication.class, args);
	}

}

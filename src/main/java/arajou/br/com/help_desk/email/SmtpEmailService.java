package arajou.br.com.help_desk.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Implementação de {@link EmailService} que envia de verdade via SMTP, usando o
 * {@link JavaMailSender} que o Spring Boot autoconfigura a partir das propriedades
 * {@code spring.mail.*} (Mailtrap, por enquanto).
 */
@Component // Registra a classe como bean gerenciado pelo Spring - é essa implementação que será injetada onde EmailService for pedido
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    /**
     * Monta e envia um e-mail de texto simples com a senha temporária.
     *
     * @param to e-mail do destinatário
     * @param temporaryPassword senha temporária em texto puro
     */
    @Override // Sobrescreve o método da interface EmailService
    public void sendTemporaryPassword(String to, String temporaryPassword){
        SimpleMailMessage message = new SimpleMailMessage();
        // Destinatário: o e-mail cadastrado do usuário recém-criado.
        message.setTo(to);
        message.setSubject("Sua senha temporária - Help Desk");
        message.setText("Sua senha temporária de acesso é: " + temporaryPassword
                + "\n\nVocê será solicitado a criar uma nova senha no primeiro login.");
        // Dispara o envio de fato através do servidor SMTP configurado.
        mailSender.send(message);
    }
}

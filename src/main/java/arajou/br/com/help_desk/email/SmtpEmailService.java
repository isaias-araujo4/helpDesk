package arajou.br.com.help_desk.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// Implementação de EmailService que envia de verdade via SMTP, usando o JavaMailSender
// que o Spring Boot autoconfigura a partir das propriedades spring.mail.* (Mailtrap, por enquanto).
@Component
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
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

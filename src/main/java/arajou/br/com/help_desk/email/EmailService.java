package arajou.br.com.help_desk.email;

// Abstrai o "como" o e-mail é enviado (hoje é SMTP via SmtpEmailService/Mailtrap,
// mas poderia virar um provedor de API tipo SendGrid/SES no futuro) do "quando"/"por quê"
// ele é enviado (o UserService só conhece essa interface, nunca a implementação concreta).
public interface EmailService {

    // Envia a senha temporária gerada no cadastro para o e-mail do usuário recém-criado.
    void sendTemporaryPassword(String to, String temporaryPassword);
}

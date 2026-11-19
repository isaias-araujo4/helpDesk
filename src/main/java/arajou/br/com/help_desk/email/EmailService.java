package arajou.br.com.help_desk.email;

/**
 * Abstrai o "como" o e-mail é enviado (hoje é SMTP via {@link SmtpEmailService}/Mailtrap,
 * mas poderia virar um provedor de API tipo SendGrid/SES no futuro) do "quando"/"por quê"
 * ele é enviado (o {@code UserService} só conhece essa interface, nunca a implementação concreta).
 */
public interface EmailService {

    /**
     * Envia a senha temporária gerada no cadastro para o e-mail do usuário recém-criado.
     *
     * @param to e-mail do destinatário
     * @param temporaryPassword senha temporária em texto puro (só existe assim neste
     *                          momento, antes de ser substituída pela senha definitiva)
     */
    void sendTemporaryPassword(String to, String temporaryPassword);
}

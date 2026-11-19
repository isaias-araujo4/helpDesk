package arajou.br.com.help_desk.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Gera a senha temporária enviada por e-mail quando um MANAGER cadastra um novo usuário.
 * O usuário loga com essa senha e depois é obrigado a trocar (ver
 * {@link arajou.br.com.help_desk.user.model.UserModel#isMustChangePassword()}).
 */
@Component // Registra a classe como bean gerenciado pelo Spring, injetável em outros componentes
public class TemporaryPasswordGenerator {

    // Conjunto de caracteres permitidos na senha gerada (letras maiúsculas/minúsculas, números e símbolos).
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";

    private static final int LENGTH = 12;

    // SecureRandom (não Random comum) porque isso gera uma senha - precisa ser
    // criptograficamente imprevisível, senão vira um ponto fraco de segurança.
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Sorteia uma nova senha temporária de {@value #LENGTH} caracteres a partir do
     * conjunto {@link #CHARACTERS}.
     *
     * @return a senha gerada, em texto puro (quem chama é responsável por criptografá-la
     *         antes de persistir)
     */
    public String generate(){
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++){
            // Sorteia um caractere aleatório do conjunto permitido, um por posição.
            password.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }

        return password.toString();
    }
}

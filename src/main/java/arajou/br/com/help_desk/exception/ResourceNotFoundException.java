package arajou.br.com.help_desk.exception;

/**
 * Lançada quando um recurso buscado por id (usuário, chamado, etc.) não existe no banco.
 * Tratada por {@link GlobalExceptionHandler}, que a converte numa resposta HTTP 404 no
 * mesmo formato {@link ErrorResponse} usado por todos os outros erros da API - antes disso,
 * alguns endpoints devolviam 404 com corpo vazio, inconsistente com o resto da aplicação.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message mensagem descrevendo qual recurso não foi encontrado
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

package arajou.br.com.help_desk.exception;

/**
 * Lançada quando uma operação é tecnicamente possível (o registro existe, os dados são
 * válidos), mas a REGRA DE NEGÓCIO não permite naquele contexto - ex: tentar pular uma
 * etapa do fluxo do chamado, ou um usuário tentar agir sobre um chamado que não é dele.
 * Diferente de {@link DuplicatedRecordException} (que é sobre dados duplicados), essa é
 * sobre "você não pode fazer isso agora/aqui".
 * <p>
 * Tratada por {@link GlobalExceptionHandler}, que a converte numa resposta HTTP 409.
 */
public class OperationNotAllowedException extends RuntimeException {

    /**
     * @param message mensagem explicando qual regra de negócio foi violada
     */
    public OperationNotAllowedException(String message) {
        super(message);
    }
}

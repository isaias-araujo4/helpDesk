package arajou.br.com.help_desk.exception;

// Lançada quando uma operação é tecnicamente possível (o registro existe, os dados são
// válidos), mas a REGRA DE NEGÓCIO não permite naquele contexto - ex: tentar pular uma
// etapa do fluxo do chamado, ou um usuário tentar agir sobre um chamado que não é dele.
// Diferente de DuplicatedRecordException (que é sobre dados duplicados), essa é sobre
// "você não pode fazer isso agora/aqui".
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}

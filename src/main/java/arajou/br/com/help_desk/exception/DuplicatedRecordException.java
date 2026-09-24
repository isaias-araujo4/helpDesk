package arajou.br.com.help_desk.exception;

// Exception de domínio lançada quando se tenta cadastrar/atualizar um registro
// que já existe (ex: e-mail já usado por outro usuário). Precisa ser uma "class"
// (não "record") porque toda exception herda de RuntimeException, e record não
// pode estender outra classe.
public class DuplicatedRecordException extends RuntimeException {

    public DuplicatedRecordException(String message) {
        // repassa a mensagem para a RuntimeException, disponível depois via getMessage()
        super(message);
    }
}

package arajou.br.com.help_desk.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Corpo padrão de resposta de erro da API - todo {@code @ExceptionHandler} do
 * {@link GlobalExceptionHandler} devolve um objeto desse tipo, pra manter o formato de
 * erro consistente em toda a API.
 *
 * @param status código HTTP do erro (duplicado no corpo além do header, por conveniência do cliente)
 * @param message mensagem geral descrevendo o erro
 * @param errors lista de erros por campo (preenchida só em erros de validação; vazia nos demais)
 */
public record ErrorResponse(int status, String message, List<FieldError> errors) {

    /**
     * Atalho pra erros genéricos sem campo específico associado (400 Bad Request).
     *
     * @param message mensagem do erro
     * @return o ErrorResponse já montado com status 400
     */
    public static ErrorResponse standardResponse(String message){
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, List.of());
    }

    /**
     * Atalho pra erros de conflito, ex: registro duplicado (409 Conflict).
     *
     * @param message mensagem do erro
     * @return o ErrorResponse já montado com status 409
     */
    public static ErrorResponse conflict(String message){
        return new ErrorResponse(HttpStatus.CONFLICT.value(), message, List.of());
    }
}

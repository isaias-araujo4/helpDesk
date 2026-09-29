package arajou.br.com.help_desk.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

// @RestControllerAdvice é o que faz essa classe interceptar
// exceptions lançadas por QUALQUER controller da aplicação, sem precisar de try/catch
// espalhado em cada endpoint. Cada @ExceptionHandler abaixo trata um tipo de exception
// e devolve um ErrorResponse com o status HTTP definido em @ResponseStatus.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Disparado automaticamente quando um @Valid falha (Bean Validation dos DTOs).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        log.error("Validation error: {}", e.getMessage());
        // Converte a lista de erros de campo do Spring (FieldError próprio deles)
        // no nosso FieldError, pra manter o formato de resposta consistente.
        List<FieldError> errors= e.getFieldErrors()
                .stream()
                .map(fieldError -> new FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());
        return new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Validation error.", errors);
    }

    // Disparado pelo UserValidator quando já existe um registro com e-mail
    // ou nome+sobrenome+setor duplicado.
    @ExceptionHandler(DuplicatedRecordException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicatedRecordException(DuplicatedRecordException e){
        return ErrorResponse.conflict(e.getMessage());
    }

    // Rede de segurança: qualquer RuntimeException não tratada por um handler mais
    // específico acima cai aqui, pra nunca vazar stack trace/erro cru pro cliente da API.
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnhandledErrors(RuntimeException e){
        log.error("Unexpected error", e);
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please contact the administrator.",
                List.of());
    }
}

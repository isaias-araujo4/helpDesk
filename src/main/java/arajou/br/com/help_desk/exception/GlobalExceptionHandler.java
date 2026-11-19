package arajou.br.com.help_desk.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException(BadCredentialsException e){
        return new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password.", List.of());
    }

    // Disparado pelo Spring Security quando um @PreAuthorize nega acesso (usuário autenticado,
    // mas sem a permissão exigida). Sem esse handler específico, caía no catch-all de
    // RuntimeException abaixo e virava 500 em vez do 403 correto.
    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationDeniedException(AuthorizationDeniedException e){
        return new ErrorResponse(HttpStatus.FORBIDDEN.value(), "You do not have permission to perform this action.", List.of());
    }

    // Disparado pelo TicketValidator quando a ação viola uma regra de negócio: pular uma
    // etapa do fluxo do chamado, ou um usuário tentar agir sobre um chamado que não é dele.
    // 409 (conflito) porque a requisição em si é válida, só conflita com o estado atual
    // do recurso ou com quem está autenticado.
    @ExceptionHandler(OperationNotAllowedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOperationNotAllowedException(OperationNotAllowedException e){
        return ErrorResponse.conflict(e.getMessage());
    }

    // Disparado pelo LoginAttemptService quando um e-mail acumulou tentativas de login
    // erradas demais - 429 (Too Many Requests) é o status HTTP padrão pra esse cenário.
    @ExceptionHandler(TooManyLoginAttemptsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleTooManyLoginAttemptsException(TooManyLoginAttemptsException e){
        return new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), e.getMessage(), List.of());
    }
}

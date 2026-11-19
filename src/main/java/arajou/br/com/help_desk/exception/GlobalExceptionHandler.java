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

/**
 * Ponto único de tratamento de exceptions de toda a API. Cada método abaixo trata um tipo
 * de exception específico e devolve um {@link ErrorResponse} com o status HTTP definido
 * em {@code @ResponseStatus}, mantendo o formato de erro consistente em toda a aplicação.
 */
@RestControllerAdvice // Faz essa classe interceptar exceptions lançadas por QUALQUER @RestController da aplicação, sem precisar de try/catch espalhado em cada endpoint
@Slf4j // Lombok: gera o campo "log" (logger) para a classe
public class GlobalExceptionHandler {

    /**
     * Disparado automaticamente quando um {@code @Valid} falha (Bean Validation dos DTOs
     * de entrada, ex: campo obrigatório ausente ou formato de e-mail inválido).
     *
     * @param e exception lançada pelo Spring com os erros de validação
     * @return resposta 422 com um FieldError para cada campo inválido
     */
    @ExceptionHandler(MethodArgumentNotValidException.class) // Registra este método como o tratador desse tipo de exception
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY) // Status HTTP devolvido quando esse handler é acionado
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

    /**
     * Disparado pelo {@code UserValidator} quando já existe um registro com e-mail
     * ou nome+sobrenome+setor duplicado.
     *
     * @param e exception com a mensagem descrevendo a duplicidade
     * @return resposta 409
     */
    @ExceptionHandler(DuplicatedRecordException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicatedRecordException(DuplicatedRecordException e){
        return ErrorResponse.conflict(e.getMessage());
    }

    /**
     * Rede de segurança: qualquer {@link RuntimeException} não tratada por um handler mais
     * específico acima cai aqui, pra nunca vazar stack trace/erro cru pro cliente da API.
     * Como é genérico demais pra ter uma mensagem útil pro cliente, loga o erro completo
     * no servidor e devolve uma mensagem só informativa.
     *
     * @param e qualquer exception não tratada especificamente
     * @return resposta 500 genérica
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnhandledErrors(RuntimeException e){
        log.error("Unexpected error", e);
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please contact the administrator.",
                List.of());
    }

    /**
     * Disparado pelo Spring Security quando o login falha (e-mail não encontrado ou senha
     * errada). A mensagem é propositalmente genérica (não diz qual dos dois está errado),
     * pra não revelar a um atacante se um e-mail existe ou não na base.
     *
     * @param e exception lançada pelo AuthenticationManager
     * @return resposta 401
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException(BadCredentialsException e){
        return new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password.", List.of());
    }

    /**
     * Disparado pelo Spring Security quando um {@code @PreAuthorize} nega acesso (usuário
     * autenticado, mas sem a permissão exigida). Sem esse handler específico, caía no
     * catch-all de RuntimeException acima e virava 500 em vez do 403 correto.
     *
     * @param e exception lançada pelo mecanismo de autorização por método
     * @return resposta 403
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationDeniedException(AuthorizationDeniedException e){
        return new ErrorResponse(HttpStatus.FORBIDDEN.value(), "You do not have permission to perform this action.", List.of());
    }

    /**
     * Disparado pelo {@code TicketValidator} quando a ação viola uma regra de negócio:
     * pular uma etapa do fluxo do chamado, ou um usuário tentar agir sobre um chamado
     * que não é dele. 409 (conflito) porque a requisição em si é válida, só conflita com
     * o estado atual do recurso ou com quem está autenticado.
     *
     * @param e exception com a mensagem descrevendo a regra violada
     * @return resposta 409
     */
    @ExceptionHandler(OperationNotAllowedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOperationNotAllowedException(OperationNotAllowedException e){
        return ErrorResponse.conflict(e.getMessage());
    }

    /**
     * Disparado pelo {@code LoginAttemptService} quando um e-mail acumulou tentativas de
     * login erradas demais - 429 (Too Many Requests) é o status HTTP padrão pra esse cenário.
     *
     * @param e exception com a mensagem informando o motivo do bloqueio
     * @return resposta 429
     */
    @ExceptionHandler(TooManyLoginAttemptsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleTooManyLoginAttemptsException(TooManyLoginAttemptsException e){
        return new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), e.getMessage(), List.of());
    }
}

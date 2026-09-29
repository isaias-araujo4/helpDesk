package arajou.br.com.help_desk.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

// Corpo padrão de resposta de erro da API - todo ExceptionHandler do GlobalExceptionHandler
// devolve um objeto desse tipo, pra manter o formato de erro consistente em toda a API.
public record ErrorResponse(int status, String message, List<FieldError> errors) {

    // Atalho pra erros genéricos sem campo específico associado (400).
    public static ErrorResponse standardResponse(String message){
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, List.of());
    }

    // Atalho pra erros de conflito, ex: registro duplicado (409).
    public static ErrorResponse conflict(String message){
        return new ErrorResponse(HttpStatus.CONFLICT.value(), message, List.of());
    }
}

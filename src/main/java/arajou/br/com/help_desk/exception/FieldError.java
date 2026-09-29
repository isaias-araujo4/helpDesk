package arajou.br.com.help_desk.exception;

// Representa o erro de um campo específico dentro de um ErrorResponse
// (ex: {"field": "email", "error": "Invalid email"}).
public record FieldError(String field, String error) {
}

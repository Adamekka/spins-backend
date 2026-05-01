package com.adamekka.spins_backend.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse>
    handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
            .body(
                new ErrorResponse(exception.getError(), exception.getMessage())
            );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleValidationException(MethodArgumentNotValidException exception) {
        String message
            = exception.getBindingResult()
                  .getFieldErrors()
                  .stream()
                  .findFirst()
                  .map(
                      error
                      -> error.getField() + " " + error.getDefaultMessage()
                  )
                  .orElse("Request validation failed");
        return ResponseEntity.badRequest().body(
            new ErrorResponse("INVALID_REQUEST", message)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse>
    handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            "INVALID_REQUEST", "Request body is missing or malformed"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse>
    handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_REQUEST", exception.getMessage()));
    }
}

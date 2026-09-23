package com.citas.api.infrastructure.adapters.in.web.error;

import com.citas.api.domain.exception.DocumentAlreadyRegisteredException;
import com.citas.api.domain.exception.DomainException;
import com.citas.api.domain.exception.EmailAlreadyRegisteredException;
import com.citas.api.domain.exception.InvalidCredentialsException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.InvalidRefreshTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail beanValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        return ApiProblems.validation(errors);
    }

    @ExceptionHandler(InvalidFieldException.class)
    ProblemDetail domainValidation(InvalidFieldException e) {
        return ApiProblems.validation(List.of(Map.of("field", e.getField(), "message", e.getMessage())));
    }

    @ExceptionHandler({EmailAlreadyRegisteredException.class, DocumentAlreadyRegisteredException.class})
    ProblemDetail conflict(DomainException e) {
        return ApiProblems.of(HttpStatus.CONFLICT, e.getCode(), e.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    ProblemDetail unauthorized(DomainException e) {
        return ApiProblems.of(HttpStatus.UNAUTHORIZED, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        return ApiProblems.of(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "El cuerpo de la petición no es JSON válido");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return ApiProblems.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Se espera application/json");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ApiProblems.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Método no permitido");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail notFound(NoResourceFoundException e) {
        return ApiProblems.of(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso no encontrado");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        // Solo el tipo de excepción: el mensaje podría contener datos de la petición.
        log.error("Error no controlado: {}", e.getClass().getName(), e);
        return ApiProblems.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno");
    }
}

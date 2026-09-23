package com.citas.api.domain.exception;

/**
 * Dato de entrada que no cumple una regla del dominio (formato, política de contraseña, catálogo).
 */
public class InvalidFieldException extends DomainException {

    private final String field;

    public InvalidFieldException(String field, String message) {
        super("VALIDATION_ERROR", message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

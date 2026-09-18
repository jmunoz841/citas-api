package com.citas.api.domain.exception;

/**
 * Violación de una regla de negocio. {@code code} es estable y viaja al cliente.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

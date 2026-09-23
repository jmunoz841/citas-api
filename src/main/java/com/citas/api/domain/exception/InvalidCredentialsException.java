package com.citas.api.domain.exception;

/**
 * Mismo mensaje para email inexistente, contraseña incorrecta o cuenta inactiva (no revela qué falló).
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Credenciales inválidas");
    }
}

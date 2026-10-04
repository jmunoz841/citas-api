package com.citas.api.domain.exception;

/** Token de recuperación inexistente, vencido, revocado o ya utilizado. */
public class InvalidPasswordResetTokenException extends DomainException {
    public InvalidPasswordResetTokenException() {
        super("INVALID_PASSWORD_RESET_TOKEN", "El enlace de recuperación no es válido o ya venció");
    }
}

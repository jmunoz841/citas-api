package com.citas.api.domain.model.user;

import com.citas.api.domain.exception.InvalidFieldException;

import java.nio.charset.StandardCharsets;

/**
 * Política de contraseña aprobada (D-004): mínimo 8 caracteres, al menos una letra y un número.
 * El máximo de 72 bytes es un límite técnico de BCrypt.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new InvalidFieldException("password", "La contraseña debe tener al menos 8 caracteres");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidFieldException("password", "La contraseña no puede superar 72 bytes");
        }
        if (rawPassword.chars().noneMatch(Character::isLetter)) {
            throw new InvalidFieldException("password", "La contraseña debe contener al menos una letra");
        }
        if (rawPassword.chars().noneMatch(Character::isDigit)) {
            throw new InvalidFieldException("password", "La contraseña debe contener al menos un número");
        }
    }
}

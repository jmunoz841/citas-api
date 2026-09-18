package com.citas.api.domain.model.user;

import com.citas.api.domain.exception.InvalidFieldException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email normalizado (sin espacios, en minúsculas). La unicidad se compara sobre este valor.
 */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_LENGTH = 254;

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("email", "El email es obligatorio");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidFieldException("email", "El email no tiene un formato válido");
        }
    }
}

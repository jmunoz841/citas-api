package com.citas.api.domain.model.user;

import com.citas.api.domain.exception.InvalidFieldException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Documento de identidad. El número se normaliza (sin puntos, guiones ni espacios, en mayúsculas)
 * para que "1.234" y "1234" se detecten como el mismo documento (supuesto S-05).
 */
public record IdentityDocument(DocumentType type, String number) {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-]");
    private static final Pattern ALLOWED = Pattern.compile("^[A-Z0-9]{3,30}$");

    public IdentityDocument {
        if (type == null) {
            throw new InvalidFieldException("documentType", "El tipo de documento es obligatorio");
        }
        if (number == null || number.isBlank()) {
            throw new InvalidFieldException("documentNumber", "El número de documento es obligatorio");
        }
        number = SEPARATORS.matcher(number).replaceAll("").toUpperCase(Locale.ROOT);
        if (!ALLOWED.matcher(number).matches()) {
            throw new InvalidFieldException("documentNumber",
                    "El número de documento debe tener entre 3 y 30 letras o dígitos");
        }
    }
}

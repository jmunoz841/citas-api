package com.citas.api.domain.model.user;

import com.citas.api.domain.exception.InvalidFieldException;

import java.util.Locale;

/**
 * Tipos de documento. Coinciden con el catálogo fijo {@code document_types} (D-008).
 */
public enum DocumentType {
    CC, CE, TI, RC, PA, PPT;

    public static DocumentType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidFieldException("documentType", "El tipo de documento es obligatorio");
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException("documentType", "Tipo de documento no soportado");
        }
    }
}

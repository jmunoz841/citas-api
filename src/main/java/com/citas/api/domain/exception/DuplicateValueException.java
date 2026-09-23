package com.citas.api.domain.exception;

/**
 * Valor único que ya existe: código profesional, matrícula o nombre de especialidad (409).
 * Email y documento tienen sus propias excepciones desde HU-001.
 */
public class DuplicateValueException extends DomainException {

    private final String field;

    public DuplicateValueException(String code, String field, String message) {
        super(code, message);
        this.field = field;
    }

    public static DuplicateValueException professionalCode() {
        return new DuplicateValueException("PROFESSIONAL_CODE_ALREADY_REGISTERED", "professionalCode",
                "El código profesional ya está registrado");
    }

    public static DuplicateValueException licenseNumber() {
        return new DuplicateValueException("LICENSE_ALREADY_REGISTERED", "licenseNumber",
                "La matrícula ya está registrada");
    }

    public static DuplicateValueException specialtyName() {
        return new DuplicateValueException("SPECIALTY_NAME_ALREADY_REGISTERED", "name",
                "Ya existe una especialidad con ese nombre");
    }

    public String getField() {
        return field;
    }
}

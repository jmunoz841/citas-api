package com.citas.api.domain.model.professional;

import com.citas.api.domain.exception.InvalidFieldException;

/**
 * Datos propios del profesional. Es un subtipo del usuario: comparte identidad con él
 * ({@code userId} es a la vez clave primaria y foránea) y añade código, matrícula y estado.
 */
public final class Professional {

    private static final int MAX_CODE_LENGTH = 30;

    private final Long userId;
    private final String professionalCode;
    private final String licenseNumber;
    private final boolean active;
    private final ProfessionalAssignments assignments;

    private Professional(Long userId, String professionalCode, String licenseNumber, boolean active,
                         ProfessionalAssignments assignments) {
        this.userId = userId;
        this.professionalCode = requireCode("professionalCode", professionalCode);
        this.licenseNumber = requireCode("licenseNumber", licenseNumber);
        this.active = active;
        this.assignments = assignments;
    }

    public static Professional createNew(String professionalCode, String licenseNumber,
                                         ProfessionalAssignments assignments) {
        return new Professional(null, professionalCode, licenseNumber, true, assignments);
    }

    public static Professional restore(Long userId, String professionalCode, String licenseNumber, boolean active,
                                       ProfessionalAssignments assignments) {
        return new Professional(userId, professionalCode, licenseNumber, active, assignments);
    }

    public Professional withUserId(Long newUserId) {
        return new Professional(newUserId, professionalCode, licenseNumber, active, assignments);
    }

    /** Desactivar conserva datos, asignaciones e historial (HU-009). */
    public Professional withActive(boolean newActive) {
        return new Professional(userId, professionalCode, licenseNumber, newActive, assignments);
    }

    public Professional withAssignments(ProfessionalAssignments newAssignments) {
        return new Professional(userId, professionalCode, licenseNumber, active, newAssignments);
    }

    private static String requireCode(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException(field, "El campo es obligatorio");
        }
        String trimmed = value.trim().toUpperCase();
        if (trimmed.length() > MAX_CODE_LENGTH) {
            throw new InvalidFieldException(field, "El campo supera " + MAX_CODE_LENGTH + " caracteres");
        }
        return trimmed;
    }

    public Long getUserId() { return userId; }
    public String getProfessionalCode() { return professionalCode; }
    public String getLicenseNumber() { return licenseNumber; }
    public boolean isActive() { return active; }
    public ProfessionalAssignments getAssignments() { return assignments; }
}

package com.citas.api.domain.model.affiliation;

/**
 * Afiliación de un usuario a un plan de EPS bajo un régimen.
 *
 * <p>El régimen es un hecho del afiliado, no del plan: una misma EPS opera en contributivo y
 * en subsidiado. Por eso se guarda aquí y no en el plan.</p>
 */
public record Affiliation(Long id, Long userId, Long planId, String regimeCode) {

    public Affiliation {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario de la afiliación es obligatorio");
        }
        if (planId == null) {
            throw new IllegalArgumentException("El plan de la afiliación es obligatorio");
        }
        if (regimeCode == null || regimeCode.isBlank()) {
            throw new IllegalArgumentException("El régimen de la afiliación es obligatorio");
        }
    }

    /** Afiliación aún no persistida. */
    public static Affiliation initial(Long userId, Long planId, String regimeCode) {
        return new Affiliation(null, userId, planId, regimeCode);
    }
}

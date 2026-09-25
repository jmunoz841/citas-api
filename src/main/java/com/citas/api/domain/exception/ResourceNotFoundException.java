package com.citas.api.domain.exception;

/**
 * Recurso inexistente al que se intenta acceder por su identificador (404).
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    public static ResourceNotFoundException specialty() {
        return new ResourceNotFoundException("La especialidad no existe");
    }

    public static ResourceNotFoundException professional() {
        return new ResourceNotFoundException("El profesional no existe");
    }
}

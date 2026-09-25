package com.citas.api.application.port.in;

/**
 * Datos de la persona detrás de una sesión, para saludarla y mostrarla en la cabecera. Se leen
 * de la base: el access token solo lleva id, email y roles.
 */
public interface GetSessionProfileUseCase {

    SessionProfile profile(Long userId);

    record SessionProfile(String firstNames, String lastNames) {
    }
}

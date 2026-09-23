package com.citas.api.infrastructure.config;

import com.citas.api.application.port.out.CatalogRepositoryPort;
import com.citas.api.application.port.out.PasswordHasherPort;
import com.citas.api.application.port.out.RefreshTokenRepositoryPort;
import com.citas.api.application.port.out.TokenProviderPort;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.application.service.AuthService;
import com.citas.api.application.service.CatalogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Cableado de los casos de uso: la capa de aplicación no usa anotaciones de componentes de Spring.
 */
@Configuration
class ApplicationConfig {

    /** Hora civil de Bogotá para todas las fechas persistidas (D-007). */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    @Bean
    AuthService authService(UserRepositoryPort users, RefreshTokenRepositoryPort refreshTokens,
                            PasswordHasherPort passwordHasher, TokenProviderPort tokenProvider, Clock clock) {
        return new AuthService(users, refreshTokens, passwordHasher, tokenProvider, clock);
    }

    @Bean
    CatalogService catalogService(CatalogRepositoryPort catalogs) {
        return new CatalogService(catalogs);
    }
}

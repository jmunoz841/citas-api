package com.citas.api.infrastructure.config;

import com.citas.api.application.port.out.AffiliationRepositoryPort;
import com.citas.api.application.port.out.AvailabilityRepositoryPort;
import com.citas.api.application.port.out.CatalogRepositoryPort;
import com.citas.api.application.port.out.PasswordHasherPort;
import com.citas.api.application.port.out.ProfessionalRepositoryPort;
import com.citas.api.application.port.out.RefreshTokenRepositoryPort;
import com.citas.api.application.port.out.SpecialtyRepositoryPort;
import com.citas.api.application.port.out.TokenProviderPort;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.application.service.AuthService;
import com.citas.api.application.service.AvailabilityService;
import com.citas.api.application.service.CatalogService;
import com.citas.api.application.service.ProfessionalService;
import com.citas.api.application.service.SpecialtyService;
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
                            AffiliationRepositoryPort affiliations, PasswordHasherPort passwordHasher,
                            TokenProviderPort tokenProvider, Clock clock) {
        return new AuthService(users, refreshTokens, affiliations, passwordHasher, tokenProvider, clock);
    }

    @Bean
    CatalogService catalogService(CatalogRepositoryPort catalogs, AffiliationRepositoryPort affiliations) {
        return new CatalogService(catalogs, affiliations);
    }

    @Bean
    AvailabilityService availabilityService(AvailabilityRepositoryPort blocks,
                                            ProfessionalRepositoryPort professionals, Clock clock) {
        return new AvailabilityService(blocks, professionals, clock);
    }

    @Bean
    SpecialtyService specialtyService(SpecialtyRepositoryPort specialties) {
        return new SpecialtyService(specialties);
    }

    @Bean
    ProfessionalService professionalService(UserRepositoryPort users, ProfessionalRepositoryPort professionals,
                                            SpecialtyRepositoryPort specialties, CatalogRepositoryPort catalogs,
                                            PasswordHasherPort passwordHasher) {
        return new ProfessionalService(users, professionals, specialties, catalogs, passwordHasher);
    }
}

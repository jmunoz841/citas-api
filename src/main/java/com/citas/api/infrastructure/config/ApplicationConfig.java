package com.citas.api.infrastructure.config;

import com.citas.api.application.port.out.AffiliationRepositoryPort;
import com.citas.api.application.port.out.AgendaQueryPort;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.application.port.out.AvailabilityRepositoryPort;
import com.citas.api.application.port.out.CatalogRepositoryPort;
import com.citas.api.application.port.out.PasswordHasherPort;
import com.citas.api.application.port.out.PasswordResetTokenGeneratorPort;
import com.citas.api.application.port.out.PasswordResetTokenRepositoryPort;
import com.citas.api.application.port.out.ProfessionalRepositoryPort;
import com.citas.api.application.port.out.RefreshTokenRepositoryPort;
import com.citas.api.application.port.out.SpecialtyRepositoryPort;
import com.citas.api.application.port.out.TokenProviderPort;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.application.port.out.InsuranceCatalogRepositoryPort;
import com.citas.api.application.port.out.RescheduleRepositoryPort;
import com.citas.api.application.service.RescheduleRequestService;
import com.citas.api.application.service.RescheduleResolutionService;
import com.citas.api.application.service.AppointmentBookingService;
import com.citas.api.application.service.AppointmentRequestService;
import com.citas.api.application.service.AuthService;
import com.citas.api.application.service.AvailabilitySearchService;
import com.citas.api.application.service.AvailabilityService;
import com.citas.api.application.service.CatalogService;
import com.citas.api.application.service.ProfessionalService;
import com.citas.api.application.service.SpecialtyService;
import com.citas.api.application.service.OwnAppointmentsService;
import com.citas.api.application.service.AppointmentCancellationService;
import com.citas.api.application.service.ProfessionalAgendaService;
import com.citas.api.application.service.ProfessionalAppointmentClosureService;
import com.citas.api.application.service.AppointmentHistoryService;
import com.citas.api.application.service.PasswordRecoveryService;
import com.citas.api.application.service.OwnProfileService;
import com.citas.api.application.service.InsuranceCatalogService;
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
    PasswordRecoveryService passwordRecoveryService(UserRepositoryPort users,
                                                    PasswordResetTokenRepositoryPort resetTokens,
                                                    PasswordResetTokenGeneratorPort resetTokenGenerator,
                                                    TokenProviderPort tokenProvider, PasswordHasherPort passwordHasher,
                                                    Clock clock) {
        return new PasswordRecoveryService(users, resetTokens, resetTokenGenerator, tokenProvider, passwordHasher, clock);
    }

    @Bean OwnProfileService ownProfileService(UserRepositoryPort users) { return new OwnProfileService(users); }
    @Bean InsuranceCatalogService insuranceCatalogService(InsuranceCatalogRepositoryPort catalogs) { return new InsuranceCatalogService(catalogs); }

    @Bean
    CatalogService catalogService(CatalogRepositoryPort catalogs, AffiliationRepositoryPort affiliations,
                                  SpecialtyRepositoryPort specialties) {
        return new CatalogService(catalogs, affiliations, specialties);
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

    @Bean
    AvailabilitySearchService availabilitySearchService(AgendaQueryPort agenda, Clock clock) {
        return new AvailabilitySearchService(agenda, clock);
    }

    @Bean
    AppointmentBookingService appointmentBookingService(SpecialtyRepositoryPort specialties,
                                                        ProfessionalRepositoryPort professionals,
                                                        AgendaQueryPort agenda,
                                                        AppointmentRepositoryPort appointments, Clock clock) {
        return new AppointmentBookingService(specialties, professionals, agenda, appointments, clock);
    }

    @Bean
    AppointmentRequestService appointmentRequestService(AppointmentRepositoryPort appointments) {
        return new AppointmentRequestService(appointments);
    }

    @Bean
    OwnAppointmentsService ownAppointmentsService(AppointmentRepositoryPort appointments) {
        return new OwnAppointmentsService(appointments);
    }

    @Bean
    AppointmentCancellationService appointmentCancellationService(AppointmentRepositoryPort appointments,
                                                                  RescheduleRepositoryPort reschedules, Clock clock) {
        return new AppointmentCancellationService(appointments, reschedules, clock);
    }

    @Bean
    RescheduleRequestService rescheduleRequestService(AppointmentRepositoryPort appointments,
                                                      RescheduleRepositoryPort reschedules, AgendaQueryPort agenda,
                                                      Clock clock) {
        return new RescheduleRequestService(appointments, reschedules, agenda, clock);
    }

    @Bean
    RescheduleResolutionService rescheduleResolutionService(RescheduleRepositoryPort reschedules,
                                                            AppointmentRepositoryPort appointments, Clock clock) {
        return new RescheduleResolutionService(reschedules, appointments, clock);
    }
    @Bean ProfessionalAgendaService professionalAgendaService(AppointmentRepositoryPort appointments) { return new ProfessionalAgendaService(appointments); }
    @Bean ProfessionalAppointmentClosureService professionalAppointmentClosureService(AppointmentRepositoryPort appointments, RescheduleRepositoryPort reschedules, Clock clock) { return new ProfessionalAppointmentClosureService(appointments, reschedules, clock); }
    @Bean AppointmentHistoryService appointmentHistoryService(AppointmentRepositoryPort appointments) { return new AppointmentHistoryService(appointments); }
}

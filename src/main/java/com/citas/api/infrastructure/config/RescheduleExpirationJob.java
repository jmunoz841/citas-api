package com.citas.api.infrastructure.config;

import com.citas.api.application.port.in.ResolveRescheduleUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * D-033: una reprogramación que sigue {@code PENDING} cuando llega la hora de la cita (o del nuevo
 * horario) se cierra como {@code CANCELLED} y libera su retención. La bandeja también las cierra
 * al consultarse; este job cubre el caso de que nadie la abra.
 */
@Configuration
@EnableScheduling
class RescheduleExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(RescheduleExpirationJob.class);

    private final ResolveRescheduleUseCase reschedules;

    RescheduleExpirationJob(ResolveRescheduleUseCase reschedules) {
        this.reschedules = reschedules;
    }

    @Scheduled(fixedDelayString = "${citas.reschedule.expiration-delay:PT5M}",
            initialDelayString = "${citas.reschedule.expiration-delay:PT5M}")
    void expireOverdue() {
        int expired = reschedules.expireOverdue();
        if (expired > 0) {
            log.info("Reprogramaciones vencidas cerradas: {}", expired);
        }
    }
}

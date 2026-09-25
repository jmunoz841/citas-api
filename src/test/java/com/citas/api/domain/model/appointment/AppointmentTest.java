package com.citas.api.domain.model.appointment;

import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.model.professional.Specialty;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Estado inicial (RN-02, RN-03) y transiciones del ADMIN (HU-015) sin base de datos.
 */
class AppointmentTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 3, 4, 8, 0);
    private static final Specialty GENERAL = Specialty.restore(1L, "Medicina General", 30, true, true);
    private static final Specialty CARDIOLOGIA = Specialty.restore(2L, "Cardiología", 60, false, true);

    @Test
    void laCitaGeneralNaceAprobadaYLaEspecializadaSolicitada() {
        assertThat(Appointment.book(9L, 5L, GENERAL, "HIC", INICIO).getStatus())
                .isEqualTo(AppointmentStatus.APPROVED);
        Appointment especializada = Appointment.book(9L, 5L, CARDIOLOGIA, "HIC", INICIO);
        assertThat(especializada.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(especializada.getEndAt()).isEqualTo(INICIO.plusMinutes(60));
    }

    @Test
    void ca01_ca02_unaSolicitudSeApruebaOSeRechaza() {
        Appointment solicitada = Appointment.book(9L, 5L, CARDIOLOGIA, "HIC", INICIO);

        assertThat(solicitada.approve().getStatus()).isEqualTo(AppointmentStatus.APPROVED);
        assertThat(solicitada.reject("Sin cupo").getStatus()).isEqualTo(AppointmentStatus.REJECTED);
    }

    @Test
    void ca03_rechazarExigeMotivo() {
        Appointment solicitada = Appointment.book(9L, 5L, CARDIOLOGIA, "HIC", INICIO);

        assertThatThrownBy(() -> solicitada.reject(" ")).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> solicitada.reject(null)).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> solicitada.reject("x".repeat(501))).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void ca04_soloUnaCitaSolicitadaSeResuelve() {
        Appointment aprobada = Appointment.book(9L, 5L, GENERAL, "HIC", INICIO);

        assertThatThrownBy(aprobada::approve).isInstanceOf(BusinessConflictException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_STATUS_TRANSITION");
        assertThatThrownBy(() -> aprobada.reject("Sin cupo")).isInstanceOf(BusinessConflictException.class);
    }
}

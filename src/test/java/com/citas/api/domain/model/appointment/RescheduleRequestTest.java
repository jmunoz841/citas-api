package com.citas.api.domain.model.appointment;

import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de dominio de la reprogramación (HU-018, HU-019, D-033), sin base de datos. */
class RescheduleRequestTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 5, 10, 0);
    private static final LocalDateTime ORIGINAL = AHORA.plusDays(1).withHour(8);
    private static final LocalDateTime NUEVA = AHORA.plusDays(2).withHour(9);

    @Test
    void hu018_ca01_abreLaSolicitudPendienteConLaFranjaOriginalComoCopia() {
        RescheduleRequest request = RescheduleRequest.open(cita(AppointmentStatus.APPROVED, ORIGINAL), NUEVA, "ICV", AHORA);

        assertThat(request.status()).isEqualTo(RescheduleStatus.PENDING);
        assertThat(request.originalStartAt()).isEqualTo(ORIGINAL);
        assertThat(request.originalSiteCode()).isEqualTo("HIC");
        assertThat(request.requestedStartAt()).isEqualTo(NUEVA);
        assertThat(request.requestedSiteCode()).isEqualTo("ICV");
        assertThat(request.decidedAt()).isNull();
    }

    @Test
    void hu018_ca02_soloSeReprogramaUnaCitaAprobadaYFutura() {
        assertThatThrownBy(() -> RescheduleRequest.open(cita(AppointmentStatus.REQUESTED, ORIGINAL), NUEVA, "HIC", AHORA))
                .isInstanceOf(BusinessConflictException.class)
                .hasFieldOrPropertyWithValue("code", "APPOINTMENT_NOT_RESCHEDULABLE");
        assertThatThrownBy(() -> RescheduleRequest.open(cita(AppointmentStatus.APPROVED, AHORA.minusHours(1)), NUEVA, "HIC", AHORA))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void laNuevaFranjaDebeSerFuturaYDistintaDeLaActual() {
        Appointment cita = cita(AppointmentStatus.APPROVED, ORIGINAL);
        assertThatThrownBy(() -> RescheduleRequest.open(cita, AHORA.minusMinutes(30), "HIC", AHORA))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> RescheduleRequest.open(cita, ORIGINAL, "HIC", AHORA))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void hu019_ca03_rechazarExigeMotivo() {
        RescheduleRequest pendiente = pendiente();
        assertThatThrownBy(() -> pendiente.reject(1L, "  ", AHORA)).isInstanceOf(InvalidFieldException.class);

        RescheduleRequest rechazada = pendiente.reject(1L, " Sin cupo ", AHORA);
        assertThat(rechazada.status()).isEqualTo(RescheduleStatus.REJECTED);
        assertThat(rechazada.decisionReason()).isEqualTo("Sin cupo");
        assertThat(rechazada.decidedByUserId()).isEqualTo(1L);
    }

    @Test
    void hu019_ca04_soloUnaPendienteSeDecide() {
        RescheduleRequest aprobada = pendiente().approve(1L, AHORA);
        assertThat(aprobada.status()).isEqualTo(RescheduleStatus.APPROVED);
        assertThatThrownBy(() -> aprobada.reject(1L, "Tarde", AHORA))
                .isInstanceOf(BusinessConflictException.class)
                .hasFieldOrPropertyWithValue("code", "RESCHEDULE_NOT_PENDING");
        assertThatThrownBy(() -> aprobada.cancel(AHORA)).isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void d033_vencidaCuandoLlegaLaHoraOriginalOLaSolicitadaYNoSePuedeAprobar() {
        RescheduleRequest pendiente = pendiente();
        assertThat(pendiente.isExpired(AHORA)).isFalse();
        assertThat(pendiente.isExpired(ORIGINAL)).isTrue();

        RescheduleRequest adelantada = RescheduleRequest.open(cita(AppointmentStatus.APPROVED, NUEVA), ORIGINAL, "HIC", AHORA);
        assertThat(adelantada.isExpired(ORIGINAL)).isTrue();
        assertThatThrownBy(() -> adelantada.approve(1L, ORIGINAL))
                .isInstanceOf(BusinessConflictException.class)
                .hasFieldOrPropertyWithValue("code", "RESCHEDULE_EXPIRED");

        RescheduleRequest cancelada = pendiente.cancel(ORIGINAL);
        assertThat(cancelada.status()).isEqualTo(RescheduleStatus.CANCELLED);
        assertThat(cancelada.decidedAt()).isEqualTo(ORIGINAL);
        assertThat(cancelada.decidedByUserId()).isNull();
    }

    @Test
    void laCitaReprogramadaConservaProfesionalEspecialidadDuracionYEstado() {
        Appointment movida = cita(AppointmentStatus.APPROVED, ORIGINAL).reschedule(NUEVA, "ICV");
        assertThat(movida.getStartAt()).isEqualTo(NUEVA);
        assertThat(movida.getSiteCode()).isEqualTo("ICV");
        assertThat(movida.getProfessionalId()).isEqualTo(20L);
        assertThat(movida.getSpecialtyId()).isEqualTo(30L);
        assertThat(movida.getDurationMinutes()).isEqualTo(60);
        assertThat(movida.getStatus()).isEqualTo(AppointmentStatus.APPROVED);
        assertThatThrownBy(() -> cita(AppointmentStatus.CANCELLED, ORIGINAL).reschedule(NUEVA, "HIC"))
                .isInstanceOf(BusinessConflictException.class);
    }

    private static RescheduleRequest pendiente() {
        return RescheduleRequest.open(cita(AppointmentStatus.APPROVED, ORIGINAL), NUEVA, "HIC", AHORA).withId(7L);
    }

    private static Appointment cita(AppointmentStatus status, LocalDateTime startAt) {
        return Appointment.restore(5L, 10L, 20L, 30L, "HIC", startAt, 60, status);
    }
}

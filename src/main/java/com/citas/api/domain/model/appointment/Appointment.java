package com.citas.api.domain.model.appointment;

import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.model.professional.Specialty;

import java.time.LocalDateTime;

/**
 * Cita de un paciente con un profesional, en una especialidad, sede y horario.
 *
 * <p>La duración es una copia de la de la especialidad al reservar: si la especialidad cambia
 * después, la cita conserva la duración con la que se agendó.</p>
 */
public final class Appointment {

    /** Longitud de {@code appointment_status_history.reason}. */
    public static final int MAX_REASON_LENGTH = 500;

    private final Long id;
    private final Long patientUserId;
    private final Long professionalId;
    private final Long specialtyId;
    private final String siteCode;
    private final LocalDateTime startAt;
    private final int durationMinutes;
    private final AppointmentStatus status;

    private Appointment(Long id, Long patientUserId, Long professionalId, Long specialtyId, String siteCode,
                        LocalDateTime startAt, int durationMinutes, AppointmentStatus status) {
        this.id = id;
        this.patientUserId = patientUserId;
        this.professionalId = professionalId;
        this.specialtyId = specialtyId;
        this.siteCode = siteCode;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.status = status;
    }

    /**
     * Cita nueva. Medicina General queda aprobada sin intervención administrativa (RN-02); una
     * especializada nace solicitada y espera la decisión del ADMIN (RN-03).
     */
    public static Appointment book(Long patientUserId, Long professionalId, Specialty specialty, String siteCode,
                                   LocalDateTime startAt) {
        AppointmentStatus initial = specialty.isGeneral() ? AppointmentStatus.APPROVED : AppointmentStatus.REQUESTED;
        return new Appointment(null, patientUserId, professionalId, specialty.getId(), siteCode, startAt,
                specialty.getDurationMinutes(), initial);
    }

    public static Appointment restore(Long id, Long patientUserId, Long professionalId, Long specialtyId,
                                      String siteCode, LocalDateTime startAt, int durationMinutes,
                                      AppointmentStatus status) {
        return new Appointment(id, patientUserId, professionalId, specialtyId, siteCode, startAt, durationMinutes,
                status);
    }

    public Appointment withId(Long newId) {
        return new Appointment(newId, patientUserId, professionalId, specialtyId, siteCode, startAt,
                durationMinutes, status);
    }

    /** El ADMIN aprueba una solicitud especializada (HU-015 CA-01). */
    public Appointment approve() {
        return resolve(AppointmentStatus.APPROVED);
    }

    /** El ADMIN rechaza una solicitud especializada; el motivo es obligatorio (RN-04). */
    public Appointment reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidFieldException("reason", "El motivo del rechazo es obligatorio");
        }
        if (reason.trim().length() > MAX_REASON_LENGTH) {
            throw new InvalidFieldException("reason", "El motivo supera " + MAX_REASON_LENGTH + " caracteres");
        }
        return resolve(AppointmentStatus.REJECTED);
    }

    /** Solo una cita {@code REQUESTED} se aprueba o se rechaza (RN-11, HU-015 CA-04). */
    private Appointment resolve(AppointmentStatus target) {
        if (status != AppointmentStatus.REQUESTED) {
            throw BusinessConflictException.invalidStatusTransition();
        }
        return new Appointment(id, patientUserId, professionalId, specialtyId, siteCode, startAt, durationMinutes,
                target);
    }

    public LocalDateTime getEndAt() {
        return startAt.plusMinutes(durationMinutes);
    }

    public Long getId() { return id; }
    public Long getPatientUserId() { return patientUserId; }
    public Long getProfessionalId() { return professionalId; }
    public Long getSpecialtyId() { return specialtyId; }
    public String getSiteCode() { return siteCode; }
    public LocalDateTime getStartAt() { return startAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public AppointmentStatus getStatus() { return status; }
}

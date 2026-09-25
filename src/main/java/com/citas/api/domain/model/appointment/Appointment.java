package com.citas.api.domain.model.appointment;

import com.citas.api.domain.model.professional.Specialty;

import java.time.LocalDateTime;

/**
 * Cita de un paciente con un profesional, en una especialidad, sede y horario.
 *
 * <p>La duración es una copia de la de la especialidad al reservar: si la especialidad cambia
 * después, la cita conserva la duración con la que se agendó.</p>
 */
public final class Appointment {

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

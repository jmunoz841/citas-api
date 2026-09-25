package com.citas.api.application.port.out;

import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.StatusChange;

import java.util.List;

/**
 * Citas, ocupación de slots e historial (V6).
 */
public interface AppointmentRepositoryPort {

    Appointment save(Appointment appointment);

    /**
     * Ocupa los slots a nombre de la cita. Si alguno ya está ocupado, la clave primaria de
     * {@code slot_reservations} rechaza la inserción y se lanza
     * {@link com.citas.api.domain.exception.BusinessConflictException#slotUnavailable()}.
     */
    void occupySlots(Appointment appointment, List<AgendaSlot> slots);

    void recordStatus(StatusChange change);
}

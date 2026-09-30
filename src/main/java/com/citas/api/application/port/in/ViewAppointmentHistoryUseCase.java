package com.citas.api.application.port.in;
import com.citas.api.domain.model.appointment.AppointmentHistoryEntry;
import java.util.List;
public interface ViewAppointmentHistoryUseCase { List<AppointmentHistoryEntry> forPatient(Long patientId, Long appointmentId); List<AppointmentHistoryEntry> forProfessional(Long professionalId, Long appointmentId); List<AppointmentHistoryEntry> forAdmin(Long appointmentId); }

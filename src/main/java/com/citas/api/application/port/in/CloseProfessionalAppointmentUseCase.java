package com.citas.api.application.port.in;
import com.citas.api.domain.model.appointment.Appointment;
import com.citas.api.domain.model.appointment.AppointmentStatus;
public interface CloseProfessionalAppointmentUseCase { Appointment close(Long professionalId, Long appointmentId, AppointmentStatus result); }

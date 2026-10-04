package com.citas.api.application.service;

import com.citas.api.application.port.in.ViewProfessionalAgendaUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.model.appointment.ProfessionalAppointmentView;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

public class ProfessionalAgendaService implements ViewProfessionalAgendaUseCase {
  private final AppointmentRepositoryPort appointments;
  public ProfessionalAgendaService(AppointmentRepositoryPort appointments) { this.appointments = appointments; }
  @Override @Transactional(readOnly = true)
  public List<ProfessionalAppointmentView> list(Long professionalId, LocalDate from, LocalDate to, String siteCode) {
    if (from == null || to == null) throw new InvalidFieldException("from", "El rango de fechas es obligatorio");
    if (from.isAfter(to)) throw new InvalidFieldException("to", "La fecha final debe ser igual o posterior a la inicial");
    return appointments.findApprovedViewsByProfessional(professionalId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), siteCode);
  }
}

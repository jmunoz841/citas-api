package com.citas.api.application.port.in;

import com.citas.api.domain.model.appointment.ProfessionalAppointmentView;
import java.time.LocalDate;
import java.util.List;

public interface ViewProfessionalAgendaUseCase {
    List<ProfessionalAppointmentView> list(Long professionalId, LocalDate from, LocalDate to, String siteCode);
}

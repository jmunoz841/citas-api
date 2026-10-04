package com.citas.api.application.service;
import com.citas.api.application.port.in.CloseProfessionalAppointmentUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.application.port.out.RescheduleRepositoryPort;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
/** Cierre COMPLETED/NO_SHOW por el profesional (HU-020); una reprogramacion pendiente se cancela (D-033). */
public class ProfessionalAppointmentClosureService implements CloseProfessionalAppointmentUseCase {
 private final AppointmentRepositoryPort appointments; private final RescheduleRepositoryPort reschedules; private final Clock clock;
 public ProfessionalAppointmentClosureService(AppointmentRepositoryPort a, RescheduleRepositoryPort r, Clock c){appointments=a;reschedules=r;clock=c;}
 @Override @Transactional public Appointment close(Long professionalId,Long appointmentId,AppointmentStatus result){
  Appointment current=appointments.findById(appointmentId).filter(a->a.getProfessionalId().equals(professionalId)).orElseThrow(()->new ResourceNotFoundException("La cita no existe"));
  LocalDateTime now=LocalDateTime.now(clock);
  if(current.getStartAt().isAfter(now)) throw new InvalidFieldException("appointmentId","La cita aun no inicia");
  Appointment closed=current.close(result); PendingReschedules.closeFor(reschedules,appointmentId,now); appointments.changeStatus(closed,AppointmentStatus.APPROVED); appointments.recordStatus(new StatusChange(appointmentId,result,StatusChange.Source.PROFESSIONAL,professionalId,null)); return closed;
 }}

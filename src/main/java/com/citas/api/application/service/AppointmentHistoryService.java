package com.citas.api.application.service;
import com.citas.api.application.port.in.ViewAppointmentHistoryUseCase;
import com.citas.api.application.port.out.AppointmentRepositoryPort;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.appointment.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public class AppointmentHistoryService implements ViewAppointmentHistoryUseCase {
 private final AppointmentRepositoryPort appointments; public AppointmentHistoryService(AppointmentRepositoryPort a){appointments=a;}
 @Override @Transactional(readOnly=true) public List<AppointmentHistoryEntry> forPatient(Long p,Long id){Appointment a=require(id); if(!a.getPatientUserId().equals(p)) throw missing(); return appointments.findHistory(id);}
 @Override @Transactional(readOnly=true) public List<AppointmentHistoryEntry> forProfessional(Long p,Long id){Appointment a=require(id); if(!a.getProfessionalId().equals(p)) throw missing(); return appointments.findHistory(id);}
 @Override @Transactional(readOnly=true) public List<AppointmentHistoryEntry> forAdmin(Long id){require(id);return appointments.findHistory(id);}
 private Appointment require(Long id){return appointments.findById(id).orElseThrow(this::missing);} private ResourceNotFoundException missing(){return new ResourceNotFoundException("La cita no existe");}}

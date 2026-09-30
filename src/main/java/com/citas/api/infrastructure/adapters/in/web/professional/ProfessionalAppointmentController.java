package com.citas.api.infrastructure.adapters.in.web.professional;

import com.citas.api.application.port.in.ViewProfessionalAgendaUseCase;
import com.citas.api.application.port.in.CloseProfessionalAppointmentUseCase;
import com.citas.api.application.port.in.ViewAppointmentHistoryUseCase;
import com.citas.api.domain.model.appointment.AppointmentStatus;
import com.citas.api.domain.model.appointment.ProfessionalAppointmentView;
import com.citas.api.infrastructure.adapters.out.security.AuthenticatedUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/v1/professional/appointments")
class ProfessionalAppointmentController {
  private final ViewProfessionalAgendaUseCase agenda;
  private final CloseProfessionalAppointmentUseCase closure;
  private final ViewAppointmentHistoryUseCase history;
  ProfessionalAppointmentController(ViewProfessionalAgendaUseCase agenda, CloseProfessionalAppointmentUseCase closure, ViewAppointmentHistoryUseCase history) { this.agenda = agenda; this.closure = closure; this.history = history; }
  @GetMapping ItemsResponse list(@AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required=false) String siteCode) {
    return new ItemsResponse(agenda.list(user.userId(), from, to, siteCode).stream().map(Item::from).toList()); }
  @PostMapping("/{appointmentId}/close") CloseResponse close(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId, @RequestParam AppointmentStatus result) {
    var appointment = closure.close(user.userId(), appointmentId, result);
    return new CloseResponse(appointment.getId(), appointment.getStatus().name()); }
  @GetMapping("/{appointmentId}/history") HistoryResponse history(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable Long appointmentId){return new HistoryResponse(history.forProfessional(user.userId(),appointmentId).stream().map(h->new HistoryItem(h.status().name(),h.source(),h.actorUserId(),h.changedAt().toString(),h.reason())).toList());}
  record ItemsResponse(List<Item> items) { }
  record CloseResponse(Long id, String status) { }
  record HistoryResponse(List<HistoryItem> items) { } record HistoryItem(String status,String source,Long actorUserId,String changedAt,String reason) { }
  record Item(Long id,String patientName,String specialtyName,String siteCode,String date,String startTime,String endTime,int durationMinutes) {
    static Item from(ProfessionalAppointmentView a){return new Item(a.id(),a.patientName(),a.specialtyName(),a.siteCode(),a.startAt().toLocalDate().toString(),a.startAt().toLocalTime().toString(),a.endAt().toLocalTime().toString(),a.durationMinutes());}}
}

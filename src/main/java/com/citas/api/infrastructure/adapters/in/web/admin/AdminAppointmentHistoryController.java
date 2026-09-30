package com.citas.api.infrastructure.adapters.in.web.admin;
import com.citas.api.application.port.in.ViewAppointmentHistoryUseCase;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/admin/appointments")
class AdminAppointmentHistoryController { private final ViewAppointmentHistoryUseCase history; AdminAppointmentHistoryController(ViewAppointmentHistoryUseCase h){history=h;}
 @GetMapping("/{appointmentId}/history") Response get(@PathVariable Long appointmentId){return new Response(history.forAdmin(appointmentId).stream().map(x->new Item(x.status().name(),x.source(),x.actorUserId(),x.changedAt().toString(),x.reason())).toList());}
 record Response(List<Item> items){} record Item(String status,String source,Long actorUserId,String changedAt,String reason){} }

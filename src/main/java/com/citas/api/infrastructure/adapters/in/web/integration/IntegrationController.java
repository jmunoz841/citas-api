package com.citas.api.infrastructure.adapters.in.web.integration;

import com.citas.api.application.port.in.AppointmentRemindersUseCase;
import com.citas.api.application.port.in.AppointmentRemindersUseCase.ReminderMark;
import com.citas.api.application.port.in.DailySummaryUseCase;
import com.citas.api.domain.model.integration.DailySummary;
import com.citas.api.domain.model.integration.ReminderCandidate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Endpoints para las automatizaciones n8n (HU-023, HU-025). Solo la credencial de servicio
 * ({@code X-Api-Key}, rol {@code INTEGRATION}) por la regla de {@code /api/v1/integrations/**} en
 * {@code SecurityConfig}. Contrato: {@code docs/contratos/integraciones.md}.
 */
@RestController
@RequestMapping("/api/v1/integrations")
class IntegrationController {

    private final AppointmentRemindersUseCase reminders;
    private final DailySummaryUseCase summaries;

    IntegrationController(AppointmentRemindersUseCase reminders, DailySummaryUseCase summaries) {
        this.reminders = reminders;
        this.summaries = summaries;
    }

    /** WF-001: citas por recordar en las próximas {@code hours} horas (por defecto 24, máx. 72). */
    @GetMapping("/reminders")
    ItemsResponse<ReminderResponse> reminders(@RequestParam(required = false) Integer hours) {
        return new ItemsResponse<>(reminders.upcoming(hours).stream().map(ReminderResponse::from).toList());
    }

    /** WF-001: marca el recordatorio como enviado; idempotente. */
    @PostMapping("/reminders/{appointmentId}/sent")
    ReminderMarkResponse markSent(@PathVariable Long appointmentId) {
        return ReminderMarkResponse.from(reminders.markSent(appointmentId));
    }

    /** WF-003: conteos del día (por defecto hoy) por sede, estado y especialidad. */
    @GetMapping("/daily-summary")
    DailySummaryResponse dailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return DailySummaryResponse.from(summaries.summary(date));
    }

    record ItemsResponse<T>(List<T> items) {
    }

    record ReminderResponse(Long appointmentId, String patientFirstNames, String patientEmail,
                            String professionalName, String specialtyName, String siteCode, String siteName,
                            String siteAddress, String date, String startTime, String endTime) {
        static ReminderResponse from(ReminderCandidate c) {
            return new ReminderResponse(c.appointmentId(), c.patientFirstNames(), c.patientEmail(),
                    c.professionalName(), c.specialtyName(), c.siteCode(), c.siteName(), c.siteAddress(),
                    c.startAt().toLocalDate().toString(), c.startAt().toLocalTime().toString(),
                    c.endAt().toLocalTime().toString());
        }
    }

    record ReminderMarkResponse(Long appointmentId, String date, String startTime, boolean alreadySent) {
        static ReminderMarkResponse from(ReminderMark mark) {
            return new ReminderMarkResponse(mark.appointmentId(), mark.startAt().toLocalDate().toString(),
                    mark.startAt().toLocalTime().toString(), mark.alreadySent());
        }
    }

    record DailySummaryResponse(String date, long total, Map<String, Long> byStatus, List<SiteResponse> sites,
                                List<SpecialtyResponse> specialties, long pendingRequests, long pendingReschedules) {
        static DailySummaryResponse from(DailySummary s) {
            return new DailySummaryResponse(s.date().toString(), s.total(), s.byStatus(),
                    s.sites().stream().map(site -> new SiteResponse(site.siteCode(), site.siteName(), site.total(),
                            site.byStatus())).toList(),
                    s.specialties().stream().map(sp -> new SpecialtyResponse(sp.specialtyName(), sp.total())).toList(),
                    s.pendingRequests(), s.pendingReschedules());
        }
    }

    record SiteResponse(String siteCode, String siteName, long total, Map<String, Long> byStatus) {
    }

    record SpecialtyResponse(String specialtyName, long total) {
    }
}

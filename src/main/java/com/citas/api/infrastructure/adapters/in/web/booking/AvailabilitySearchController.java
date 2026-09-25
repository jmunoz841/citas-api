package com.citas.api.infrastructure.adapters.in.web.booking;

import com.citas.api.application.port.in.SearchAvailabilityUseCase;
import com.citas.api.application.port.in.SearchAvailabilityUseCase.AppointmentType;
import com.citas.api.application.port.in.SearchAvailabilityUseCase.AvailableSlot;
import com.citas.api.application.port.in.SearchAvailabilityUseCase.SearchQuery;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Búsqueda de horarios reservables para el USER (HU-012). Solo devuelve inicios que pueden
 * completar la duración de la especialidad; nunca slots sueltos.
 */
@RestController
@RequestMapping("/api/v1/availability")
class AvailabilitySearchController {

    private final SearchAvailabilityUseCase search;

    AvailabilitySearchController(SearchAvailabilityUseCase search) {
        this.search = search;
    }

    @GetMapping
    ItemsResponse<AvailableSlotResponse> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String siteCode,
            @RequestParam(required = false) AppointmentType type,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) Long professionalId) {
        List<AvailableSlotResponse> items = search.search(
                        new SearchQuery(date, siteCode, type, specialtyId, professionalId)).stream()
                .map(AvailableSlotResponse::from).toList();
        return new ItemsResponse<>(items);
    }

    record AvailableSlotResponse(Long professionalId, String professionalName, Long specialtyId,
                                 String specialtyName, AppointmentType type, int durationMinutes, String siteCode,
                                 String date, String startTime, String endTime) {

        static AvailableSlotResponse from(AvailableSlot slot) {
            return new AvailableSlotResponse(slot.professionalId(), slot.professionalName(), slot.specialtyId(),
                    slot.specialtyName(), slot.type(), slot.durationMinutes(), slot.siteCode(),
                    slot.startAt().toLocalDate().toString(), slot.startAt().toLocalTime().toString(),
                    slot.endAt().toLocalTime().toString());
        }
    }

    record ItemsResponse<T>(List<T> items) {
    }
}

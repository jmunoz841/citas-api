package com.citas.api.application.service;

import com.citas.api.application.port.in.SearchAvailabilityUseCase;
import com.citas.api.application.port.out.AgendaQueryPort;
import com.citas.api.application.port.out.AgendaQueryPort.ProfessionalOffer;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.model.agenda.AgendaSlot;
import com.citas.api.domain.model.agenda.SlotPlanner;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Búsqueda de disponibilidad (HU-012).
 *
 * <p>La base filtra quién puede atender (profesional, especialidad y asociación activos) y
 * entrega los slots del día con su ocupación; el dominio decide qué inicios son reservables
 * según la duración de cada especialidad.</p>
 */
public class AvailabilitySearchService implements SearchAvailabilityUseCase {

    private final AgendaQueryPort agenda;
    private final Clock clock;

    public AvailabilitySearchService(AgendaQueryPort agenda, Clock clock) {
        this.agenda = agenda;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlot> search(SearchQuery query) {
        if (query.date() == null) {
            throw new InvalidFieldException("date", "La fecha es obligatoria");
        }
        Boolean general = query.type() == null ? null : query.type() == AppointmentType.GENERAL;
        List<ProfessionalOffer> offers = agenda.findOffers(query.specialtyId(), general, query.professionalId());
        if (offers.isEmpty()) {
            return List.of();
        }

        List<Long> professionalIds = offers.stream().map(ProfessionalOffer::professionalId).distinct().toList();
        String siteCode = query.siteCode() == null || query.siteCode().isBlank()
                ? null : query.siteCode().trim().toUpperCase();
        Map<Long, List<AgendaSlot>> slotsByProfessional = agenda.findSlots(professionalIds,
                        query.date().atStartOfDay(), query.date().plusDays(1).atStartOfDay(), siteCode)
                .stream().collect(Collectors.groupingBy(AgendaSlot::professionalId));

        LocalDateTime now = LocalDateTime.now(clock);
        return offers.stream()
                .flatMap(offer -> SlotPlanner.bookableStarts(
                                slotsByProfessional.getOrDefault(offer.professionalId(), List.of()),
                                offer.durationMinutes(), now).stream()
                        .map(start -> toAvailable(offer, start)))
                .sorted(Comparator.comparing(AvailableSlot::startAt)
                        .thenComparing(AvailableSlot::professionalName)
                        .thenComparing(AvailableSlot::specialtyName))
                .toList();
    }

    private static AvailableSlot toAvailable(ProfessionalOffer offer, AgendaSlot start) {
        return new AvailableSlot(offer.professionalId(), offer.professionalName(), offer.specialtyId(),
                offer.specialtyName(), offer.general() ? AppointmentType.GENERAL : AppointmentType.SPECIALIZED,
                offer.durationMinutes(), start.siteCode(), start.startAt(),
                start.startAt().plusMinutes(offer.durationMinutes()));
    }
}

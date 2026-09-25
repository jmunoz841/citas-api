package com.citas.api.domain.model.agenda;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Qué slots necesita una cita según la duración de su especialidad: 1 para 30 minutos y 2
 * consecutivos para 60 (RN-05). Los slots de una misma cita pertenecen al mismo bloque: dos
 * bloques contiguos no se combinan (supuesto de HU-012).
 */
public final class SlotPlanner {

    private SlotPlanner() {
    }

    public static int slotsNeeded(int durationMinutes) {
        return durationMinutes / AvailabilityBlock.SLOT_MINUTES;
    }

    /**
     * Primer slot de cada horario reservable: libre, posterior a {@code now} (RN-06) y seguido de
     * los slots libres que exija la duración dentro del mismo bloque (HU-012 CA-01, CA-02).
     */
    public static List<AgendaSlot> bookableStarts(List<AgendaSlot> slots, int durationMinutes, LocalDateTime now) {
        Map<Long, Map<LocalDateTime, AgendaSlot>> byBlock = indexByBlock(slots);
        return slots.stream()
                .filter(slot -> slot.startAt().isAfter(now))
                .filter(slot -> run(byBlock, slot, durationMinutes)
                        .filter(run -> run.stream().noneMatch(AgendaSlot::taken))
                        .isPresent())
                .sorted(Comparator.comparing(AgendaSlot::startAt))
                .toList();
    }

    /**
     * Slots que ocuparía una cita que empieza en {@code startAt}, o vacío si el horario no existe
     * completo en un mismo bloque. <b>No</b> mira si están ocupados: eso lo decide la clave
     * primaria de {@code slot_reservations} al insertar, que es segura ante concurrencia.
     */
    public static Optional<List<AgendaSlot>> allocate(List<AgendaSlot> slots, LocalDateTime startAt,
                                                      int durationMinutes) {
        return slots.stream()
                .filter(slot -> slot.startAt().equals(startAt))
                .findFirst()
                .flatMap(first -> run(indexByBlock(slots), first, durationMinutes));
    }

    private static Optional<List<AgendaSlot>> run(Map<Long, Map<LocalDateTime, AgendaSlot>> byBlock,
                                                  AgendaSlot first, int durationMinutes) {
        Map<LocalDateTime, AgendaSlot> block = byBlock.get(first.blockId());
        List<AgendaSlot> run = new ArrayList<>();
        for (int i = 0; i < slotsNeeded(durationMinutes); i++) {
            AgendaSlot next = block.get(first.startAt().plusMinutes((long) i * AvailabilityBlock.SLOT_MINUTES));
            if (next == null) {
                return Optional.empty();
            }
            run.add(next);
        }
        return Optional.of(List.copyOf(run));
    }

    private static Map<Long, Map<LocalDateTime, AgendaSlot>> indexByBlock(List<AgendaSlot> slots) {
        Map<Long, Map<LocalDateTime, AgendaSlot>> byBlock = new HashMap<>();
        for (AgendaSlot slot : slots) {
            byBlock.computeIfAbsent(slot.blockId(), id -> new HashMap<>()).put(slot.startAt(), slot);
        }
        return byBlock;
    }
}

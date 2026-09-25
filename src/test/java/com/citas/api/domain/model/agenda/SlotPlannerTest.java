package com.citas.api.domain.model.agenda;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reglas de slots 30/60 de HU-012 (RN-01, RN-05, RN-06) sin base de datos.
 */
class SlotPlannerTest {

    private static final LocalDate DIA = LocalDate.of(2030, 3, 4);
    private static final LocalDateTime ANTES = DIA.minusDays(1).atStartOfDay();

    @Test
    void ca01_treintaMinutosOfreceSoloLosSlotsLibres() {
        List<AgendaSlot> bloque = bloque(1L, "08:00", "10:00", Set.of("08:30"));

        assertThat(inicios(SlotPlanner.bookableStarts(bloque, 30, ANTES)))
                .containsExactly("08:00", "09:00", "09:30");
    }

    @Test
    void ca02_sesentaMinutosConEl0830OcupadoSoloOfrece0900() {
        List<AgendaSlot> bloque = bloque(1L, "08:00", "10:00", Set.of("08:30"));

        assertThat(inicios(SlotPlanner.bookableStarts(bloque, 60, ANTES))).containsExactly("09:00");
    }

    @Test
    void sesentaMinutosNoUsaElUltimoSlotDelBloque() {
        List<AgendaSlot> bloque = bloque(1L, "08:00", "09:30", Set.of());

        assertThat(inicios(SlotPlanner.bookableStarts(bloque, 60, ANTES))).containsExactly("08:00", "08:30");
    }

    @Test
    void dosBloquesContiguosNoSeCombinanParaUnaCitaDeSesenta() {
        List<AgendaSlot> slots = new ArrayList<>(bloque(1L, "08:00", "08:30", Set.of()));
        slots.addAll(bloque(2L, "08:30", "09:00", Set.of()));

        assertThat(SlotPlanner.bookableStarts(slots, 60, ANTES)).isEmpty();
        assertThat(SlotPlanner.allocate(slots, DIA.atTime(8, 0), 60)).isEmpty();
    }

    @Test
    void ca04_losHorariosPasadosNoSeOfrecen() {
        List<AgendaSlot> bloque = bloque(1L, "08:00", "10:00", Set.of());

        // A las 08:40 ya pasaron 08:00 y 08:30; tampoco se ofrece un inicio exactamente "ahora".
        assertThat(inicios(SlotPlanner.bookableStarts(bloque, 30, DIA.atTime(8, 40))))
                .containsExactly("09:00", "09:30");
        assertThat(inicios(SlotPlanner.bookableStarts(bloque, 30, DIA.atTime(9, 0))))
                .containsExactly("09:30");
    }

    @Test
    void allocateDevuelveLosSlotsConsecutivosAunqueEstenOcupados() {
        // La ocupación la decide la clave primaria al insertar, no el planificador.
        List<AgendaSlot> bloque = bloque(1L, "08:00", "10:00", Set.of("08:30"));

        assertThat(SlotPlanner.allocate(bloque, DIA.atTime(8, 0), 60))
                .hasValueSatisfying(run -> assertThat(inicios(run)).containsExactly("08:00", "08:30"));
    }

    @Test
    void allocateFallaSiElHorarioNoExiste() {
        List<AgendaSlot> bloque = bloque(1L, "08:00", "10:00", Set.of());

        assertThat(SlotPlanner.allocate(bloque, DIA.atTime(11, 0), 30)).isEmpty();
        assertThat(SlotPlanner.allocate(bloque, DIA.atTime(9, 30), 60)).isEmpty();
    }

    private static List<AgendaSlot> bloque(Long blockId, String desde, String hasta, Set<String> ocupados) {
        List<AgendaSlot> slots = new ArrayList<>();
        LocalDateTime fin = DIA.atTime(LocalTime.parse(hasta));
        long id = blockId * 100;
        for (LocalDateTime t = DIA.atTime(LocalTime.parse(desde)); t.isBefore(fin); t = t.plusMinutes(30)) {
            slots.add(new AgendaSlot(id++, blockId, 7L, "HIC", t, ocupados.contains(t.toLocalTime().toString())));
        }
        return slots;
    }

    private static List<String> inicios(List<AgendaSlot> slots) {
        return slots.stream().map(slot -> slot.startAt().toLocalTime().toString()).toList();
    }
}

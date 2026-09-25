package com.citas.api.application.service;

import com.citas.api.application.port.in.ManageAvailabilityUseCase;
import com.citas.api.application.port.out.AvailabilityRepositoryPort;
import com.citas.api.application.port.out.ProfessionalRepositoryPort;
import com.citas.api.domain.exception.BusinessConflictException;
import com.citas.api.domain.exception.InvalidFieldException;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.agenda.AvailabilityBlock;
import com.citas.api.domain.model.professional.Professional;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bloques de disponibilidad (HU-010).
 *
 * <p>Aquí viven las reglas que MySQL no puede expresar: no publicar en el pasado y que el
 * profesional esté activo. El solapamiento y la pertenencia a la sede los garantiza además
 * la base, con un índice único y una clave foránea compuesta.</p>
 */
public class AvailabilityService implements ManageAvailabilityUseCase {

    private final AvailabilityRepositoryPort blocks;
    private final ProfessionalRepositoryPort professionals;
    private final Clock clock;

    public AvailabilityService(AvailabilityRepositoryPort blocks, ProfessionalRepositoryPort professionals,
                               Clock clock) {
        this.blocks = blocks;
        this.professionals = professionals;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AvailabilityBlock create(Long professionalId, BlockCommand command) {
        Professional professional = requireActiveProfessional(professionalId);
        AvailabilityBlock block = toBlock(professionalId, command);
        validate(professional, block, null);
        return blocks.save(block);
    }

    @Override
    @Transactional
    public AvailabilityBlock update(Long professionalId, Long blockId, BlockCommand command) {
        Professional professional = requireActiveProfessional(professionalId);
        AvailabilityBlock current = requireOwnBlock(professionalId, blockId);
        requireNoCommittedSlots(current);

        AvailabilityBlock updated = current.withSchedule(command.siteCode(),
                LocalDateTime.of(command.date(), command.startTime()),
                LocalDateTime.of(command.date(), command.endTime()));
        validate(professional, updated, blockId);
        return blocks.save(updated);
    }

    @Override
    @Transactional
    public void delete(Long professionalId, Long blockId) {
        AvailabilityBlock block = requireOwnBlock(professionalId, blockId);
        requireNoCommittedSlots(block);
        blocks.deleteById(blockId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityBlock> listOwn(Long professionalId, LocalDate date, String siteCode) {
        return blocks.findByProfessional(professionalId, date, siteCode);
    }

    private AvailabilityBlock toBlock(Long professionalId, BlockCommand command) {
        if (command.date() == null || command.startTime() == null || command.endTime() == null) {
            throw new InvalidFieldException("date", "Fecha, hora de inicio y hora de fin son obligatorias");
        }
        return AvailabilityBlock.createNew(professionalId, command.siteCode(),
                LocalDateTime.of(command.date(), command.startTime()),
                LocalDateTime.of(command.date(), command.endTime()));
    }

    private void validate(Professional professional, AvailabilityBlock block, Long excludedBlockId) {
        if (block.startsBefore(LocalDateTime.now(clock))) {
            throw new InvalidFieldException("startTime", "No se puede publicar disponibilidad en el pasado");
        }
        if (!professional.getAssignments().siteCodes().contains(block.getSiteCode())) {
            throw new InvalidFieldException("siteCode", "No estás asignado a esa sede");
        }
        if (blocks.overlaps(professional.getUserId(), block, excludedBlockId)) {
            throw new InvalidFieldException("startTime", "El bloque se cruza con otro que ya publicaste");
        }
    }

    private Professional requireActiveProfessional(Long professionalId) {
        Professional professional = professionals.findByUserId(professionalId)
                .orElseThrow(ResourceNotFoundException::professional);
        if (!professional.isActive()) {
            throw new InvalidFieldException("professional", "Tu cuenta profesional está inactiva");
        }
        return professional;
    }

    /** Un bloque ajeno se trata como inexistente: no se revela que existe (CA-06). */
    private AvailabilityBlock requireOwnBlock(Long professionalId, Long blockId) {
        AvailabilityBlock block = blocks.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("El bloque no existe"));
        if (!block.getProfessionalId().equals(professionalId)) {
            throw new ResourceNotFoundException("El bloque no existe");
        }
        return block;
    }

    /**
     * CA-05: un bloque con slots reservados o retenidos no se edita ni se elimina, porque
     * regenerar o borrar sus slots dejaría citas sin horario. Si una reserva se cuela entre esta
     * comprobación y el borrado, la FK RESTRICT {@code fk_sr_slot} lo impide en la base.
     */
    private void requireNoCommittedSlots(AvailabilityBlock block) {
        if (blocks.hasOccupiedSlots(block.getId())) {
            throw BusinessConflictException.blockHasAppointments();
        }
    }
}

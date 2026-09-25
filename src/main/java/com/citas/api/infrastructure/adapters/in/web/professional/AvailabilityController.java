package com.citas.api.infrastructure.adapters.in.web.professional;

import com.citas.api.application.port.in.ManageAvailabilityUseCase;
import com.citas.api.application.port.in.ManageAvailabilityUseCase.BlockCommand;
import com.citas.api.domain.model.agenda.AvailabilityBlock;
import com.citas.api.infrastructure.adapters.out.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Bloques de disponibilidad del profesional autenticado (HU-010).
 *
 * <p>El identificador del profesional sale siempre del access token, nunca del cuerpo ni de la
 * ruta: así nadie puede tocar la agenda de otro (CA-06).</p>
 *
 * <p>Fechas como {@code YYYY-MM-DD} y horas como {@code HH:mm}, en hora local de Bogotá.</p>
 */
@RestController
@RequestMapping("/api/v1/professional/availability-blocks")
class AvailabilityController {

    private final ManageAvailabilityUseCase availability;

    AvailabilityController(ManageAvailabilityUseCase availability) {
        this.availability = availability;
    }

    @GetMapping
    ItemsResponse<BlockResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam(required = false) String siteCode) {
        List<BlockResponse> items = availability.listOwn(user.userId(), date, siteCode).stream()
                .map(BlockResponse::from).toList();
        return new ItemsResponse<>(items);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BlockResponse create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody BlockRequest request) {
        return BlockResponse.from(availability.create(user.userId(), request.toCommand()));
    }

    @PatchMapping("/{id}")
    BlockResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                         @Valid @RequestBody BlockRequest request) {
        return BlockResponse.from(availability.update(user.userId(), id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        availability.delete(user.userId(), id);
    }

    record BlockRequest(
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @NotBlank @Size(max = 10) String siteCode) {

        BlockCommand toCommand() {
            return new BlockCommand(date, startTime, endTime, siteCode);
        }
    }

    record BlockResponse(Long id, String siteCode, String date, String startTime, String endTime, int slots) {

        static BlockResponse from(AvailabilityBlock block) {
            return new BlockResponse(block.getId(), block.getSiteCode(),
                    block.getStartAt().toLocalDate().toString(),
                    block.getStartAt().toLocalTime().toString(),
                    block.getEndAt().toLocalTime().toString(),
                    block.slotStarts().size());
        }
    }

    record ItemsResponse<T>(List<T> items) {
    }
}

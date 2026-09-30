package com.citas.api.domain.model.appointment;
import java.time.LocalDateTime;
public record AppointmentHistoryEntry(AppointmentStatus status, String source, Long actorUserId, LocalDateTime changedAt, String reason) { }

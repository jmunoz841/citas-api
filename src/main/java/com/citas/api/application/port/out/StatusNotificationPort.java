package com.citas.api.application.port.out;

import com.citas.api.domain.model.integration.StatusNotification;

/**
 * Notificación saliente de cambios de estado (HU-024). El adaptador la entrega <b>después</b> de que
 * la transacción se confirme y nunca lanza: si el destino falla, solo registra el error sin datos
 * personales (CA-02).
 */
public interface StatusNotificationPort {

    void publish(StatusNotification notification);
}

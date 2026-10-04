package com.citas.api.application.port.in;

public interface RequestPasswordResetUseCase {
    void request(RequestPasswordResetCommand command);

    record RequestPasswordResetCommand(String email) { }
}

package com.citas.api.application.port.in;

public interface ResetPasswordUseCase {
    void reset(ResetPasswordCommand command);

    record ResetPasswordCommand(String token, String password) {
        @Override public String toString() { return "ResetPasswordCommand[token=***, password=***]"; }
    }
}

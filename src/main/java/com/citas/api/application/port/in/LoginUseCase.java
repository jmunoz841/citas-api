package com.citas.api.application.port.in;

public interface LoginUseCase {

    AuthTokens login(LoginCommand command);

    record LoginCommand(String email, String password) {

        @Override
        public String toString() {
            return "LoginCommand[email=" + email + ", password=***]";
        }
    }
}

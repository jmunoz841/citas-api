package com.citas.api.application.port.in;

import com.citas.api.domain.model.user.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);

    record RegisterUserCommand(String firstNames, String lastNames, String documentType, String documentNumber,
                               String email, String phone, String password) {

        @Override
        public String toString() {
            return "RegisterUserCommand[email=" + email + ", documentType=" + documentType + ", password=***]";
        }
    }
}

package com.citas.api.application.port.in;

import com.citas.api.domain.model.user.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);

    /**
     * {@code insurancePlanId} y {@code regimeCode} son opcionales pero van en pareja: la
     * afiliación inicial solo se crea si llegan ambos (HU-004, CA-06).
     */
    record RegisterUserCommand(String firstNames, String lastNames, String documentType, String documentNumber,
                               String email, String phone, String password, Long insurancePlanId,
                               String regimeCode) {

        /** Registro sin afiliación. */
        public RegisterUserCommand(String firstNames, String lastNames, String documentType, String documentNumber,
                                   String email, String phone, String password) {
            this(firstNames, lastNames, documentType, documentNumber, email, phone, password, null, null);
        }

        @Override
        public String toString() {
            return "RegisterUserCommand[email=" + email + ", documentType=" + documentType + ", password=***]";
        }
    }
}

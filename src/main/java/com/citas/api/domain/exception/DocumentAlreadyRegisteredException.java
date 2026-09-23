package com.citas.api.domain.exception;

public class DocumentAlreadyRegisteredException extends DomainException {

    public DocumentAlreadyRegisteredException() {
        super("DOCUMENT_ALREADY_REGISTERED", "El documento ya está registrado");
    }
}

package com.citas.api.application.port.in;

public interface ManageOwnProfileUseCase {
    Profile view(Long userId);
    Profile update(Long userId, UpdateProfileCommand command);

    record UpdateProfileCommand(String firstNames, String lastNames, String phone) { }
    record Profile(Long id, String firstNames, String lastNames, String documentType, String documentNumber,
                   String email, String phone) { }
}

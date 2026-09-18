package com.citas.api.application.port.out;

import com.citas.api.domain.model.user.Email;
import com.citas.api.domain.model.user.IdentityDocument;
import com.citas.api.domain.model.user.User;

import java.util.Optional;

public interface UserRepositoryPort {

    boolean existsByEmail(Email email);

    boolean existsByDocument(IdentityDocument document);

    /**
     * Persiste el usuario. Si una restricción única se viola por concurrencia, lanza
     * {@code EmailAlreadyRegisteredException} o {@code DocumentAlreadyRegisteredException}.
     */
    User save(User user);

    Optional<User> findByEmail(Email email);

    Optional<User> findById(Long id);
}

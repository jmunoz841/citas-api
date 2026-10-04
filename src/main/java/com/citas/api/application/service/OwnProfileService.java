package com.citas.api.application.service;

import com.citas.api.application.port.in.ManageOwnProfileUseCase;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.domain.exception.ResourceNotFoundException;
import com.citas.api.domain.model.user.User;
import org.springframework.transaction.annotation.Transactional;

public class OwnProfileService implements ManageOwnProfileUseCase {
    private final UserRepositoryPort users;
    public OwnProfileService(UserRepositoryPort users) { this.users = users; }
    @Override @Transactional(readOnly = true)
    public Profile view(Long userId) { return toProfile(find(userId)); }
    @Override @Transactional
    public Profile update(Long userId, UpdateProfileCommand command) {
        User saved = users.save(find(userId).withProfile(command.firstNames(), command.lastNames(), command.phone()));
        return toProfile(saved);
    }
    private User find(Long userId) { return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("El usuario no existe")); }
    private static Profile toProfile(User user) { return new Profile(user.getId(), user.getFirstNames(), user.getLastNames(),
            user.getDocument().type().name(), user.getDocument().number(), user.getEmail().value(), user.getPhone()); }
}

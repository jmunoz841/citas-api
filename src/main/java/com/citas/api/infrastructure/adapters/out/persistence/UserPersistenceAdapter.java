package com.citas.api.infrastructure.adapters.out.persistence;

import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.domain.exception.DocumentAlreadyRegisteredException;
import com.citas.api.domain.exception.EmailAlreadyRegisteredException;
import com.citas.api.domain.model.user.DocumentType;
import com.citas.api.domain.model.user.Email;
import com.citas.api.domain.model.user.IdentityDocument;
import com.citas.api.domain.model.user.Role;
import com.citas.api.domain.model.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }

    @Override
    public boolean existsByDocument(IdentityDocument document) {
        return repository.existsByDocumentTypeCodeAndDocumentNumber(document.type().name(), document.number());
    }

    @Override
    public User save(User user) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            // Registro concurrente: la restricción única de MySQL es la última defensa (CA-03, CA-04).
            String detail = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (detail.contains("uk_users_email")) {
                throw new EmailAlreadyRegisteredException();
            }
            if (detail.contains("uk_users_document")) {
                throw new DocumentAlreadyRegisteredException();
            }
            throw e;
        }
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserPersistenceAdapter::toDomain);
    }

    private static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.getId(), user.getFirstNames(), user.getLastNames(),
                user.getDocument().type().name(), user.getDocument().number(), user.getEmail().value(),
                user.getPhone(), user.getPasswordHash(), user.isActive(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet()));
    }

    private static User toDomain(UserJpaEntity entity) {
        return User.restore(entity.getId(), entity.getFirstNames(), entity.getLastNames(),
                new IdentityDocument(DocumentType.valueOf(entity.getDocumentTypeCode()), entity.getDocumentNumber()),
                new Email(entity.getEmail()), entity.getPhone(), entity.getPasswordHash(), entity.isActive(),
                entity.getRoleCodes().stream().map(Role::valueOf).collect(Collectors.toSet()));
    }
}

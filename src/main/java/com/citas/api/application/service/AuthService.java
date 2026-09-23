package com.citas.api.application.service;

import com.citas.api.application.port.in.AuthTokens;
import com.citas.api.application.port.in.LoginUseCase;
import com.citas.api.application.port.in.LogoutUseCase;
import com.citas.api.application.port.in.RefreshSessionUseCase;
import com.citas.api.application.port.in.RegisterUserUseCase;
import com.citas.api.application.port.out.PasswordHasherPort;
import com.citas.api.application.port.out.RefreshTokenRepositoryPort;
import com.citas.api.application.port.out.TokenProviderPort;
import com.citas.api.application.port.out.TokenProviderPort.IssuedToken;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.domain.exception.DocumentAlreadyRegisteredException;
import com.citas.api.domain.exception.EmailAlreadyRegisteredException;
import com.citas.api.domain.exception.InvalidCredentialsException;
import com.citas.api.domain.exception.InvalidRefreshTokenException;
import com.citas.api.domain.model.auth.RefreshToken;
import com.citas.api.domain.model.user.DocumentType;
import com.citas.api.domain.model.user.Email;
import com.citas.api.domain.model.user.IdentityDocument;
import com.citas.api.domain.model.user.PasswordPolicy;
import com.citas.api.domain.model.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Casos de uso de HU-001: registro, login, refresh con rotación y logout.
 */
public class AuthService implements RegisterUserUseCase, LoginUseCase, RefreshSessionUseCase, LogoutUseCase {

    private final UserRepositoryPort users;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final PasswordHasherPort passwordHasher;
    private final TokenProviderPort tokenProvider;
    private final Clock clock;
    private final String timingDummyHash;

    public AuthService(UserRepositoryPort users, RefreshTokenRepositoryPort refreshTokens,
                       PasswordHasherPort passwordHasher, TokenProviderPort tokenProvider, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.clock = clock;
        // Se compara contra este hash cuando el email no existe, para no revelar su existencia por tiempo de respuesta.
        this.timingDummyHash = passwordHasher.hash("timing-dummy-password-1");
    }

    @Override
    @Transactional
    public User register(RegisterUserCommand command) {
        PasswordPolicy.validate(command.password());
        Email email = new Email(command.email());
        IdentityDocument document = new IdentityDocument(DocumentType.fromCode(command.documentType()),
                command.documentNumber());

        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        if (users.existsByDocument(document)) {
            throw new DocumentAlreadyRegisteredException();
        }

        User user = User.registerNew(command.firstNames(), command.lastNames(), document, email, command.phone(),
                passwordHasher.hash(command.password()));
        return users.save(user);
    }

    @Override
    @Transactional
    public AuthTokens login(LoginCommand command) {
        Email email = new Email(command.email());
        String rawPassword = command.password() == null ? "" : command.password();

        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            passwordHasher.matches(rawPassword, timingDummyHash);
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        boolean passwordOk = passwordHasher.matches(rawPassword, user.getPasswordHash());
        if (!passwordOk || !user.isActive()) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user).tokens();
    }

    @Override
    @Transactional
    public AuthTokens refresh(String refreshToken) {
        Long userId = tokenProvider.parseRefreshToken(refreshToken).orElseThrow(InvalidRefreshTokenException::new);
        RefreshToken stored = refreshTokens.findByHashForUpdate(tokenProvider.hash(refreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        LocalDateTime now = LocalDateTime.now(clock);
        if (!stored.getUserId().equals(userId) || !stored.isUsableAt(now)) {
            throw new InvalidRefreshTokenException();
        }
        User user = users.findById(userId).filter(User::isActive).orElseThrow(InvalidRefreshTokenException::new);

        Issued issued = issueTokens(user);
        stored.rotateTo(issued.refreshTokenId(), now);
        refreshTokens.save(stored);
        return issued.tokens();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (tokenProvider.parseRefreshToken(refreshToken).isEmpty()) {
            return;
        }
        refreshTokens.findByHashForUpdate(tokenProvider.hash(refreshToken)).ifPresent(token -> {
            token.revoke(LocalDateTime.now(clock));
            refreshTokens.save(token);
        });
    }

    private Issued issueTokens(User user) {
        Instant now = clock.instant();
        IssuedToken access = tokenProvider.issueAccessToken(user, now);
        IssuedToken refresh = tokenProvider.issueRefreshToken(user.getId(), now);

        RefreshToken saved = refreshTokens.save(RefreshToken.issue(user.getId(), tokenProvider.hash(refresh.value()),
                toLocal(now), toLocal(refresh.expiresAt())));

        AuthTokens tokens = new AuthTokens(access.value(), secondsBetween(now, access.expiresAt()),
                refresh.value(), secondsBetween(now, refresh.expiresAt()));
        return new Issued(tokens, saved.getId());
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, clock.getZone());
    }

    private static long secondsBetween(Instant from, Instant to) {
        return Duration.between(from, to).toSeconds();
    }

    private record Issued(AuthTokens tokens, Long refreshTokenId) {
    }
}

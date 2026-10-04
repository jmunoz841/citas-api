package com.citas.api.application.service;

import com.citas.api.application.port.in.RequestPasswordResetUseCase;
import com.citas.api.application.port.in.ResetPasswordUseCase;
import com.citas.api.application.port.out.PasswordHasherPort;
import com.citas.api.application.port.out.PasswordResetTokenGeneratorPort;
import com.citas.api.application.port.out.PasswordResetTokenRepositoryPort;
import com.citas.api.application.port.out.TokenProviderPort;
import com.citas.api.application.port.out.UserRepositoryPort;
import com.citas.api.domain.exception.InvalidPasswordResetTokenException;
import com.citas.api.domain.model.auth.PasswordResetToken;
import com.citas.api.domain.model.user.Email;
import com.citas.api.domain.model.user.PasswordPolicy;
import com.citas.api.domain.model.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Casos de uso HU-002. El token claro solo existe mientras se calcula su hash; nunca se registra ni se retorna. */
public class PasswordRecoveryService implements RequestPasswordResetUseCase, ResetPasswordUseCase {
    private static final int TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepositoryPort users;
    private final PasswordResetTokenRepositoryPort resetTokens;
    private final PasswordResetTokenGeneratorPort tokenGenerator;
    private final TokenProviderPort tokenProvider;
    private final PasswordHasherPort passwordHasher;
    private final Clock clock;

    public PasswordRecoveryService(UserRepositoryPort users, PasswordResetTokenRepositoryPort resetTokens,
                                   PasswordResetTokenGeneratorPort tokenGenerator, TokenProviderPort tokenProvider,
                                   PasswordHasherPort passwordHasher, Clock clock) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.tokenGenerator = tokenGenerator;
        this.tokenProvider = tokenProvider;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void request(RequestPasswordResetCommand command) {
        Email email = new Email(command.email());
        users.findByEmail(email).filter(User::isActive).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now(clock);
            resetTokens.revokeActiveByUserId(user.getId(), now);
            String rawToken = tokenGenerator.generate();
            resetTokens.save(PasswordResetToken.issue(user.getId(), tokenProvider.hash(rawToken), now,
                    now.plusMinutes(TOKEN_VALIDITY_MINUTES)));
        });
    }

    @Override
    @Transactional
    public void reset(ResetPasswordCommand command) {
        PasswordPolicy.validate(command.password());
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken token = resetTokens.findByHashForUpdate(tokenProvider.hash(command.token()))
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        User user = users.findById(token.getUserId()).filter(User::isActive)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        token.consume(now);
        resetTokens.save(token);
        users.save(user.withPasswordHash(passwordHasher.hash(command.password())));
    }
}

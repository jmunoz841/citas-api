package com.citas.api.infrastructure.adapters.out.security;

import com.citas.api.application.port.out.PasswordResetTokenGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** Genera secretos opacos URL-safe; el valor claro nunca se persiste ni se registra. */
@Component
class SecurePasswordResetTokenGenerator implements PasswordResetTokenGeneratorPort {
    private final SecureRandom random = new SecureRandom();
    @Override public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

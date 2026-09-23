package com.citas.api.infrastructure.adapters.out.security;

import com.citas.api.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        try {
            return encoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            // BCrypt rechaza contraseñas de más de 72 bytes: se trata como credencial incorrecta.
            return false;
        }
    }
}

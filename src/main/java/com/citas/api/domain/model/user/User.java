package com.citas.api.domain.model.user;

import com.citas.api.domain.exception.InvalidFieldException;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cuenta del sistema. Solo conoce el hash de la contraseña, nunca el valor en claro.
 */
public final class User {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;

    private final Long id;
    private final String firstNames;
    private final String lastNames;
    private final IdentityDocument document;
    private final Email email;
    private final String phone;
    private final String passwordHash;
    private final boolean active;
    private final Set<Role> roles;

    private User(Long id, String firstNames, String lastNames, IdentityDocument document, Email email,
                 String phone, String passwordHash, boolean active, Set<Role> roles) {
        this.id = id;
        this.firstNames = requireText("firstNames", firstNames, MAX_NAME_LENGTH);
        this.lastNames = requireText("lastNames", lastNames, MAX_NAME_LENGTH);
        this.document = document;
        this.email = email;
        this.phone = requireText("phone", phone, MAX_PHONE_LENGTH);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash es obligatorio");
        }
        this.passwordHash = passwordHash;
        this.active = active;
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Un usuario debe tener al menos un rol");
        }
        this.roles = Set.copyOf(roles);
    }

    /** Registro público: siempre rol USER y cuenta activa (HU-001). */
    public static User registerNew(String firstNames, String lastNames, IdentityDocument document, Email email,
                                   String phone, String passwordHash) {
        return new User(null, firstNames, lastNames, document, email, phone, passwordHash, true,
                EnumSet.of(Role.USER));
    }

    /** Alta de un profesional por un ADMIN: rol PROFESSIONAL y cuenta activa (HU-008). */
    public static User createProfessional(String firstNames, String lastNames, IdentityDocument document, Email email,
                                          String phone, String passwordHash) {
        return new User(null, firstNames, lastNames, document, email, phone, passwordHash, true,
                EnumSet.of(Role.PROFESSIONAL));
    }

    public static User restore(Long id, String firstNames, String lastNames, IdentityDocument document, Email email,
                               String phone, String passwordHash, boolean active, Set<Role> roles) {
        return new User(id, firstNames, lastNames, document, email, phone, passwordHash, active, roles);
    }

    private static String requireText(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException(field, "El campo es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidFieldException(field, "El campo supera " + maxLength + " caracteres");
        }
        return trimmed;
    }

    public Long getId() { return id; }
    public String getFirstNames() { return firstNames; }
    public String getLastNames() { return lastNames; }
    public IdentityDocument getDocument() { return document; }
    public Email getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public Set<Role> getRoles() { return roles; }
}

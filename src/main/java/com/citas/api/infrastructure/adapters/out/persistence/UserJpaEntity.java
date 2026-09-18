package com.citas.api.infrastructure.adapters.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Tabla {@code users} (V1). {@code created_at}/{@code updated_at} los gestiona MySQL y no se mapean.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_names", nullable = false, length = 100)
    private String firstNames;

    @Column(name = "last_names", nullable = false, length = 100)
    private String lastNames;

    @Column(name = "document_type_code", nullable = false, length = 10)
    private String documentTypeCode;

    @Column(name = "document_number", nullable = false, length = 30)
    private String documentNumber;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_code", nullable = false, length = 20)
    private Set<String> roleCodes = new HashSet<>();

    protected UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String firstNames, String lastNames, String documentTypeCode, String documentNumber,
                         String email, String phone, String passwordHash, boolean active, Set<String> roleCodes) {
        this.id = id;
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.documentTypeCode = documentTypeCode;
        this.documentNumber = documentNumber;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.active = active;
        this.roleCodes = new HashSet<>(roleCodes);
    }

    public Long getId() { return id; }
    public String getFirstNames() { return firstNames; }
    public String getLastNames() { return lastNames; }
    public String getDocumentTypeCode() { return documentTypeCode; }
    public String getDocumentNumber() { return documentNumber; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public Set<String> getRoleCodes() { return roleCodes; }
}

package com.citas.api.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByDocumentTypeCodeAndDocumentNumber(String documentTypeCode, String documentNumber);

    Optional<UserJpaEntity> findByEmail(String email);
}

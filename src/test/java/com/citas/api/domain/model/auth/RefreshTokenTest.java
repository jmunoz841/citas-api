package com.citas.api.domain.model.auth;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final LocalDateTime ISSUED = LocalDateTime.of(2026, 9, 18, 10, 0);

    @Test
    void esUsableAntesDeExpirarYSinRevocar() {
        RefreshToken token = RefreshToken.issue(1L, "hash", ISSUED, ISSUED.plusDays(7));

        assertThat(token.isUsableAt(ISSUED.plusDays(6))).isTrue();
        assertThat(token.isUsableAt(ISSUED.plusDays(7))).isFalse();
    }

    @Test
    void rotarLoRevocaYEnlazaConSuReemplazo() {
        RefreshToken token = RefreshToken.issue(1L, "hash", ISSUED, ISSUED.plusDays(7));

        token.rotateTo(99L, ISSUED.plusHours(1));

        assertThat(token.isUsableAt(ISSUED.plusHours(2))).isFalse();
        assertThat(token.getRevokedAt()).isEqualTo(ISSUED.plusHours(1));
        assertThat(token.getReplacedByTokenId()).isEqualTo(99L);
    }

    @Test
    void revocarDosVecesConservaLaPrimeraFecha() {
        RefreshToken token = RefreshToken.issue(1L, "hash", ISSUED, ISSUED.plusDays(7));

        token.revoke(ISSUED.plusHours(1));
        token.revoke(ISSUED.plusHours(5));

        assertThat(token.getRevokedAt()).isEqualTo(ISSUED.plusHours(1));
    }
}

package com.citas.api.domain.model.user;

import com.citas.api.domain.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Segura123", "abcdefg1", "1234567a", "ñandú2026"})
    void aceptaContrasenasQueCumplenLaPolitica(String password) {
        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Abc123", "solotexto", "12345678"})
    void rechazaContrasenasQueNoCumplen(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("field").isEqualTo("password");
    }

    @Test
    void rechazaMasDe72Bytes() {
        assertThatThrownBy(() -> PasswordPolicy.validate("a1".repeat(37)))
                .isInstanceOf(InvalidFieldException.class);
    }
}

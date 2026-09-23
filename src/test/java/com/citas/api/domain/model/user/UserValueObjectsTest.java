package com.citas.api.domain.model.user;

import com.citas.api.domain.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserValueObjectsTest {

    @Test
    void emailSeNormalizaAMinusculasSinEspacios() {
        assertThat(new Email("  Ana.Perez@Example.COM ").value()).isEqualTo("ana.perez@example.com");
    }

    @Test
    void emailConFormatoInvalidoSeRechaza() {
        assertThatThrownBy(() -> new Email("no-es-email")).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void numeroDeDocumentoSeNormaliza() {
        IdentityDocument document = new IdentityDocument(DocumentType.CC, " 1.234.567-8 ");
        assertThat(document.number()).isEqualTo("12345678");
        assertThat(new IdentityDocument(DocumentType.PA, "ab 123").number()).isEqualTo("AB123");
    }

    @Test
    void tipoDeDocumentoDesconocidoSeRechaza() {
        assertThatThrownBy(() -> DocumentType.fromCode("XX"))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("field").isEqualTo("documentType");
        assertThat(DocumentType.fromCode("cc")).isEqualTo(DocumentType.CC);
    }

    @Test
    void registroNuevoSiempreEsUserActivo() {
        User user = User.registerNew(" Ana ", "Pérez", new IdentityDocument(DocumentType.CC, "1234"),
                new Email("ana@example.com"), "3001234567", "$2a$10$hash");

        assertThat(user.getRoles()).containsExactly(Role.USER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getFirstNames()).isEqualTo("Ana");
    }

    @Test
    void nombreVacioSeRechaza() {
        assertThatThrownBy(() -> User.registerNew(" ", "Pérez", new IdentityDocument(DocumentType.CC, "1234"),
                new Email("ana@example.com"), "3001234567", "$2a$10$hash"))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("field").isEqualTo("firstNames");
    }
}

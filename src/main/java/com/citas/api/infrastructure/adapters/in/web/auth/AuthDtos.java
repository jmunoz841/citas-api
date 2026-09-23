package com.citas.api.infrastructure.adapters.in.web.auth;

import com.citas.api.application.port.in.AuthTokens;
import com.citas.api.domain.model.user.Role;
import com.citas.api.domain.model.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Contrato REST de autenticación (ver docs/contratos/autenticacion.md).
 */
final class AuthDtos {

    private AuthDtos() {
    }

    record RegisterRequest(
            @NotBlank @Size(max = 100) String firstNames,
            @NotBlank @Size(max = 100) String lastNames,
            @NotBlank @Size(max = 10) String documentType,
            @NotBlank @Size(max = 30) String documentNumber,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[0-9+()\\- ]{7,20}$", message = "debe tener entre 7 y 20 dígitos o símbolos + ( ) -")
            String phone,
            @NotBlank String password,
            // Afiliación opcional (HU-004): si llega una, deben llegar las dos.
            Long insurancePlanId,
            @Size(max = 20) String regimeCode) {

        @Override
        public String toString() {
            return "RegisterRequest[email=" + email + ", password=***]";
        }
    }

    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

        @Override
        public String toString() {
            return "LoginRequest[email=" + email + ", password=***]";
        }
    }

    record RefreshTokenRequest(@NotBlank String refreshToken) {

        @Override
        public String toString() {
            return "RefreshTokenRequest[refreshToken=***]";
        }
    }

    record UserResponse(Long id, String firstNames, String lastNames, String documentType, String documentNumber,
                        String email, String phone, List<String> roles) {

        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getFirstNames(), user.getLastNames(),
                    user.getDocument().type().name(), user.getDocument().number(), user.getEmail().value(),
                    user.getPhone(), user.getRoles().stream().map(Role::name).sorted().toList());
        }
    }

    record TokenResponse(String tokenType, String accessToken, long expiresIn, String refreshToken,
                         long refreshExpiresIn) {

        static TokenResponse from(AuthTokens tokens) {
            return new TokenResponse("Bearer", tokens.accessToken(), tokens.accessExpiresInSeconds(),
                    tokens.refreshToken(), tokens.refreshExpiresInSeconds());
        }

        @Override
        public String toString() {
            return "TokenResponse[tokenType=" + tokenType + ", accessToken=***, refreshToken=***]";
        }
    }

    record SessionResponse(Long userId, String email, List<String> roles) {
    }
}

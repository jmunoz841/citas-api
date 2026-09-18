package com.citas.api.infrastructure.adapters.out.security;

import java.util.Set;

/**
 * Principal de Spring Security construido a partir de un access token válido.
 */
public record AuthenticatedUser(Long userId, String email, Set<String> roles) {
}

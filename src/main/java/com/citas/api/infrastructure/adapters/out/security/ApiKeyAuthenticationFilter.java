package com.citas.api.infrastructure.adapters.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Credencial de servicio de n8n (D-042). Solo en {@code /api/v1/integrations/**}: si la petición trae
 * {@code X-Api-Key} igual a {@code INTEGRATION_API_KEY}, queda autenticada con el único rol
 * {@code INTEGRATION}. En cualquier otra ruta la cabecera se ignora, así que la clave no abre el resto
 * de la API. Sin clave configurada, la integración queda deshabilitada (401).
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    public static final String PATH_PREFIX = "/api/v1/integrations/";
    public static final String ROLE = "INTEGRATION";

    private final byte[] expectedKey;

    public ApiKeyAuthenticationFilter(String apiKey) {
        this.expectedKey = apiKey == null ? new byte[0] : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        // Comparación en tiempo constante: no revela por tiempos cuántos caracteres coinciden.
        if (expectedKey.length > 0 && provided != null
                && MessageDigest.isEqual(expectedKey, provided.trim().getBytes(StandardCharsets.UTF_8))) {
            var authentication = new UsernamePasswordAuthenticationToken("n8n-integration", null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}

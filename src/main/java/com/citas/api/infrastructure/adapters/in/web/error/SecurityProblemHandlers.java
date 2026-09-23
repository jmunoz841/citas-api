package com.citas.api.infrastructure.adapters.in.web.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuestas 401/403 de Spring Security con el mismo formato ProblemDetail que el resto de la API.
 */
@Component
public class SecurityProblemHandlers {

    private final ObjectMapper objectMapper;

    SecurityProblemHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> write(response,
                ApiProblems.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Se requiere un access token válido"));
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> write(response,
                ApiProblems.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acceso denegado"));
    }

    private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}

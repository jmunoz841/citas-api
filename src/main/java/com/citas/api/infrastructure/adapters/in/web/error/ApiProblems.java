package com.citas.api.infrastructure.adapters.in.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.Map;

/**
 * Formato uniforme de error (RFC 9457 ProblemDetail) con {@code code} estable y {@code errors} por campo.
 */
public final class ApiProblems {

    private ApiProblems() {
    }

    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }

    public static ProblemDetail validation(List<Map<String, String>> errors) {
        ProblemDetail problem = of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }
}

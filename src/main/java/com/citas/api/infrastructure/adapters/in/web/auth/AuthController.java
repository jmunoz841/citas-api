package com.citas.api.infrastructure.adapters.in.web.auth;

import com.citas.api.application.port.in.LoginUseCase;
import com.citas.api.application.port.in.LoginUseCase.LoginCommand;
import com.citas.api.application.port.in.LogoutUseCase;
import com.citas.api.application.port.in.RefreshSessionUseCase;
import com.citas.api.application.port.in.RegisterUserUseCase;
import com.citas.api.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.citas.api.domain.model.user.User;
import com.citas.api.infrastructure.adapters.in.web.auth.AuthDtos.LoginRequest;
import com.citas.api.infrastructure.adapters.in.web.auth.AuthDtos.RefreshTokenRequest;
import com.citas.api.infrastructure.adapters.in.web.auth.AuthDtos.RegisterRequest;
import com.citas.api.infrastructure.adapters.in.web.auth.AuthDtos.SessionResponse;
import com.citas.api.infrastructure.adapters.in.web.auth.AuthDtos.TokenResponse;
import com.citas.api.infrastructure.adapters.in.web.auth.AuthDtos.UserResponse;
import com.citas.api.infrastructure.adapters.out.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;

    AuthController(RegisterUserUseCase registerUser, LoginUseCase login, RefreshSessionUseCase refreshSession,
                   LogoutUseCase logout) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUser.register(new RegisterUserCommand(request.firstNames(), request.lastNames(),
                request.documentType(), request.documentNumber(), request.email(), request.phone(),
                request.password()));
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(login.login(new LoginCommand(request.email(), request.password())));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshSession.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.logout(request.refreshToken());
    }

    /** Recurso protegido mínimo: datos de la sesión tomados del access token (CA-08). */
    @GetMapping("/session")
    SessionResponse session(@AuthenticationPrincipal AuthenticatedUser user) {
        return new SessionResponse(user.userId(), user.email(), List.copyOf(user.roles().stream().sorted().toList()));
    }
}

package com.citas.api.infrastructure.adapters.in.web.auth;

import com.citas.api.application.port.in.GetSessionProfileUseCase;
import com.citas.api.application.port.in.GetSessionProfileUseCase.SessionProfile;
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
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;
    private final GetSessionProfileUseCase sessionProfile;

    AuthController(RegisterUserUseCase registerUser, LoginUseCase login, RefreshSessionUseCase refreshSession,
                   LogoutUseCase logout, GetSessionProfileUseCase sessionProfile) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
        this.sessionProfile = sessionProfile;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUser.register(new RegisterUserCommand(request.firstNames(), request.lastNames(),
                request.documentType(), request.documentNumber(), request.email(), request.phone(),
                request.password(), request.insurancePlanId(), request.regimeCode()));
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

    /**
     * Recurso protegido mínimo (CA-08): id, email y roles salen del access token; los nombres,
     * de la base, para que el cliente pueda saludar y mostrar al usuario en la cabecera.
     */
    @GetMapping("/session")
    SessionResponse session(@AuthenticationPrincipal AuthenticatedUser user) {
        SessionProfile profile = sessionProfile.profile(user.userId());
        return new SessionResponse(user.userId(), user.email(), profile.firstNames(), profile.lastNames(),
                List.copyOf(user.roles().stream().sorted().toList()));
    }
}

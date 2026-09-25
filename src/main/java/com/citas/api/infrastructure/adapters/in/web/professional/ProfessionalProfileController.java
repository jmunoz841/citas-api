package com.citas.api.infrastructure.adapters.in.web.professional;

import com.citas.api.application.port.in.ViewOwnProfessionalProfileUseCase;
import com.citas.api.application.port.in.ViewOwnProfessionalProfileUseCase.OwnProfile;
import com.citas.api.infrastructure.adapters.out.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Perfil del profesional autenticado para "Mi agenda" (HU-010): solo lectura y siempre el propio,
 * porque el id sale del access token.
 */
@RestController
@RequestMapping("/api/v1/professional/me")
class ProfessionalProfileController {

    private final ViewOwnProfessionalProfileUseCase profile;

    ProfessionalProfileController(ViewOwnProfessionalProfileUseCase profile) {
        this.profile = profile;
    }

    @GetMapping
    ProfileResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ProfileResponse.from(profile.ownProfile(user.userId()));
    }

    record ProfileResponse(Long id, String firstNames, String lastNames, boolean active,
                           SpecialtyResponse primarySpecialty, List<SiteResponse> sites) {

        static ProfileResponse from(OwnProfile own) {
            return new ProfileResponse(own.view().professional().getUserId(), own.view().firstNames(),
                    own.view().lastNames(), own.view().professional().isActive(),
                    new SpecialtyResponse(own.primarySpecialty().getId(), own.primarySpecialty().getName(),
                            own.primarySpecialty().getDurationMinutes()),
                    own.sites().stream().map(site -> new SiteResponse(site.code(), site.name())).toList());
        }
    }

    record SpecialtyResponse(Long id, String name, int durationMinutes) {
    }

    record SiteResponse(String code, String name) {
    }
}

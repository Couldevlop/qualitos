package com.openlab.qualitos.quality.storyboard;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint Storyboard IA (CLAUDE.md §7.4) : {@code POST /api/v1/ai/storyboard} reçoit une
 * liste d'indicateurs (label, valeur, tendance/cible optionnelles) + une période, et renvoie
 * un récit narratif généré par l'IA + le rappel des chiffres source (explicabilité §12.3).
 *
 * <p>POST car l'appel a un effet de bord (relais LLM via la passerelle IA). Authentifié
 * (SecurityConfig {@code anyRequest().authenticated()}) et restreint par {@code @PreAuthorize}
 * aux rôles de pilotage qualité (défense en profondeur, OWASP A01 — cohérent avec C1). Le
 * tenant provient du JWT, jamais du corps de requête (règle 18.2 #2). La validation des bornes
 * (Jakarta {@code @Valid}) → 400 ; l'indisponibilité de la passerelle → 502
 * ({@code AiGatewayException} mappée par {@code GlobalExceptionHandler}).
 */
@RestController
@RequestMapping("/api/v1/ai/storyboard")
public class StoryboardController {

    private final StoryboardService storyboardService;

    public StoryboardController(StoryboardService storyboardService) {
        this.storyboardService = storyboardService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','AUDITOR','ADMIN_TENANT','SUPER_ADMIN','ADMIN')")
    public StoryboardDto.StoryboardResponse generate(
            @Valid @RequestBody StoryboardDto.StoryboardRequest request) {
        return storyboardService.generate(request);
    }
}

package com.openlab.qualitos.core.partner;

import com.openlab.qualitos.core.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Ce que voit un consultant : les clients chez qui il peut entrer aujourd'hui.
 *
 * <p>La liste ne contient que des accès <b>ouverts ce jour</b> — cabinet actif,
 * consultant actif, accord accordé et dans ses dates. Un accord proposé mais
 * non accordé n'y figure pas : le cabinet apprendrait sinon, par la console, la
 * liste des clients à qui on l'a proposé et qui n'ont pas répondu, ce qui est
 * une information du client, pas du cabinet.
 *
 * <p>Pas de {@code @SaasOnly} : le portefeuille d'un consultant existe aussi
 * chez un client qui héberge lui-même et qui fait intervenir un cabinet.
 */
@RestController
@RequestMapping("/api/v1/partner/portfolio")
@PreAuthorize("hasRole('CONSULTANT')")
@Tag(name = "Partner portfolio", description = "Les clients où un consultant peut entrer")
public class PartnerPortfolioController {

    private final PartnerAccessService service;

    public PartnerPortfolioController(PartnerAccessService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "The clients this consultant can enter today")
    public ResponseEntity<List<PartnerDto.EngagementResponse>> portfolio() {
        return ResponseEntity.ok(service.portfolioOf(CurrentUser.userId().orElse(null)));
    }

    /**
     * Rattache le compte connecté à sa fiche de consultant, par le courriel que
     * son cabinet a inscrit.
     *
     * <p>Appelé à l'ouverture de la console du cabinet. Il est idempotent et ne
     * crée rien : une personne dont aucune fiche ne porte le courriel reste
     * sans portefeuille, et c'est au cabinet de l'inscrire — la route ne doit
     * pas pouvoir servir à s'inviter dans un cabinet.
     */
    @PostMapping("/link")
    @Operation(summary = "Link the signed-in account to its consultant record")
    public ResponseEntity<PartnerDto.ConsultantResponse> link(@AuthenticationPrincipal Jwt jwt) {
        UUID account = CurrentUser.userId().orElse(null);
        String email = jwt == null ? null : jwt.getClaimAsString("email");
        return service.linkAccount(account, email)
                .map(PartnerDto.ConsultantResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

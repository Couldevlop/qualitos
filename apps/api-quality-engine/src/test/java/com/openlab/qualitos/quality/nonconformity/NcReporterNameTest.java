package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * « Détecté par » (§4.3).
 *
 * <p>Un signalement sans auteur lisible se retrouve, des mois plus tard, sans
 * personne à qui demander ce qui avait été vu. L'identifiant Keycloak seul n'y
 * suffit pas : il ne désigne personne dans une liste, et il ne désigne plus rien
 * du tout le jour où le compte est supprimé de l'annuaire. Le nom est donc
 * recopié au moment du signalement — depuis le jeton, jamais depuis le corps de
 * la requête, qui est falsifiable (OWASP A01).
 */
@ExtendWith(MockitoExtension.class)
class NcReporterNameTest {

    @Mock NonConformityRepository repo;
    @Mock CapaCaseRepository capaRepo;
    @Mock ApplicationEventPublisher events;
    @InjectMocks NcService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SUB = UUID.randomUUID();
    private static final UUID BODY_REPORTER = UUID.randomUUID();

    @BeforeEach
    void ctx() {
        TenantContext.setTenantId(TENANT.toString());
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String name) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", SUB.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (name != null) {
            builder.claim("name", name);
        }
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(builder.build(), List.of(), SUB.toString()));
    }

    private NcDto.CreateRequest request() {
        return new NcDto.CreateRequest(
                "Joint torique défectueux", "détail", NcCategory.PRODUCT, NcSeverity.MAJOR,
                Instant.now(), "Atelier 3", 48.85, 2.35, null, BODY_REPORTER, null, null, null);
    }

    private NonConformity saved() {
        ArgumentCaptor<NonConformity> captor = ArgumentCaptor.forClass(NonConformity.class);
        verify(repo).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordsTheNameCarriedByTheToken() {
        authenticateAs("Amina Dridi");

        NcDto.Response response = service.create(request());

        assertThat(saved().getReporterName()).isEqualTo("Amina Dridi");
        assertThat(response.reporterName()).isEqualTo("Amina Dridi");
    }

    @Test
    void takesTheIdentityFromTheTokenAndNotFromTheBody() {
        // Le corps propose un autre rapporteur : il est ignoré, sans quoi
        // n'importe qui pourrait signer un signalement au nom d'un collègue.
        authenticateAs("Amina Dridi");

        service.create(request());

        assertThat(saved().getReporterId()).isEqualTo(SUB);
        assertThat(saved().getReporterId()).isNotEqualTo(BODY_REPORTER);
    }

    @Test
    void leavesTheNameEmptyWhenTheTokenPublishesNone() {
        authenticateAs(null);

        service.create(request());

        // Aucun nom disponible : la colonne reste vide. La liste affichera « — »,
        // ce qui dit la vérité, plutôt qu'un UUID qui n'apprend rien.
        assertThat(saved().getReporterName()).isNull();
    }

    @Test
    void leavesTheNameEmptyOutsideAnyAuthenticatedContext() {
        service.create(request());

        assertThat(saved().getReporterName()).isNull();
        // Hors contexte authentifié (bancs de service), l'identifiant du corps
        // reste le seul repli documenté.
        assertThat(saved().getReporterId()).isEqualTo(BODY_REPORTER);
    }
}

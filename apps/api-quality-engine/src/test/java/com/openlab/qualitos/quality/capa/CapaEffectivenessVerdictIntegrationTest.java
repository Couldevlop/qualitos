package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.AuthzDto;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.SystemRole;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'évaluation d'efficacité sur les vrais services, la vraie base et les vrais
 * droits (ADR 0143).
 *
 * <p>Deux choses ne se prouvent qu'ici. La première est que le dossier se
 * CLÔTURE réellement au verdict de son action : le refus de clôture relit en
 * base les vérifications promises, et un banc qui simule cette lecture ne
 * prouve que ce qu'il a simulé. La seconde est que la PERSONNE DÉSIGNÉE
 * franchit toute la chaîne — la portée du registre, puis la garde du jugement —
 * avec les droits que la plateforme livre à un simple utilisateur.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class CapaEffectivenessVerdictIntegrationTest {

    @Autowired AuthorizationService authorization;
    @Autowired CapaService capas;
    @Autowired TransactionTemplate tx;

    final UUID tenant = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();
    final UUID pilote = UUID.randomUUID();
    final UUID verificateur = UUID.randomUUID();
    final UUID passant = UUID.randomUUID();

    <T> T dans(Supplier<T> f) {
        return tx.execute(s -> f.get());
    }

    void connecte(UUID user, String... roles) {
        TenantContext.setTenantId(tenant.toString());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString())
                .claim("tenant_id", tenant.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList(),
                user.toString()));
    }

    /**
     * L'utilisateur garde ses droits livrés, moins « voir tous les dossiers » :
     * c'est l'état d'un compte de terrain à qui l'on confie une vérification, et
     * c'est exactement celui où le défaut se voyait.
     */
    @BeforeEach
    void restreindreLUtilisateur() {
        connecte(admin, "ADMIN_TENANT");
        EnumSet<Permission> droits = EnumSet.noneOf(Permission.class);
        for (Permission p : Permission.values()) {
            if (p.grantedByDefaultTo(SystemRole.USER)) {
                droits.add(p);
            }
        }
        droits.remove(Permission.CAPA_VIEW_ALL);
        dans(() -> authorization.updateRole("USER", new AuthzDto.RoleCommand(null, null, null,
                droits.stream().map(Permission::code).toList())));
        assertThat(droits).doesNotContain(Permission.CAPA_VERIFY);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /** Un dossier d'une action, qui a promis de vérifier son effet. */
    private CapaDto.CaseResponse dossierResolu() {
        CapaDto.CaseResponse ouvert = dans(() -> capas.openCase(
                new CapaDto.OpenCaseRequest("Remplacer le joint", "Fuite au presse-étoupe",
                        CapaType.CORRECTIVE, null, CapaCriticity.HIGH, CapaSourceType.INTERNAL,
                        null, pilote, "Paul Pilote", LocalDate.now().plusDays(30),
                        true, verificateur, "Vera Vérificatrice",
                        "Relever le débit de fuite sur trois cycles",
                        "Aucune fuite sur trois cycles"),
                CapaSourceType.INTERNAL, null));
        UUID action = ouvert.actions().get(0).id();
        // L'action FAITE résout le dossier : son statut se déduit de ses actions
        // (ADR 0117).
        dans(() -> capas.updateAction(ouvert.id(), action,
                new CapaDto.ActionRequest("Remplacer le joint", null, CapaActionStatus.DONE,
                        null, null, null, null, null, null, null, null, null, null)));
        CapaDto.CaseResponse resolu = dans(() -> capas.findById(ouvert.id()));
        assertThat(resolu.status()).isEqualTo(CapaStatus.RESOLVED);
        return resolu;
    }

    private CapaDto.EffectivenessRequest efficace() {
        return new CapaDto.EffectivenessRequest(null, EffectivenessVerdict.EFFECTIVE,
                "4,2 L/h", "0 L/h", "Aucune fuite sur trois cycles", null);
    }

    /**
     * LE DÉFAUT CORRIGÉ, de bout en bout : l'action jugée efficace depuis sa
     * discipline 8D clôture son dossier, et l'écran d'efficacité n'a plus rien à
     * redemander.
     */
    @Test
    void leVerdictDUneActionClotureSonDossierEnBase() {
        connecte(admin, "ADMIN_TENANT");
        CapaDto.CaseResponse resolu = dossierResolu();
        UUID action = resolu.actions().get(0).id();

        dans(() -> capas.verifyActionEffectiveness(resolu.id(), action, efficace()));

        CapaDto.CaseResponse apres = dans(() -> capas.findById(resolu.id()));
        assertThat(apres.status()).isEqualTo(CapaStatus.CLOSED);
        assertThat(apres.closedAt()).isNotNull();
        assertThat(apres.effectivenessVerified()).isTrue();
    }

    /**
     * LA PERSONNE DÉSIGNÉE JUGE, avec les droits d'un simple utilisateur : elle
     * voit le dossier dont on lui a confié la vérification, et son verdict
     * passe. Les deux verrous que l'on vient de lever sont sur ce chemin, et
     * aucun autre banc ne les franchit tous les deux.
     */
    @Test
    void laPersonneDesigneeVoitSonDossierEtLeJuge() {
        connecte(admin, "ADMIN_TENANT");
        CapaDto.CaseResponse resolu = dossierResolu();
        UUID action = resolu.actions().get(0).id();

        connecte(verificateur, "USER");
        assertThat(dans(() -> capas.findById(resolu.id())).id()).isEqualTo(resolu.id());
        dans(() -> capas.verifyActionEffectiveness(resolu.id(), action, efficace()));

        connecte(admin, "ADMIN_TENANT");
        assertThat(dans(() -> capas.findById(resolu.id())).status()).isEqualTo(CapaStatus.CLOSED);
    }

    /**
     * ET LE CHEMIN NE S'OUVRE PAS A TOUT LE MONDE : un utilisateur qui n'est pas
     * désigné ne juge pas. Le dossier, il ne le voit même pas.
     */
    @Test
    void unPassantNeJugePas() {
        connecte(admin, "ADMIN_TENANT");
        CapaDto.CaseResponse resolu = dossierResolu();
        UUID action = resolu.actions().get(0).id();

        connecte(passant, "USER");
        assertThatThrownBy(() -> dans(() -> capas.findById(resolu.id())))
                .isInstanceOf(CapaNotFoundException.class);
        assertThatThrownBy(() -> dans(() ->
                capas.verifyActionEffectiveness(resolu.id(), action, efficace())))
                .isInstanceOf(CapaNotFoundException.class);
    }

    /**
     * LE VERDICT DU DOSSIER AUSSI : c'est ce que l'écran d'efficacité envoie, et
     * la personne désignée s'y faisait refuser l'envoi après avoir rempli le
     * formulaire.
     *
     * <p>Sur un dossier dont la PROMESSE est au niveau du dossier et de lui
     * seul — le cas que l'écran d'efficacité sert : quand c'est l'action qui a
     * promis, c'est sur sa ligne qu'elle se juge, et le dossier suit (ADR 0143).
     */
    @Test
    void laPersonneDesigneeJugeLeDossierDepuisLEcranDEfficacite() {
        connecte(admin, "ADMIN_TENANT");
        CapaDto.CaseResponse dossier = dans(() -> capas.createCase(
                new CapaDto.CreateCaseRequest("Reprendre la gamme", null, CapaType.CORRECTIVE,
                        CapaCriticity.HIGH, CapaSourceType.INTERNAL, null, pilote, null,
                        LocalDate.now().plusDays(30))));
        dans(() -> capas.updateCase(dossier.id(), new CapaDto.UpdateCaseRequest(
                null, null, null, null, null, null, true, verificateur, "Vera Vérificatrice",
                null, "Aucun rebut sur 90 jours", null)));
        // L'action ne promet RIEN : la question a été tranchée sur le dossier.
        dans(() -> capas.addAction(dossier.id(),
                new CapaDto.ActionRequest("Reprendre la gamme", null, CapaActionStatus.DONE,
                        null, pilote, "Paul Pilote", null, null,
                        null, null, null, null, null)));
        assertThat(dans(() -> capas.findById(dossier.id())).status())
                .isEqualTo(CapaStatus.RESOLVED);

        connecte(verificateur, "USER");
        dans(() -> capas.verifyEffectiveness(dossier.id(), efficace()));

        connecte(admin, "ADMIN_TENANT");
        assertThat(dans(() -> capas.findById(dossier.id())).status()).isEqualTo(CapaStatus.CLOSED);
    }

    /**
     * Le refus porte bien sur le JUGEMENT, et non sur la lecture : un auditeur
     * voit tous les dossiers, et c'est la désignation qui lui manque ici.
     */
    @Test
    void celuiQuiVoitToutSansDroitDeVerifierNeJugePas() {
        connecte(admin, "ADMIN_TENANT");
        CapaDto.CaseResponse resolu = dossierResolu();
        UUID action = resolu.actions().get(0).id();
        // Un rôle qui voit tout, mais à qui le client a retiré la vérification.
        EnumSet<Permission> droits = EnumSet.of(Permission.CAPA_VIEW_ALL);
        dans(() -> authorization.updateRole("AUDITOR", new AuthzDto.RoleCommand(null, null, null,
                droits.stream().map(Permission::code).toList())));

        connecte(passant, "AUDITOR");
        assertThat(dans(() -> capas.findById(resolu.id())).id()).isEqualTo(resolu.id());
        assertThatThrownBy(() -> dans(() ->
                capas.verifyActionEffectiveness(resolu.id(), action, efficace())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> dans(() -> capas.verifyEffectiveness(resolu.id(), efficace())))
                .isInstanceOf(AccessDeniedException.class);
    }
}

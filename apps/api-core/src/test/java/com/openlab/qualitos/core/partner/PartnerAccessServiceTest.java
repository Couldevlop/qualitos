package com.openlab.qualitos.core.partner;

import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantRepository;
import com.openlab.qualitos.core.platformaudit.PlatformAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La règle que tout le domaine partenaire existe pour tenir : <b>un consultant
 * n'entre jamais chez un client sans accord enregistré de ce client</b>.
 *
 * <p>Les cas ci-dessous sont les façons dont cette règle se contourne quand on
 * ne la tient pas : un cabinet suspendu dont les accès restent ouverts, une
 * mission terminée qu'aucun traitement n'a fermée, une lecture qui écrit, et
 * l'identifiant d'un accord du voisin.
 */
class PartnerAccessServiceTest {

    private static final UUID CLIENT = UUID.fromString("00000000-0000-0000-0000-00000000c111");
    private static final UUID AUTRE_CLIENT = UUID.fromString("00000000-0000-0000-0000-00000000c222");
    private static final UUID CABINET = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID COMPTE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ACCORD = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 5);

    private PartnerFirmRepository firms;
    private PartnerConsultantRepository consultants;
    private PartnerEngagementRepository engagements;
    private TenantRepository tenants;
    private PlatformAuditService auditTrail;
    private PartnerAccessService service;

    @BeforeEach
    void setUp() {
        firms = mock(PartnerFirmRepository.class);
        consultants = mock(PartnerConsultantRepository.class);
        engagements = mock(PartnerEngagementRepository.class);
        tenants = mock(TenantRepository.class);
        auditTrail = mock(PlatformAuditService.class);
        Clock clock = Clock.fixed(AUJOURD_HUI.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        service = new PartnerAccessService(firms, consultants, engagements, tenants, clock, auditTrail);
        when(engagements.save(any(PartnerEngagement.class))).thenAnswer(i -> i.getArgument(0));
        when(tenants.findById(CLIENT)).thenReturn(Optional.of(
                Tenant.builder().id(CLIENT).slug("client").name("Metalux").build()));
        when(firms.findById(CABINET)).thenReturn(Optional.of(cabinet(true)));
    }

    private static PartnerFirm cabinet(boolean actif) {
        return PartnerFirm.builder().id(CABINET).slug("qualiconseil").name("QualiConseil")
                .active(actif).build();
    }

    private static PartnerConsultant consultant(boolean actif) {
        return PartnerConsultant.builder().id(UUID.randomUUID()).firmId(CABINET).accountId(COMPTE)
                .displayName("Claire Vasseur").email("claire@qualiconseil.fr").active(actif).build();
    }

    private static PartnerEngagement accord(EngagementStatus statut, EngagementScope portee,
                                            LocalDate debut, LocalDate fin) {
        return PartnerEngagement.builder().id(ACCORD).tenantId(CLIENT).firmId(CABINET)
                .scope(portee).status(statut).startsOn(debut).endsOn(fin)
                .proposedAt(Instant.parse("2026-10-01T00:00:00Z")).build();
    }

    // ================= côté client : accorder, refuser, reprendre =================

    @Test
    void accorder_borne_la_mission_et_garde_qui_a_decide() {
        when(engagements.findById(ACCORD)).thenReturn(Optional.of(
                accord(EngagementStatus.PROPOSED, EngagementScope.INTERVENE, null, null)));

        PartnerDto.EngagementResponse rendu = service.grant(CLIENT, ACCORD,
                new PartnerDto.GrantRequest(EngagementScope.READ, LocalDate.of(2026, 12, 31), "Audit blanc"),
                COMPTE);

        assertThat(rendu.status()).isEqualTo(EngagementStatus.GRANTED);
        // Le client peut RESTREINDRE ce que l'éditeur a proposé : c'est sa
        // décision, pas une contresignature de celle de l'éditeur.
        assertThat(rendu.scope()).isEqualTo(EngagementScope.READ);
        assertThat(rendu.startsOn()).isEqualTo(AUJOURD_HUI);
        assertThat(rendu.endsOn()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(rendu.openToday()).isTrue();
        assertThat(rendu.purpose()).isEqualTo("Audit blanc");
    }

    @Test
    void l_accord_d_un_AUTRE_client_est_introuvable_et_non_interdit() {
        // Répondre 403 confirmerait que cet identifiant existe ailleurs : c'est
        // déjà une information sur le portefeuille d'un autre.
        PartnerEngagement chezLeVoisin = accord(EngagementStatus.PROPOSED, EngagementScope.READ, null, null);
        chezLeVoisin.setTenantId(AUTRE_CLIENT);
        when(engagements.findById(ACCORD)).thenReturn(Optional.of(chezLeVoisin));

        assertThatThrownBy(() -> service.grant(CLIENT, ACCORD,
                new PartnerDto.GrantRequest(EngagementScope.READ, LocalDate.of(2026, 12, 31), null), COMPTE))
                .isInstanceOf(PartnerExceptions.EngagementNotFound.class);
        verify(engagements, never()).save(any());
    }

    @Test
    void on_n_accorde_pas_deux_fois_le_meme_accord() {
        when(engagements.findById(ACCORD)).thenReturn(Optional.of(
                accord(EngagementStatus.GRANTED, EngagementScope.READ, AUJOURD_HUI, AUJOURD_HUI)));

        assertThatThrownBy(() -> service.grant(CLIENT, ACCORD,
                new PartnerDto.GrantRequest(EngagementScope.READ, LocalDate.of(2026, 12, 31), null), COMPTE))
                .isInstanceOf(PartnerExceptions.EngagementNotDecidable.class);
    }

    @Test
    void refuser_ferme_sans_rien_ouvrir() {
        when(engagements.findById(ACCORD)).thenReturn(Optional.of(
                accord(EngagementStatus.PROPOSED, EngagementScope.INTERVENE, null, null)));

        PartnerDto.EngagementResponse rendu = service.decline(CLIENT, ACCORD, COMPTE);

        assertThat(rendu.status()).isEqualTo(EngagementStatus.DECLINED);
        assertThat(rendu.openToday()).isFalse();
    }

    @Test
    void refuser_n_est_possible_que_sur_une_proposition() {
        when(engagements.findById(ACCORD)).thenReturn(Optional.of(
                accord(EngagementStatus.REVOKED, EngagementScope.READ, null, null)));

        assertThatThrownBy(() -> service.decline(CLIENT, ACCORD, COMPTE))
                .isInstanceOf(PartnerExceptions.EngagementNotDecidable.class);
    }

    @Test
    void reprendre_se_distingue_d_une_expiration() {
        // Un accord expiré se renouvelle ; un accord repris a été retiré. Un
        // rapport d'audit doit savoir lequel s'est produit, dix-huit mois après.
        when(engagements.findById(ACCORD)).thenReturn(Optional.of(
                accord(EngagementStatus.GRANTED, EngagementScope.INTERVENE,
                        AUJOURD_HUI.minusDays(10), AUJOURD_HUI.plusDays(10))));

        PartnerDto.EngagementResponse rendu = service.revoke(CLIENT, ACCORD, COMPTE);

        assertThat(rendu.status()).isEqualTo(EngagementStatus.REVOKED);
        assertThat(rendu.revokedAt()).isNotNull();
        assertThat(rendu.openToday()).isFalse();
    }

    @Test
    void on_ne_reprend_pas_ce_qui_n_a_jamais_ete_accorde() {
        when(engagements.findById(ACCORD)).thenReturn(Optional.of(
                accord(EngagementStatus.PROPOSED, EngagementScope.READ, null, null)));

        assertThatThrownBy(() -> service.revoke(CLIENT, ACCORD, COMPTE))
                .isInstanceOf(PartnerExceptions.EngagementNotDecidable.class);
    }

    @Test
    void un_accord_inexistant_ne_se_decide_pas() {
        when(engagements.findById(ACCORD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(CLIENT, ACCORD, COMPTE))
                .isInstanceOf(PartnerExceptions.EngagementNotFound.class);
    }

    @Test
    void le_client_voit_ses_accords_avec_le_nom_du_cabinet() {
        when(engagements.findByTenantIdOrderByProposedAtDesc(CLIENT)).thenReturn(List.of(
                accord(EngagementStatus.GRANTED, EngagementScope.READ,
                        AUJOURD_HUI.minusDays(1), AUJOURD_HUI.plusDays(30))));
        when(firms.findAllById(any())).thenReturn(List.of(cabinet(true)));

        List<PartnerDto.EngagementResponse> rendu = service.listForTenant(CLIENT);

        assertThat(rendu).hasSize(1);
        assertThat(rendu.get(0).firmName()).isEqualTo("QualiConseil");
        assertThat(rendu.get(0).tenantName()).isEqualTo("Metalux");
    }

    // ================= la garde : entrer chez un client =================

    @Test
    void un_accord_en_vigueur_ouvre_l_acces() {
        donneUnAccesOuvert(EngagementScope.INTERVENE);

        PartnerEngagement rendu = service.requireAccess(COMPTE, CLIENT, true);

        assertThat(rendu.getId()).isEqualTo(ACCORD);
    }

    @Test
    void un_compte_sans_fiche_de_consultant_n_entre_nulle_part() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, false))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class)
                .hasMessageContaining("rattaché à aucun cabinet");
    }

    @Test
    void desactiver_un_consultant_lui_retire_TOUS_ses_clients_d_un_coup() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(false)));

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, false))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void suspendre_le_cabinet_ferme_l_acces_sans_toucher_aux_accords() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        when(firms.findById(CABINET)).thenReturn(Optional.of(cabinet(false)));

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, false))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class)
                .hasMessageContaining("suspendu");
        // L'accord n'est pas consulté : la suspension court-circuite, et surtout
        // elle ne le modifie pas — une mesure conservatoire doit pouvoir se lever.
        verify(engagements, never()).save(any());
    }

    @Test
    void un_client_qui_n_a_rien_accorde_reste_ferme() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        when(engagements.findLive(CLIENT, CABINET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, false))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class)
                .hasMessageContaining("aucun accès");
    }

    @Test
    void une_mission_terminee_se_ferme_SANS_traitement_de_fond() {
        // C'est le cœur : la date fait foi à la lecture. Si la fermeture
        // dépendait d'un travail nocturne, un accès périmé resterait ouvert
        // chaque fois que ce travail ne tournerait pas.
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        when(engagements.findLive(CLIENT, CABINET)).thenReturn(Optional.of(
                accord(EngagementStatus.GRANTED, EngagementScope.INTERVENE,
                        AUJOURD_HUI.minusDays(60), AUJOURD_HUI.minusDays(1))));

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, false))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class)
                .hasMessageContaining("pas en vigueur");
    }

    @Test
    void une_mission_pas_encore_commencee_ne_s_ouvre_pas_d_avance() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        when(engagements.findLive(CLIENT, CABINET)).thenReturn(Optional.of(
                accord(EngagementStatus.GRANTED, EngagementScope.READ,
                        AUJOURD_HUI.plusDays(3), AUJOURD_HUI.plusDays(30))));

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, false))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class);
    }

    @Test
    void un_accord_en_LECTURE_n_autorise_pas_a_ecrire() {
        donneUnAccesOuvert(EngagementScope.READ);

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, true))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class)
                .hasMessageContaining("lecture");
        // ... mais il autorise bien à lire.
        assertThat(service.requireAccess(COMPTE, CLIENT, false)).isNotNull();
    }

    @Test
    void un_cabinet_disparu_ferme_au_lieu_de_tomber() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        when(firms.findById(CABINET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireAccess(COMPTE, CLIENT, false))
                .isInstanceOf(PartnerExceptions.AccessNotGranted.class);
    }

    // ================= le portefeuille d'un consultant =================

    @Test
    void le_portefeuille_ne_montre_que_les_acces_OUVERTS_aujourd_hui() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        PartnerEngagement perime = accord(EngagementStatus.GRANTED, EngagementScope.READ,
                AUJOURD_HUI.minusDays(60), AUJOURD_HUI.minusDays(2));
        perime.setTenantId(AUTRE_CLIENT);
        when(engagements.findGrantedForFirm(CABINET)).thenReturn(List.of(
                accord(EngagementStatus.GRANTED, EngagementScope.INTERVENE,
                        AUJOURD_HUI.minusDays(5), AUJOURD_HUI.plusDays(5)),
                perime));
        when(tenants.findAllById(any())).thenReturn(List.of(
                Tenant.builder().id(CLIENT).slug("metalux").name("Metalux").build()));

        List<PartnerDto.EngagementResponse> rendu = service.portfolioOf(COMPTE);

        assertThat(rendu).hasSize(1);
        assertThat(rendu.get(0).tenantName()).isEqualTo("Metalux");
    }

    @Test
    void un_consultant_desactive_a_un_portefeuille_vide_sans_lever() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(false)));

        assertThat(service.portfolioOf(COMPTE)).isEmpty();
    }

    @Test
    void un_compte_sans_fiche_a_un_portefeuille_vide_sans_lever() {
        // Se connecter avec le rôle mais sans fiche n'est pas une panne : c'est
        // un rattachement qui reste à faire, et l'écran doit pouvoir le dire.
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.empty());

        assertThat(service.portfolioOf(COMPTE)).isEmpty();
        assertThat(service.portfolioOf(null)).isEmpty();
    }

    @Test
    void le_portefeuille_d_un_cabinet_suspendu_est_vide() {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        when(firms.findById(CABINET)).thenReturn(Optional.of(cabinet(false)));

        assertThat(service.portfolioOf(COMPTE)).isEmpty();
    }

    // ================= le rattachement d'un compte =================

    @Test
    void la_premiere_connexion_rattache_le_compte_par_son_courriel() {
        PartnerConsultant fiche = PartnerConsultant.builder().id(UUID.randomUUID()).firmId(CABINET)
                .displayName("Claire Vasseur").email("claire@qualiconseil.fr").active(true).build();
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.empty());
        when(consultants.findFirstByAccountIdIsNullAndEmailIgnoreCase("claire@qualiconseil.fr"))
                .thenReturn(Optional.of(fiche));
        when(consultants.save(any(PartnerConsultant.class))).thenAnswer(i -> i.getArgument(0));

        Optional<PartnerConsultant> rendu = service.linkAccount(COMPTE, " claire@qualiconseil.fr ");

        assertThat(rendu).isPresent();
        assertThat(rendu.get().getAccountId()).isEqualTo(COMPTE);
    }

    @Test
    void un_compte_deja_rattache_ne_change_pas_de_fiche() {
        PartnerConsultant deja = consultant(true);
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(deja));

        assertThat(service.linkAccount(COMPTE, "autre@ailleurs.fr")).contains(deja);
        verify(consultants, never()).save(any());
    }

    @Test
    void le_rattachement_ne_CREE_rien() {
        // Sans quoi cette route servirait à s'inviter dans un cabinet.
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.empty());
        when(consultants.findFirstByAccountIdIsNullAndEmailIgnoreCase(any()))
                .thenReturn(Optional.empty());

        assertThat(service.linkAccount(COMPTE, "inconnu@ailleurs.fr")).isEmpty();
        assertThat(service.linkAccount(null, "claire@qualiconseil.fr")).isEmpty();
        assertThat(service.linkAccount(COMPTE, "  ")).isEmpty();
        verify(consultants, never()).save(any());
    }

    private void donneUnAccesOuvert(EngagementScope portee) {
        when(consultants.findByAccountId(COMPTE)).thenReturn(Optional.of(consultant(true)));
        when(engagements.findLive(CLIENT, CABINET)).thenReturn(Optional.of(
                accord(EngagementStatus.GRANTED, portee,
                        AUJOURD_HUI.minusDays(5), AUJOURD_HUI.plusDays(25))));
    }
}

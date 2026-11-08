package com.openlab.qualitos.core.partner;

import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.tenant.TenantRepository;
import com.openlab.qualitos.core.platformaudit.PlatformAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le côté éditeur : constituer les cabinets et leur portefeuille.
 *
 * <p>Le test qui compte le plus ici est celui qui vérifie ce que l'éditeur
 * <b>ne fait pas</b> : proposer un client n'ouvre aucun accès.
 */
class PartnerFirmServiceTest {

    private static final UUID CABINET = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID CLIENT = UUID.fromString("00000000-0000-0000-0000-00000000c111");
    private static final UUID ACTEUR = UUID.fromString("00000000-0000-0000-0000-0000000000a9");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 5);

    private PartnerFirmRepository firms;
    private PartnerConsultantRepository consultants;
    private PartnerEngagementRepository engagements;
    private TenantRepository tenants;
    private PlatformAuditService auditTrail;
    private PartnerFirmService service;

    @BeforeEach
    void setUp() {
        firms = mock(PartnerFirmRepository.class);
        consultants = mock(PartnerConsultantRepository.class);
        engagements = mock(PartnerEngagementRepository.class);
        tenants = mock(TenantRepository.class);
        auditTrail = mock(PlatformAuditService.class);
        Clock clock = Clock.fixed(AUJOURD_HUI.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        service = new PartnerFirmService(firms, consultants, engagements, tenants, clock, auditTrail);
        when(firms.save(any(PartnerFirm.class))).thenAnswer(i -> {
            PartnerFirm f = i.getArgument(0);
            if (f.getId() == null) {
                f.setId(CABINET);
            }
            return f;
        });
        when(consultants.save(any(PartnerConsultant.class))).thenAnswer(i -> i.getArgument(0));
        when(engagements.save(any(PartnerEngagement.class))).thenAnswer(i -> i.getArgument(0));
        when(firms.findById(CABINET)).thenReturn(Optional.of(cabinet()));
        when(tenants.findById(CLIENT)).thenReturn(Optional.of(
                Tenant.builder().id(CLIENT).slug("metalux").name("Metalux").build()));
    }

    private static PartnerFirm cabinet() {
        return PartnerFirm.builder().id(CABINET).slug("qualiconseil").name("QualiConseil")
                .active(true).createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
    }

    private static PartnerDto.FirmRequest demande(String slug) {
        return new PartnerDto.FirmRequest(slug, "QualiConseil", "812 345 678",
                "contact@qualiconseil.fr", "+33 1 23 45 67 89");
    }

    // ================= les cabinets =================

    @Test
    void creer_un_cabinet_le_rend_actif_et_sans_mission() {
        PartnerDto.FirmResponse rendu = service.create(demande("qualiconseil"));

        assertThat(rendu.active()).isTrue();
        assertThat(rendu.consultantCount()).isZero();
        assertThat(rendu.grantedCount()).isZero();
    }

    @Test
    void deux_cabinets_ne_partagent_pas_un_identifiant_d_URL() {
        when(firms.existsBySlug("qualiconseil")).thenReturn(true);

        assertThatThrownBy(() -> service.create(demande("qualiconseil")))
                .isInstanceOf(PartnerExceptions.FirmAlreadyExists.class);
        verify(firms, never()).save(any());
    }

    @Test
    void les_champs_laisses_vides_sont_nuls_et_non_des_chaines_vides() {
        // Une chaîne vide en base se lit « renseigné à vide », ce qui n'est pas
        // la même chose que « pas renseigné » sur une mention légale.
        PartnerDto.FirmResponse rendu = service.create(
                new PartnerDto.FirmRequest("cab", "Cabinet", "  ", "", null));

        assertThat(rendu.registrationNumber()).isNull();
        assertThat(rendu.contactEmail()).isNull();
        assertThat(rendu.contactPhone()).isNull();
    }

    @Test
    void modifier_un_cabinet_garde_son_identifiant_s_il_ne_change_pas() {
        PartnerDto.FirmResponse rendu = service.update(CABINET, demande("qualiconseil"));

        assertThat(rendu.slug()).isEqualTo("qualiconseil");
        // Le slug inchangé ne doit pas être refusé comme « déjà pris » par
        // lui-même : c'est le piège classique du contrôle d'unicité en mise à jour.
        verify(firms, never()).existsBySlug(any());
    }

    @Test
    void renommer_vers_un_identifiant_deja_pris_est_refuse() {
        when(firms.existsBySlug("autre")).thenReturn(true);

        assertThatThrownBy(() -> service.update(CABINET, demande("autre")))
                .isInstanceOf(PartnerExceptions.FirmAlreadyExists.class);
    }

    @Test
    void suspendre_un_cabinet_ne_touche_PAS_a_ses_accords() {
        PartnerDto.FirmResponse rendu = service.setActive(CABINET, false);

        assertThat(rendu.active()).isFalse();
        // Une mesure conservatoire doit pouvoir se lever : supprimer les accords
        // en ferait une décision irréversible, que seul le client pourrait défaire.
        verify(engagements, never()).save(any());
    }

    @Test
    void un_cabinet_inconnu_est_introuvable() {
        when(firms.findById(CABINET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(CABINET))
                .isInstanceOf(PartnerExceptions.FirmNotFound.class);
    }

    @Test
    void la_liste_compte_les_consultants_actifs_et_les_missions_accordees() {
        when(firms.findAllByOrderByNameAsc(any()))
                .thenReturn(new PageImpl<>(List.of(cabinet())));
        when(consultants.countByFirmIdAndActiveTrue(CABINET)).thenReturn(3L);
        when(engagements.countByFirmIdAndStatus(CABINET, EngagementStatus.GRANTED)).thenReturn(7L);

        var page = service.list(PageRequest.of(0, 20));

        assertThat(page.getContent().get(0).consultantCount()).isEqualTo(3L);
        assertThat(page.getContent().get(0).grantedCount()).isEqualTo(7L);
    }

    // ================= les consultants =================

    @Test
    void inscrire_un_consultant_avant_qu_il_ait_un_compte() {
        // Exiger le compte d'abord ferait d'une arrivée deux gestes dans deux
        // outils, dans le bon ordre.
        PartnerDto.ConsultantResponse rendu = service.addConsultant(CABINET,
                new PartnerDto.ConsultantRequest(" Claire Vasseur ", " claire@qualiconseil.fr "));

        assertThat(rendu.displayName()).isEqualTo("Claire Vasseur");
        assertThat(rendu.email()).isEqualTo("claire@qualiconseil.fr");
        assertThat(rendu.linked()).isFalse();
        assertThat(rendu.active()).isTrue();
    }

    @Test
    void le_meme_courriel_ne_s_inscrit_pas_deux_fois_dans_un_cabinet() {
        when(consultants.existsByFirmIdAndEmailIgnoreCase(eq(CABINET), any())).thenReturn(true);

        assertThatThrownBy(() -> service.addConsultant(CABINET,
                new PartnerDto.ConsultantRequest("Claire", "claire@qualiconseil.fr")))
                .isInstanceOf(PartnerExceptions.ConsultantAlreadyExists.class);
    }

    @Test
    void un_consultant_ne_se_desactive_pas_depuis_l_ecran_d_un_AUTRE_cabinet() {
        UUID ailleurs = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
        UUID consultantId = UUID.randomUUID();
        when(consultants.findById(consultantId)).thenReturn(Optional.of(
                PartnerConsultant.builder().id(consultantId).firmId(ailleurs)
                        .displayName("X").email("x@y.fr").active(true).build()));

        assertThatThrownBy(() -> service.setConsultantActive(CABINET, consultantId, false))
                .isInstanceOf(PartnerExceptions.ConsultantNotFound.class);
        verify(consultants, never()).save(any());
    }

    @Test
    void desactiver_puis_reactiver_un_consultant() {
        UUID consultantId = UUID.randomUUID();
        when(consultants.findById(consultantId)).thenReturn(Optional.of(
                PartnerConsultant.builder().id(consultantId).firmId(CABINET)
                        .displayName("Claire").email("c@q.fr").active(true).build()));

        assertThat(service.setConsultantActive(CABINET, consultantId, false).active()).isFalse();
        assertThat(service.setConsultantActive(CABINET, consultantId, true).active()).isTrue();
    }

    @Test
    void lister_les_consultants_d_un_cabinet_inconnu_est_introuvable() {
        when(firms.findById(CABINET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listConsultants(CABINET))
                .isInstanceOf(PartnerExceptions.FirmNotFound.class);
    }

    @Test
    void lister_les_consultants_rend_leur_rattachement() {
        when(consultants.findByFirmIdOrderByDisplayNameAsc(CABINET)).thenReturn(List.of(
                PartnerConsultant.builder().id(UUID.randomUUID()).firmId(CABINET)
                        .accountId(UUID.randomUUID()).displayName("Claire").email("c@q.fr")
                        .active(true).build()));

        assertThat(service.listConsultants(CABINET)).singleElement()
                .extracting(PartnerDto.ConsultantResponse::linked).isEqualTo(true);
    }

    // ================= le portefeuille =================

    @Test
    void proposer_un_client_n_ouvre_AUCUN_acces() {
        // C'est la règle de la spécification : l'éditeur constitue le
        // portefeuille, seul le client accorde.
        PartnerDto.EngagementResponse rendu = service.propose(CABINET,
                new PartnerDto.ProposeRequest(CLIENT, EngagementScope.INTERVENE, "Accompagnement ISO 9001"),
                ACTEUR);

        assertThat(rendu.status()).isEqualTo(EngagementStatus.PROPOSED);
        assertThat(rendu.openToday()).isFalse();
        assertThat(rendu.startsOn()).isNull();
        assertThat(rendu.endsOn()).isNull();
        assertThat(rendu.tenantName()).isEqualTo("Metalux");
    }

    @Test
    void on_ne_propose_pas_un_client_qui_n_existe_pas() {
        when(tenants.findById(CLIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.propose(CABINET,
                new PartnerDto.ProposeRequest(CLIENT, EngagementScope.READ, null), ACTEUR))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void un_second_accord_vivant_est_refuse() {
        // Deux accords en cours sur le même couple rendraient indécidable lequel
        // borne l'accès.
        when(engagements.findLive(CLIENT, CABINET)).thenReturn(Optional.of(
                PartnerEngagement.builder().id(UUID.randomUUID()).tenantId(CLIENT).firmId(CABINET)
                        .scope(EngagementScope.READ).status(EngagementStatus.GRANTED).build()));

        assertThatThrownBy(() -> service.propose(CABINET,
                new PartnerDto.ProposeRequest(CLIENT, EngagementScope.READ, null), ACTEUR))
                .isInstanceOf(PartnerExceptions.EngagementAlreadyLive.class);
        verify(engagements, never()).save(any());
    }

    @Test
    void le_portefeuille_montre_tout_l_historique_avec_le_nom_des_clients() {
        when(engagements.findByFirmIdOrderByProposedAtDesc(CABINET)).thenReturn(List.of(
                PartnerEngagement.builder().id(UUID.randomUUID()).tenantId(CLIENT).firmId(CABINET)
                        .scope(EngagementScope.READ).status(EngagementStatus.REVOKED).build(),
                PartnerEngagement.builder().id(UUID.randomUUID()).tenantId(CLIENT).firmId(CABINET)
                        .scope(EngagementScope.INTERVENE).status(EngagementStatus.GRANTED)
                        .startsOn(AUJOURD_HUI.minusDays(1)).endsOn(AUJOURD_HUI.plusDays(1)).build()));
        when(tenants.findAllById(any())).thenReturn(List.of(
                Tenant.builder().id(CLIENT).slug("metalux").name("Metalux").build()));

        List<PartnerDto.EngagementResponse> rendu = service.portfolio(CABINET);

        // L'historique est gardé : un accord repris reste visible, c'est ce qui
        // permet de distinguer plus tard une reprise d'une expiration.
        assertThat(rendu).hasSize(2);
        assertThat(rendu.get(0).status()).isEqualTo(EngagementStatus.REVOKED);
        assertThat(rendu.get(0).openToday()).isFalse();
        assertThat(rendu.get(1).openToday()).isTrue();
        assertThat(rendu).allMatch(e -> "Metalux".equals(e.tenantName()));
    }
}

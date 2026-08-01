package com.openlab.qualitos.quality.supplier;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prédiction de risque fournisseur (§4.6, §6.5).
 *
 * <p>Le modèle vit dans ai-service ; ce service ne fait que traduire l'historique réel
 * du fournisseur en caractéristiques. Ces tests verrouillent cette traduction — c'est
 * là que se logerait une invention de donnée.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupplierRiskPredictionServiceTest {

    @Mock SupplierRepository supplierRepo;
    @Mock SupplierNonConformityRepository ncRepo;
    @Mock SupplierAuditRecordRepository auditRepo;
    @Mock AiGatewayClient ai;

    SupplierRiskPredictionService service;

    static final UUID SUPPLIER_ID = UUID.randomUUID();
    static final LocalDate TODAY = LocalDate.parse("2026-08-01");
    static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new SupplierRiskPredictionService(supplierRepo, ncRepo, auditRepo, ai, CLOCK);
    }

    private Supplier supplier(int score, LocalDate lastAudit) {
        Supplier s = new Supplier();
        s.setId(SUPPLIER_ID);
        s.setTenantId(UUID.randomUUID());
        s.setCode("SUP-1");
        s.setName("Fournisseur Alpha");
        s.setScore(score);
        s.setLastAuditAt(lastAudit);
        return s;
    }

    private void givenSupplier(Supplier s) {
        when(supplierRepo.findById(SUPPLIER_ID)).thenReturn(Optional.of(s));
    }

    private void givenAiScore(double score, String level) {
        when(ai.predictSupplierRisk(anyMap())).thenReturn(Map.of(
                "score", score,
                "level", level,
                "drivers", List.of(
                        Map.of("feature", "nc_rate", "value", 0.5, "weight", 1.6, "contribution", 0.8),
                        Map.of("feature", "audit_score", "value", 0.7, "weight", 1.4, "contribution", -0.2))));
    }

    // ---- Traduction des caractéristiques ----------------------------------------

    @Test
    void deduit_le_taux_de_non_conformites_ouvertes_de_l_historique_reel() {
        givenSupplier(supplier(80, TODAY.minusDays(30)));
        when(ncRepo.countBySupplierIdAndStatus(SUPPLIER_ID, NonConformityStatus.OPEN)).thenReturn(3L);
        when(ncRepo.countBySupplierIdAndStatus(SUPPLIER_ID, NonConformityStatus.IN_REVIEW)).thenReturn(1L);
        when(ncRepo.countBySupplierIdAndStatus(SUPPLIER_ID, NonConformityStatus.RESOLVED)).thenReturn(6L);

        Map<String, Double> f = service.buildFeatures(supplier(80, TODAY.minusDays(30)));

        // 4 ouvertes sur 10 au total.
        assertThat(f.get("nc_rate")).isEqualTo(0.4);
    }

    @Test
    void n_envoie_pas_de_taux_quand_le_fournisseur_n_a_aucune_non_conformite() {
        // Envoyer 0 serait un signal « exemplaire » ; l'absence de données n'est pas
        // une bonne nouvelle mesurée. Le modèle accepte des caractéristiques partielles.
        Map<String, Double> f = service.buildFeatures(supplier(80, TODAY.minusDays(30)));

        assertThat(f).doesNotContainKey("nc_rate");
        assertThat(f).doesNotContainKey("nc_trend");
    }

    @Test
    void mesure_la_tendance_par_la_part_des_non_conformites_recentes() {
        when(ncRepo.countBySupplierIdAndStatus(SUPPLIER_ID, NonConformityStatus.OPEN)).thenReturn(4L);
        when(ncRepo.countBySupplierIdAndStatusAndDetectedOnAfter(
                eq(SUPPLIER_ID), eq(NonConformityStatus.OPEN), any())).thenReturn(3L);

        Map<String, Double> f = service.buildFeatures(supplier(80, TODAY.minusDays(30)));

        assertThat(f.get("nc_trend")).isEqualTo(0.75);
    }

    @Test
    void normalise_le_score_qualite_entre_zero_et_un() {
        Map<String, Double> f = service.buildFeatures(supplier(72, TODAY.minusDays(10)));
        assertThat(f.get("audit_score")).isEqualTo(0.72);
    }

    @Test
    void traite_un_fournisseur_jamais_audite_comme_le_cas_le_plus_defavorable() {
        when(auditRepo.findLatestAuditDate(SUPPLIER_ID)).thenReturn(Optional.empty());

        Map<String, Double> f = service.buildFeatures(supplier(80, null));

        assertThat(f.get("days_since_last_audit")).isEqualTo(1.0);
    }

    @Test
    void plafonne_l_anciennete_du_dernier_audit() {
        // Au-delà du plafond, un audit plus ancien n'aggrave plus le risque : sans
        // borne, un fournisseur audité il y a 10 ans écraserait toutes les autres
        // dimensions du modèle.
        when(auditRepo.findLatestAuditDate(SUPPLIER_ID))
                .thenReturn(Optional.of(TODAY.minusDays(SupplierRiskPredictionService.AUDIT_AGE_CAP_DAYS * 3)));

        Map<String, Double> f = service.buildFeatures(supplier(80, null));

        assertThat(f.get("days_since_last_audit")).isEqualTo(1.0);
    }

    @Test
    void prefere_la_date_du_registre_d_audit_a_celle_portee_par_le_fournisseur() {
        when(auditRepo.findLatestAuditDate(SUPPLIER_ID))
                .thenReturn(Optional.of(TODAY.minusDays(SupplierRiskPredictionService.AUDIT_AGE_CAP_DAYS / 2)));

        Map<String, Double> f = service.buildFeatures(supplier(80, TODAY.minusDays(2000)));

        assertThat(f.get("days_since_last_audit")).isEqualTo(0.5);
    }

    // ---- Appel au modèle ---------------------------------------------------------

    @Test
    void restitue_le_score_le_niveau_et_les_facteurs_du_modele() {
        givenSupplier(supplier(80, TODAY.minusDays(30)));
        givenAiScore(64.5, "high");

        SupplierDto.RiskPrediction p = service.predict(SUPPLIER_ID);

        assertThat(p.supplierId()).isEqualTo(SUPPLIER_ID);
        assertThat(p.score()).isEqualTo(64.5);
        assertThat(p.level()).isEqualTo("high");
        assertThat(p.drivers()).hasSize(2);
    }

    @Test
    void classe_les_facteurs_par_contribution_decroissante() {
        givenSupplier(supplier(80, TODAY.minusDays(30)));
        givenAiScore(64.5, "high");

        SupplierDto.RiskPrediction p = service.predict(SUPPLIER_ID);

        // Le facteur le plus déterminant d'abord, quel que soit son signe : c'est ce
        // que l'utilisateur cherche en ouvrant la prédiction (§12.3).
        assertThat(p.drivers().get(0).feature()).isEqualTo("nc_rate");
    }

    @Test
    void renvoie_les_caracteristiques_envoyees_au_modele_pour_l_explicabilite() {
        givenSupplier(supplier(80, TODAY.minusDays(30)));
        givenAiScore(20.0, "low");

        SupplierDto.RiskPrediction p = service.predict(SUPPLIER_ID);

        // Sans elles, le score serait une boîte noire — ce qu'interdit §12.3.
        assertThat(p.features()).containsKey("audit_score");
    }

    @Test
    void echoue_explicitement_sur_un_fournisseur_inconnu_sans_appeler_le_modele() {
        when(supplierRepo.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.predict(SUPPLIER_ID))
                .isInstanceOf(SupplierNotFoundException.class);
        verify(ai, never()).predictSupplierRisk(anyMap());
    }
}

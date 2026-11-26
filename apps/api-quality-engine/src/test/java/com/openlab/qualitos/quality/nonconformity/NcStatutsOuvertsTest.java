package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * « OUVERTES » N'EST PAS UN STATUT MAIS UN ENSEMBLE.
 *
 * <p>Un écart qu'on instruit traverse {@code UNDER_ANALYSIS}, puis
 * {@code ACTION_DEFINED}, puis {@code RESOLVED} sans avoir quitté le travail en
 * cours. Le filtre ne demandait qu'un statut à la fois : l'écart sur lequel on
 * venait de définir une action disparaissait de la liste des ouvertes — c'est-à-dire
 * au moment précis où il commence à mériter un suivi — et l'écart résolu, qui
 * attend encore sa vérification d'efficacité et sa clôture, se rangeait avec les
 * dossiers terminés.
 *
 * <p>Sur la VRAIE base, et non sur un dépôt simulé : ce qui est en cause ici est
 * le prédicat {@code in} de la spécification, et un dépôt simulé l'aurait accepté
 * sans jamais l'exécuter.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class NcStatutsOuvertsTest {

    @Autowired NcService ncs;
    @Autowired TransactionTemplate tx;

    final UUID tenant = UUID.randomUUID();
    final UUID manager = UUID.randomUUID();

    <T> T dans(Supplier<T> f) {
        return tx.execute(s -> f.get());
    }

    @BeforeEach
    void connecte() {
        TenantContext.setTenantId(tenant.toString());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(manager.toString())
                .claim("tenant_id", tenant.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_QUALITY_MANAGER")), manager.toString()));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private UUID declarer(String titre) {
        return dans(() -> ncs.create(new NcDto.CreateRequest(titre, "Constat", NcCategory.PROCESS,
                null, NcSeverity.MINOR, Instant.now(), null, null, null, null, null, null, null,
                null, null, null, null, null, List.of()))).id();
    }

    private List<NcStatus> statutsRendus(List<NcStatus> demandes) {
        return dans(() -> ncs.findAll(demandes, null, null, null, null, null, null,
                PageRequest.of(0, 50))).getContent().stream()
                .map(NcDto.Response::status)
                .toList();
    }

    @Test
    void les_quatre_statuts_du_travail_en_cours_se_demandent_ensemble() {
        UUID ouvert = declarer("Fuite au presse-étoupe");
        UUID enAnalyse = declarer("Étiquetage illisible");
        dans(() -> ncs.startAnalysis(enAnalyse, new NcDto.StartAnalysisRequest("Serrage non maîtrisé")));

        List<NcStatus> rendus = statutsRendus(List.of(NcStatus.OPEN, NcStatus.UNDER_ANALYSIS,
                NcStatus.ACTION_DEFINED, NcStatus.RESOLVED));

        assertThat(rendus).containsExactlyInAnyOrder(NcStatus.OPEN, NcStatus.UNDER_ANALYSIS);
        assertThat(ouvert).isNotEqualTo(enAnalyse);
    }

    /**
     * Un écart en analyse ne se lit PLUS sous le seul {@code OPEN} : c'est
     * exactement ce que l'ancien filtre promettait et ne tenait pas.
     */
    @Test
    void un_seul_statut_demande_ne_rend_que_celui_la() {
        declarer("Fuite au presse-étoupe");
        UUID enAnalyse = declarer("Étiquetage illisible");
        dans(() -> ncs.startAnalysis(enAnalyse, new NcDto.StartAnalysisRequest("Serrage non maîtrisé")));

        assertThat(statutsRendus(List.of(NcStatus.OPEN))).containsExactly(NcStatus.OPEN);
    }

    /**
     * Une liste VIDE vaut « pas de filtre », jamais « aucun statut ».
     *
     * <p>Un {@code in} sur rien ne rendrait aucun écart : le registre
     * paraîtrait vide alors qu'on n'a rien demandé.
     */
    @Test
    void une_liste_vide_ne_filtre_rien() {
        declarer("Fuite au presse-étoupe");

        assertThat(statutsRendus(List.of())).hasSize(1);
        assertThat(statutsRendus(null)).hasSize(1);
    }
}

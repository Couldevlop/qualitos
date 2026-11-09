package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.riskregister.application.RiskRegisterDto;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.riskregister.domain.Identification;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RiskLevel;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Ce que le registre des risques reçoit quand une exigence de partie
 * intéressée lui envoie son risque.
 *
 * <p>La passerelle est le seul endroit où les deux modèles se touchent : ce qui
 * s'y trompe produit une ligne de registre fausse, ou un refus que l'écran ne
 * sait pas expliquer. On vérifie donc la commande elle-même, champ par champ.
 * Ce que le registre fait ensuite d'une commande valide est son affaire, et ses
 * propres tests le couvrent.
 */
class RiskStakeholderGatewayTest {

    private static final UUID PROPRIETAIRE = UUID.randomUUID();
    private static final UUID LIGNE = UUID.randomUUID();

    private final RiskRegisterService registre = mock(RiskRegisterService.class);
    private final RiskStakeholderGateway gateway = new RiskStakeholderGateway(registre);

    @BeforeEach
    void leRegistreAccepte() {
        when(registre.createRisk(any())).thenAnswer(appel -> vue(appel.getArgument(0)));
    }

    @Test
    void laLigneNaitAvecLOrigineExigenceEtSansSource() {
        UUID id = gateway.open(risque("Retard de livraison", "Tracer les lots"));

        assertThat(id).isEqualTo(LIGNE);
        RiskRegisterDto.RiskCommand c = commande();
        assertThat(c.origin()).isEqualTo(RegisterOrigin.STAKEHOLDER);
        assertThat(c.originRef()).isEqualTo("Client A / Tracer les lots");
        // Pas de sourceId : une exigence n'est pas un objet que le registre relit,
        // et la contrainte de base refuse une source sur cette origine.
        assertThat(c.sourceId()).isNull();
        assertThat(c.title()).isEqualTo("Retard de livraison");
        assertThat(c.type()).isEqualTo(RegisterType.QUALITY);
        assertThat(c.process()).isEqualTo("Production");
        assertThat(c.owner()).isEqualTo("Alice Martin");
        assertThat(c.ownerUserId()).isEqualTo(PROPRIETAIRE);
        assertThat(c.cause()).isEqualTo("Tracer les lots");
        assertThat(c.grossSeverity()).isEqualTo(4);
        assertThat(c.grossProbability()).isEqualTo(3);
    }

    @Test
    void niDecisionNiStatutNiCotationResiduelleNeSontImposes() {
        // Ils se prononcent dans la fiche du risque. Les décider ici ferait
        // apparaître au registre un risque que personne n'a arbitré.
        gateway.open(risque("Retard de livraison", "Tracer les lots"));

        RiskRegisterDto.RiskCommand c = commande();
        assertThat(c.decision()).isNull();
        assertThat(c.status()).isNull();
        assertThat(c.residualSeverity()).isNull();
        assertThat(c.residualProbability()).isNull();
        assertThat(c.nextReviewOn()).isNull();
    }

    @Test
    void unEnonceTropLongEstAbregeAuLieuDEtreRefuse() {
        // Une exigence va jusqu'à 500 caractères, l'intitulé du registre s'arrête
        // à 255 : refuser laisserait le risque hors du registre.
        String tresLong = "a".repeat(500);

        gateway.open(risque(tresLong, tresLong));

        RiskRegisterDto.RiskCommand c = commande();
        assertThat(c.title()).hasSize(Identification.TITLE_MAX).endsWith("…");
        assertThat(c.originRef()).hasSize(Identification.ORIGIN_REF_MAX).endsWith("…");
        // L'énoncé complet reste lisible : il part dans l'effet redouté, que le
        // registre stocke sur 4 000 caractères.
        assertThat(c.effect()).hasSizeGreaterThan(Identification.TITLE_MAX);
    }

    @Test
    void uneNatureInconnueEstUnRefusPasUnRabattementSurQualite() {
        StakeholderPorts.NewRisk inconnue = new StakeholderPorts.NewRisk(
                "Client A / Tracer les lots", "Retard", "Tracer les lots", "Retard",
                "PRESQUE_QUALITE", "Production", 3, 3, PROPRIETAIRE, "Alice Martin");

        assertThatThrownBy(() -> gateway.open(inconnue))
                .isInstanceOf(StakeholderValidationException.class);
        verifyNoInteractions(registre);
    }

    @Test
    void uneNatureAbsenteEstUnRefus() {
        StakeholderPorts.NewRisk sansNature = new StakeholderPorts.NewRisk(
                "Client A / Tracer les lots", "Retard", "Tracer les lots", "Retard",
                "  ", "Production", 3, 3, PROPRIETAIRE, "Alice Martin");

        assertThatThrownBy(() -> gateway.open(sansNature))
                .isInstanceOf(StakeholderValidationException.class);
        verifyNoInteractions(registre);
    }

    @Test
    void laNatureEstLueSansEgardALaCasse() {
        gateway.open(new StakeholderPorts.NewRisk(
                "Client A / Tracer les lots", "Retard", "Tracer les lots", "Retard",
                " health_safety ", "Production", 3, 3, PROPRIETAIRE, "Alice Martin"));

        assertThat(commande().type()).isEqualTo(RegisterType.HEALTH_SAFETY);
    }

    // ---------- aides ----------

    private static StakeholderPorts.NewRisk risque(String titre, String exigence) {
        return new StakeholderPorts.NewRisk("Client A / " + exigence, titre, exigence,
                titre + " (impact estime : MEDIUM)", "QUALITY", "Production", 4, 3,
                PROPRIETAIRE, "Alice Martin");
    }

    /** La commande que la passerelle a passée au registre. */
    private RiskRegisterDto.RiskCommand commande() {
        ArgumentCaptor<RiskRegisterDto.RiskCommand> capteur =
                ArgumentCaptor.forClass(RiskRegisterDto.RiskCommand.class);
        verify(registre).createRisk(capteur.capture());
        return capteur.getValue();
    }

    private static RiskRegisterDto.RiskView vue(RiskRegisterDto.RiskCommand c) {
        return new RiskRegisterDto.RiskView(LIGNE, "RSK-001", c.title(), c.type(), c.process(),
                c.site(), c.owner(), c.cause(), c.effect(), c.origin(), c.originRef(), null,
                c.grossSeverity(), c.grossProbability(),
                c.grossSeverity() * c.grossProbability(), RiskLevel.HIGH,
                null, null, null, null, null, null, List.of(), null, null,
                Instant.EPOCH, Instant.EPOCH, c.ownerUserId());
    }
}

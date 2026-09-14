package com.openlab.qualitos.quality.apqp;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Copie le référentiel dans le cycle d'un projet qui vient de naître.
 *
 * <p>Composant à part, et non méthode privée d'un service : deux appelants en ont
 * besoin — l'ouverture d'un projet, et la réinitialisation de son cycle — et les
 * laisser chacun avec sa copie aurait fait diverger les deux amorçages au premier
 * ajustement du référentiel.
 *
 * <p>Chaque ligne garde sa CLÉ en plus de son texte : le texte stocké est le
 * français, langue source du projet, et la clé permet de rendre la ligne dans la
 * langue demandée tant que personne ne l'a retouchée (ADR 0070).
 */
@Component
public class ApqpCycleSeeder {

    private final ApqpPhaseRepository phases;

    public ApqpCycleSeeder(ApqpPhaseRepository phases) {
        this.phases = phases;
    }

    /** Écrit les cinq phases du référentiel et leurs livrables dans ce projet. */
    public void amorcer(ApqpProject projet) {
        Locale source = Locale.forLanguageTag(ApqpReferenceTranslations.DEFAUT);
        int rang = 1;
        List<ApqpPhase> cycle = new ArrayList<>();

        for (ApqpReference.PhaseModele modele : ApqpReference.PHASES) {
            ApqpPhase phase = new ApqpPhase();
            phase.setProject(projet);
            phase.setTenantId(projet.getTenantId());
            phase.setPosition(rang++);
            phase.setReferenceKey(modele.cle());
            phase.setTitle(ApqpReferenceTranslations.texte(modele.cle() + ".title", source));
            phase.setPurpose(ApqpReferenceTranslations.texte(modele.cle() + ".purpose", source));
            phase.setQuestion(ApqpReferenceTranslations.texte(modele.cle() + ".question", source));

            int rangLivrable = 1;
            for (ApqpReference.LivrableModele modeleLivrable : modele.livrables()) {
                ApqpDeliverable livrable = new ApqpDeliverable();
                livrable.setReferenceKey(modeleLivrable.cle());
                livrable.setLabel(ApqpReferenceTranslations.texte(modeleLivrable.cle(), source));
                livrable.setExpectedArtifact(
                        ApqpReferenceTranslations.texte(modeleLivrable.artefactCle(), source));
                livrable.setPosition(rangLivrable++);
                // Valeur d'AMORÇAGE de la colonne « PPAP Req'd » : le client la
                // pilote ensuite livrable par livrable (ADR 0072).
                livrable.setPpap(modeleLivrable.ppap());
                livrable.setStatus(ApqpDeliverableStatus.NOT_STARTED);
                livrable.setPercentComplete(0);
                phase.addDeliverable(livrable);
            }
            cycle.add(phase);
        }
        phases.saveAll(cycle);
    }
}

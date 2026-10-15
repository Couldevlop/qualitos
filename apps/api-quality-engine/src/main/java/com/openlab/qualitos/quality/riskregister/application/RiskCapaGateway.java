package com.openlab.qualitos.quality.riskregister.application;

import com.openlab.qualitos.quality.riskregister.domain.Risk;
import com.openlab.qualitos.quality.riskregister.domain.RiskCapaKind;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port vers le module CAPA : ouvrir un dossier depuis un risque, et retrouver
 * ceux qui en procèdent.
 *
 * <p>Le lien est porté par le dossier CAPA lui-même (source {@code RISK},
 * référence du risque) et non par une table du registre : c'est déjà ainsi que
 * la CAPA dit d'où elle vient, et une seconde table pourrait le contredire.
 */
public interface RiskCapaGateway {

    /**
     * Ouvre le dossier ET son action : une CAPA porte une seule action, celle
     * qu'on décide ici, confiée au compte {@code ownerId} de l'annuaire, qui
     * pilote aussi le dossier (ADR 0088).
     */
    LinkedCapa open(Risk risk, String title, String description, RiskCapaKind kind, UUID ownerId,
                    String ownerName, LocalDate dueDate, Verification verification);

    List<LinkedCapa> linkedTo(Risk risk);

    /**
     * La décision de vérification d'efficacité prise à l'ouverture (ADR 0073,
     * 0088) : {@code required} nul laisse la question ouverte. Le module CAPA
     * en tient les gardes — exiger sans désigner est refusé.
     */
    record Verification(Boolean required, UUID assigneeId, String assigneeName, String instructions) {
        /** Aucune décision transmise : la question reste ouverte sur le dossier. */
        public static final Verification UNDECIDED = new Verification(null, null, null, null);
    }

    /** Un dossier CAPA réduit à ce que le tableau « Traitement » montre. */
    record LinkedCapa(UUID id, String title, LocalDate dueDate, String status, RiskCapaKind kind,
                      String assignee) {}
}

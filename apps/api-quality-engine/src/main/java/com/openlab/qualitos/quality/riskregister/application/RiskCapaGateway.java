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
     * Ouvre le dossier ET son action : le dossier issu d'un risque porte une
     * seule action, celle qu'on décide ici, confiée à {@code assignee}.
     */
    LinkedCapa open(Risk risk, String title, String description, RiskCapaKind kind, String assignee,
                    LocalDate dueDate, UUID ownerId);

    List<LinkedCapa> linkedTo(Risk risk);

    /** Un dossier CAPA réduit à ce que le tableau « Traitement » montre. */
    record LinkedCapa(UUID id, String title, LocalDate dueDate, String status, RiskCapaKind kind,
                      String assignee) {}
}

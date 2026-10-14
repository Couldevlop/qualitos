package com.openlab.qualitos.core.directory;

import com.openlab.qualitos.core.user.DirectoryRow;

import java.util.List;
import java.util.UUID;

/**
 * Le contrat de l'annuaire en lecture restreinte (ADR 0091).
 *
 * <p>Volontairement pauvre : de quoi reconnaître une personne et la désigner,
 * rien de ce qui sert à l'administrer (rôles, invitation, compte de connexion,
 * dates). Un champ ajouté ici devient lisible par TOUS les membres du client.
 */
public final class DirectoryDto {

    private DirectoryDto() {}

    /**
     * Une personne de l'annuaire.
     *
     * @param displayName le nom saisi à l'invitation, ou l'adresse quand aucun nom
     *                    n'a été enregistré — jamais vide
     * @param email       l'adresse professionnelle : elle départage deux homonymes,
     *                    qu'aucun autre champ restreint ne distingue
     * @param active      faux pour un ancien membre, lu par son identifiant afin
     *                    d'afficher le responsable d'un dossier passé ; la
     *                    recherche ne rend que des membres actifs
     */
    public record Entry(UUID id, String displayName, String email, boolean active) {

        static Entry from(DirectoryRow row) {
            String name = row.displayName() == null || row.displayName().isBlank()
                    ? row.email() : row.displayName();
            return new Entry(row.id(), name, row.email(), row.active());
        }
    }

    /** Une page de résultats, sous une forme stable (pas la sérialisation interne de Spring Data). */
    public record Page(List<Entry> content, int page, int size, long totalElements, int totalPages) {}
}

package com.openlab.qualitos.quality.costofquality.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Un libellé de ligne : ce que la liste déroulante propose.
 *
 * <p>Deux origines. Les libellés LIVRÉS n'ont pas de tenant et portent un
 * {@code code} stable, que l'écran traduit ; ils sont communs à tous les
 * clients. Les libellés SAISIS en texte libre appartiennent à un tenant et
 * n'ont pas de code : leur nom est le texte tapé, et ils rejoignent la liste
 * pour les mois suivants.
 *
 * <p>{@code partControl} dit si une ligne de ce libellé porte sur des pièces.
 * C'est lui, et lui seul, qui rend obligatoires la référence, le nombre de
 * pièces, le lot et la date de réception ou de fabrication.
 */
public final class CoqLabel {

    public static final int NAME_MAX = 150;
    /** Les libellés saisis se rangent après ceux du catalogue, puis par nom. */
    public static final int CUSTOM_POSITION = 1000;

    private final UUID id;
    private final UUID tenantId;
    private final CoqCategory category;
    private final String code;
    private final String name;
    private final boolean partControl;
    private final int position;

    public CoqLabel(UUID id, UUID tenantId, CoqCategory category, String code,
                    String name, boolean partControl, int position) {
        this.id = id;
        this.tenantId = tenantId;
        this.category = Objects.requireNonNull(category, "category");
        this.code = code;
        this.name = name;
        this.partControl = partControl;
        this.position = position;
    }

    /** Un libellé tapé en texte libre par un utilisateur de ce tenant. */
    public static CoqLabel custom(UUID tenantId, CoqCategory category, String name, boolean partControl) {
        Objects.requireNonNull(tenantId, "tenantId");
        if (category == null) {
            throw new CoqValidationException("category", "La famille de coût est obligatoire.");
        }
        String nom = name == null ? "" : name.strip();
        if (nom.isEmpty()) {
            throw new CoqValidationException("name", "Le libellé est obligatoire.");
        }
        if (nom.length() > NAME_MAX) {
            throw new CoqValidationException("name",
                    "Le libellé dépasse " + NAME_MAX + " caractères.");
        }
        return new CoqLabel(null, tenantId, category, null, nom, partControl, CUSTOM_POSITION);
    }

    /** @return vrai pour un libellé du catalogue livré, commun à tous les tenants. */
    public boolean builtIn() {
        return tenantId == null;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public CoqCategory getCategory() { return category; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isPartControl() { return partControl; }
    public int getPosition() { return position; }
}

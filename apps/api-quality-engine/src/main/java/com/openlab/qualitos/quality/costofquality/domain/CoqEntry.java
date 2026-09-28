package com.openlab.qualitos.quality.costofquality.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Une imputation de coût : un montant, sous un libellé, à une date.
 *
 * <p><b>La date d'imputation range la ligne dans son mois.</b> Il n'y a pas de
 * champ « période » à côté : deux dates qui pourraient se contredire en
 * feraient une de trop, et c'est la date comptable qui fait foi. Corriger la
 * date déplace donc la ligne d'un mois à l'autre, et c'est voulu.
 *
 * <p>La famille est recopiée du libellé à l'écriture. Un libellé ne change
 * jamais de famille, et la porter ici laisse les totaux se calculer sans
 * relire le catalogue.
 */
public final class CoqEntry {

    public static final int RESPONSIBLE_MAX = 150;
    public static final int COMMENT_MAX = 2000;
    public static final int PART_REFERENCE_MAX = 120;
    public static final int LOT_MAX = 120;
    /** NUMERIC(14,2) : douze chiffres avant la virgule. */
    private static final BigDecimal AMOUNT_MAX = new BigDecimal("999999999999.99");

    private final UUID id;
    private final UUID tenantId;
    private UUID labelId;
    private CoqCategory category;
    private BigDecimal amount;
    private String responsible;
    private LocalDate imputationDate;
    private String comment;
    private boolean partControl;
    private String partReference;
    private Integer partQuantity;
    private String lot;
    private LocalDate receivedOrMadeOn;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("java:S107") // reconstitution depuis la persistance : un champ par colonne
    public CoqEntry(UUID id, UUID tenantId, UUID labelId, CoqCategory category, BigDecimal amount,
                    String responsible, LocalDate imputationDate, String comment,
                    boolean partControl, String partReference, Integer partQuantity, String lot,
                    LocalDate receivedOrMadeOn, UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.labelId = labelId;
        this.category = category;
        this.amount = amount;
        this.responsible = responsible;
        this.imputationDate = imputationDate;
        this.comment = comment;
        this.partControl = partControl;
        this.partReference = partReference;
        this.partQuantity = partQuantity;
        this.lot = lot;
        this.receivedOrMadeOn = receivedOrMadeOn;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Une ligne neuve, validée. */
    public static CoqEntry record(UUID tenantId, CoqLabel label, CoqEntryDetails details,
                                  UUID createdBy, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId");
        CoqEntry ligne = new CoqEntry(null, tenantId, null, null, null, null, null, null,
                false, null, null, null, null, createdBy, now, now);
        ligne.apply(label, details, now);
        return ligne;
    }

    /** Corrige la ligne : libellé, montant, date et détails, revalidés d'un bloc. */
    public void revise(CoqLabel label, CoqEntryDetails details, Instant now) {
        apply(label, details, now);
    }

    private void apply(CoqLabel label, CoqEntryDetails d, Instant now) {
        if (label == null) {
            throw new CoqValidationException("labelId", "Le libellé est obligatoire.");
        }
        if (d == null) {
            throw new CoqValidationException("amount", "Le montant est obligatoire.");
        }
        BigDecimal montant = requireAmount(d.amount());
        String qui = requireText("responsible", d.responsible(), RESPONSIBLE_MAX,
                "Le responsable est obligatoire.");
        if (d.imputationDate() == null) {
            throw new CoqValidationException("imputationDate", "La date d'imputation est obligatoire.");
        }
        String note = optionalText("comment", d.comment(), COMMENT_MAX);

        String reference = null;
        Integer quantite = null;
        String numLot = null;
        LocalDate receptionOuFabrication = null;
        if (label.isPartControl()) {
            reference = requireText("partReference", d.partReference(), PART_REFERENCE_MAX,
                    "La référence de la pièce ou du produit est obligatoire.");
            if (d.partQuantity() == null || d.partQuantity() < 1) {
                throw new CoqValidationException("partQuantity",
                        "Le nombre de pièces est obligatoire et doit être au moins 1.");
            }
            quantite = d.partQuantity();
            numLot = requireText("lot", d.lot(), LOT_MAX, "Le lot est obligatoire.");
            if (d.receivedOrMadeOn() == null) {
                throw new CoqValidationException("receivedOrMadeOn",
                        "La date de réception ou de fabrication est obligatoire.");
            }
            receptionOuFabrication = d.receivedOrMadeOn();
        }

        this.labelId = label.getId();
        this.category = label.getCategory();
        this.partControl = label.isPartControl();
        this.amount = montant;
        this.responsible = qui;
        this.imputationDate = d.imputationDate();
        this.comment = note;
        this.partReference = reference;
        this.partQuantity = quantite;
        this.lot = numLot;
        this.receivedOrMadeOn = receptionOuFabrication;
        this.updatedAt = now;
    }

    private static BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null) {
            throw new CoqValidationException("amount", "Le montant est obligatoire.");
        }
        if (amount.signum() < 0) {
            throw new CoqValidationException("amount", "Le montant ne peut pas être négatif.");
        }
        if (amount.scale() > 2) {
            throw new CoqValidationException("amount", "Le montant a au plus deux décimales.");
        }
        if (amount.compareTo(AMOUNT_MAX) > 0) {
            throw new CoqValidationException("amount", "Le montant dépasse la limite admise.");
        }
        return amount;
    }

    private static String requireText(String field, String value, int max, String missing) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            throw new CoqValidationException(field, missing);
        }
        if (v.length() > max) {
            throw new CoqValidationException(field, "Ce champ dépasse " + max + " caractères.");
        }
        return v;
    }

    private static String optionalText(String field, String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.strip();
        if (v.length() > max) {
            throw new CoqValidationException(field, "Ce champ dépasse " + max + " caractères.");
        }
        return v;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getLabelId() { return labelId; }
    public CoqCategory getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getResponsible() { return responsible; }
    public LocalDate getImputationDate() { return imputationDate; }
    public String getComment() { return comment; }
    public boolean isPartControl() { return partControl; }
    public String getPartReference() { return partReference; }
    public Integer getPartQuantity() { return partQuantity; }
    public String getLot() { return lot; }
    public LocalDate getReceivedOrMadeOn() { return receivedOrMadeOn; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

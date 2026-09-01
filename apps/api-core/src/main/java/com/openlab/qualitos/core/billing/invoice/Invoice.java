package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Une facture émise : l'en-tête, et ses lignes.
 *
 * <p><b>Rien n'y est joint, tout y est recopié.</b> Le libellé du module, le
 * palier, le montant unitaire viennent de l'abonnement au moment de l'émission
 * et n'en dépendent plus. Une jointure ferait changer une facture de l'an
 * dernier le jour où le contrat change — or une pièce comptable ne se corrige
 * pas, elle s'annule par une autre pièce.
 *
 * <p>Tout est {@code updatable = false} à deux exceptions près, {@code sentAt}
 * et {@code sentTo} : l'envoi est le seul événement qui arrive à une facture
 * après son émission. Le reste, s'il devait changer, exigerait un avoir.
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 24)
    @Pattern(regexp = "^FA-[0-9]{4}-[0-9]{4,}$")
    @Column(name = "number", nullable = false, length = 24, updatable = false)
    private String number;

    @Column(name = "fiscal_year", nullable = false, updatable = false)
    private int fiscalYear;

    @Column(name = "period_year", nullable = false, updatable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false, updatable = false)
    private int periodMonth;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @PositiveOrZero
    @Column(name = "total_cents", nullable = false, updatable = false)
    private long totalCents;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @NotNull
    @Column(name = "issued_by", nullable = false, updatable = false)
    private UUID issuedBy;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Size(max = 320)
    @Column(name = "sent_to", length = 320)
    private String sentTo;

    // CascadeType.ALL + orphanRemoval : les lignes n'existent pas sans leur
    // facture, exactement ce que dit la clé étrangère ON DELETE CASCADE.
    // EAGER : une facture sans ses lignes ne dit pas ce qu'elle facture, et
    // tous les usages — rendu PDF, envoi, consultation — les veulent.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id", nullable = false)
    @OrderBy("lineNo ASC")
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    /** La période facturée, telle qu'elle a été demandée. */
    public YearMonth period() {
        return YearMonth.of(periodYear, periodMonth);
    }

    /** Le total, devise comprise — jamais un {@code long} nu. */
    public Money total() {
        return Money.of(totalCents, currency);
    }

    /** Une facture envoyée l'est une fois pour toutes. */
    public boolean isSent() {
        return sentAt != null;
    }

    /**
     * Marque la facture envoyée, à une date et vers un destinataire nommés.
     *
     * <p>Un second envoi est refusé plutôt qu'ignoré : deux exemplaires de la
     * même facture, c'est un litige — le client ne sait pas s'il doit payer une
     * fois ou deux, et rien dans les deux exemplaires ne le lui dit.
     */
    public void markSent(String recipient, Instant when) {
        if (isSent()) {
            throw new IllegalStateException(
                    "Facture " + number + " deja envoyee le " + sentAt + " a " + sentTo);
        }
        this.sentTo = recipient;
        this.sentAt = when;
    }
}

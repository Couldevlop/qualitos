package com.openlab.qualitos.core.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * L'identité de FACTURATION d'un client, distincte de son identité technique
 * portée par {@link com.openlab.qualitos.core.tenant.Tenant}.
 *
 * <p>Table séparée de {@code tenants} : un tenant existe dès l'inscription,
 * son profil de facturation n'arrive qu'à la signature du contrat. Les fondre
 * obligerait à inventer des valeurs vides le jour de la création du tenant.
 *
 * <p>Un seul profil par tenant (contrainte {@code UNIQUE} en base sur
 * {@code tenant_id}) : deux profils pour un même client donneraient deux
 * factures. C'est pourquoi cette entité n'a pas de constructeur "creation
 * forcee" — {@link BillingProfileService#upsert} decide lui-meme s'il cree ou
 * met a jour, a partir de ce qui existe deja.
 */
@Entity
@Table(
    name = "billing_profiles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_billing_profiles_tenant", columnNames = "tenant_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingProfile {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 250)
    @Column(name = "legal_name", nullable = false, length = 250)
    private String legalName;

    @Size(max = 64)
    @Column(name = "vat_number", length = 64)
    private String vatNumber;

    @NotBlank
    @Size(max = 250)
    @Column(name = "address_line1", nullable = false, length = 250)
    private String addressLine1;

    @Size(max = 250)
    @Column(name = "address_line2", length = 250)
    private String addressLine2;

    @NotBlank
    @Size(max = 32)
    @Column(name = "postal_code", nullable = false, length = 32)
    private String postalCode;

    @NotBlank
    @Size(max = 120)
    @Column(name = "city", nullable = false, length = 120)
    private String city;

    // ISO 3166-1 alpha-2, meme forme que la contrainte SQL chk_billing_country.
    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$")
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @NotBlank
    @Size(max = 320)
    @Column(name = "billing_email", nullable = false, length = 320)
    private String billingEmail;

    // ISO 4217, meme forme que Money (billing.Money) et la contrainte SQL
    // chk_billing_currency.
    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "billing_exempt", nullable = false)
    private boolean billingExempt;

    @Size(max = 250)
    @Column(name = "exemption_reason", length = 250)
    private String exemptionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

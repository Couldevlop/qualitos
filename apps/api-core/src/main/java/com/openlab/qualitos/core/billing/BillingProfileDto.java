package com.openlab.qualitos.core.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO du profil de facturation. Records Java 21, comme
 * {@link com.openlab.qualitos.core.tenant.TenantDto} : la commande d'écriture
 * ({@code SaveCommand}) et la vue de lecture ({@code View}) n'ont pas la même
 * forme — la commande ne connaît pas encore l'id ni les horodatages, que le
 * service seul attribue.
 */
public sealed interface BillingProfileDto permits BillingProfileDto.SaveCommand, BillingProfileDto.View {

    /**
     * Commande d'écriture (création ou mise à jour). Les validations
     * annotées miroitent les contraintes SQL de {@code billing_profiles} ;
     * la règle "exemption motivée", elle, n'est pas déclarative — elle
     * dépend de deux champs à la fois et est vérifiée par
     * {@link BillingProfileService}.
     */
    record SaveCommand(
        @NotBlank
        @Size(max = 250)
        String legalName,

        @Size(max = 64)
        String vatNumber,

        @NotBlank
        @Size(max = 250)
        String addressLine1,

        @Size(max = 250)
        String addressLine2,

        @NotBlank
        @Size(max = 32)
        String postalCode,

        @NotBlank
        @Size(max = 120)
        String city,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}$")
        String countryCode,

        @NotBlank
        @Size(max = 320)
        String billingEmail,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        boolean billingExempt,

        @Size(max = 250)
        String exemptionReason
    ) implements BillingProfileDto {}

    record View(
        UUID id,
        UUID tenantId,
        String legalName,
        String vatNumber,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String city,
        String countryCode,
        String billingEmail,
        String currency,
        boolean billingExempt,
        String exemptionReason,
        Instant createdAt,
        Instant updatedAt
    ) implements BillingProfileDto {

        public static View from(BillingProfile profile) {
            return new View(
                profile.getId(),
                profile.getTenantId(),
                profile.getLegalName(),
                profile.getVatNumber(),
                profile.getAddressLine1(),
                profile.getAddressLine2(),
                profile.getPostalCode(),
                profile.getCity(),
                profile.getCountryCode(),
                profile.getBillingEmail(),
                profile.getCurrency(),
                profile.isBillingExempt(),
                profile.getExemptionReason(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
            );
        }
    }
}

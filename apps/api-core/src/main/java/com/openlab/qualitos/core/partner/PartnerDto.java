package com.openlab.qualitos.core.partner;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Ce qui entre et ce qui sort du domaine partenaire. */
public final class PartnerDto {

    private PartnerDto() {
    }

    // ---------- le cabinet ----------

    public record FirmRequest(
            @NotBlank @Size(min = 3, max = 63)
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
                    message = "identifiant attendu en minuscules, chiffres et tirets")
            String slug,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 64) String registrationNumber,
            @Email @Size(max = 254) String contactEmail,
            @Size(max = 32) String contactPhone
    ) {}

    public record FirmResponse(
            UUID id,
            String slug,
            String name,
            String registrationNumber,
            String contactEmail,
            String contactPhone,
            boolean active,
            long consultantCount,
            long grantedCount,
            Instant createdAt
    ) {
        static FirmResponse from(PartnerFirm firm, long consultants, long granted) {
            return new FirmResponse(firm.getId(), firm.getSlug(), firm.getName(),
                    firm.getRegistrationNumber(), firm.getContactEmail(), firm.getContactPhone(),
                    firm.isActive(), consultants, granted, firm.getCreatedAt());
        }
    }

    // ---------- les consultants ----------

    public record ConsultantRequest(
            @NotBlank @Size(max = 255) String displayName,
            @NotBlank @Email @Size(max = 254) String email
    ) {}

    public record ConsultantResponse(
            UUID id,
            UUID firmId,
            String displayName,
            String email,
            boolean active,
            /** Vrai des que la personne s'est connectee une premiere fois. */
            boolean linked
    ) {
        static ConsultantResponse from(PartnerConsultant c) {
            return new ConsultantResponse(c.getId(), c.getFirmId(), c.getDisplayName(),
                    c.getEmail(), c.isActive(), c.getAccountId() != null);
        }
    }

    // ---------- les accords ----------

    /** Ce que l'editeur propose : un cabinet, un client, et l'objet de la mission. */
    public record ProposeRequest(
            @NotNull UUID tenantId,
            @NotNull EngagementScope scope,
            @Size(max = 500) String purpose
    ) {}

    /**
     * Ce que le client accorde. La fin est OBLIGATOIRE et dans le futur : un
     * acces sans terme devient un acces oublie, et c'est exactement ce que cet
     * accord existe pour empecher.
     */
    public record GrantRequest(
            @NotNull EngagementScope scope,
            @NotNull @Future LocalDate endsOn,
            @Size(max = 500) String purpose
    ) {}

    public record EngagementResponse(
            UUID id,
            UUID tenantId,
            String tenantName,
            UUID firmId,
            String firmName,
            EngagementScope scope,
            EngagementStatus status,
            LocalDate startsOn,
            LocalDate endsOn,
            String purpose,
            /** Vrai si l'acces est ouvert AUJOURD'HUI : statut accorde ET dates tenantes. */
            boolean openToday,
            Instant proposedAt,
            Instant decidedAt,
            Instant revokedAt
    ) {
        static EngagementResponse from(PartnerEngagement e, String tenantName, String firmName,
                                       LocalDate today) {
            return new EngagementResponse(e.getId(), e.getTenantId(), tenantName,
                    e.getFirmId(), firmName, e.getScope(), e.getStatus(),
                    e.getStartsOn(), e.getEndsOn(), e.getPurpose(),
                    e.opensAccessOn(today), e.getProposedAt(), e.getDecidedAt(), e.getRevokedAt());
        }
    }
}

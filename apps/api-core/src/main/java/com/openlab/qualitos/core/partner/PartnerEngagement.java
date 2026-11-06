package com.openlab.qualitos.core.partner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * L'accord par lequel un client laisse un cabinet entrer chez lui.
 *
 * <p>C'est la piece qui porte la regle de la specification (D2) : <b>un
 * consultant n'entre jamais chez un client sans accord enregistre DE CE
 * CLIENT</b>. L'editeur constitue le portefeuille — il propose ; il n'accorde
 * pas. Seul le client accorde, et il peut reprendre a tout instant.
 *
 * <p>L'accord porte sur le CABINET, pas sur chaque consultant : le client
 * contracte avec un cabinet, qui change de consultant en cours de mission sans
 * que le client ait a re-signer. La personne, elle, est tracee a l'usage —
 * chaque acces est journalise nominativement.
 */
@Entity
@Table(name = "partner_engagements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerEngagement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 16)
    private EngagementScope scope;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EngagementStatus status = EngagementStatus.PROPOSED;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Column(name = "proposed_by")
    private UUID proposedBy;

    @Column(name = "proposed_at")
    private Instant proposedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Size(max = 500)
    @Column(name = "purpose", length = 500)
    private String purpose;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Vrai si l'acces est ouvert AUJOURD'HUI.
     *
     * <p>Le statut ne suffit pas : un accord accorde dont la fin est passee
     * n'ouvre plus rien, et personne n'est venu changer sa ligne en base. La
     * date fait foi a chaque lecture, sinon l'expiration dependrait d'un travail
     * de fond qui peut ne pas tourner.
     */
    public boolean opensAccessOn(LocalDate day) {
        if (status != EngagementStatus.GRANTED || day == null) {
            return false;
        }
        boolean commence = startsOn == null || !day.isBefore(startsOn);
        boolean pasFini = endsOn == null || !day.isAfter(endsOn);
        return commence && pasFini;
    }
}

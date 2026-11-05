package com.openlab.qualitos.core.partner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
import java.util.UUID;

/**
 * Une personne du cabinet.
 *
 * <p>{@code accountId} est le {@code sub} du jeton (ADR 0091), et il reste nul
 * tant que la personne ne s'est pas connectee : on inscrit un consultant au
 * cabinet avant qu'il ait un compte, sinon une arrivee demanderait deux gestes
 * dans deux outils, dans le bon ordre.
 *
 * <p>Une personne appartient a UN cabinet. Deux cabinets sur le meme compte
 * rendraient indecidable au nom de qui elle entre chez un client — et donc quel
 * accord l'autorise.
 */
@Entity
@Table(name = "partner_consultants")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerConsultant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "firm_id", nullable = false)
    private UUID firmId;

    @Column(name = "account_id")
    private UUID accountId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}

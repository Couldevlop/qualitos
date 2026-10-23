package com.openlab.qualitos.quality.docs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Un signataire désigné à la soumission d'une version (ADR 0099) : une personne
 * de l'annuaire, à son rang (1 à 3). Les rangs signent dans l'ordre.
 */
@Entity
@Table(name = "document_version_signatories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_doc_signatory_position", columnNames = {"version_id", "position"}),
                @UniqueConstraint(name = "uk_doc_signatory_signer", columnNames = {"version_id", "signer_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class DocumentVersionSignatory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false, updatable = false)
    private DocumentVersion version;

    @Column(nullable = false, updatable = false)
    private int position;

    /** Le compte de l'annuaire (le {@code sub} du jeton). */
    @Column(name = "signer_id", nullable = false, updatable = false)
    private UUID signerId;

    /** Le nom affiché, relevé dans l'annuaire à la désignation. */
    @Column(name = "signer_name", nullable = false, length = 255)
    private String signerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SignatoryDecision decision = SignatoryDecision.PENDING;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(length = 1000)
    private String comment;
}

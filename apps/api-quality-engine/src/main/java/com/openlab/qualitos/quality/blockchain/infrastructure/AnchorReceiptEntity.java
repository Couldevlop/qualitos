package com.openlab.qualitos.quality.blockchain.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Reçu d'ancrage signé et chaîné (ADR 0012 Phase A). Append-only — aucune
 * méthode de mutation après construction ; l'immutabilité est aussi garantie
 * par un trigger Postgres (Flyway V62).
 */
@Entity
@Table(name = "anchor_receipts")
public class AnchorReceiptEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "seq_no", nullable = false)
    private long seqNo;

    @Column(name = "merkle_root", nullable = false, length = 64)
    private String merkleRoot;

    @Column(name = "prev_receipt_hash", nullable = false, length = 64)
    private String prevReceiptHash;

    @Column(name = "receipt_hash", nullable = false, length = 64)
    private String receiptHash;

    @Column(name = "signature", nullable = false, columnDefinition = "text")
    private String signature;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AnchorReceiptEntity() {
        // JPA
    }

    public AnchorReceiptEntity(UUID id, UUID tenantId, long seqNo, String merkleRoot,
                               String prevReceiptHash, String receiptHash, String signature,
                               Instant signedAt, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.seqNo = seqNo;
        this.merkleRoot = merkleRoot;
        this.prevReceiptHash = prevReceiptHash;
        this.receiptHash = receiptHash;
        this.signature = signature;
        this.signedAt = signedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public long getSeqNo() { return seqNo; }
    public String getMerkleRoot() { return merkleRoot; }
    public String getPrevReceiptHash() { return prevReceiptHash; }
    public String getReceiptHash() { return receiptHash; }
    public String getSignature() { return signature; }
    public Instant getSignedAt() { return signedAt; }
    public Instant getCreatedAt() { return createdAt; }
}

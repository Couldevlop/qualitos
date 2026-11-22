package com.openlab.qualitos.quality.derogation.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * La dérogation telle qu'elle est stockée. Rien d'autre.
 *
 * <p>Aucune règle ici : elles sont toutes sur l'agrégat {@code Derogation}, que
 * cette classe ne fait que transporter vers la base et en ramener. Les
 * énumérations sont écrites en {@code String} parce que la migration les contrôle
 * par des contraintes nommées, lisibles dans la base.
 */
@Entity
@Table(name = "derogations",
        indexes = {
                @Index(name = "ix_derogations_tenant_status",
                        columnList = "tenant_id, status, created_at"),
                @Index(name = "ix_derogations_tenant_owner",
                        columnList = "tenant_id, created_by_user_id")
        })
public class DerogationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 40, updatable = false)
    private String reference;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_quality_contact", length = 200)
    private String customerQualityContact;

    @Column(name = "customer_order_no", length = 100)
    private String customerOrderNo;

    @Column(name = "part_reference", length = 200)
    private String partReference;

    @Column(name = "part_index", length = 40)
    private String partIndex;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "shipment_stage", nullable = false, length = 20)
    private String shipmentStage;

    @Column(name = "deviation_origin", nullable = false, length = 20)
    private String deviationOrigin;

    @Column(nullable = false, length = 20)
    private String occurrence;

    @Column(name = "duration_kind", nullable = false, length = 20)
    private String durationKind;

    @Column(name = "duration_detail", length = 300)
    private String durationDetail;

    @Column(columnDefinition = "text")
    private String justification;

    @Column(name = "root_cause", columnDefinition = "text")
    private String rootCause;

    @Column(columnDefinition = "text")
    private String containment;

    @Column(name = "corrective_action", columnDefinition = "text")
    private String correctiveAction;

    @Column(name = "capa_reference", length = 200)
    private String capaReference;

    @Column(name = "attachments_note", length = 500)
    private String attachmentsNote;

    @Column(name = "manager_account_id")
    private UUID managerAccountId;

    @Column(name = "manager_account_name", length = 200)
    private String managerAccountName;

    @Column(name = "signed_by_name", length = 200)
    private String signedByName;

    @Column(name = "signature_png")
    private byte[] signaturePng;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "amend_reason", columnDefinition = "text")
    private String amendReason;

    @Column(name = "customer_email", length = 320)
    private String customerEmail;

    @Column(name = "send_comment", columnDefinition = "text")
    private String sendComment;

    @Column(name = "pdf_language", length = 5)
    private String pdfLanguage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "customer_decision", nullable = false, length = 30)
    private String customerDecision;

    @Column(name = "customer_decision_date")
    private LocalDate customerDecisionDate;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_by_name", length = 200, updatable = false)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID v) {
        this.tenantId = v;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String v) {
        this.reference = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String v) {
        this.customerName = v;
    }

    public String getCustomerQualityContact() {
        return customerQualityContact;
    }

    public void setCustomerQualityContact(String v) {
        this.customerQualityContact = v;
    }

    public String getCustomerOrderNo() {
        return customerOrderNo;
    }

    public void setCustomerOrderNo(String v) {
        this.customerOrderNo = v;
    }

    public String getPartReference() {
        return partReference;
    }

    public void setPartReference(String v) {
        this.partReference = v;
    }

    public String getPartIndex() {
        return partIndex;
    }

    public void setPartIndex(String v) {
        this.partIndex = v;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String v) {
        this.batchNumber = v;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal v) {
        this.quantity = v;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate v) {
        this.requestDate = v;
    }

    public String getShipmentStage() {
        return shipmentStage;
    }

    public void setShipmentStage(String v) {
        this.shipmentStage = v;
    }

    public String getDeviationOrigin() {
        return deviationOrigin;
    }

    public void setDeviationOrigin(String v) {
        this.deviationOrigin = v;
    }

    public String getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(String v) {
        this.occurrence = v;
    }

    public String getDurationKind() {
        return durationKind;
    }

    public void setDurationKind(String v) {
        this.durationKind = v;
    }

    public String getDurationDetail() {
        return durationDetail;
    }

    public void setDurationDetail(String v) {
        this.durationDetail = v;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String v) {
        this.justification = v;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String v) {
        this.rootCause = v;
    }

    public String getContainment() {
        return containment;
    }

    public void setContainment(String v) {
        this.containment = v;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String v) {
        this.correctiveAction = v;
    }

    public String getCapaReference() {
        return capaReference;
    }

    public void setCapaReference(String v) {
        this.capaReference = v;
    }

    public String getAttachmentsNote() {
        return attachmentsNote;
    }

    public void setAttachmentsNote(String v) {
        this.attachmentsNote = v;
    }

    public UUID getManagerAccountId() {
        return managerAccountId;
    }

    public void setManagerAccountId(UUID v) {
        this.managerAccountId = v;
    }

    public String getManagerAccountName() {
        return managerAccountName;
    }

    public void setManagerAccountName(String v) {
        this.managerAccountName = v;
    }

    public String getSignedByName() {
        return signedByName;
    }

    public void setSignedByName(String v) {
        this.signedByName = v;
    }

    public byte[] getSignaturePng() {
        return signaturePng;
    }

    public void setSignaturePng(byte[] v) {
        this.signaturePng = v;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant v) {
        this.signedAt = v;
    }

    public String getAmendReason() {
        return amendReason;
    }

    public void setAmendReason(String v) {
        this.amendReason = v;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String v) {
        this.customerEmail = v;
    }

    public String getSendComment() {
        return sendComment;
    }

    public void setSendComment(String v) {
        this.sendComment = v;
    }

    public String getPdfLanguage() {
        return pdfLanguage;
    }

    public void setPdfLanguage(String v) {
        this.pdfLanguage = v;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant v) {
        this.sentAt = v;
    }

    public String getCustomerDecision() {
        return customerDecision;
    }

    public void setCustomerDecision(String v) {
        this.customerDecision = v;
    }

    public LocalDate getCustomerDecisionDate() {
        return customerDecisionDate;
    }

    public void setCustomerDecisionDate(LocalDate v) {
        this.customerDecisionDate = v;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID v) {
        this.createdByUserId = v;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String v) {
        this.createdByName = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant v) {
        this.createdAt = v;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant v) {
        this.updatedAt = v;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long v) {
        this.version = v;
    }
}

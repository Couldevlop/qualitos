package com.openlab.qualitos.quality.docs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DocumentDto {

    private DocumentDto() {}

    public record CreateDocumentRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 255) String title,
            String description,
            @NotNull DocumentType type,
            @NotNull UUID ownerId,
            boolean mandatoryRead,
            /** Contenu de la première version DRAFT (optionnel). */
            String initialContent,
            String initialContentUri,
            String initialChangeNote,
            /** Dossier de l'arborescence (optionnel, ADR 0094). */
            UUID folderId,
            /** Processus de rattachement (optionnel, ADR 0094). */
            UUID processId
    ) {

        public CreateDocumentRequest(String code, String title, String description, DocumentType type,
                                     UUID ownerId, boolean mandatoryRead, String initialContent,
                                     String initialContentUri, String initialChangeNote) {
            this(code, title, description, type, ownerId, mandatoryRead, initialContent,
                    initialContentUri, initialChangeNote, null, null);
        }
    }

    /**
     * Mise à jour partielle : un champ absent reste inchangé. Pour sortir un
     * document de son dossier ou de son processus, {@code clearFolder} et
     * {@code clearProcess} le disent explicitement.
     */
    public record UpdateDocumentRequest(
            @Size(max = 255) String title,
            String description,
            DocumentType type,
            UUID ownerId,
            Boolean mandatoryRead,
            UUID folderId,
            UUID processId,
            LocalDate nextReviewDate,
            Boolean clearFolder,
            Boolean clearProcess
    ) {

        public UpdateDocumentRequest(String title, String description, DocumentType type, UUID ownerId,
                                     Boolean mandatoryRead) {
            this(title, description, type, ownerId, mandatoryRead, null, null, null, null, null);
        }
    }

    /**
     * {@code authorId} n'est plus cru : l'auteur est l'utilisateur du jeton
     * (ADR 0080). Un identifiant différent est refusé (403) ; absent, il est déduit.
     */
    public record CreateVersionRequest(
            String content,
            String contentUri,
            String changeNote,
            UUID authorId
    ) {}

    public record UpdateVersionRequest(
            String content,
            String contentUri,
            String changeNote
    ) {}

    /**
     * L'approbateur est l'utilisateur du jeton (ADR 0080) ; le champ n'est gardé
     * que pour vérifier qu'il concorde. Le commentaire accompagne une étape de
     * circuit.
     */
    public record ApprovalRequest(UUID approverId, @Size(max = 1000) String comment) {

        public ApprovalRequest(UUID approverId) {
            this(approverId, null);
        }
    }

    /** Un refus se motive : la version revient en brouillon et l'auteur lit pourquoi. */
    public record RejectionRequest(@NotBlank @Size(max = 1000) String reason) {}

    /** Celui qui acquitte est l'utilisateur du jeton (ADR 0080) : nul n'acquitte pour un autre. */
    public record AcknowledgeRequest(UUID userId) {}

    public record DocumentResponse(
            UUID id,
            UUID tenantId,
            String code,
            String title,
            String description,
            DocumentType type,
            DocumentStatus status,
            UUID ownerId,
            UUID currentVersionId,
            boolean mandatoryRead,
            Instant createdAt,
            Instant updatedAt,
            List<VersionResponse> versions,
            UUID folderId,
            UUID processId,
            DocumentLifecycle lifecycle,
            DisplayStatus displayStatus,
            LocalDate nextReviewDate
    ) {}

    /** Le fichier d'une version : ce que l'écran en montre, jamais sa clé de stockage. */
    public record FileInfo(
            String fileName,
            String contentType,
            long sizeBytes,
            boolean previewable,
            /** Le fichier a été retiré au terme de sa conservation ; l'empreinte reste. */
            boolean purged
    ) {}

    public record VersionResponse(
            UUID id,
            UUID documentId,
            Integer versionNumber,
            String content,
            String contentUri,
            String contentHash,
            String changeNote,
            VersionStatus status,
            UUID authorId,
            UUID approvedBy,
            Instant approvedAt,
            Instant publishedAt,
            String blockchainTxHash,
            Instant createdAt,
            Instant updatedAt,
            UUID rejectedBy,
            Instant rejectedAt,
            String rejectionReason,
            FileInfo file,
            /** Les signataires de la dernière soumission, dans l'ordre ; vide sans circuit de signature. */
            List<SignatoryView> signatories
    ) {}

    /** Un signataire désigné et sa signature (ADR 0099). */
    public record SignatoryView(
            int position,
            UUID userId,
            String name,
            SignatoryDecision decision,
            Instant decidedAt,
            String comment
    ) {}

    /** Une personne désignée à la soumission : son compte de l'annuaire et son nom affiché. */
    public record SignatoryRequest(@NotNull UUID userId, @NotBlank @Size(max = 255) String name) {}

    /** La soumission à validation : de un à trois signataires, dans l'ordre où ils signeront. */
    public record SubmitRequest(@NotNull @Size(min = 1, max = 3) List<@NotNull @Valid SignatoryRequest> signatories) {}

    public record AcknowledgmentResponse(
            UUID id,
            UUID versionId,
            UUID userId,
            Instant acknowledgedAt
    ) {}

    /** Les filtres de la liste ; un champ {@code null} ne filtre rien. */
    public record ListFilter(DocumentView view, UUID folderId, String q, DisplayStatus displayStatus,
                             DocumentType type, UUID processId, UUID ownerId) {

        public static ListFilter of(DocumentStatus status) {
            return new ListFilter(status == DocumentStatus.ARCHIVED ? DocumentView.ARCHIVED
                    : status == DocumentStatus.ACTIVE ? DocumentView.ALL : null,
                    null, null, null, null, null, null);
        }
    }

    /** Les compteurs des vues rapides, pour les pastilles du panneau de gauche. */
    public record ViewCounts(long all, long mine, long toValidate, long reviewDue, long archived) {}

    /** Un lien présigné vers le fichier d'une version, valable quelques minutes. */
    public record FileLink(String url, String fileName, String contentType, long sizeBytes, long expiresInSeconds) {}

    // ---------- arborescence, processus, paramétrage ----------

    public record FolderView(UUID id, UUID parentId, String name, int position) {}

    public record FolderRequest(@NotBlank @Size(max = 120) String name, UUID parentId,
                                @Min(0) @Max(10_000) Integer position) {}

    public record ProcessView(UUID id, String name, int position) {}

    public record ProcessRequest(@NotBlank @Size(max = 120) String name, @Min(0) @Max(10_000) Integer position) {}

    public record SettingsView(int reviewPeriodMonths, int reviewWarningDays, int maxFileMb, int platformMaxFileMb,
                               Integer quotaMb, Integer obsoleteRetentionYears) {}

    public record SettingsForm(
            @Min(1) @Max(120) int reviewPeriodMonths,
            @Min(0) @Max(365) int reviewWarningDays,
            @Min(1) @Max(DocumentSettingsRow.PLATFORM_MAX_FILE_MB) int maxFileMb,
            @Min(1) @Max(10_000_000) Integer quotaMb,
            @Min(1) @Max(100) Integer obsoleteRetentionYears
    ) {}

    /** Ce que le dépôt admet, pour que l'écran refuse avant d'envoyer. */
    public record UploadPolicy(int maxFileMb, List<String> allowedTypes, List<String> allowedExtensions) {}

    /**
     * L'occupation du stockage par la GED d'un client : ce que pèsent ses fichiers,
     * ce qu'ils occupent réellement après compression et dédoublonnage, et le quota.
     */
    public record StorageUsage(
            /** Fichiers distincts réellement stockés. */
            long files,
            /** Ce que pèseraient les fichiers de toutes les versions, sans dédoublonnage. */
            long logicalBytes,
            /** Ce que pèsent les fichiers distincts, avant compression. */
            long originalBytes,
            /** Ce que le stockage occupe réellement. */
            long storedBytes,
            Long quotaBytes) {}
}

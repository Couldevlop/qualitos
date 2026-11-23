package com.openlab.qualitos.quality.derogation.application;

import com.openlab.qualitos.quality.derogation.domain.CustomerDecision;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationLine;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import com.openlab.qualitos.quality.derogation.domain.DeviationOrigin;
import com.openlab.qualitos.quality.derogation.domain.DurationKind;
import com.openlab.qualitos.quality.derogation.domain.Occurrence;
import com.openlab.qualitos.quality.derogation.domain.ShipmentStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Ce que l'application reçoit et rend.
 *
 * <p>Ni annotation de validation, ni annotation de sérialisation : ce sont des
 * affaires de la couche {@code web}, qui porte ses propres enregistrements. Ici
 * ne vivent que des types que le service manipule.
 */
public final class DerogationDto {

    private DerogationDto() {
    }

    /** Une ligne d'écart, telle qu'elle se saisit. */
    public record LineCommand(
            String specification,
            String actualValue,
            BigDecimal nonConformingQty
    ) {}

    /**
     * La fiche, à la création comme à la modification.
     *
     * <p>Les six sections arrivent ensemble : le formulaire se renvoie entier, et
     * accepter des fragments obligerait à deviner ce qui a été effacé et ce qui
     * n'a pas été touché.
     */
    public record SaveCommand(
            String customerName,
            String customerQualityContact,
            String customerOrderNo,
            String partReference,
            String partIndex,
            String batchNumber,
            BigDecimal quantity,
            LocalDate requestDate,

            ShipmentStage shipmentStage,
            DeviationOrigin deviationOrigin,
            Occurrence occurrence,
            DurationKind durationKind,
            String durationDetail,

            List<LineCommand> lines,
            String justification,

            String rootCause,
            String containment,
            String correctiveAction,
            String capaReference,
            String attachmentsNote
    ) {}

    /** La demande de validation : le manager choisi dans l'annuaire. */
    public record RequestApprovalCommand(UUID managerAccountId) {}

    /** La signature : le nom saisi, et le tracé en PNG. */
    public record ApproveCommand(String signedByName, byte[] signaturePng) {}

    /** Le renvoi pour correction : le motif est obligatoire. */
    public record SendBackCommand(String reason) {}

    /** L'envoi au client : les deux champs qui s'ajoutent, plus la langue. */
    public record SendToCustomerCommand(
            String customerEmail,
            String comment,
            DerogationLanguage language
    ) {}

    /** La réponse du client, portée à la main. */
    public record CustomerDecisionCommand(CustomerDecision decision, LocalDate decidedOn) {}

    /** Les critères du registre, tels que l'écran les offre. */
    public record SearchCommand(
            UUID owner,
            String partReference,
            String customerName,
            DerogationStatus status,
            int page,
            int size
    ) {}

    /** Une ligne du registre : juste ce que le grand tableau affiche. */
    public record RowView(
            UUID id,
            String reference,
            DerogationStatus status,
            Instant createdAt,
            /** La colonne « produit » : la référence pièce de la fiche. */
            String partReference,
            String customerName,
            /** L'{@code owner} du registre : le créateur du dossier. */
            String createdByName,
            /** La colonne « défaut » : la PREMIÈRE ligne d'écart, ou rien. */
            String firstDeviation,
            /** La colonne « pièces jointes », en dernier comme la maquette. */
            String attachmentsNote,
            CustomerDecision customerDecision
    ) {
        /** La ligne de registre d'un dossier. */
        public static RowView of(Derogation d) {
            return new RowView(d.getId(), d.getReference(), d.getStatus(), d.getCreatedAt(),
                    d.getIdentification() == null ? null : d.getIdentification().partReference(),
                    d.getIdentification() == null ? null : d.getIdentification().customerName(),
                    d.getCreatedByName(),
                    d.firstLine().map(DerogationLine::summary).orElse(null),
                    d.getAnalysis() == null ? null : d.getAnalysis().attachmentsNote(),
                    d.getAnswer().decision());
        }
    }

    /** Une tranche du registre. */
    public record PageView<T>(List<T> content, long total, int page, int size) {

        public PageView {
            content = List.copyOf(content);
        }
    }

    /** La fiche complète. */
    public record View(
            UUID id,
            String reference,
            DerogationStatus status,
            boolean editable,

            String customerName,
            String customerQualityContact,
            String customerOrderNo,
            String partReference,
            String partIndex,
            String batchNumber,
            BigDecimal quantity,
            LocalDate requestDate,

            ShipmentStage shipmentStage,
            DeviationOrigin deviationOrigin,
            Occurrence occurrence,
            DurationKind durationKind,
            String durationDetail,

            List<LineView> lines,
            String justification,

            String rootCause,
            String containment,
            String correctiveAction,
            String capaReference,
            String attachmentsNote,

            UUID managerAccountId,
            String managerAccountName,
            String signedByName,
            /** Le tracé en base64, pour le réafficher. Absent si non signé. */
            String signaturePngBase64,
            Instant signedAt,
            String amendReason,

            String customerEmail,
            String sendComment,
            DerogationLanguage language,
            Instant sentAt,

            CustomerDecision customerDecision,
            LocalDate customerDecisionDate,

            UUID createdByUserId,
            String createdByName,
            Instant createdAt,
            Instant updatedAt
    ) {
        /** La fiche d'un dossier. */
        public static View of(Derogation d) {
            var ident = d.getIdentification();
            var nat = d.getNature();
            var ana = d.getAnalysis();
            var app = d.getApproval();
            var env = d.getDispatch();
            return new View(d.getId(), d.getReference(), d.getStatus(),
                    d.getStatus().isEditable(),
                    ident == null ? null : ident.customerName(),
                    ident == null ? null : ident.customerQualityContact(),
                    ident == null ? null : ident.customerOrderNo(),
                    ident == null ? null : ident.partReference(),
                    ident == null ? null : ident.partIndex(),
                    ident == null ? null : ident.batchNumber(),
                    ident == null ? null : ident.quantity(),
                    ident == null ? null : ident.requestDate(),
                    nat == null ? null : nat.shipmentStage(),
                    nat == null ? null : nat.deviationOrigin(),
                    nat == null ? null : nat.occurrence(),
                    nat == null ? null : nat.durationKind(),
                    nat == null ? null : nat.durationDetail(),
                    d.getLines().stream().map(LineView::of).toList(),
                    d.getJustification(),
                    ana == null ? null : ana.rootCause(),
                    ana == null ? null : ana.containment(),
                    ana == null ? null : ana.correctiveAction(),
                    ana == null ? null : ana.capaReference(),
                    ana == null ? null : ana.attachmentsNote(),
                    app.managerAccountId(), app.managerAccountName(), app.signedByName(),
                    app.signaturePng() == null
                            ? null : Base64.getEncoder().encodeToString(app.signaturePng()),
                    app.signedAt(), app.amendReason(),
                    env.customerEmail(), env.comment(), env.language(), env.sentAt(),
                    d.getAnswer().decision(), d.getAnswer().decidedOn(),
                    d.getCreatedByUserId(), d.getCreatedByName(),
                    d.getCreatedAt(), d.getUpdatedAt());
        }
    }

    public record LineView(
            int position,
            String specification,
            String actualValue,
            BigDecimal nonConformingQty
    ) {
        public static LineView of(DerogationLine l) {
            return new LineView(l.position(), l.specification(), l.actualValue(),
                    l.nonConformingQty());
        }
    }

    /** Le compteur de « Mes validations » : l'alerte rouge et son nombre. */
    public record PendingCount(long count) {}
}

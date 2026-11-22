package com.openlab.qualitos.quality.derogation.web;

import com.openlab.qualitos.quality.derogation.application.DerogationDto;
import com.openlab.qualitos.quality.derogation.domain.CustomerDecision;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DeviationOrigin;
import com.openlab.qualitos.quality.derogation.domain.DurationKind;
import com.openlab.qualitos.quality.derogation.domain.Occurrence;
import com.openlab.qualitos.quality.derogation.domain.ShipmentStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Ce que l'API reçoit, et ce qu'elle valide.
 *
 * <p>Distinct du DTO d'application : ici vivent les contraintes de <b>forme</b>
 * — longueurs, format d'adresse, base64 — et nulle part ailleurs. Les
 * contraintes de <b>fond</b> — quels champs sont exigés pour demander une
 * signature — sont sur l'agrégat, parce qu'elles dépendent de l'état du dossier
 * et qu'une annotation ne sait pas le lire.
 *
 * <p>Aucune borne ici n'est obligatoire au sens métier : un brouillon
 * s'enregistre incomplet, « autant de fois qu'il veut ».
 */
public final class DerogationWebDto {

    private DerogationWebDto() {
    }

    /** Une ligne d'écart. */
    public record LinePayload(
            @Size(max = 500) String specification,
            @Size(max = 500) String actualValue,
            @DecimalMin("0") BigDecimal nonConformingQty
    ) {
        DerogationDto.LineCommand toCommand() {
            return new DerogationDto.LineCommand(specification, actualValue, nonConformingQty);
        }
    }

    /** La fiche, à la création comme à la modification. */
    public record SavePayload(
            @Size(max = 200) String customerName,
            @Size(max = 200) String customerQualityContact,
            @Size(max = 100) String customerOrderNo,
            @Size(max = 200) String partReference,
            @Size(max = 40) String partIndex,
            @Size(max = 100) String batchNumber,
            @DecimalMin("0") BigDecimal quantity,
            LocalDate requestDate,

            ShipmentStage shipmentStage,
            DeviationOrigin deviationOrigin,
            Occurrence occurrence,
            DurationKind durationKind,
            @Size(max = 300) String durationDetail,

            @Valid @Size(max = 50) List<LinePayload> lines,
            @Size(max = 20000) String justification,

            @Size(max = 20000) String rootCause,
            @Size(max = 20000) String containment,
            @Size(max = 20000) String correctiveAction,
            @Size(max = 200) String capaReference,
            @Size(max = 500) String attachmentsNote
    ) {
        DerogationDto.SaveCommand toCommand() {
            return new DerogationDto.SaveCommand(customerName, customerQualityContact,
                    customerOrderNo, partReference, partIndex, batchNumber, quantity,
                    requestDate, shipmentStage, deviationOrigin, occurrence, durationKind,
                    durationDetail,
                    lines == null ? List.of() : lines.stream().map(LinePayload::toCommand).toList(),
                    justification, rootCause, containment, correctiveAction, capaReference,
                    attachmentsNote);
        }
    }

    /** La demande de validation : le manager choisi dans l'annuaire. */
    public record RequestApprovalPayload(@NotNull UUID managerAccountId) {

        DerogationDto.RequestApprovalCommand toCommand() {
            return new DerogationDto.RequestApprovalCommand(managerAccountId);
        }
    }

    /**
     * La signature : le nom saisi, et le tracé.
     *
     * <p>Le tracé arrive en base64, éventuellement préfixé par le
     * {@code data:image/png;base64,} que produit la zone de dessin de la
     * maquette. Ce préfixe est retiré ici, à la frontière — c'est une convention
     * du transport, pas une règle du dossier.
     */
    public record ApprovePayload(
            @NotBlank @Size(max = 200) String signedByName,
            @NotBlank @Size(max = 2_000_000) String signaturePngBase64
    ) {
        DerogationDto.ApproveCommand toCommand() {
            return new DerogationDto.ApproveCommand(signedByName, decoder(signaturePngBase64));
        }

        /** Rend un tableau VIDE si le base64 est illisible : le domaine le refusera. */
        private static byte[] decoder(String base64) {
            String brut = base64 == null ? "" : base64.trim();
            int virgule = brut.indexOf(',');
            if (brut.startsWith("data:") && virgule > 0) {
                brut = brut.substring(virgule + 1);
            }
            try {
                return Base64.getDecoder().decode(brut);
            } catch (IllegalArgumentException e) {
                return new byte[0];
            }
        }
    }

    /** Le renvoi pour correction : le motif est obligatoire. */
    public record SendBackPayload(@NotBlank @Size(max = 20000) String reason) {

        DerogationDto.SendBackCommand toCommand() {
            return new DerogationDto.SendBackCommand(reason);
        }
    }

    /** L'envoi au client : les deux champs qui s'ajoutent, plus la langue. */
    public record SendToCustomerPayload(
            @NotBlank @Email @Size(max = 320) String customerEmail,
            @Size(max = 20000) String comment,
            @NotNull DerogationLanguage language
    ) {
        DerogationDto.SendToCustomerCommand toCommand() {
            return new DerogationDto.SendToCustomerCommand(customerEmail, comment, language);
        }
    }

    /** La décision du client. */
    public record CustomerDecisionPayload(
            @NotNull CustomerDecision decision,
            @NotNull LocalDate decidedOn
    ) {
        DerogationDto.CustomerDecisionCommand toCommand() {
            return new DerogationDto.CustomerDecisionCommand(decision, decidedOn);
        }
    }
}

package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.domain.CustomerDecision;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import com.openlab.qualitos.quality.derogation.domain.DeviationOrigin;
import com.openlab.qualitos.quality.derogation.domain.DurationKind;
import com.openlab.qualitos.quality.derogation.domain.Occurrence;
import com.openlab.qualitos.quality.derogation.domain.ShipmentStage;

import java.util.Map;

/**
 * Les libellés du document, en français et en anglais.
 *
 * <p><b>Pourquoi ici et non dans les fichiers de traduction de l'application.</b>
 * Ce document part à un tiers et sa langue est figée au dossier : elle ne suit
 * pas la langue de l'écran de celui qui le réémet. Les deux jeux doivent donc
 * être disponibles côté serveur en même temps, ce que le mécanisme
 * d'internationalisation d'Angular ne permet pas — il compile un paquet par
 * langue.
 *
 * <p>Les libellés français sont ceux de la maquette, au mot près : ils sont figés
 * (ADR 0052). Les anglais en sont la traduction d'usage industriel — « concession »
 * et non « derogation », qui en anglais ne dit pas cela.
 */
final class DerogationLabels {

    private final DerogationLanguage langue;

    DerogationLabels(DerogationLanguage langue) {
        this.langue = langue;
    }

    private static final Map<String, String> FR = Map.ofEntries(
            Map.entry("title", "Demande de dérogation client"),
            Map.entry("reference", "Référence"),
            Map.entry("status", "Statut"),
            Map.entry("s1", "1. Identification"),
            Map.entry("customer", "Client"),
            Map.entry("contact", "Contact qualité client"),
            Map.entry("order", "N° de commande"),
            Map.entry("part", "Réf. pièce / désignation"),
            Map.entry("index", "Indice"),
            Map.entry("batch", "N° de lot"),
            Map.entry("quantity", "Quantité concernée"),
            Map.entry("requestDate", "Date de la demande"),
            Map.entry("s2", "2. Nature de la demande"),
            Map.entry("shipment", "Expédition"),
            Map.entry("origin", "Origine"),
            Map.entry("occurrence", "Occurrence"),
            Map.entry("duration", "Durée"),
            Map.entry("durationDetail", "Précision"),
            Map.entry("s3", "3. Écart constaté"),
            Map.entry("specification", "Spécification attendue"),
            Map.entry("actual", "Valeur réelle"),
            Map.entry("ncQty", "Qté non conforme"),
            Map.entry("justification", "Justification"),
            Map.entry("s4", "4. Cause, confinement et actions"),
            Map.entry("rootCause", "Cause racine"),
            Map.entry("containment", "Confinement"),
            Map.entry("corrective", "Action corrective"),
            Map.entry("capaRef", "Réf. 8D / CAPA"),
            Map.entry("attachments", "Pièces jointes"),
            Map.entry("s5", "5. Approbation interne"),
            Map.entry("manager", "Manager qualité"),
            Map.entry("signedBy", "Signé par"),
            Map.entry("signedAt", "Le"),
            Map.entry("signature", "Signature"),
            Map.entry("s6", "6. Décision du client"),
            Map.entry("decision", "Décision"),
            Map.entry("decisionDate", "Date de la décision"),
            Map.entry("empty", "Non renseigné"),
            Map.entry("noLine", "Aucun écart saisi"),
            Map.entry("notSigned", "Non signé"),
            Map.entry("footer", "Document non scellé — la pièce opposable est"
                    + " l'exemplaire signé retourné par le client."));

    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("title", "Customer concession request"),
            Map.entry("reference", "Reference"),
            Map.entry("status", "Status"),
            Map.entry("s1", "1. Identification"),
            Map.entry("customer", "Customer"),
            Map.entry("contact", "Customer quality contact"),
            Map.entry("order", "Purchase order no."),
            Map.entry("part", "Part no. / description"),
            Map.entry("index", "Index"),
            Map.entry("batch", "Batch no."),
            Map.entry("quantity", "Quantity concerned"),
            Map.entry("requestDate", "Request date"),
            Map.entry("s2", "2. Nature of the request"),
            Map.entry("shipment", "Shipment"),
            Map.entry("origin", "Origin"),
            Map.entry("occurrence", "Occurrence"),
            Map.entry("duration", "Duration"),
            Map.entry("durationDetail", "Details"),
            Map.entry("s3", "3. Deviation"),
            Map.entry("specification", "Required specification"),
            Map.entry("actual", "Actual value"),
            Map.entry("ncQty", "Nonconforming qty"),
            Map.entry("justification", "Justification"),
            Map.entry("s4", "4. Cause, containment and actions"),
            Map.entry("rootCause", "Root cause"),
            Map.entry("containment", "Containment"),
            Map.entry("corrective", "Corrective action"),
            Map.entry("capaRef", "8D / CAPA ref."),
            Map.entry("attachments", "Attachments"),
            Map.entry("s5", "5. Internal approval"),
            Map.entry("manager", "Quality manager"),
            Map.entry("signedBy", "Signed by"),
            Map.entry("signedAt", "On"),
            Map.entry("signature", "Signature"),
            Map.entry("s6", "6. Customer decision"),
            Map.entry("decision", "Decision"),
            Map.entry("decisionDate", "Decision date"),
            Map.entry("empty", "Not provided"),
            Map.entry("noLine", "No deviation recorded"),
            Map.entry("notSigned", "Not signed"),
            Map.entry("footer", "Unsealed document - the binding copy is"
                    + " the signed original returned by the customer."));

    /**
     * Le libellé d'une clé.
     *
     * <p>Une clé absente rend la clé elle-même plutôt que {@code null} : un
     * libellé manquant doit se voir sur le document et non faire échouer son
     * émission.
     */
    String t(String cle) {
        Map<String, String> jeu = langue == DerogationLanguage.EN ? EN : FR;
        return jeu.getOrDefault(cle, cle);
    }

    String of(DerogationStatus statut) {
        boolean en = langue == DerogationLanguage.EN;
        return switch (statut) {
            case DRAFT -> en ? "Draft" : "Brouillon";
            case PENDING_INTERNAL -> en ? "Pending internal approval" : "En attente interne";
            case TO_AMEND -> en ? "To amend" : "À modifier";
            case QUALITY_APPROVED -> en ? "Quality approved" : "Approuvé qualité";
            case PENDING_CUSTOMER -> en ? "Pending customer" : "En attente client";
            case ACCEPTED -> en ? "Accepted" : "Acceptée";
            case REFUSED -> en ? "Refused" : "Refusée";
        };
    }

    String of(CustomerDecision decision) {
        boolean en = langue == DerogationLanguage.EN;
        return switch (decision) {
            case PENDING -> en ? "Awaiting answer" : "En attente";
            case ACCEPTED -> en ? "Accepted" : "Acceptée";
            case ACCEPTED_WITH_CONDITIONS ->
                    en ? "Accepted with conditions" : "Acceptée sous conditions";
            case REFUSED -> en ? "Refused" : "Refusée";
        };
    }

    String of(ShipmentStage etape) {
        boolean en = langue == DerogationLanguage.EN;
        return etape == ShipmentStage.ALREADY_SHIPPED
                ? (en ? "Parts already shipped" : "Pièces déjà expédiées")
                : (en ? "Parts to ship" : "Pièces à expédier");
    }

    String of(DeviationOrigin origine) {
        boolean en = langue == DerogationLanguage.EN;
        return origine == DeviationOrigin.PROCESS
                ? (en ? "Process related" : "Lié au process")
                : (en ? "Product related" : "Lié au produit");
    }

    String of(Occurrence occurrence) {
        boolean en = langue == DerogationLanguage.EN;
        return occurrence == Occurrence.REPEAT
                ? (en ? "Repeat" : "Répétition")
                : (en ? "First occurrence" : "Première occurrence");
    }

    String of(DurationKind duree) {
        boolean en = langue == DerogationLanguage.EN;
        return duree == DurationKind.PERMANENT
                ? (en ? "Permanent" : "Permanente")
                : (en ? "Temporary" : "Temporaire");
    }
}

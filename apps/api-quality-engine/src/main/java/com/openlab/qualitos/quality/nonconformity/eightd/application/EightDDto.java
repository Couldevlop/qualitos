package com.openlab.qualitos.quality.nonconformity.eightd.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ce que le cas d'usage reçoit et rend. Aucun type technique ne traverse. */
public final class EightDDto {

    private EightDDto() {
    }

    /**
     * Les trois disciplines sans source. Les trois champs sont facultatifs : un
     * rapport incomplet s'émet, et se déclare partiel.
     */
    public record SaveCommand(String team, String containment, String recognition) {
    }

    /**
     * Une discipline telle que l'écran la montre.
     *
     * @param sourced     faux quand rien n'a été trouvé ni saisi
     * @param sourceLabel d'où vient le contenu, ou ce qui manque — toujours renseigné
     * @param editable    vrai pour D1, D3 et D8, les seules qui se saisissent
     */
    public record DisciplineView(
            String code,
            String title,
            boolean sourced,
            String sourceLabel,
            List<String> lines,
            boolean editable) {
    }

    /** La preuve d'intégrité d'un rapport émis ; nulle tant qu'il est en brouillon. */
    public record SealView(
            String sha256Hex,
            String anchorTxRef,
            String verificationCode,
            Instant issuedAt,
            String issuedByName) {
    }

    /**
     * Le rapport complet : les huit disciplines, l'état, et la preuve si elle existe.
     *
     * @param partial      vrai dès qu'une discipline n'a rien à montrer
     * @param missingCodes les codes de ces disciplines, pour que l'écran les désigne
     */
    public record ReportView(
            UUID ncId,
            String ncReference,
            String ncTitle,
            String status,
            boolean issuable,
            boolean partial,
            List<String> missingCodes,
            String team,
            String containment,
            String recognition,
            List<DisciplineView> disciplines,
            SealView seal) {
    }

    /** Le PDF émis, avec le nom de fichier que le serveur propose. */
    public record PdfResult(byte[] pdf, String fileName, String sha256Hex, String verificationCode) {
    }

    /**
     * Réponse de la vérification publique : des faits d'intégrité, jamais de
     * données métier du tenant (OWASP A01).
     */
    public record VerificationResult(
            boolean valid,
            String code,
            String sha256Hex,
            String anchorTxRef,
            String ncReference,
            Instant issuedAt) {

        /** Code inconnu : invalide, et rien d'autre — pas un 404, pour ne pas énumérer. */
        public static VerificationResult unknown(String code) {
            return new VerificationResult(false, code, null, null, null, null);
        }
    }
}

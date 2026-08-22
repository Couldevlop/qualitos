package com.openlab.qualitos.quality.controlplan.domain;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * L'empreinte d'un control plan : ce qui fait qu'un document est CE document.
 *
 * <p>Objet de domaine, sans Spring ni JPA. Le calcul vit ici et non dans un
 * service technique parce que le choix de ce qui entre dans l'empreinte est une
 * décision métier : deux control plans qui contiennent les mêmes contrôles, dans
 * le même ordre, aux mêmes tolérances, sont le même document — quels que soient
 * les identifiants de leurs lignes ou l'ordre dans lequel la base les a rendues.
 *
 * <p><b>Ce qui entre dans l'empreinte</b> : l'identité du plan (produit, phase,
 * code, révision), son approbateur, et chaque ligne dans son intégralité — y
 * compris le lien vers la ligne de PFMEA qui la justifie. Ajouter une
 * justification à un contrôle CHANGE le document : c'est précisément ce qu'un
 * auditeur vient vérifier.
 *
 * <p><b>Ce qui n'y entre pas</b> : les identifiants techniques des lignes et les
 * horodatages de modification. Recopier un plan à l'ouverture d'une révision
 * crée des lignes neuves ; si leurs identifiants comptaient, une révision qui ne
 * change rien produirait une empreinte différente, et l'empreinte ne dirait plus
 * rien du contenu.
 *
 * <p>Les champs sont séparés par des caractères qui ne peuvent pas apparaître
 * dans une valeur (unité de groupe et de enregistrement ASCII). Un simple
 * point-virgule aurait permis de déplacer du texte d'un champ à l'autre sans
 * changer l'empreinte — une faille discrète, mais réelle sur un document qu'on
 * signe.
 */
public final class ControlPlanFingerprint {

    /** Séparateur de champs : caractère de contrôle, jamais saisissable. */
    private static final char FIELD = '\u001F';
    /** Séparateur de lignes. */
    private static final char RECORD = '\u001E';

    private ControlPlanFingerprint() {
    }

    /**
     * Rend le SHA-256, en minuscules hexadécimales, du plan et de ses lignes.
     *
     * @param plan  le plan, tel qu'il vient d'être approuvé
     * @param lines ses lignes, dans n'importe quel ordre — elles sont triées ici
     */
    public static String of(ControlPlan plan, List<ControlPlanLine> lines) {
        return sha256Hex(canonical(plan, lines));
    }

    /**
     * Le texte canonique dont on prend l'empreinte. Exposé pour que les tests
     * puissent constater CE qui est signé, et pas seulement que quelque chose
     * l'a été.
     */
    public static String canonical(ControlPlan plan, List<ControlPlanLine> lines) {
        // Le type de document ouvre le texte, sans séparateur devant : une
        // empreinte doit rester lisible par l'humain qui la rejoue à la main.
        StringBuilder out = new StringBuilder(512).append("control-plan");
        append(out, plan.getTenantId());
        append(out, plan.getProductId());
        append(out, plan.getPhase());
        append(out, plan.getCode());
        append(out, plan.getRevision());
        append(out, plan.getApprovedBy());
        append(out, plan.getApprovedAt());

        // Tri sur le rang PUIS sur la caractéristique : deux lignes peuvent
        // partager un rang, et un tri instable rendrait l'empreinte dépendante
        // de l'ordre de lecture de la base — le même document produirait deux
        // empreintes selon le plan d'exécution choisi.
        lines.stream()
                .sorted(Comparator.<ControlPlanLine>comparingInt(line -> line.getSequenceNo())
                        .thenComparing(l -> text(l.getCharacteristicLabel()))
                        .thenComparing(l -> text(l.getCharacteristicNo())))
                .forEach(line -> {
                    out.append(RECORD);
                    append(out, line.getSequenceNo());
                    append(out, line.getOperationId());
                    append(out, line.getMachine());
                    append(out, line.getCharacteristicNo());
                    append(out, line.getCharacteristicLabel());
                    append(out, line.getCharacteristicType());
                    append(out, line.getSpecialClass());
                    append(out, line.getSpecification());
                    append(out, line.getToleranceLower());
                    append(out, line.getToleranceUpper());
                    append(out, line.getUnit());
                    append(out, line.getMeasurementTechnique());
                    append(out, line.getSampleSize());
                    append(out, line.getSampleFrequency());
                    append(out, line.getControlMethod());
                    append(out, line.getReactionPlan());
                    append(out, line.getFmeaItemId());
                });
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        out.append(FIELD).append(render(value));
    }

    /**
     * Une valeur absente s'écrit chaîne vide, et une tolérance se normalise :
     * {@code 10.0} et {@code 10.00} sont le même nombre, et les rendre
     * différemment ferait changer l'empreinte d'un document que personne n'a
     * touché — au gré de la précision rendue par la base.
     */
    private static String render(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        if (value instanceof UUID id) return id.toString();
        return value.toString();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String sha256Hex(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            // SHA-256 est exigé de toute plateforme Java. S'il manquait, rien de
            // ce qui touche à la preuve ne tiendrait : échouer est la seule
            // réponse honnête.
            throw new IllegalStateException("SHA-256 indisponible", impossible);
        }
    }
}

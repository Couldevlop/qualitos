package com.openlab.qualitos.quality.apqp;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Valide le contenu d'un livrable SELON SON GENRE, et le rend en JSON.
 *
 * <p>Une seule colonne {@code jsonb} porte les deux formes possibles — des
 * sous-points, ou des mesures. Ce qui empêche cette colonne de devenir un
 * fourre-tout n'est pas son type mais ce validateur : une forme inattendue est
 * refusée ici, avant que la base n'en voie rien.
 *
 * <p>Sérialisation écrite à la main : deux formes fermées, trois champs chacune,
 * et rien qui vienne d'ailleurs que de ce fichier. Passer par un mapper ferait
 * dépendre ce qui entre en base d'une configuration tenue ailleurs, qui peut
 * changer sans qu'on s'en avise. La relecture, elle, passe par le mapper : on
 * contrôle ce qui entre, on ne se défie pas de ce qu'on ressort.
 */
@Component
public class ApqpDeliverableDataValidator {

    /** Douze mesures : au-delà, ce n'est plus un livrable mais un relevé. */
    static final int MAX_MESURES = 12;

    /** Trente sous-points : au-delà, la checklist est une procédure déguisée. */
    static final int MAX_POINTS = 30;

    static final int MAX_LABEL_MESURE = 120;
    static final int MAX_LABEL_POINT = 200;
    static final int MAX_VALEUR = 60;
    static final int MAX_UNITE = 20;

    /**
     * @param kind le genre du livrable, qui fixe la forme attendue
     * @param rows les lignes saisies, éventuellement absentes
     * @return le JSON à stocker, ou {@code null} s'il n'y a rien à stocker
     * @throws ApqpDeliverableValidationException si la forme ne convient pas au genre
     */
    public String valider(ApqpDeliverableKind kind, List<ApqpDto.DataRow> rows) {
        if (rows == null || rows.isEmpty()) {
            // Rien plutôt qu'un tableau vide : l'écran distingue « pas de
            // sous-points » de « des sous-points tous décochés ».
            return null;
        }
        return switch (kind) {
            case CHECKLIST -> points(rows);
            case DATA_ENTRY -> mesures(rows);
            case ATTACHMENT, MODULE_LINK -> throw new ApqpDeliverableValidationException(
                    "A " + kind + " deliverable carries no data rows");
        };
    }

    private String points(List<ApqpDto.DataRow> rows) {
        if (rows.size() > MAX_POINTS) {
            throw new ApqpDeliverableValidationException(
                    "At most " + MAX_POINTS + " checklist items on a deliverable");
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            ApqpDto.DataRow row = rows.get(i);
            String label = libelle(row.label(), MAX_LABEL_POINT);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"label\":\"").append(label)
                    .append("\",\"checked\":").append(Boolean.TRUE.equals(row.checked()))
                    .append('}');
        }
        return json.append(']').toString();
    }

    private String mesures(List<ApqpDto.DataRow> rows) {
        if (rows.size() > MAX_MESURES) {
            throw new ApqpDeliverableValidationException(
                    "At most " + MAX_MESURES + " measurements on a deliverable");
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            ApqpDto.DataRow row = rows.get(i);
            String label = libelle(row.label(), MAX_LABEL_MESURE);
            String valeur = texte(row.value(), MAX_VALEUR, "value");
            String unite = texte(row.unit(), MAX_UNITE, "unit");
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"label\":\"").append(label)
                    .append("\",\"value\":\"").append(valeur)
                    .append("\",\"unit\":\"").append(unite)
                    .append("\",\"measuredAt\":")
                    .append(row.measuredAt() == null ? "null" : "\"" + row.measuredAt() + "\"")
                    .append('}');
        }
        return json.append(']').toString();
    }

    /**
     * Un libellé est obligatoire : une mesure sans intitulé, ou un point sans
     * énoncé, n'apprend rien à qui relit le livrable six mois plus tard.
     */
    private String libelle(String valeur, int max) {
        if (valeur == null || valeur.isBlank()) {
            throw new ApqpDeliverableValidationException("A data row needs a label");
        }
        String rogne = valeur.trim();
        if (rogne.length() > max) {
            throw new ApqpDeliverableValidationException(
                    "Label longer than " + max + " characters");
        }
        return echapper(rogne);
    }

    private String texte(String valeur, int max, String champ) {
        if (valeur == null) {
            return "";
        }
        String rogne = valeur.trim();
        if (rogne.length() > max) {
            throw new ApqpDeliverableValidationException(
                    champ + " longer than " + max + " characters");
        }
        return echapper(rogne);
    }

    /**
     * Échappement JSON du strict nécessaire.
     *
     * <p>Les caractères de contrôle sont RETIRÉS plutôt qu'encodés : aucun n'a de
     * sens dans l'intitulé d'une mesure, et les laisser passer encodés
     * reviendrait à accepter une saisie qu'aucun écran ne sait rendre.
     */
    private String echapper(String valeur) {
        return valeur.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replaceAll("\\p{Cntrl}", "");
    }
}

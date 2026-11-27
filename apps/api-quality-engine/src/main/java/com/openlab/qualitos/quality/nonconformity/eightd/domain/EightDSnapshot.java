package com.openlab.qualitos.quality.nonconformity.eightd.domain;

import java.util.List;
import java.util.Objects;

/**
 * Le contenu FIGÉ d'un rapport 8D : huit sections de texte, déjà mises en forme.
 *
 * <p>C'est le cœur de la décision de conception. Le rapport ne conserve pas des
 * identifiants vers les objets qu'il a agrégés, mais <b>le texte qu'il en a
 * tiré</b>. Deux raisons, toutes deux des raisons de preuve :
 *
 * <ol>
 *   <li>Un 8D émis doit dire, dans dix ans, ce qu'il disait le jour de la
 *       clôture. Des identifiants le feraient varier avec les objets qu'ils
 *       désignent — une CAPA rouverte, un Ishikawa complété, un plan de
 *       surveillance révisé — et le document ne vaudrait plus rien.</li>
 *   <li>L'empreinte scellée est calculée sur le PDF. Pour qu'elle reste
 *       vérifiable, le rendu doit être une fonction PURE de ce qu'on stocke :
 *       pas d'horloge, pas de requête, pas d'identifiant à résoudre. D'où des
 *       lignes de texte, et des dates déjà formatées (même leçon que l'ADR
 *       0062 : une empreinte ne se calcule que sur des valeurs stockables).</li>
 * </ol>
 *
 * <p>Les enregistrements sont immuables et sans dépendance technique : le même
 * objet sert au rendu PDF, à la réponse HTTP et à la sérialisation JSON.
 */
public record EightDSnapshot(
        String ncReference,
        String ncTitle,
        String tenantLabel,
        String issuedAtText,
        String issuedByName,
        boolean partial,
        List<Section> sections,
        /**
         * La langue du document, figée à l'émission — {@code "fr"} aujourd'hui.
         *
         * <p>{@code null} à la relecture d'un rapport scellé AVANT que le
         * document parle français. L'empreinte scellée porte sur les octets du
         * PDF : si le rendu changeait de mots sous un instantané déjà scellé,
         * l'empreinte ne retomberait plus juste et le téléchargement casserait
         * en silence. Le rapport se relit donc exactement comme il a été émis,
         * et seul un rapport neuf parle la nouvelle langue.
         */
        String langue,
        /**
         * LA MISE EN PAGE SOUS LAQUELLE CE RAPPORT A ETE SCELLE.
         *
         * <p>Un rapport emis est re-rendu depuis son instantane et son empreinte
         * comparee au sceau : changer un trait du dessin -- une largeur de
         * colonne, un pied de page, un numero de page -- rendrait indelivrable
         * tout rapport deja emis, sans qu'aucune donnee n'ait bouge. Les
         * changements de CONTENU echappent a cela (une liste vide se rend comme
         * avant) ; ceux de DESSIN, non.
         *
         * <p>D'ou ce marqueur : {@code null} designe le dessin d'origine, et le
         * moteur de rendu garde ce chemin intact pour toujours. Toute refonte
         * prend un nom, et seuls les rapports scelles sous ce nom la recoivent.
         *
         * <p>Un champ a n'ecrire qu'une fois : on en ajoute un, on ne modifie
         * jamais le sens d'un existant.
         */
        String layout) {

    /** Le nom du dessin en vigueur, ecrit par l'assembleur sur tout rapport neuf. */
    public static final String MISE_EN_PAGE_COURANTE = "2026-12";

    /**
     * Le dessin remanie de novembre 2026 : largeurs de colonnes, pied de page,
     * pagination, accents. Les rapports scelles sous ce nom se redessinent
     * ainsi pour toujours, et le dessin courant l'a repris en entier.
     */
    private static final String MISE_EN_PAGE_2026_11 = "2026-11";

    /** Vrai quand ce rapport se dessine selon la mise en page remaniee. */
    public boolean dessinRemanie() {
        return MISE_EN_PAGE_2026_11.equals(layout)
                || MISE_EN_PAGE_COURANTE.equals(layout);
    }

    /**
     * Vrai quand ce rapport ECRIT « Non renseignee. » sous une discipline vide.
     *
     * <p>Le dessin courant ne l'ecrit plus : une rubrique vide se laisse vide,
     * et ce qui manque est deja nomme par le bandeau « rapport partiel » et par
     * le libelle de provenance de la discipline.
     *
     * <p>Les rapports scelles AVANT ce dessin continuent de l'ecrire, et c'est
     * la raison d'etre du marqueur : leur empreinte porte sur les octets du PDF,
     * re-rendu a chaque telechargement et compare au sceau. Retirer cette ligne
     * pour eux les rendrait indelivrables sans qu'aucune donnee n'ait bouge.
     */
    public boolean annonceLesDisciplinesVides() {
        return !MISE_EN_PAGE_COURANTE.equals(layout);
    }

    /**
     * Un instantane sans marqueur de mise en page : le dessin d'origine.
     *
     * <p>Jackson relit par ce chemin les rapports scelles avant le marqueur, et
     * les bancs anterieurs aussi.
     */
    public EightDSnapshot(String ncReference, String ncTitle, String tenantLabel,
                          String issuedAtText, String issuedByName, boolean partial,
                          List<Section> sections, String langue) {
        this(ncReference, ncTitle, tenantLabel, issuedAtText, issuedByName, partial,
                sections, langue, null);
    }

    /**
     * Un instantané sans langue : celle d'avant, pour relire l'existant.
     *
     * <p>Jackson relit les rapports déjà scellés par ce chemin — leur JSON n'a
     * pas de champ {@code langue} —, et les bancs d'essai antérieurs aussi.
     */
    public EightDSnapshot(String ncReference, String ncTitle, String tenantLabel,
                          String issuedAtText, String issuedByName, boolean partial,
                          List<Section> sections) {
        this(ncReference, ncTitle, tenantLabel, issuedAtText, issuedByName, partial,
                sections, null, null);
    }

    public EightDSnapshot {
        ncReference = requireText(ncReference, "ncReference");
        ncTitle = requireText(ncTitle, "ncTitle");
        sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
        if (sections.size() != EightDDiscipline.values().length) {
            throw new IllegalArgumentException(
                    "un rapport 8D porte exactement " + EightDDiscipline.values().length
                    + " sections, reçu " + sections.size());
        }
    }

    /**
     * Une discipline rendue.
     *
     * @param code         « D1 » … « D8 »
     * @param title        le titre de la discipline
     * @param sourced      vrai si du contenu a été trouvé ou saisi
     * @param sourceLabel  POURQUOI le contenu manque, et rien d'autre : une section
     *                     vide et silencieuse laisse croire qu'il n'y avait rien à dire,
     *                     alors qu'elle dit seulement qu'on n'a pas cherché. Vide dès que
     *                     la discipline porte quelque chose — le tableau, la figure ou le
     *                     texte disent alors d'où ils viennent mieux qu'une ligne de
     *                     provenance au-dessus d'eux, qui répétait le dossier déjà nommé
     * @param lines        le contenu, ligne à ligne, déjà mis en forme
     */
    public record Section(
            String code,
            String title,
            boolean sourced,
            String sourceLabel,
            List<String> lines,
            /**
             * Les figures de la discipline — un tracé dit ce qu'une énumération
             * de causes ne dit pas.
             *
             * <p>Des DONNÉES, pas une image : des catégories, des libellés et des
             * niveaux. Le rendu reste donc une fonction pure de ce qu'on stocke,
             * et l'empreinte scellée reste vérifiable — c'est la même raison qui
             * fait stocker du texte plutôt que des identifiants.
             *
             * <p>{@code null} à la relecture d'un rapport émis AVANT les figures :
             * il se relit alors exactement comme il a été scellé, sans figure, et
             * son empreinte continue de tomber juste.
             */
            List<Figure> figures,
            /**
             * Les tableaux de la discipline — les actions décidées, telles que
             * l'écran les présente : une ligne par action, avec sa nature, son
             * porteur, son échéance, son avancement et sa preuve.
             *
             * <p>Des DONNÉES, pour la même raison que les figures : le rendu
             * reste une fonction pure de ce qu'on stocke, donc l'empreinte
             * scellée reste vérifiable. Une image de tableau ferait dépendre
             * cette empreinte d'un moteur de rendu.
             *
             * <p>{@code null} à la relecture d'un rapport émis AVANT les
             * tableaux : il se relit alors exactement comme il a été scellé.
             */
            List<Table> tables,
            /**
             * La FICHE de la discipline : des libelles et leurs valeurs, en
             * deux colonnes alignees.
             *
             * <p>« Reference : Int-NC-2026-0005 », « Gravite : Majeure » :
             * ecrites en lignes de texte, ces sept donnees se lisaient comme
             * une phrase par ligne, et l'oeil ne retrouvait pas la valeur qu'il
             * cherchait. En deux colonnes, les libelles s'alignent et les
             * valeurs se lisent les unes sous les autres -- c'est la forme que
             * la liste de recette montre.
             *
             * <p>Vide a la relecture d'un rapport emis AVANT la fiche : il se
             * relit alors exactement comme il a ete scelle, en lignes de texte,
             * et son empreinte continue de tomber juste. C'est pourquoi la
             * fiche S'AJOUTE aux lignes au lieu de les remplacer dans le
             * modele : une discipline porte l'une ou l'autre, selon son age.
             */
            List<Field> fields) {

        public Section {
            code = requireText(code, "code");
            title = requireText(title, "title");
            // VIDE EST UNE VALEUR : la discipline servie n'annonce pas sa
            // provenance. Les rapports scellés avant cette règle en portent
            // une, et se relisent inchangés -- l'empreinte tient à cela.
            sourceLabel = sourceLabel == null ? "" : sourceLabel.trim();
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            figures = figures == null ? List.of() : List.copyOf(figures);
            tables = tables == null ? List.of() : List.copyOf(tables);
            fields = fields == null ? List.of() : List.copyOf(fields);
        }

        /** Une discipline qui ne porte que du texte — le cas de quatre d'entre elles. */
        public Section(String code, String title, boolean sourced, String sourceLabel,
                       List<String> lines) {
            this(code, title, sourced, sourceLabel, lines, List.of(), List.of(), List.of());
        }

        /** Une discipline qui trace mais ne tabule pas — le D4. */
        public Section(String code, String title, boolean sourced, String sourceLabel,
                       List<String> lines, List<Figure> figures) {
            this(code, title, sourced, sourceLabel, lines, figures, List.of(), List.of());
        }

        /**
         * Une discipline qui tabule — D1, D3, D5, D7 — et les rapports scelles
         * avant la fiche, que Jackson relit par ce chemin.
         */
        public Section(String code, String title, boolean sourced, String sourceLabel,
                       List<String> lines, List<Figure> figures, List<Table> tables) {
            this(code, title, sourced, sourceLabel, lines, figures, tables, List.of());
        }
    }

    /**
     * Une ligne de fiche : un libelle et sa valeur.
     *
     * @param label le libelle, sans son deux-points -- c'est le rendu qui le pose
     * @param value la valeur, deja mise en forme
     */
    public record Field(String label, String value) {

        public Field {
            label = requireText(label, "label");
            value = value == null ? "" : value;
        }
    }

    /**
     * Un tableau, décrit par ses colonnes et ses lignes plutôt que dessiné.
     *
     * <p>C'est la forme sous laquelle l'écran présente les actions décidées, et
     * le document émis doit se présenter comme l'écran : une énumération en
     * phrases ne se lit pas en revue, et surtout elle ne se compare pas d'une
     * ligne à l'autre — ce qu'un tableau fait d'un coup d'œil.
     *
     * @param caption le dossier CAPA dont ces actions relèvent ; un tableau sans
     *                titre ne se rattache à rien quand l'écart en porte plusieurs
     * @param columns les en-têtes, dans l'ordre
     * @param rows    les lignes, dans l'ordre ; chacune a autant de cellules que
     *                de colonnes — une ligne plus courte décalerait silencieusement
     *                les valeurs d'une colonne, et un rapport signé affirmerait
     *                alors quelque chose que personne n'a écrit
     */
    public record Table(String caption, List<String> columns, List<Row> rows) {

        public Table {
            columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
            if (columns.isEmpty()) {
                throw new IllegalArgumentException("un tableau sans colonne ne se rend pas");
            }
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            for (Row row : rows) {
                if (row.cells().size() != columns.size()) {
                    throw new IllegalArgumentException(
                            "ligne de " + row.cells().size() + " cellules pour "
                            + columns.size() + " colonnes");
                }
            }
        }
    }

    /** Une ligne de tableau : ses cellules, dans l'ordre des colonnes. */
    public record Row(List<String> cells) {

        public Row {
            cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
        }
    }

    /**
     * Une figure, décrite par ses nœuds plutôt que dessinée.
     *
     * @param kind    la forme à tracer
     * @param caption l'énoncé du problème qu'elle creuse — une figure sans énoncé
     *                ne se rattache à rien
     * @param nodes   les nœuds, dans l'ordre où ils doivent être lus
     */
    public record Figure(FigureKind kind, String caption, List<Node> nodes) {

        public Figure {
            Objects.requireNonNull(kind, "kind");
            nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        }
    }

    /** Les formes que le rapport sait tracer. */
    public enum FigureKind {
        /** La descente des 5 pourquoi : une échelle, du symptôme à la cause racine. */
        WHY_CHAIN,
        /** L'arête de poisson : une épine, des branches par catégorie. */
        FISHBONE
    }

    /**
     * Un nœud de figure.
     *
     * @param group la branche où le placer (catégorie 6M), ou {@code null} quand
     *              la forme n'en a pas — une échelle n'a pas de branches
     * @param label ce qui s'écrit dessus
     * @param depth 1 = premier niveau (branche, ou premier « pourquoi ») ;
     *              2 et au-delà = sous-cause, ou pourquoi suivant. Le rang porte
     *              l'ORDRE dans une échelle, et le NIVEAU dans une arête.
     */
    public record Node(String group, String label, int depth) {

        public Node {
            label = requireText(label, "label");
            if (depth < 1) {
                throw new IllegalArgumentException("depth >= 1 attendu, reçu " + depth);
            }
        }
    }

    /** Les codes des disciplines qui n'ont rien à montrer — ce qui rend le rapport partiel. */
    public List<String> missingCodes() {
        return sections.stream().filter(s -> !s.sourced()).map(Section::code).toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " requis");
        }
        return value;
    }
}

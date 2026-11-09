package com.openlab.qualitos.quality.settings.catalog;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.openlab.qualitos.quality.settings.catalog.CatalogEntry.of;

/**
 * Les listes déroulantes ouvertes au paramétrage du client, et ce que la
 * plateforme y met par défaut.
 *
 * <p>Un seul mécanisme, pas douze : chaque liste est nommée ici, avec son mode
 * ({@link CatalogMode}) et ses valeurs de départ. Ajouter une liste au
 * paramétrage, c'est ajouter une constante ici — ni table, ni contrôleur, ni
 * écran supplémentaires.
 *
 * <p>Les libellés de départ sont en français, langue de référence de la
 * plateforme. Tant que le client n'a rien réglé, les écrans n'affichent PAS ces
 * libellés : ils gardent leurs propres traductions (six langues). Dès qu'il
 * règle sa liste, ce sont ses mots qui s'affichent, dans sa langue — traduire
 * les mots d'un client serait les trahir.
 */
public enum CatalogId {

    /**
     * Catégories de non-conformité. Renommables : la catégorie commande le
     * rattachement des natures d'écart (voir le catalogue de sous-catégories,
     * lui entièrement libre) et des tableaux de bord.
     */
    NC_CATEGORY("nc-category", CatalogMode.RENAME, List.of(
            of("PRODUCT", "Produit ou prestation"),
            of("PROCESS", "Processus"),
            of("DOCUMENTATION", "Documentation"),
            of("SUPPLIER", "Fournisseur"),
            of("SAFETY", "Sécurité"),
            of("ENVIRONMENT", "Environnement"),
            of("OTHER", "Autre"))),

    /** Types d'audit. Renommables : le type ouvre ou ferme des étapes du programme. */
    AUDIT_TYPE("audit-type", CatalogMode.RENAME, List.of(
            of("INTERNAL", "Interne"),
            of("EXTERNAL", "Externe"),
            of("SUPPLIER", "Fournisseur"),
            of("LPA", "LPA (audit de poste par couche)"),
            of("CERTIFICATION", "Certification"),
            of("SURVEILLANCE", "Surveillance"))),

    /** Familles de fournisseurs. */
    SUPPLIER_TYPE("supplier-type", CatalogMode.RENAME, List.of(
            of("RAW_MATERIAL", "Matière première"),
            of("COMPONENT", "Composant"),
            of("SERVICE", "Service"),
            of("CONTRACT_MANUFACTURER", "Sous-traitant"),
            of("SOFTWARE", "Logiciel / SaaS"),
            of("LOGISTICS", "Logistique"),
            of("OTHER", "Autre"))),

    /** Types d'incident santé-sécurité-environnement. */
    EHS_INCIDENT_TYPE("ehs-incident-type", CatalogMode.RENAME, List.of(
            of("INJURY", "Accident corporel"),
            of("NEAR_MISS", "Presque-accident"),
            of("ENVIRONMENTAL", "Environnement (déversement, pollution)"),
            of("SECURITY", "Sécurité / sûreté"),
            of("PROPERTY_DAMAGE", "Dommage matériel"),
            of("OTHER", "Autre"))),

    /** Types de projet APQP. */
    APQP_PROJECT_TYPE("apqp-project-type", CatalogMode.RENAME, List.of(
            of("NPI", "Nouveau produit"),
            of("TOW", "Transfert de production"),
            of("MAJOR_MODIFICATION", "Modification majeure"),
            of("OTHER", "Autre"))),

    /**
     * Natures de dossier CAPA. Renommables seulement : la nature commande les
     * verrous du dossier — un confinement ne se clôt pas comme un correctif, et
     * un préventif se déclenche sur un écart qui n'est pas survenu.
     */
    CAPA_NATURE("capa-nature", CatalogMode.RENAME, List.of(
            of("CORRECTIVE", "Correctif"),
            of("PREVENTIVE", "Préventif"),
            of("CONTAINMENT", "Confinement"))),

    /** Types de demande de modification (MOC). */
    MOC_CHANGE_TYPE("moc-change-type", CatalogMode.RENAME, List.of(
            of("PERMANENT", "Permanente"),
            of("TEMPORARY", "Temporaire"))),

    /**
     * Branches du diagramme d'Ishikawa. Renommables : le modèle 6M / 7M / 8M
     * décide lesquelles sont ouvertes, et les mots de chacune appartiennent au
     * secteur — « Main-d'œuvre » dans un atelier, « Équipe soignante » au bloc.
     */
    ISHIKAWA_CATEGORY("ishikawa-category", CatalogMode.RENAME, List.of(
            of("METHODS", "Méthodes"),
            of("MANPOWER", "Main-d'œuvre"),
            of("MACHINES", "Machines"),
            of("MATERIALS", "Matières"),
            of("MEASUREMENTS", "Mesures"),
            of("ENVIRONMENT", "Milieu"),
            of("MANAGEMENT", "Management"),
            of("MONEY", "Moyens financiers"))),

    /** Fréquences de mesure d'un indicateur. */
    KPI_FREQUENCY("kpi-frequency", CatalogMode.RENAME, List.of(
            of("REALTIME", "Temps réel"),
            of("DAILY", "Quotidienne"),
            of("WEEKLY", "Hebdomadaire"),
            of("MONTHLY", "Mensuelle"),
            of("QUARTERLY", "Trimestrielle"),
            of("YEARLY", "Annuelle"),
            of("ON_DEMAND", "À la demande"))),

    /** Mécanismes de détrompeur. */
    POKAYOKE_MECHANISM("pokayoke-mechanism", CatalogMode.RENAME, List.of(
            of("PHYSICAL_SHAPE", "Forme physique"),
            of("INTERLOCK", "Verrouillage"),
            of("LIMIT_SWITCH", "Fin de course"),
            of("SENSOR", "Capteur"),
            of("VISION", "Vision"),
            of("CHECKLIST", "Liste de vérification"),
            of("COLOR_CODING", "Code couleur"),
            of("POSITION_REFERENCE", "Référence de position"),
            of("COUNTER", "Comptage"),
            of("SOFTWARE_VALIDATION", "Contrôle logiciel"),
            of("OTHER", "Autre"))),

    /**
     * Zones et secteurs d'audit 5S. Entièrement au client : une zone est un lieu
     * de SON site. Saisie en texte libre, elle se réécrivait à chaque audit
     * (« Atelier 1 », « atelier1 », « At. 1 ») et la tendance par zone — la
     * carte de chaleur promise — n'existait pas.
     */
    FIVES_ZONE("fives-zone", CatalogMode.OPEN, List.of(
            of("PRODUCTION", "Production"),
            of("STOCKAGE", "Stockage et magasin"),
            of("EXPEDITION", "Réception et expédition"),
            of("MAINTENANCE", "Maintenance et atelier technique"),
            of("LABORATOIRE", "Laboratoire et contrôle"),
            of("BUREAUX", "Bureaux"),
            of("COMMUNS", "Locaux communs et vestiaires"),
            of("EXTERIEUR", "Extérieurs et abords"))),

    /** Secteurs de parties intéressées, pour l'aide à la saisie. */
    STAKEHOLDER_SECTOR("stakeholder-sector", CatalogMode.OPEN, List.of(
            of("INDUSTRIE", "Industrie et production"),
            of("SANTE", "Santé et médico-social"),
            of("PHARMA", "Pharmacie et dispositifs médicaux"),
            of("AGRO", "Agroalimentaire"),
            of("BANQUE", "Banque, finance et assurance"),
            of("IT", "Informatique et services numériques"),
            of("AERO", "Aéronautique et défense"),
            of("AUTOMOBILE", "Automobile"),
            of("BTP", "BTP et construction"),
            of("ENERGIE", "Énergie et utilités"),
            of("TRANSPORT", "Transport et logistique"),
            of("PUBLIC", "Secteur public et administration"),
            of("EDUCATION", "Éducation et formation"),
            of("RETAIL", "Commerce et distribution")));

    /** Au-delà, ce n'est plus une liste déroulante mais un champ libre déguisé. */
    public static final int MAX_ENTRIES = 200;

    private static final Map<String, CatalogId> BY_SLUG = bySlug();

    private final String slug;
    private final CatalogMode mode;
    private final List<CatalogEntry> defaults;

    CatalogId(String slug, CatalogMode mode, List<CatalogEntry> defaults) {
        this.slug = slug;
        this.mode = mode;
        this.defaults = List.copyOf(defaults);
    }

    /** Le nom de la liste dans les URL, en minuscules à tirets. */
    public String slug() {
        return slug;
    }

    public CatalogMode mode() {
        return mode;
    }

    /** Les valeurs de la plateforme, dans leur ordre d'affichage. */
    public List<CatalogEntry> defaults() {
        return defaults;
    }

    /** La liste nommée par ce slug, si elle est ouverte au paramétrage. */
    public static Optional<CatalogId> bySlug(String slug) {
        return slug == null ? Optional.empty()
                : Optional.ofNullable(BY_SLUG.get(slug.strip().toLowerCase(java.util.Locale.ROOT)));
    }

    private static Map<String, CatalogId> bySlug() {
        Map<String, CatalogId> index = new LinkedHashMap<>();
        for (CatalogId id : values()) {
            index.put(id.slug, id);
        }
        return Map.copyOf(index);
    }

    /** Les listes ouvertes au paramétrage, dans l'ordre d'affichage de l'écran. */
    public static List<CatalogId> all() {
        return Arrays.asList(values());
    }
}

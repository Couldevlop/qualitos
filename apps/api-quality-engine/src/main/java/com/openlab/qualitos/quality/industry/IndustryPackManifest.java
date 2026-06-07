package com.openlab.qualitos.quality.industry;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Modèle interne normalisé d'un manifeste d'Industry Pack (CLAUDE.md §5.1, ADR 0019).
 *
 * <p>Deux formats YAML convergent vers ce modèle unique, sérialisé tel quel dans
 * {@code industry_packs.manifest_json} :
 * <ul>
 *   <li><b>schéma plat</b> historique (clé {@code code}, {@code kpis} = liste de slugs,
 *       {@code standards}, {@code templates} = map) ; rétro-compatibilité totale ;</li>
 *   <li><b>schéma riche canonique</b> (clé {@code pack_id}, {@code norms}, {@code kpis} = objets §6.6,
 *       {@code ishikawa_templates}, {@code poka_yoke_library}, {@code sector}…), normalisé
 *       vers ce modèle par {@code IndustryPackRichManifest.toManifest()}.</li>
 * </ul>
 *
 * <p>Champs obligatoires (après normalisation) : code, name, version. Le reste est optionnel.
 * Les références ({@code standards}/{@code norms}) pointent vers le catalogue Standards Hub ;
 * leur existence est vérifiée au chargement (WARN si inconnue), elles ne bloquent pas le pack.
 *
 * <p>Annoté {@code @JsonInclude(NON_NULL)} : les champs riches absents du schéma plat ne
 * polluent pas le {@code manifest_json} des 3 packs historiques (régression préservée).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndustryPackManifest {

    private String code;
    private String name;
    private String description;
    private String version;
    private String locale;
    /** Secteur(s) ISIC/NACE/slug (schéma riche : {@code sectors}). */
    private List<String> sectors;
    private List<String> tags;
    /** Normes référencées (plat : {@code standards} ; riche : {@code norms}) — codes Standards Hub. */
    private List<String> standards;
    /** KPIs slugs (schéma plat uniquement). Null pour le schéma riche. */
    private List<String> kpis;
    /** KPIs définis §6.6 (schéma riche). Null pour le schéma plat. */
    private List<Kpi> richKpis;
    private List<String> connectors;
    /** Templates map (schéma plat : workflows/ishikawa/poka-yoke = listes de slugs). */
    private Map<String, List<String>> templates;
    /** Templates Ishikawa structurés 6M/7M (schéma riche). */
    private List<IshikawaTemplate> ishikawaTemplates;
    /** Bibliothèque Poka-Yoke (schéma riche). */
    private List<PokaYoke> pokaYokeLibrary;
    /** Glossaire term→définition (les deux schémas convergent vers cette map). */
    private Map<String, String> glossary;
    /** Parcours de formation (schéma riche) — payload brut conservé. */
    private List<Map<String, Object>> trainingPaths;
    /** Modèles de documents (schéma riche) — payload brut conservé. */
    private List<Object> documentsTemplates;
    /** Modèles de processus BPMN (schéma riche) — payload brut conservé. */
    private List<Object> processesTemplates;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public List<String> getSectors() { return sectors; }
    public void setSectors(List<String> sectors) { this.sectors = sectors; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getStandards() { return standards; }
    public void setStandards(List<String> standards) { this.standards = standards; }

    public List<String> getKpis() { return kpis; }
    public void setKpis(List<String> kpis) { this.kpis = kpis; }

    public List<Kpi> getRichKpis() { return richKpis; }
    public void setRichKpis(List<Kpi> richKpis) { this.richKpis = richKpis; }

    public List<String> getConnectors() { return connectors; }
    public void setConnectors(List<String> connectors) { this.connectors = connectors; }

    public Map<String, List<String>> getTemplates() { return templates; }
    public void setTemplates(Map<String, List<String>> templates) { this.templates = templates; }

    public List<IshikawaTemplate> getIshikawaTemplates() { return ishikawaTemplates; }
    public void setIshikawaTemplates(List<IshikawaTemplate> ishikawaTemplates) {
        this.ishikawaTemplates = ishikawaTemplates;
    }

    public List<PokaYoke> getPokaYokeLibrary() { return pokaYokeLibrary; }
    public void setPokaYokeLibrary(List<PokaYoke> pokaYokeLibrary) {
        this.pokaYokeLibrary = pokaYokeLibrary;
    }

    public Map<String, String> getGlossary() { return glossary; }
    public void setGlossary(Map<String, String> glossary) { this.glossary = glossary; }

    public List<Map<String, Object>> getTrainingPaths() { return trainingPaths; }
    public void setTrainingPaths(List<Map<String, Object>> trainingPaths) {
        this.trainingPaths = trainingPaths;
    }

    public List<Object> getDocumentsTemplates() { return documentsTemplates; }
    public void setDocumentsTemplates(List<Object> documentsTemplates) {
        this.documentsTemplates = documentsTemplates;
    }

    public List<Object> getProcessesTemplates() { return processesTemplates; }
    public void setProcessesTemplates(List<Object> processesTemplates) {
        this.processesTemplates = processesTemplates;
    }

    // -------------------------------------------------------------------------
    // Sous-structures du schéma riche (sérialisées dans manifest_json)
    // -------------------------------------------------------------------------

    /** Définition d'un KPI §6.6 (schéma riche). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Kpi {
        private String kpiId;
        private String name;
        private String category;
        private String formula;
        private String unit;
        private String target;
        private String thresholdWarning;
        private String thresholdCritical;
        private String dataSource;
        private String refreshFrequency;
        private String owner;
        private List<String> applicableIndustries;
        private List<String> relatedKpis;
        private String explainability;

        public String getKpiId() { return kpiId; }
        public void setKpiId(String kpiId) { this.kpiId = kpiId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getThresholdWarning() { return thresholdWarning; }
        public void setThresholdWarning(String thresholdWarning) { this.thresholdWarning = thresholdWarning; }
        public String getThresholdCritical() { return thresholdCritical; }
        public void setThresholdCritical(String thresholdCritical) { this.thresholdCritical = thresholdCritical; }
        public String getDataSource() { return dataSource; }
        public void setDataSource(String dataSource) { this.dataSource = dataSource; }
        public String getRefreshFrequency() { return refreshFrequency; }
        public void setRefreshFrequency(String refreshFrequency) { this.refreshFrequency = refreshFrequency; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public List<String> getApplicableIndustries() { return applicableIndustries; }
        public void setApplicableIndustries(List<String> applicableIndustries) { this.applicableIndustries = applicableIndustries; }
        public List<String> getRelatedKpis() { return relatedKpis; }
        public void setRelatedKpis(List<String> relatedKpis) { this.relatedKpis = relatedKpis; }
        public String getExplainability() { return explainability; }
        public void setExplainability(String explainability) { this.explainability = explainability; }
    }

    /** Template Ishikawa avec branches 6M/7M (schéma riche). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IshikawaTemplate {
        private String id;
        private String name;
        private String problemArchetype;
        /** Branches (man/machine/material/method/measurement/environment[/management…]) → causes semées. */
        private Map<String, List<String>> branches;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProblemArchetype() { return problemArchetype; }
        public void setProblemArchetype(String problemArchetype) { this.problemArchetype = problemArchetype; }
        public Map<String, List<String>> getBranches() { return branches; }
        public void setBranches(Map<String, List<String>> branches) { this.branches = branches; }
    }

    /** Entrée de bibliothèque Poka-Yoke (schéma riche). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PokaYoke {
        private String id;
        private String name;
        private String description;
        private List<String> sectorFit;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getSectorFit() { return sectorFit; }
        public void setSectorFit(List<String> sectorFit) { this.sectorFit = sectorFit; }
    }
}

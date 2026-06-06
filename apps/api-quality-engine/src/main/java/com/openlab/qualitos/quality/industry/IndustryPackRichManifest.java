package com.openlab.qualitos.quality.industry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Représentation typée du <b>schéma riche canonique</b> d'un Industry Pack (ADR 0019,
 * fichiers {@code libs/industry-packs/packs/*.yaml} portés dans le classpath de l'engine).
 *
 * <p>Détecté par la présence de la clé {@code pack_id}. Parsé via SnakeYAML typed
 * Constructor (durcissement anti-désérialisation conservé) avec un {@code PropertyUtils}
 * qui convertit les clés YAML snake_case en propriétés JavaBean camelCase.
 *
 * <p>Normalisé vers {@link IndustryPackManifest} par {@link #toManifest()} : le {@code code}
 * DB devient {@code pack_id}, {@code norms} alimente {@code standards}, le glossaire
 * liste-d'objets devient une map term→définition.
 */
public class IndustryPackRichManifest {

    private String packId;
    private String version;
    private String name;
    private String description;
    private String locale;
    private List<String> sectors;
    private List<String> norms;
    private List<KpiNode> kpis;
    private List<GlossaryNode> glossary;
    private List<String> connectorsRequired;
    private List<IshikawaNode> ishikawaTemplates;
    private List<PokaYokeNode> pokaYokeLibrary;
    private List<Map<String, Object>> trainingPaths;
    private List<Object> documentsTemplates;
    private List<Object> processesTemplates;

    public String getPackId() { return packId; }
    public void setPackId(String packId) { this.packId = packId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public List<String> getSectors() { return sectors; }
    public void setSectors(List<String> sectors) { this.sectors = sectors; }
    public List<String> getNorms() { return norms; }
    public void setNorms(List<String> norms) { this.norms = norms; }
    public List<KpiNode> getKpis() { return kpis; }
    public void setKpis(List<KpiNode> kpis) { this.kpis = kpis; }
    public List<GlossaryNode> getGlossary() { return glossary; }
    public void setGlossary(List<GlossaryNode> glossary) { this.glossary = glossary; }
    public List<String> getConnectorsRequired() { return connectorsRequired; }
    public void setConnectorsRequired(List<String> connectorsRequired) { this.connectorsRequired = connectorsRequired; }
    public List<IshikawaNode> getIshikawaTemplates() { return ishikawaTemplates; }
    public void setIshikawaTemplates(List<IshikawaNode> ishikawaTemplates) { this.ishikawaTemplates = ishikawaTemplates; }
    public List<PokaYokeNode> getPokaYokeLibrary() { return pokaYokeLibrary; }
    public void setPokaYokeLibrary(List<PokaYokeNode> pokaYokeLibrary) { this.pokaYokeLibrary = pokaYokeLibrary; }
    public List<Map<String, Object>> getTrainingPaths() { return trainingPaths; }
    public void setTrainingPaths(List<Map<String, Object>> trainingPaths) { this.trainingPaths = trainingPaths; }
    public List<Object> getDocumentsTemplates() { return documentsTemplates; }
    public void setDocumentsTemplates(List<Object> documentsTemplates) { this.documentsTemplates = documentsTemplates; }
    public List<Object> getProcessesTemplates() { return processesTemplates; }
    public void setProcessesTemplates(List<Object> processesTemplates) { this.processesTemplates = processesTemplates; }

    // -------------------------------------------------------------------------
    // Nœuds typés (SnakeYAML a besoin de types concrets pour les listes imbriquées)
    // -------------------------------------------------------------------------

    public static class KpiNode {
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

    public static class GlossaryNode {
        private String term;
        private String definition;
        public String getTerm() { return term; }
        public void setTerm(String term) { this.term = term; }
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }

    public static class IshikawaNode {
        private String id;
        private String name;
        private String problemArchetype;
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

    public static class PokaYokeNode {
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

    // -------------------------------------------------------------------------
    // Normalisation vers le modèle interne unique
    // -------------------------------------------------------------------------

    /** Convertit ce manifeste riche vers le modèle normalisé sérialisé en {@code manifest_json}. */
    public IndustryPackManifest toManifest() {
        IndustryPackManifest m = new IndustryPackManifest();
        m.setCode(packId);                 // code DB = pack_id
        m.setName(name);
        m.setVersion(version);
        m.setDescription(description);
        m.setLocale(locale);
        m.setSectors(sectors);
        m.setStandards(norms);             // norms → standards (refs Standards Hub)
        m.setConnectors(connectorsRequired);
        m.setTrainingPaths(trainingPaths);
        m.setDocumentsTemplates(documentsTemplates);
        m.setProcessesTemplates(processesTemplates);

        if (kpis != null) {
            List<IndustryPackManifest.Kpi> out = new ArrayList<>(kpis.size());
            for (KpiNode k : kpis) {
                IndustryPackManifest.Kpi rk = new IndustryPackManifest.Kpi();
                rk.setKpiId(k.getKpiId());
                rk.setName(k.getName());
                rk.setCategory(k.getCategory());
                rk.setFormula(k.getFormula());
                rk.setUnit(k.getUnit());
                rk.setTarget(k.getTarget());
                rk.setThresholdWarning(k.getThresholdWarning());
                rk.setThresholdCritical(k.getThresholdCritical());
                rk.setDataSource(k.getDataSource());
                rk.setRefreshFrequency(k.getRefreshFrequency());
                rk.setOwner(k.getOwner());
                rk.setApplicableIndustries(k.getApplicableIndustries());
                rk.setRelatedKpis(k.getRelatedKpis());
                rk.setExplainability(k.getExplainability());
                out.add(rk);
            }
            m.setRichKpis(out);
        }

        if (glossary != null) {
            Map<String, String> g = new LinkedHashMap<>();
            for (GlossaryNode gn : glossary) {
                if (gn != null && gn.getTerm() != null) g.put(gn.getTerm(), gn.getDefinition());
            }
            m.setGlossary(g);
        }

        if (ishikawaTemplates != null) {
            List<IndustryPackManifest.IshikawaTemplate> out = new ArrayList<>(ishikawaTemplates.size());
            for (IshikawaNode in : ishikawaTemplates) {
                IndustryPackManifest.IshikawaTemplate t = new IndustryPackManifest.IshikawaTemplate();
                t.setId(in.getId());
                t.setName(in.getName());
                t.setProblemArchetype(in.getProblemArchetype());
                t.setBranches(in.getBranches());
                out.add(t);
            }
            m.setIshikawaTemplates(out);
        }

        if (pokaYokeLibrary != null) {
            List<IndustryPackManifest.PokaYoke> out = new ArrayList<>(pokaYokeLibrary.size());
            for (PokaYokeNode pn : pokaYokeLibrary) {
                IndustryPackManifest.PokaYoke p = new IndustryPackManifest.PokaYoke();
                p.setId(pn.getId());
                p.setName(pn.getName());
                p.setDescription(pn.getDescription());
                p.setSectorFit(pn.getSectorFit());
                out.add(p);
            }
            m.setPokaYokeLibrary(out);
        }

        return m;
    }
}

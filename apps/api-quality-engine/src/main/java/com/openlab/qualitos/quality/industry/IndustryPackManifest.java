package com.openlab.qualitos.quality.industry;

import java.util.List;
import java.util.Map;

/**
 * Structure d'un manifeste YAML de pack sectoriel (CLAUDE.md §5.1).
 *
 * Champs obligatoires : code, name, version. Le reste est optionnel.
 * Les références (standards, kpis, workflows…) pointent vers d'autres modules
 * QualitOS — leur existence est validée par le module concerné, pas ici.
 */
public class IndustryPackManifest {

    private String code;
    private String name;
    private String description;
    private String version;
    private String locale;
    private List<String> tags;
    private List<String> standards;
    private List<String> kpis;
    private List<String> connectors;
    private Map<String, List<String>> templates;
    private Map<String, String> glossary;

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

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getStandards() { return standards; }
    public void setStandards(List<String> standards) { this.standards = standards; }

    public List<String> getKpis() { return kpis; }
    public void setKpis(List<String> kpis) { this.kpis = kpis; }

    public List<String> getConnectors() { return connectors; }
    public void setConnectors(List<String> connectors) { this.connectors = connectors; }

    public Map<String, List<String>> getTemplates() { return templates; }
    public void setTemplates(Map<String, List<String>> templates) { this.templates = templates; }

    public Map<String, String> getGlossary() { return glossary; }
    public void setGlossary(Map<String, String> glossary) { this.glossary = glossary; }
}

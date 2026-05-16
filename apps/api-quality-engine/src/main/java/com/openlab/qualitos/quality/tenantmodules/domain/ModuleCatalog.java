package com.openlab.qualitos.quality.tenantmodules.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.openlab.qualitos.quality.tenantmodules.domain.BillingTier.*;

/**
 * Catalogue immuable des modules QualitOS, défini en code (single source of
 * truth). Le service refuse toute activation sur un code absent du catalogue,
 * empêche d'activer si le tier du tenant est insuffisant, et valide les
 * dépendances avant d'activer.
 */
public final class ModuleCatalog {

    private static final Map<String, ModuleCatalogEntry> ENTRIES;
    static {
        List<ModuleCatalogEntry> list = List.of(
                // Core méthodes (toujours dispo en FREE)
                ModuleCatalogEntry.of("pdca",        "PDCA cycles",         "methodes",  FREE,       List.of(),                  true),
                ModuleCatalogEntry.of("ishikawa",    "Ishikawa diagrams",   "methodes",  FREE,       List.of(),                  true),
                ModuleCatalogEntry.of("fives",       "5S audits",           "methodes",  FREE,       List.of(),                  true),
                ModuleCatalogEntry.of("circle",      "Quality circles",     "methodes",  STANDARD,   List.of(),                  false),
                ModuleCatalogEntry.of("dmaic",       "DMAIC + Poka-Yoke",   "methodes",  STANDARD,   List.of(),                  false),

                // Pipelines transverses
                ModuleCatalogEntry.of("capa",        "CAPA",                "transverse", FREE,      List.of(),                  true),
                ModuleCatalogEntry.of("docs",        "Document control",    "transverse", FREE,      List.of(),                  true),
                ModuleCatalogEntry.of("audit",       "Audit management",    "transverse", FREE,      List.of(),                  true),
                ModuleCatalogEntry.of("risk",        "Risk / FMEA",         "transverse", STANDARD,  List.of("capa"),            false),
                ModuleCatalogEntry.of("supplier",    "Supplier QM",         "transverse", STANDARD,  List.of("capa", "audit"),   false),
                ModuleCatalogEntry.of("training",    "Training & Competency","transverse", STANDARD, List.of(),                  false),
                ModuleCatalogEntry.of("change",      "Change management",   "transverse", STANDARD,  List.of("docs"),            false),
                ModuleCatalogEntry.of("complaints",  "Customer complaints", "transverse", STANDARD,  List.of("capa"),            false),
                ModuleCatalogEntry.of("calibration", "Calibration & Equip", "transverse", STANDARD,  List.of(),                  false),
                ModuleCatalogEntry.of("ehs",         "EHS incidents",       "transverse", STANDARD,  List.of("capa"),            false),
                ModuleCatalogEntry.of("standards",   "Standards Hub",       "transverse", PRO,       List.of("docs", "audit"),   false),

                // Plateforme
                ModuleCatalogEntry.of("kpi",         "KPI engine",          "platform",  STANDARD,   List.of(),                  false),
                ModuleCatalogEntry.of("industry",    "Industry Packs",      "platform",  PRO,        List.of(),                  false),
                ModuleCatalogEntry.of("iot",         "IoT Hub",             "platform",  PRO,        List.of(),                  false),
                ModuleCatalogEntry.of("auditlog",    "Audit event log",     "platform",  STANDARD,   List.of(),                  true),
                ModuleCatalogEntry.of("blockchain",  "Blockchain anchor",   "platform",  ENTERPRISE, List.of("auditlog"),        false),

                // Intégrations
                ModuleCatalogEntry.of("webhooks",    "Outbound webhooks",   "integrations", STANDARD,   List.of(),               false),
                ModuleCatalogEntry.of("itsm",        "ITSM connectors",     "integrations", PRO,        List.of(),               false));

        java.util.LinkedHashMap<String, ModuleCatalogEntry> map = new java.util.LinkedHashMap<>();
        for (ModuleCatalogEntry e : list) map.put(e.code(), e);
        ENTRIES = java.util.Collections.unmodifiableMap(map);
    }

    private ModuleCatalog() {}

    public static List<ModuleCatalogEntry> all() { return List.copyOf(ENTRIES.values()); }

    public static Optional<ModuleCatalogEntry> find(String code) {
        return Optional.ofNullable(ENTRIES.get(code));
    }

    public static ModuleCatalogEntry require(String code) {
        ModuleCatalogEntry e = ENTRIES.get(code);
        if (e == null) throw new ModuleActivationStateException("Unknown module: " + code);
        return e;
    }

    public static boolean contains(String code) { return ENTRIES.containsKey(code); }
}

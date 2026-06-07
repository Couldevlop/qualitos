# libs/industry-packs — ⚠️ DÉPRÉCIÉ (répertoire éditorial historique)

> **Statut : déprécié depuis l'ADR 0019 (2026-06-06).**
>
> Ce répertoire n'est **plus le schéma canonique** et n'est **plus consommé au runtime**.
> Il est conservé uniquement comme archive éditoriale du contenu P4 initial.

## Où vit désormais le contenu canonique

Le schéma riche canonique des Industry Packs et leur contenu de production vivent
maintenant dans le **classpath de l'engine** :

```
apps/api-quality-engine/src/main/resources/industry-packs/*.yml   (et *.yaml)
```

C'est ce répertoire que charge `IndustryPackLoader` (chemin A de l'ADR 0019), qui
sait désormais lire :

- le **schéma plat** historique (clé `code`) — rétro-compatibilité ;
- le **schéma riche canonique** (clé `pack_id`) — normalisé vers `IndustryPackManifest`.

Le test de contrat correspondant est
`apps/api-quality-engine/src/test/java/.../industry/packs/P4IndustryPacksYamlTest.java`,
qui scanne désormais les ressources MAIN de l'engine.

## Pourquoi cette dépréciation

Cf. ADR `docs/adr/0019-industry-packs-schema-canonique.md` : trois implémentations
parallèles d'Industry Packs coexistaient (loader runtime plat, `libs/industry-commons`,
`libs/industry-packs`). L'ADR 0019 tranche : **un seul schéma canonique** (riche),
**un seul emplacement runtime** (classpath de l'engine), **un seul loader** convergent.

## Schéma riche canonique (référence)

```yaml
pack_id: <slug>                 # = code DB après normalisation
version: 1.0.0
name: <name>
sectors: [<tags>]               # repli en tags si 'tags' absent
norms: [<norm-ids>]             # vérifiés contre le catalogue Standards Hub (WARN si inconnu)
kpis:                           # objets §6.6 complets
  - kpi_id: <id>
    name: <name>
    category: <cat>
    formula: <formula>
    unit: <unit>
    target: <target>
    threshold_warning: <...>
    threshold_critical: <...>
    data_source: <source>
    refresh_frequency: <freq>
    owner: <owner>
    applicable_industries: [<...>]
    related_kpis: [<...>]
    explainability: <texte>
glossary:                       # liste d'objets → map term→définition
  - {term: <T>, definition: <D>}
connectors_required: [<connector ids>]
ishikawa_templates:
  - id: <id>
    name: <name>
    problem_archetype: <texte>
    branches: {man, machine, material, method, measurement, environment[, management…]}
poka_yoke_library:
  - {id, name, description, sector_fit: [<...>]}
training_paths: [{role, modules}]
documents_templates: [<paths>]
processes_templates: [<paths>]
```

> Pour ajouter un secteur : écrire **un** fichier YAML riche dans
> `apps/api-quality-engine/src/main/resources/industry-packs/` — plus d'ambiguïté de
> schéma, de répertoire ou d'extension.

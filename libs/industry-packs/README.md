# libs/industry-packs

YAML Industry Packs livrés en P4 et consommés par le service `libs/industry-commons/` (SPI géré par P3).

## Contrat YAML

Chaque pack respecte le schéma défini en P3 :

```yaml
pack_id: <slug>
version: 1.0.0
name: <name>
sectors: [<tags>]
norms: [<norm-ids referencing Standards Hub>]
kpis: [<inline KPI defs per CLAUDE.md §6.6>]
glossary: [{term, definition}]
connectors_required: [<connector ids — see api-connectors>]
ishikawa_templates: [{problem_archetype, branches: {man,machine,material,method,measurement,environment}}]
poka_yoke_library: [{id, name, description, sector_fit}]
training_paths: [{role, modules}]
documents_templates: [<paths>]
processes_templates: [<paths>]
```

## Packs livrés P4

- `automotive.yaml` — IATF 16949 + APQP/PPAP + OEM CSR (Renault/Stellantis/BMW/Toyota)
- `aerospace.yaml` — AS9100 + NADCAP + FAI AS9102
- `pharma.yaml` — ISO 13485 + FDA 21 CFR Part 11 + GxP IQ/OQ/PQ + GAMP 5
- `agro.yaml` — HACCP + ISO 22000 + IFS Food + BRCGS
- `banking.yaml` — DORA + Bâle III/IV + LCB-FT + MiFID II + ISO 27001
- `public.yaml` — ISO 18091 + Marianne + RGAA 4.1

Chaque pack contient au minimum :
- 6 KPIs sectoriels (formules complètes §6.6)
- 8 templates Ishikawa
- 4 entrées Poka-Yoke
- Liens vers les templates documentaires Standards Hub (P4 step 1)

## Sécurité

- Empreinte SHA-256 calculée et stockée par `IndustryPackLoaderService` (P3) au chargement.
- Parsing YAML avec **SafeConstructor** (anti-désérialisation arbitraire — OWASP A03 / A08).
- Activation pack tenant uniquement via super-admin authentifié (RBAC, OWASP A01).

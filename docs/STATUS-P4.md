# Statut P4 — Standards Hub × 6 normes + Industry Packs sectoriels + Connecteurs + IMS

> Mise à jour : 2026-05-17. Cf. CLAUDE.md §5, §8, §17.

## Livré (sprint 1)

### Standards Hub — 6 nouvelles normes (Flyway V52)
- **IATF 16949:2016** (Automobile) — APQP/PPAP, MSA, SPC ; ≥15 clauses HLS + spécifiques OEM.
- **AS9100D** (Aéronautique) — counterfeit parts, FAI, configuration management ; ≥15 clauses.
- **ISO 13485:2016** (Dispositifs médicaux) — classification, doc technique MDR, PMS ; ≥15 clauses.
- **FDA 21 CFR Part 11** (Signatures électroniques) — audit trail, CSV ; ≥15 sous-sections.
- **ISO 22000:2018 / HACCP** (Agro-alimentaire) — CCP, PRP, PRPo, plans surveillance ; ≥15 clauses.
- **DORA (UE) 2022/2554** (Finance) — registre TIC, TLPT, reporting incidents ; ≥15 articles.

Chaque norme : metadata complète per §8.3, ≥3 templates documentaires markdown sous `apps/api-quality-engine/src/main/resources/standards/templates/<norm-id>/`, chemin certification §8.5.

### Industry Packs sectoriels — 6 packs YAML (compat SPI P3)
- `automotive.yaml` (IATF 16949 + APQP/PPAP + OEM CSR : Renault, Stellantis, BMW, Toyota)
- `aerospace.yaml` (AS9100 + NADCAP + FAI)
- `pharma.yaml` (ISO 13485 + FDA 21 CFR Part 11 + GxP IQ/OQ/PQ + GAMP 5)
- `agro.yaml` (HACCP + ISO 22000 + IFS Food + BRCGS)
- `banking.yaml` (DORA + Bâle III/IV + LCB-FT + MiFID II)
- `public.yaml` (ISO 18091 + Marianne + RGAA 4.1)

Chaque pack : ≥6 KPIs sectoriels (formules §6.6), ≥8 templates Ishikawa, ≥4 Poka-Yoke.

### IMS multi-norm overlap (§8.9)
- Flyway : table `norm_clause_mapping` (norm_a, clause_a, norm_b, clause_b, mapping_type, evidence_overlap_pct).
- Seed des overlaps HLS Annexe SL pour clauses §4–10 de ISO 9001 / 14001 / 45001 / 22301 / 27001.
- Endpoint `GET /api/v1/standards/coverage-matrix?tenant=...` — matrice cross-norm rendue par `GetCoverageMatrixUseCase`.

### api-connectors (Spring Boot 3.5, Java 21, port 8084)
- `ConnectorProvider` SPI (clean arch hexagonale).
- Adapters stub : SAP ERP (REST), ServiceNow ITSM (REST avec mocks), HL7 FHIR R5 (cible `http://hapi.fhir.org/baseR5/` allow-listée), OPC-UA (placeholder).
- REST `/api/v1/connectors` (list, configure, trigger sync, history).
- Encryption AES-256-GCM des configs au repos (clés via env).
- Distroless image, RFC 7807, OpenAPI 3.1.

## En cours / non livré (sprint 2)

- **api-connectors sprint complet** : 15+ tests requis pour gate JaCoCo 85 % ; complétion progressive sprint 2.
- **MES OPC-UA réel** : driver Eclipse Milo, sprint P3/P4.2.
- **HL7 FHIR R5 production** : aujourd'hui contre serveur test public ; intégration EHR réelle = sprint P4.2.
- **CSR OEM-specific** : seeds Renault/Stellantis/BMW/Toyota déclarés ; règles métier détaillées = sprint P4.2.
- **Audit blanc IA par norme** (§8.7) : moteur d'alignement RAG → P5 ai-service consommera Standards Hub.
- **Marketplace packs sectoriels** (§8.11) : couvert par P5 (`marketplace_pack` entity).

## Conflits avec P3 / P5

- **P3** définit `libs/industry-commons/` (SPI). P4 ajoute les YAML packs dans `libs/industry-packs/packs/` en respectant le contrat. **Order de merge** : merger P3 d'abord (SPI), puis P4 (packs).
- **P5** : ajoute `marketplace_pack` qui pourrait référencer les packs de P4 — pas de collision schéma (tables disjointes).
- **docker-compose** : port 8084 (api-connectors) ; pas de collision (P3 = 8083/8090/1883/18083, P5 = 8085/6333/11434).
- **Migrations Flyway** : V52 (P4 standards) ; vérifier que P5 (V50/V51) et P4 (V52) ne se croisent pas après merge.

## Démo curl

```bash
TOK=$(curl -s -X POST http://localhost:8080/realms/qualitos/protocol/openid-connect/token \
  -d 'client_id=qualitos-web&grant_type=password&username=superadmin&password=superadmin' \
  | jq -r .access_token)

# List norms (devrait inclure les 6 nouvelles)
curl -H "Authorization: Bearer $TOK" http://localhost:8082/api/v1/standards | jq '.content[].code'

# Dossier complet d'une norme (e.g. IATF)
curl -H "Authorization: Bearer $TOK" http://localhost:8082/api/v1/standards/iatf-16949

# Industry Packs disponibles
curl -H "Authorization: Bearer $TOK" http://localhost:8181/api/v1/industry-packs

# Trigger sync connecteur (e.g. ServiceNow stub)
curl -X POST -H "Authorization: Bearer $TOK" \
  http://localhost:8084/api/v1/connectors/servicenow-stub/sync

# Coverage matrix multi-norm
curl -H "Authorization: Bearer $TOK" \
  'http://localhost:8082/api/v1/standards/coverage-matrix?tenant=00000000-0000-0000-0000-000000000099'
```

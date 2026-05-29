# ADR 0016 — Chaîne capteur → CAPA (détection de seuil in-engine)

- **Statut** : Accepté
- **Date** : 2026-05-29
- **Owners** : @Couldevlop

## Contexte

Le cas d'usage emblématique §9.9 (« une dérive capteur ouvre automatiquement une
non-conformité ») n'était pas fonctionnel (cf. `docs/AUDIT-stub-vs-reel.md`) :
- `api-iot-hub` POSTait vers un endpoint engine `/api/v1/nc/from-iot` **inexistant**,
  avec un registre de seuils en mémoire **vide** ;
- l'enum `CapaSourceType.IOT_ALERT` existait mais n'était **jamais** utilisé.

Deux options pour câbler la détection :
- **A. Cross-service** : implémenter `/api/v1/nc/from-iot` côté engine, appelé par
  `api-iot-hub`. Le `tenantId` arrive alors dans le body → tension avec §18.2 #2
  (« jamais de `tenant_id` depuis le body »), exige un client de service Keycloak
  (`qualitos-iot`) + une validation device→tenant, et ajoute une surface SSRF.
- **B. In-engine** : l'engine ingère **déjà** la télémétrie (`TelemetryIngestionService`)
  avec le `tenantId` résolu depuis le JWT. La détection + l'ouverture de CAPA s'y
  greffent sans aucun appel réseau.

## Décision

**Option B.** La détection de seuil et l'ouverture de CAPA vivent dans
`TelemetryIngestionService`, dans la **même transaction** que l'ingestion.

1. Nouveau modèle `IotThreshold` (table `iot_thresholds`, Flyway **V65**) : bornes
   `min/max`, criticité CAPA cible, **`capaOwnerId` porté par le seuil** (pas
   d'assignation implicite), `deviceId` nullable (seuil tenant-large). CRUD exposé
   sous `/api/v1/iot/thresholds`.
2. À l'ingestion d'une mesure numérique : `findApplicable(tenant, device, metric)` ;
   pour le premier seuil dépassé, ouverture d'une CAPA via le `CapaService` existant
   (`sourceType=IOT_ALERT`, `type=CORRECTIVE`, criticité = celle du seuil).
3. **Anti-spam / idempotence** : `sourceRef` stable (`iot:device:{id}:metric:{metric}`) ;
   on n'ouvre pas de nouvelle CAPA tant qu'une CAPA `OPEN`/`IN_PROGRESS` existe pour
   cette origine. Une fois clôturée, une nouvelle dérive rouvre une CAPA.
4. **Atomicité** : la CAPA est créée dans la transaction d'ingestion. Si la création
   échoue, l'ingestion rollback (cohérence forte assumée — la mesure n'est pas perdue
   silencieusement sans sa NC).

## Conséquences

- Le cas d'usage §9.9 fonctionne end-to-end dans un seul service, 100 % testable,
  sans surface SSRF ni tenant venant du body.
- Le chemin `api-iot-hub → /api/v1/nc/from-iot` reste **hors périmètre** (documenté) :
  il faudra, pour l'activer, un client de service Keycloak `qualitos-iot` + validation
  tenant côté engine. `api-iot-hub` reste utile comme ingestion edge buffer.
- Itérations ultérieures (§9.9 « + lien FMEA + cycle PDCA », outbox/retry) non couvertes ici.

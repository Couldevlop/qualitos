# Inventaire des listes déroulantes — ce que l'administrateur du client doit pouvoir changer

> Demande : « l'admin du tenant doit pouvoir personnaliser toutes les infos du tenant,
> notamment les listes déroulantes et autres. »
>
> Établi le 2026-11-09 par relevé exhaustif des listes codées en dur de `apps/web`
> (`readonly …s: X[] = [ … ]`, `const … = ['…']`, tables de libellés) et des réglages
> par client déjà servis par `apps/api-quality-engine`.

## 1. La règle de tri

Toutes les listes ne se valent pas. Trois familles, et une seule est paramétrable :

| Famille | Définition | Paramétrable ? |
| --- | --- | --- |
| **A — Machine d'état** | Les valeurs commandent le code : transitions autorisées, verrous de phase, calculs. `CapaStatus`, `DmaicPhase`, `PdcaPhase`, `NcStatus`, `AuditStatus`… | **Non.** Renommer l'affichage, oui (§3) ; ajouter une valeur, non — aucun chemin de code ne la connaîtrait. |
| **B — Exigence externe** | Les valeurs viennent d'un texte : RGPD, AI Act, NIS 2, DORA, ISO, IATF. `BreachSeverity`, `AdmType`, `ObligationLevel` (MUST/SHOULD/MAY), `EvidenceType`, `CharacteristicClass`. | **Non.** Les changer, c'est sortir du référentiel ; la plateforme perdrait sa valeur de preuve. |
| **C — Taxonomie maison** | Les valeurs décrivent l'organisation : ses natures d'écart, ses types d'audit, ses familles de fournisseurs, ses zones. Rien dans le code n'en dépend. | **Oui.** C'est l'objet de cet inventaire. Règle §18.2 #9 : aucune liste sectorielle codée en dur. |

Une échelle cotée (gravité 1-4, criticité, RPN) est de famille A par ses **paliers** —
ils entrent dans des calculs et des seuils — et de famille C par ses **libellés** :
le client dit « Sévère » ou « Bloquant » comme il veut, sans déplacer les paliers.

## 2. Ce qui est déjà paramétrable (livré)

| Liste | Surface d'administration | Serveur |
| --- | --- | --- |
| Natures de non-conformité, par catégorie | `/admin/nc-settings` | `NcSubcategoryCatalog` |
| Questionnaire de la fiche de résolution, types de défaillance, échelle de sévérité | `/admin/dmaic-settings` | `ResolutionSettings` |
| Types et familles de documents, dossiers | `/admin/document-settings` | `DocumentCatalogService` |
| Règles chiffrées des demandes de changement | `/admin/moc-settings` | `MocSettings` |
| Seuils de capabilité, règles de carte | `/admin/spc-settings` | `SpcSettings` |
| Échelles FMEA (gravité / occurrence / détection) | fenêtre « Référentiel » du module FMEA | `fmeascale` |
| Matrice des rôles, circuits de validation, modules actifs | `/admin/roles-matrix`, `/admin/circuits`, `/admin/tenant-modules` | `authz`, `circuit`, `tenant_modules` |
| Bibliothèque de détrompeurs (alimentée par les actions DMAIC) | fiche de résolution → « Inscrire un détrompeur » | `pokayoke_devices.tenant_id` (V161) |

## 3. Ce qui reste codé en dur — famille C, à ouvrir

Par ordre de valeur décroissante pour le client. Chaque ligne nomme le fichier
d'où la liste doit sortir.

| # | Liste | Où elle est codée aujourd'hui | Pourquoi l'ouvrir |
| --- | --- | --- | --- |
| 1 | **Catégories de non-conformité** (7 valeurs) | `admin/pages/nc-settings` (`CATEGORIES`), `nc/pages/nc-create-dialog`, `nc/pages/nc-list` | Les natures sont déjà au client, mais le niveau au-dessus ne l'est pas : un CHU n'a pas de catégorie « Fournisseur » en tête, il a « Parcours patient ». Le plafond actuel oblige à ranger sous « Autre ». |
| 2 | **Types d'audit** | `audits/pages/audits-create-dialog`, `-edit-dialog`, `-planning` | Interne / externe / fournisseur ne couvre ni les audits croisés, ni les LPA par couche, ni les visites réglementaires. |
| 3 | **Types et familles de fournisseurs** | `suppliers/pages/suppliers-list` (`types`) | Une famille de fournisseurs est propre au métier : façonnier, sous-traitant de stérilisation, prestataire TIC (DORA). |
| 4 | **Types d'incident EHS** | `ehs/pages/ehs-list` (`types`), `-report-dialog` | ISO 45001 laisse la typologie à l'organisation ; la liste actuelle est industrielle. |
| 5 | **Zones et secteurs d'audit 5S** | saisie libre aujourd'hui, pas de référentiel | Une zone saisie en texte libre se réécrit à chaque audit : plus de tendance par zone, alors que la carte de chaleur §3.2 la promet. |
| 6 | **Types de projet APQP** | `apqp/pages/apqp-project-dialog`, `-list` | NPI / TOW / modification majeure sont les mots de l'automobile ; l'aéro et le médical ont les leurs. |
| 7 | **Natures de CAPA** | `capa/pages/capa-create-dialog` (`natures`) | Correctif / préventif / confinement est le socle ; certains clients distinguent « amélioration » et « dérogation ». |
| 8 | **Types de modification (MOC)** | `changes/pages/moc-request-dialog` | Les règles chiffrées sont déjà paramétrables, pas la typologie qu'elles classent. |
| 9 | **Catégories d'Ishikawa (6M)** | `dashboard-builder/application/widget-render.service.ts`, éditeur Ishikawa | Les branches 6M / 7M / 8M sont annoncées « configurables selon le secteur » (§3.5) ; elles ne le sont pas. |
| 10 | **Secteurs de parties intéressées** | `stakeholders/pages/stakeholder-assistant` (`sectors`) | Liste d'aide à la saisie, utile à adapter au secteur du client. |
| 11 | **Mécanismes de détrompeur** | `dmaic/pokayoke.labels.ts` | Taxonomie de la plateforme, suffisante en l'état ; à ouvrir seulement si un client demande un mécanisme absent. |
| 12 | **Fréquences de KPI, sources de mesure** | `kpis/pages/kpis-dialog`, `-measurement-dialog` | Famille A pour les sources (elles commandent la collecte), C pour les fréquences libres (« par lot », « par campagne »). |

## 4. La forme à retenir — un seul mécanisme, pas douze

Les réglages déjà livrés ont chacun leur table, leur contrôleur et leur écran.
Douze de plus coûteraient douze fois. Le socle commun à bâtir :

- **Un catalogue nommé par client** : `(tenant_id, catalogue, code, libellé, ordre, actif)`,
  une seule table, une seule API (`GET/PUT /api/v1/settings/catalogs/{catalogue}`),
  un seul écran d'administration qui liste les catalogues ouverts.
- **Valeurs de la plateforme par défaut**, exactement comme `ResolutionDefaults` :
  la liste actuelle devient le défaut, le client s'en écarte s'il veut. Aucune
  régression pour les clients existants, aucune migration de données.
- **Le code ne meurt jamais** : retirer une entrée ne reclasse pas l'historique.
  Les enregistrements gardent leur code, affiché tel quel — effacer un classement
  passé réécrirait l'histoire. C'est déjà la règle de `/admin/nc-settings`.
- **Réservé à l'administrateur du client** (droit `tenant.settings`), hors gestion
  des utilisateurs : règle de paramétrage de la maison.
- **Les libellés des familles A et B restent renommables** par le même mécanisme,
  avec les paliers et les codes verrouillés : le client dit « Bloquant » là où la
  plateforme dit « CRITICAL », sans toucher aux transitions ni aux calculs.

## 5. Ce que cet inventaire ne couvre pas

- Les colonnes de tableaux (`displayedColumns`) : affichage, pas donnée métier —
  elles relèvent des préférences d'écran, pas du paramétrage du client.
- Les listes de fournisseurs techniques (connecteurs ITSM, ERP, EHR, modes
  d'authentification) : elles disent ce que la plateforme sait parler.
- Les exemples de requêtes en langage naturel : aide à la saisie, sans effet.

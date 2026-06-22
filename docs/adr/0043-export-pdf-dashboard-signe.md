# ADR 0043 — Export PDF d'un dashboard, signé ML-DSA + ancré blockchain avec QR de vérification

- **Statut** : Accepté
- **Date** : 2026-06-22
- **Owners** : @Couldevlop
- **Phase** : P5 (excellence opérationnelle, CLAUDE.md §7.3 / §7.4)

## Contexte

CLAUDE.md §7.3 (« Export : PNG, PDF signé blockchain, Excel/CSV ») et §7.4
(« Rapports : PDF avec **signature ML-DSA** + **QR code blockchain** ») exigent
qu'un tableau de bord puisse être exporté en un document **officiel, vérifiable
a posteriori**. C'était le dernier reliquat de §7.3/§7.4 : le builder de
dashboards (`apps/web/.../dashboard-builder`, ADR 0034/0035) sauvegardait et
signait son **layout**, mais ne produisait aucun artefact PDF diffusable.

L'infrastructure de confiance existe déjà et **doit être réutilisée** (pas de
réimplémentation crypto/ancrage) :

- `HybridSignatureService` (lib `security-commons-crypto`, ADR 0011) — signe et
  re-vérifie une enveloppe hybride **Ed25519 + ML-DSA-65** ; le contexte
  `audit-report` est déjà utilisé pour les dossiers de certification signés.
- `BlockchainAnchorPort` (`...quality.blockchain.domain`) — ancre une empreinte
  (stub en dev, Hyperledger Fabric en prod, §11.3).
- Journal d'audit chaîné append-only (`AuditEventService`, §11.5) pour
  l'invariant §18.2 #5 (une action signée/ancrée est auditable).
- Pattern de vérification **publique par code opaque** déjà éprouvé par les
  certificats Academy (`GET /api/v1/training/certificates/{code}`).

## Décision

### Backend — Clean / hexagonal (`quality.dashboards.export`)

- **domain** : agrégat immuable `DashboardExport` (tenant/user du JWT, code de
  vérification 32-car URL-safe, empreinte SHA-256, enveloppe de signature,
  référence d'ancrage), view-model `DashboardExportModel` et ports
  `DashboardExportRepository` / `DashboardPdfRenderPort`. **Zéro** dépendance
  framework/PDF (vérifié par ArchUnit).
- **application** : `DashboardExportService` orchestre le cas d'usage via des
  ports uniquement — charge le nom du dashboard (tenant-scopé, visibilité
  héritée de `DashboardLayoutService`), génère un code aléatoire, **rend le PDF
  en un seul passage**, calcule le SHA-256 des octets rendus, **signe** cette
  empreinte (hybride), **ancre** l'empreinte, **persiste** le reçu et
  **journalise** l'action. La vérification publique revalide la signature stockée
  sur l'empreinte stockée.
- **infrastructure** : `PdfBoxDashboardRenderAdapter` (Apache PDFBox + QR ZXing),
  adapter JPA, bridges vers le port blockchain et le journal d'audit, builder
  d'URL de vérification configurable, provider tenant JWT.
- **web** : `POST /api/v1/dashboards/custom/{id}/export/pdf` (authentifié,
  `application/pdf` + en-têtes d'intégrité `X-Export-*`) et
  `GET /api/v1/dashboards/public/exports/{code}/verify` (**permitAll**).

### Choix structurants

1. **Rendu mono-passe, le PDF n'embarque PAS sa propre empreinte.** Un document
   ne peut pas contenir le hash de ses octets finaux (paradoxe d'auto-référence).
   Le PDF porte le **code de vérification** (stable) + un **QR** vers l'endpoint
   public ; le SHA-256 est calculé sur les octets finaux, c'est lui qui est signé,
   ancré et stocké. Le round-trip `verify` revalide donc exactement l'empreinte
   des octets téléchargés (testé).
2. **Le code opaque EST l'autorité** côté vérification publique : pas de JWT, pas
   de tenant. La réponse ne contient que des **faits d'intégrité** (booléen +
   empreinte + référence d'ancrage + nom du dashboard + date) — jamais de données
   métier tenant (OWASP A01). Un code inconnu renvoie `valid=false` (pas
   d'énumération de tenants).
3. **Bibliothèque PDF** : Apache PDFBox 3.0.5 (moteur pur-Java mature, pas de CVE
   Haute/Critique connue, §18.2 #1) ; QR via ZXing core 3.5.3. Ajoutées au seul
   `apps/api-quality-engine/pom.xml`.
4. **Multi-tenant strict** : tenant + user toujours issus du JWT (§18.2 #2). La
   visibilité du dashboard (propriétaire OU partagé dans le tenant) est déléguée
   à `DashboardLayoutService.get` — accès croisé → 404, sans fuite.

## Conséquences

- **Migration** `V96__create_dashboard_exports.sql` (V94/V95 réservées) : table
  `dashboard_exports` (reçu immuable, `verification_code` unique, contraintes
  CHECK regex sur le code et le SHA). Enveloppe de signature en `TEXT` +
  `@JdbcTypeCode(Types.LONGVARCHAR)`.
- **Sécurité** : nouvel exposé public minimal (verify), exempté d'auth via un
  matcher `permitAll` ciblé ; en-têtes `X-Export-*` exposés via CORS.
- **Front** : bouton « Exporter en PDF (signé) » dans l'éditeur de dashboard
  (i18n), téléchargement du blob, métadonnées d'intégrité lues dans les en-têtes.
- **Tests** : domain + application (round-trip signature, anti-tamper, code
  inconnu) + infra (PDF parsable non vide, QR, adapters) + web (MockMvc) ; front
  Karma (service + http adapter + éditeur). Couverture du module export ~95 %.

## Alternatives écartées

- **Rendu HTML→PDF (comme le dossier de certification)** : produit du HTML, pas
  un vrai PDF diffusable et intégrant nativement un QR. PDFBox donne un PDF/A-able
  pur-Java sans navigateur headless.
- **Empreinte sur le layout JSON plutôt que sur le PDF** : ne prouverait pas
  l'intégrité du document réellement diffusé. On signe l'artefact livré.
- **Double passe pour imprimer le hash dans le PDF** : impossible (auto-référence)
  et inutile — le QR + code suffisent à la vérification serveur.

## Note d'intégration

`docs/adr/README.md` n'a **pas** été modifié (réservé à l'intégrateur). Ligne
d'index à ajouter :

`| [0043](./0043-export-pdf-dashboard-signe.md) | Export PDF d'un dashboard, signé ML-DSA + ancré blockchain (QR de vérification) | Accepté |`

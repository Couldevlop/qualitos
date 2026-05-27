# ADR 0012 — Ancrage blockchain réel : reçus signés (Phase A) → Hyperledger Fabric (Phase B)

- **Statut** : Accepté — Phase A validée (WS3). Phase B (Fabric, WS4) à rouvrir.
- **Date** : 2026-05-26
- **Owners** : @Couldevlop

## Contexte

CLAUDE.md §11.3, §11.5 et §18.2-5 imposent l'ancrage blockchain des preuves
d'intégrité (rapports d'audit, NC, décisions qualité, certificats) sur un
**Hyperledger Fabric** permissionné, avec un chaincode Go exposant
`AnchorAudit(hash, tenant, ts)` et `VerifyEvidence(hash)`, et **uniquement des
hashes on-chain** (jamais de données personnelles).

État actuel constaté :

- L'architecture hexagonale est **déjà en place et propre** dans
  `apps/api-quality-engine/.../blockchain/` : `AnchoringService` charge les
  `AuditEvent` non ancrés d'un tenant, calcule un **Merkle root** (`MerkleTree`),
  le soumet via le port `BlockchainAnchorPort.submitRoot()`, puis marque les
  événements (`AuditEventAnchorablesAdapter`, idempotent).
- `AuditEvent` porte déjà une **chaîne de hash inviolable** : `integrityHash`
  (SHA-256 chaîné à `previousHash`) + `sequenceNo` strictement croissant par tenant.
- Le **seul** point stubbé est `StubBlockchainAnchorAdapter.submitRoot()` →
  `"stub-tx-" + UUID`.
- Manquent : le vrai backend, le **chemin de vérification** (`VerifyEvidence`), un
  **scheduler** (ancrage uniquement manuel via `POST /api/v1/blockchain/anchor/run`),
  et l'app `apps/blockchain-service` attendue (§18.1).

## Décision

Mise en œuvre en **deux phases** pour livrer de la valeur vérifiable sans bloquer
sur l'infrastructure Fabric (lourde).

### Phase A — Notarisation cryptographique signée (sans réseau Fabric)

- `SignedAnchorAdapter implements BlockchainAnchorPort` : signe le Merkle root via
  le `HybridSignatureService` d'[ADR 0011](./0011-pq-crypto-agility-signing.md)
  (contexte `blockchain-block`) et persiste un **reçu d'ancrage append-only**
  chaîné :

  ```
  anchor_receipt (Flyway V53) :
    id, tenant_id, merkle_root, prev_receipt_hash,   -- chaîne les reçus entre eux
    signature_envelope, signed_at, seq_no, event_count, tx_ref
  ```

  `txRef` = `id` du reçu (compatible avec le contrat `submitRoot → String ≤ 200`).
- **Scheduler** `@Scheduled` qui lance un batch d'ancrage par tenant actif (l'API
  manuelle existante est conservée).
- **Vérification** : `GET /api/v1/blockchain/verify?hash=<integrityHash>` →
  preuve d'inclusion Merkle de l'événement dans le reçu + vérification de la
  signature hybride du reçu → `VERIFIED | TAMPERED | NOT_ANCHORED`.
- `StubBlockchainAnchorAdapter` repassé en `@Profile("test")` (il n'est plus le
  bean par défaut).

### Phase B — Hyperledger Fabric réel

- `infra/blockchain/` : réseau Fabric 2.5 de test (docker-compose : orderer, 1-2
  peers, CA) + **chaincode Go** `qualitos-anchor` exposant `AnchorAudit(root,
  tenant, ts)` et `VerifyEvidence(root)`.
- `apps/blockchain-service` (Spring Boot + **Fabric Gateway SDK**, mTLS) exposant
  `submitRoot` / `verify` au quality-engine.
- `FabricBlockchainAnchorAdapter` (dans api-quality-engine) appelle
  `blockchain-service` et stocke le **txId Fabric** dans `anchor_receipt.tx_ref`.
- Bascule par **feature-flag / `@Profile`** : `signed` (défaut) → `fabric` ;
  `SignedAnchorAdapter` reste le fallback si Fabric est indisponible.

## Justification

- La Phase A apporte une **notarisation vérifiable de bout en bout**, testable en
  CI **sans** déployer un réseau Fabric, en réutilisant la chaîne de hash
  `AuditEvent` déjà présente — gros saut vs le stub, à faible coût.
- La Phase B ajoute la **décentralisation consortium** (non-répudiation
  multi-parties) quand l'infra le justifie.
- Conforme §11.3 RGPD : seuls des **hashes** (Merkle roots) sont signés/ancrés.

## Conséquences

- ✅ Vérification publique d'intégrité dès la Phase A (reçus chaînés + signature
  hybride = tamper-evidence sans Fabric).
- ✅ Port `BlockchainAnchorPort` inchangé → les deux phases coexistent sans
  refactor des appelants.
- ⚠ Phase A = notarisation **centralisée** (c'est le serveur qui signe) : la
  non-répudiation multi-parties exige Fabric (Phase B).
- ⚠ L'infra Fabric (orderer/peers/CA, chaincode Go, CI) est lourde → assumée et
  isolée en Phase B.

## Tests d'invariant

- Preuve d'inclusion Merkle correcte / incorrecte (événement absent → `NOT_ANCHORED`).
- `anchor_receipt` **append-only** : tout UPDATE rejeté (test + contrainte/trigger).
- Vérification signature du reçu (altération root → `TAMPERED`).
- Idempotence du batch (re-run ne ré-ancre pas, `blockchainTxRef` déjà posé).
- Isolation tenant : un reçu/événement d'un autre tenant → 404 (OWASP A01).
- Phase B : test d'intégration Testcontainers contre un réseau Fabric de test.

## Références

- CLAUDE.md §11.3, §11.5, §18.1, §18.2-5, §21 (RGPD on-chain).
- Fichiers : `apps/api-quality-engine/.../blockchain/`, `apps/blockchain-service/`
  (Phase B), `infra/blockchain/` (Phase B).
- Dépend de [0011](./0011-pq-crypto-agility-signing.md) (service de signature hybride).

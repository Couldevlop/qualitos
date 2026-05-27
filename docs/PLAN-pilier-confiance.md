# Plan — Pilier « Confiance » : crypto post-quantique + blockchain réels

> Statut : **WS1-3 LIVRÉS & VÉRIFIÉS** (`mvn verify` vert : crypto 67 tests + quality-engine 3144 tests, gate JaCoCo OK). WS4 (Fabric) et WS5 (TLS hybride) à rouvrir. Date : 2026-05-26.
>
> **Réalisé** : crypto PQ réelle (ML-DSA-65 + ML-KEM-768, hybride Ed25519+ML-DSA-65) ;
> dossier de certification + dashboards signés et vérifiables ; ancrage Phase A
> (`SignedAnchorAdapter` + reçus chaînés append-only `anchor_receipts` V62 +
> scheduler + endpoint `GET /api/v1/blockchain/verify`). Stub blockchain → `@Profile("test")`.
> Branche `feature/pilier-confiance-crypto`, non commité.
> ADR liés : [0011](./adr/0011-pq-crypto-agility-signing.md) (crypto),
> [0012](./adr/0012-blockchain-anchoring-fabric.md) (ancrage).
> Cf. CLAUDE.md §11.3-5, §18.2-5, §22.7.

## Objectif

Rendre **réels** les deux mécanismes de confiance aujourd'hui en placeholder/stub,
pour honorer l'invariant §18.2-5 (« aucune action critique sans MFA + ancrage
blockchain ») et la promesse §2.1 (« signée ML-DSA, ancrée blockchain »).

## Constat de départ (vérifié dans le code)

| Élément | État réel |
| --- | --- |
| Lib `security-commons-crypto` | Existe **en worktree non mergé**, hors pom parent. Suites crypto-agility OK, **SPI sign/verify vides** (pas de primitive). |
| Signatures (`signatureHash`/`signature`) | Chaînes placeholder partout (dashboard, doc, certif, dossier certif, audit). |
| Module `blockchain/` | Archi hexagonale **propre** : `AnchoringService` → Merkle root → `BlockchainAnchorPort`. Seul `StubBlockchainAnchorAdapter` est faux. |
| `AuditEvent` | Chaîne de hash **déjà inviolable** (`integrityHash`/`previousHash`/`sequenceNo`). |
| Manquants | Adapter Bouncy Castle, scheduler d'ancrage, chemin `verify`, `apps/blockchain-service`, réseau Fabric + chaincode Go. |
| Stack réelle | **Java 21** (le CLAUDE.md dit 25 — écart à noter). |

## Workstreams & séquençage

```
WS1 (crypto socle) ──► WS2 (signer les artefacts)
       │
       └──► WS3 (ancrage Phase A : reçus signés) ──► WS4 (Fabric Phase B)

WS5 (TLS hybride) : indépendant, différé (nécessite la gateway)
```

| WS | Contenu | Effort | Dépend de | Démontrable |
| --- | --- | --- | --- | --- |
| **WS1 — Socle crypto** | Merge lib → `libs/security-commons-crypto` + pom parent ; `bcprov-jdk18on` ; `BouncyCastleSignatureProvider` (Ed25519+ML-DSA-65) + `KemProvider` (X25519+ML-KEM-768) ; `HybridSignatureService` + enveloppe versionnée ; `SigningKeyProvider` (adapter dev keystore) ; `docs/security/cbom.md` ; KAT NIST + ArchUnit. | **3-4 j** | — | `sign/verify` ML-DSA-65 réel, tests verts ≥85 %. |
| **WS2 — Signer les artefacts** | `SignatureService` dans api-quality-engine ; remplacer les placeholders pour : rapport d'audit, dossier de certification (Standards Hub), dashboard layout, certificats de formation. Endpoint/QR de vérification certificat. | **2-3 j** | WS1 | Un dossier de certif téléchargé porte une vraie signature hybride vérifiable. |
| **WS3 — Ancrage Phase A** | `SignedAnchorAdapter` (signe le Merkle root, reçu append-only chaîné, Flyway V53) ; scheduler `@Scheduled` par tenant ; `GET /blockchain/verify?hash=` (preuve d'inclusion Merkle + vérif signature) ; stub → `@Profile("test")`. | **3-4 j** | WS1 | `verify` retourne VERIFIED/TAMPERED/NOT_ANCHORED, prouvable end-to-end sans Fabric. |
| **WS4 — Fabric Phase B** | `infra/blockchain/` réseau Fabric 2.5 test (docker-compose) + chaincode Go (`AnchorAudit`/`VerifyEvidence`) ; `apps/blockchain-service` (Spring Boot + Fabric Gateway SDK, mTLS) ; `FabricBlockchainAnchorAdapter` + bascule feature-flag ; test Testcontainers. | **6-9 j** | WS3 | Ancrage réel sur Fabric, txId on-chain dans le reçu. |
| **WS5 — TLS hybride** | X25519+ML-KEM-768 sur le flux entrant (couche gateway/ingress, pas Java). | **2-3 j** | gateway déployée | Handshake PQ-hybride. **Différé.** |

**Tranche « cœur démontrable » = WS1 + WS2 + WS3 ≈ 8-11 j** → vraie crypto PQ +
ancrage vérifiable de bout en bout, **sans** dépendre de l'infra Fabric.
WS4 (Fabric) est le gros morceau d'infrastructure, à planifier ensuite.

## Hors périmètre (assumé)

- E-signature liée à l'identité **utilisateur** (au-delà de la clé plateforme) → lot ultérieur, réutilise `SignatureService`.
- Vault Transit pour les clés → port prêt, adapter prod différé (Vault non déployé).
- WS5 TLS hybride → dépend de la mise en place de la gateway (autre pilier).

## Definition of Done (rappel §20, applicable à chaque WS)

Tests ≥85 % · SAST/SCA verts · ADR à jour · pas de secret en clair · multi-tenant
filtré par JWT · doc Wiki technique. Les ADR 0011/0012 passent de **Proposé** à
**Accepté** à la validation.

## Question ouverte pour validation

1. **Périmètre immédiat** : on s'arrête à la tranche cœur (WS1-3) puis on rouvre la
   décision pour Fabric (WS4) ? — ou on enchaîne WS4 directement ?
2. **Provider crypto** : `bcprov-jdk18on` (rapide, non-FIPS) en dev avec bascule
   BC-FIPS prévue — OK ? (cf. ADR 0011 §2.)

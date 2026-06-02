# CBOM — Cryptographic Bill of Materials

> Cf. CLAUDE.md §11.1, §11.4, §21 (risque migration PQ) ; ADR 0011, ADR 0012.
> Module : `libs/security-commons-crypto`. Dernière mise à jour : 2026-05-28 (WS5).

Le CBOM recense les algorithmes cryptographiques utilisés, leur statut FIPS, et le
plan de migration post-quantique. La **crypto-agility** (suites nommées + SPI)
permet de changer un algorithme sans toucher aux consommateurs.

## Provider

| Provider | Version | Statut FIPS | Usage |
| --- | --- | --- | --- |
| `org.bouncycastle:bcprov-jdk18on` | 1.81 | ❌ **non FIPS-validé** | dev / on-prem non régulé |
| `org.bouncycastle:bctls-jdk18on` | 1.81 | ❌ non FIPS-validé | TLS hybride X25519+ML-KEM-768 (profil `tls` api-core, §11.4) ; prod régulée → `bctls-fips` |
| `org.bouncycastle:bc-fips` | (cible) | ✅ FIPS 140-3 | **prod régulée** — bascule prévue via le même SPI |
| JDK (SunEC) | Java 21 | — | Ed25519 (classique) |

> ⚠ **Tenants régulés** (santé, défense, finance) : la prod DOIT basculer sur
> `bc-fips`. Le SPI `SignatureProvider`/`KemProvider` rend ce changement transparent
> pour les appelants.

## Algorithmes

### Signatures

| Algorithme | Standard | Type | Taille sig. | Statut |
| --- | --- | --- | --- | --- |
| Ed25519 | RFC 8032 | classique | 64 o | actif (companion hybride) |
| ML-DSA-44 | FIPS 204 | post-quantique | ~2420 o | disponible |
| **ML-DSA-65** | FIPS 204 | post-quantique | ~3309 o | **actif (défaut PQ)** |
| ML-DSA-87 | FIPS 204 | post-quantique | ~4595 o | disponible (haute sécurité) |

### KEM (encapsulation de clé)

| Algorithme | Standard | Type | Statut |
| --- | --- | --- | --- |
| X25519 | RFC 7748 | classique | companion hybride TLS `X25519MLKEM768` (WS5, spécifié — ADR 0015) ; repli clients non-PQ |
| ML-KEM-512/768/1024 | FIPS 203 | post-quantique | **ML-KEM-768 actif** |

### Symétrique / hachage

| Algorithme | Usage |
| --- | --- |
| AES-256-GCM | chiffrement au repos des clés de signature (`EncryptedFileSigningKeyProvider`) |
| SHA-256 | chaîne d'intégrité des `AuditEvent` + arbre de Merkle (ancrage, ADR 0012) |

## Suites configurées (`CryptoSuiteConfig`)

| Suite | KEM | Signature | Contextes | PQ-ready |
| --- | --- | --- | --- | --- |
| `tls-hybrid-p3` | X25519 + ML-KEM-768 | Ed25519 + ML-DSA-65 | `tls`, `northbound-api` | ✅ |
| `audit-report` | ML-KEM-768 | Ed25519 + ML-DSA-65 | `audit-report`, `certificate` | ✅ |
| `blockchain-block` | ML-KEM-768 | Ed25519 + ML-DSA-65 | `blockchain-block` | ✅ |
| `legacy-classical` | X25519 | Ed25519 | `legacy-tls` | ❌ DEPRECATED |

## Stratégie hybride

Signatures critiques = **Ed25519 ‖ ML-DSA-65** (enveloppe `SignatureEnvelope`).
La vérification exige les **deux** parts valides : la garantie survit à la rupture
d'une seule primitive (IETF draft-ietf-tls-hybrid-design).

## Gestion des clés

- Clé de signature **plateforme** (attestation d'intégrité), pas par tenant.
- Dev/on-prem : `EncryptedFileSigningKeyProvider` (AES-256-GCM, clé via env — jamais
  de secret en clair, §18.2-3).
- Prod : Vault Transit (port `SigningKeyProvider` prêt, adapter différé).
- Rotation : modélisée par `keyRef` ; une signature épingle le `keyRef` de la clé
  qui l'a produite (vérification survit à la rotation).

## Plan de migration / veille

| Échéance | Action |
| --- | --- |
| Court terme | bcprov-jdk18on en dev — **marqué non-FIPS ici**. |
| Avant prod régulée | bascule `bc-fips`, revalidation des suites, mise à jour de ce CBOM. |
| WS5 (spécifié — ADR 0015) | TLS hybride `X25519MLKEM768` : Mode A ingress (Envoy/Istio+OQS ou nginx+oqs-provider) ou Mode B app-level (BC-JSSE). Config de réf. `infra/tls/`. Handshake à valider en CI/infra. |
| Continu | suivre les avis NIST/ANSSI ; déprécier `legacy-classical` dès que possible. |

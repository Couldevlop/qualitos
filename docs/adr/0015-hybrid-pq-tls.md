# ADR 0015 — TLS hybride post-quantique (X25519 + ML-KEM-768)

- **Statut** : Accepté (WS5) — **activé le 2026-05-29** comme profil Spring `tls`
  optionnel (OFF par défaut) sur api-core, prouvé par `HybridTlsHandshakeTest`
  (handshake TLS 1.3 hybride en loopback). Nécessite **Bouncy Castle 1.81** (1re
  version exposant le groupe nommé `X25519MLKEM768` / RFC 9794, 0x11EC, dans BCJSSE).
- **Date** : 2026-05-28
- **Owners** : @Couldevlop
- **Lié à** : [0011](./0011-pq-crypto-agility-signing.md) (crypto-agility + signatures),
  CBOM `docs/security/cbom.md`.

## Contexte

CLAUDE.md §11.4 impose un **TLS hybride (X25519 + ML-KEM-768)** sur les flux
entrants, avec **crypto-agility** et **CBOM** maintenu. État constaté :

- La suite **`tls-hybrid-p3`** (X25519 + ML-KEM-768 / Ed25519 + ML-DSA-65) est déjà
  modélisée dans `CryptoSuiteConfig` (contextes `tls`, `northbound-api`) — c'est la
  **source de vérité** des groupes à activer. Mais elle n'est **pas encore appliquée
  au transport TLS**.
- Le JDK 21 (SunJSSE) **ne supporte pas** l'échange de clés hybride ML-KEM nativement
  (groupe `X25519MLKEM768`). Il faut donc un terminateur compatible.
- L'architecture (§10.1) place la terminaison TLS à l'**ingress / API Gateway** + un
  **service mesh Istio** (mTLS interne). L'app applicative n'expose pas TLS en direct
  en déploiement standard.

## Décision

### 1. Terminaison hybride à l'ingress (déploiement standard)

Le groupe TLS 1.3 **`X25519MLKEM768`** (draft-kwiatkowski-tls-ecdhe-mlkem /
draft-ietf-tls-hybrid-design) est activé sur le **terminateur d'entrée** :

- **Envoy/Istio Gateway** compilé avec BoringSSL+OQS, **ou** un reverse-proxy
  **nginx + oqs-provider** (OpenSSL 3 + liboqs) devant la Gateway.
- Repli **`X25519`** classique pour les clients non-PQ (mappé sur la suite
  `legacy-classical`, marquée DEPRECATED au CBOM).
- Derrière l'ingress, le trafic est-ouest reste protégé par le **mTLS Istio**.

### 2. Alternative app-level (mono-service / on-prem sans ingress)

Pour un déploiement où l'app termine TLS elle-même : **provider JSSE Bouncy Castle**
(`bctls` en dev, **`bctls-fips` en prod**) + `jdk.tls.namedGroups=X25519MLKEM768`,
exposé par Spring Boot via `server.ssl`. Voir `infra/tls/` (config de référence).

### 3. Source de vérité = `CryptoSuiteConfig`

Les groupes activés dérivent de la suite résolue pour le contexte `tls`
(`tls-hybrid-p3`). Changer d'algorithme TLS = **changer la suite + la config
ingress**, jamais le code applicatif (crypto-agility, §11.4).

### 4. Migration FIPS (prod régulée)

Cohérent avec ADR 0011 : la prod régulée bascule `bcprov-jdk18on` → **`bc-fips`**
(FIPS 140-3) pour les signatures **et** le chemin TLS BC-JSSE (`bctls-fips`, mode
approved). Revalidation des suites + mise à jour du CBOM. Le SPI rend la bascule
transparente pour les consommateurs.

## Justification

- Terminer à l'ingress évite d'embarquer un provider TLS PQ dans chaque service et
  centralise la veille crypto (un seul point à mettre à jour).
- `X25519MLKEM768` est le groupe hybride en cours de standardisation IETF, déjà
  déployé par les navigateurs/CDN majeurs en 2025 — interopérable.
- L'hybride protège **dès aujourd'hui** contre « harvest-now, decrypt-later » sans
  sacrifier la compat (repli X25519).

## Conséquences

- ✅ Confidentialité de transport résistante au quantique sur les flux entrants.
- ✅ Crypto-agility : la suite `tls-hybrid-p3` pilote les groupes ; bascule FIPS via SPI.
- ⚠ Dépend d'un terminateur compatible (Envoy+OQS / nginx+oqs-provider / BC-JSSE) —
  **non fourni par le JDK 21 seul**.
- ⚠ Non vérifiable E2E dans l'environnement de dev actuel (pas d'ingress OQS, deps BC
  non téléchargeables ici) → config de référence livrée, handshake à valider en CI/infra.

## Tests d'invariant

- `CryptoSuiteConfig.resolveFor("tls")` renvoie une suite **PQ-ready** (`tls-hybrid-p3`).
- Handshake (env TLS) : `openssl s_client -groups X25519MLKEM768` réussit ; un client
  PQ négocie bien le groupe hybride ; un client legacy retombe sur X25519.
- CBOM à jour : statut WS5 + provider FIPS.

## Références

- CLAUDE.md §10.1, §11.4, §21 (risque migration PQ).
- IETF draft-ietf-tls-hybrid-design ; draft-kwiatkowski-tls-ecdhe-mlkem.
- NIST FIPS 203 (ML-KEM). oqs-provider (Open Quantum Safe).
- Fichiers : `libs/security-commons-crypto/` (suite `tls-hybrid-p3`), `infra/tls/`,
  `docs/security/cbom.md`.

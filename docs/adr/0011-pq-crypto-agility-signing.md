# ADR 0011 — Crypto-agility & signatures post-quantiques (ML-DSA hybride)

- **Statut** : Accepté (périmètre WS1-3 validé 2026-05-26)
- **Date** : 2026-05-26
- **Owners** : @Couldevlop

## Contexte

CLAUDE.md §2.1-5, §11.4 et §18.2-5 imposent que toute action critique (rapport
d'audit, dossier de certification, certificat de formation, bloc d'ancrage
blockchain) soit **signée ML-DSA** avec une stratégie **hybride** (Ed25519 +
ML-DSA-65) et une **crypto-agility** documentée (CBOM).

État actuel constaté :

- La lib `security-commons-crypto` existe (suites `tls-hybrid-p3`, `audit-pq`,
  `legacy-classical` ; enum `SignatureAlgorithm` ED25519 / ML-DSA-44/65/87 ;
  `CryptoSuiteConfig`) **mais uniquement dans un worktree non mergé**, absente
  du `pom.xml` parent (modules = `industry-commons`, `api-core`,
  `api-quality-engine`, `api-iot-hub`).
- Les ports `SignatureProvider.sign()/verify()` et `KemProvider` sont des **SPI
  vides** : aucune primitive réelle. Aucune dépendance Bouncy Castle dans les pom.
- En conséquence, tous les champs `signatureHash` / `signature` (DashboardLayout,
  MarketplacePack, DocumentVersion, TrainingEnrollment, CertificationDossier,
  AuditEvent) sont des **chaînes placeholder**, sans valeur cryptographique.

## Décision

### 1. Promotion de la lib crypto

`security-commons-crypto` est promu du worktree vers `libs/security-commons-crypto`
et ajouté aux `<modules>` du pom parent. Le découpage hexagonal existant est
conservé (`domain/model`, `domain/port`, `application`).

### 2. Primitives réelles via Bouncy Castle (provider derrière le SPI)

- Dépendance `org.bouncycastle:bcprov-jdk18on` (≥ 1.78, qui livre ML-DSA / FIPS 204
  et ML-KEM / FIPS 203) ajoutée en `dependencyManagement` du pom parent.
- Adapters en `infrastructure/` :
  - `BouncyCastleSignatureProvider` — Ed25519 et ML-DSA-65.
  - `BouncyCastleKemProvider` — X25519 et ML-KEM-768.
- **Crypto-agility** : la production pourra basculer sur `bc-fips` 2.x (provider
  FIPS-validé) en remplaçant l'adapter, **sans toucher aux consommateurs** — c'est
  l'objet même du SPI.

### 3. Signature hybride par défaut

Un `HybridSignatureService` (couche `application`) résout la `CryptoSuite` par
contexte (`audit-report`, `blockchain-block`, `certificate`) et produit une
**enveloppe de signature versionnée** :

```
SignatureEnvelope v1 = {
  suite, algos: [Ed25519, ML-DSA-65],
  publicKeys: [...], signatures: [sig_classical, sig_pq],
  signedAt, keyRef
}  → encodée CBOR puis Base64URL.
```

La vérification exige que **les deux** signatures soient valides (défense en
profondeur : la garantie survit à la rupture d'une courbe). Conformément à §11.4,
le mode hybride prime ; la suite `audit-pq` pure-PQ reste disponible par
configuration.

### 4. Gestion des clés

- Port `SigningKeyProvider` (couche `domain/port`).
- Adapter **dev** : keystore PKCS#12 chiffré, clé déverrouillée par variable
  d'environnement (jamais de secret en clair — §18.2-3).
- Adapter **prod** : HashiCorp Vault Transit — **port prêt, implémentation différée**
  (Vault non déployé aujourd'hui).
- Périmètre v1 : **clé de signature plateforme** (attestation d'intégrité). La
  e-signature liée à l'identité utilisateur (DocumentVersion, certificats) est un
  lot ultérieur qui réutilisera le même service.

### 5. CBOM

`docs/security/cbom.md` (Crypto Bill of Materials) est créé/maintenu : algos,
suites, statut FIPS (`bcprov` non-validé en dev → à marquer explicitement),
contextes d'usage, plan de migration.

## Justification

- Bouncy Castle pur d'abord car le provider FIPS-validé impose licence + cycle de
  validation : on débloque les signatures réelles tout de suite, et le SPI garantit
  le passage à BC-FIPS plus tard (crypto-agility — mitige le risque §21 « migration
  crypto post-quantique »).
- Hybride Ed25519+ML-DSA-65 = recommandation de migration PQ (IETF
  draft-ietf-tls-hybrid-design), déjà modélisée dans `CryptoSuite`.

## Conséquences

- ✅ ML-DSA-65 réel, vérifiable, sur les artefacts critiques (débloque §18.2-5).
- ✅ Crypto-agility effective : changer d'algo = changer une suite, pas le code.
- ⚠ `bcprov-jdk18on` n'est pas FIPS-validé → le CBOM doit marquer « non-FIPS (dev) »
  et la prod régulée devra basculer sur `bc-fips`.
- ⚠ Signatures ML-DSA-65 ≈ 3,3 Ko + Ed25519 64 o : impact taille sur les enveloppes
  (acceptable, hors chemin chaud).

## Tests d'invariant

- **KAT** (Known Answer Tests) NIST FIPS 204 pour ML-DSA-65.
- Round-trip `sign → verify` (succès) et altération message/signature (échec).
- Test : une signature hybride dont **une** des deux parts est invalide → rejet.
- **ArchUnit** : `domain/` ne dépend d'aucune classe `org.bouncycastle`.
- JaCoCo ≥ 85 % lignes / 75 % branches sur le module.

## Références

- CLAUDE.md §2.1-5, §11.1, §11.4, §18.2-3/5, §21.
- NIST FIPS 203 (ML-KEM), FIPS 204 (ML-DSA).
- Fichiers cibles : `libs/security-commons-crypto/`, `docs/security/cbom.md`.
- Lié à [0012](./0012-blockchain-anchoring-fabric.md) (consomme le service de signature).

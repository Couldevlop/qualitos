# QualitOS — Securite - security by design

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

---

## 11. Sécurité — _security by design_

### 11.1 OWASP ASVS Level 3

| Catégorie OWASP               | Mise en œuvre QualitOS                                                                             |
| ----------------------------- | -------------------------------------------------------------------------------------------------- |
| A01 Broken Access Control     | RBAC + ABAC (Spring Security + OPA/Rego), test d'autorisation systématique, RLS PostgreSQL         |
| A02 Cryptographic Failures    | TLS 1.3 obligatoire, AES-256-GCM au repos, Vault Transit, rotation 90j                             |
| A03 Injection                 | Requêtes paramétrées (JPA/Criteria), validation Jakarta + OpenAPI, échappement Angular             |
| A04 Insecure Design           | Threat modeling STRIDE par module, ADR sécurité, abuse cases dans les tests                        |
| A05 Security Misconfiguration | Distroless, CIS Kubernetes Benchmark, CSP stricte, OWASP Secure Headers                            |
| A06 Vulnerable Components     | OWASP DC + Snyk + Trivy à chaque build, SBOM signé, **0 CVE Critique/Haute** en prod               |
| A07 Auth Failures             | Keycloak + MFA obligatoire admin, WebAuthn, blocage brute-force, sessions 15 min + refresh rotatif |
| A08 Software & Data Integrity | Signatures Cosign, SLSA Level 3, blockchain pour intégrité applicative                             |
| A09 Logging & Monitoring      | Logs structurés JSON, OpenTelemetry, SIEM (Wazuh/Elastic), alertes SOC                             |
| A10 SSRF                      | Allow-list destinations sortantes, proxy egress, validation stricte URLs                           |

### 11.2 OWASP **LLM Top 10** (spécifique IA)

- **LLM01 Prompt Injection** : sanitization, séparation system/user, bouclier sémantique (Rebuff).
- **LLM02 Insecure Output** : validation et échappement avant affichage Angular.
- **LLM03 Training Data Poisoning** : provenance datasets, signatures, MLflow + DVC.
- **LLM04 Model DoS** : quotas par tenant, rate-limit, timeouts, circuit breaker.
- **LLM06 Sensitive Info Disclosure** : filtres PII (Microsoft Presidio), redaction auto.
- **LLM07 Insecure Plugin Design** : tool-use sandboxé, allow-list stricte, audit appels.

### 11.3 Blockchain : ancrage et preuves

- **Hyperledger Fabric** (consortium permissionné).
- **Smart contracts (chaincode Go)** :
  - `AnchorAudit(hash, tenant_id, timestamp, type)`
  - `VerifyEvidence(hash)`
- **RGPD-compatible** : uniquement des **hashes** sur la chaîne, jamais de données personnelles.
- Cas d'usage : signature d'un rapport d'audit, traçabilité d'une NC, preuve d'horodatage d'une décision qualité, certificats de formation vérifiables.

### 11.4 Cryptographie post-quantique

- **TLS hybride** (X25519 + ML-KEM-768) sur les flux entrants.
- **Signatures hybrides** (Ed25519 + ML-DSA-65) pour rapports critiques et blocs blockchain.
- **Bouncy Castle FIPS Java + liboqs Python**.
- **Crypto-agility** : algorithmes configurables, **CBOM** maintenu.

### 11.5 Audit & traçabilité

- **Tout** est journalisé : connexion, lecture, modification, export, action IA, transition de cycle.
- Logs **immuables** (append-only) en PostgreSQL + ancrage périodique en blockchain.
- Recherche full-text (OpenSearch).
- Rapports d'audit générés en un clic, signés ML-DSA, ancrés blockchain.

---


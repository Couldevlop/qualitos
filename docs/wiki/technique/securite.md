# Sécurité

[← Retour à l'espace technique](README.md)

Cette page ne redit pas `CLAUDE.md` §11. Elle dit **où** chaque protection est écrite,
et **ce qui est réellement actif** — la distinction importe : une protection livrée mais
désactivée par défaut protège autant qu'une absence tant que personne ne l'allume.

---

## 1. Identité, client, autorisation

| Barrière | Fichier qui fait foi |
| --- | --- |
| Validation du jeton OIDC par la clé publique du realm | `application.yml` de chaque service, `jwk-set-uri` |
| Client lu du jeton, jamais du corps | `TenantJwtFilter`, `TenantContext` — ADR 0001, §18.2 point 2 |
| Autorisation décidée avant la lecture du corps | ADR 0065 |
| Matrice de rôles de `CLAUDE.md` §16 | ADR 0020 |
| Droits réglés **par client, action par action** | ADR 0078, écran `/admin/roles` |
| Portée de lecture : chacun voit ce qui le concerne, ou tout le registre | ADR 0081 |
| Second facteur exigé sur un acte sensible | `StepUpGuard`, `StepUpAuthentication`, `StepUpProperties` (paliers `acr` de Keycloak) |
| Comptes créés dans le client du jeton, jamais ailleurs | ADR 0079, 0093 |
| Troisième surface pour un consultant de cabinet, sans client | ADR 0106 — tenu par une coquille d'autorisation séparée, pas par un menu masqué |

Une conséquence pratique, apprise en préproduction : **toute nouvelle surface exposée par
`api-core` doit être routée explicitement dans l'ingress**. Une surface non routée répond
404 alors qu'elle fonctionne en local, et un banc de test vérifie désormais la règle.

---

## 2. Cryptographie

| Capacité | État | Preuve |
| --- | --- | --- |
| Signature ML-DSA-65 | **réelle** | `BouncyCastleSignatureProvider`, Bouncy Castle 1.86 |
| Encapsulation de clé ML-KEM-768 | **réelle** | `BouncyCastleKemProvider`, aller-retour testé |
| Signature hybride Ed25519 + ML-DSA | **réelle** | `HybridSignatureService` : deux signatures calculées, les deux vérifiées |
| Ancrage de preuves phase A (arbre de Merkle, reçus chaînés) | **réel** | `SignedAnchorAdapter`, `MerkleTree`, `AnchorVerificationService` |
| TLS hybride X25519 + ML-KEM-768 | **réel mais inactif** | profil Spring `tls` (ADR 0015), désactivé par défaut |
| Ancrage phase B sur Hyperledger Fabric | **non opérationnel** | profil `fabric` désactivé, `blockchain-service` `enabled: false` dans le chart, réseau Fabric absent (ADR 0012) |

Le détail des algorithmes et de leur agilité est tenu dans
[`docs/security/cbom.md`](../../security/cbom.md). La règle qui les gouverne :
aucune donnée personnelle sur la chaîne, jamais — seulement des empreintes (§11.3).

---

## 3. Modèles de menace

Quatre modèles STRIDE sont écrits, un par surface d'attaque qui n'est pas une API métier
ordinaire :

| Modèle | Surface |
| --- | --- |
| [`threat-model-iot.md`](../../security/threat-model-iot.md) | Ingestion depuis le terrain, passerelle Edge, jumeau d'équipement |
| [`threat-model-nlq.md`](../../security/threat-model-nlq.md) | Requête en langage naturel traduite en SQL |
| [`threat-model-ai-service.md`](../../security/threat-model-ai-service.md) | Passerelle de modèles, RAG, garde-fous |
| [`threat-model-connectors.md`](../../security/threat-model-connectors.md) | Connecteurs vers les systèmes tiers |

Les relevés OWASP par phase sont dans `docs/security/owasp-checklist-p3.md`, `-p4.md`
et `-p5.md`. **Attention en les lisant** : celui de P3 renvoie à des ADR 0004 et 0005
qui n'ont jamais été écrites (voir la note de [l'index des ADR](../../adr/README.md)).

---

## 4. Le chemin des modèles de langage

C'est la surface la plus récente et la plus exposée. Quatre protections, toutes dans le
code :

1. **Passerelle unique.** Aucun appel de modèle ne part d'ailleurs que de l'ai-service
   (§18.2 point 4). Le moteur l'appelle avec un jeton de service (ADR 0014, 0021, 0048).
2. **Garde-fous de service.** Débit par minute, quota journalier, disjoncteur, borne de
   taille de requête, cloisonnés par client, appliqués **avant** le départ réseau ;
   rejets en 429, 413, 503 (ADR 0017, OWASP LLM04).
3. **Fournisseur imposé par le client.** La politique du client est relue à chaque appel,
   sans repli : si elle est illisible, l'appel échoue — la souveraineté ne se dégrade
   jamais en silence (ADR 0102).
4. **Sûreté de la requête en langage naturel.** Filtre d'injection, rédaction des données
   personnelles à l'entrée **et** à la sortie, invite contrainte, validateur `sqlglot`
   obligatoire (un seul ordre, `SELECT` seul, tables en liste blanche, `tenant_id`
   imposé), exécution par un rôle de base sans droit d'écriture (ADR 0009).

Et la règle qui les surplombe : **l'IA propose, l'humain décide.** Aucun document
normatif n'est publié, aucune clôture n'est prononcée, aucune action n'est prise sur une
prévision sans qu'une personne l'ait demandée et signée (ADR 0032, 0104, 0110).

---

## 5. Ce que la chaîne de construction vérifie

Sept vérifications bloquantes sur chaque révision — leur détail est dans
[Exploitation et CI/CD](exploitation-et-ci.md) :

- secrets dans l'historique complet (gitleaks) ;
- analyse statique (Semgrep, les constats de niveau `ERROR` bloquent) ;
- composition des dépendances (Trivy sur l'arborescence, OWASP Dependency-Check) ;
- analyse des images produites (Trivy) ;
- analyse dynamique de l'application en marche (OWASP ZAP) ;
- couverture de tests, seuil 85 % de lignes et 75 % de branches ;
- rendu du chart et parité du mode léger, pour qu'une règle d'ingress ou une politique
  réseau manquante se voie avant le déploiement.

La règle non négociable : **aucune dépendance portant une vulnérabilité critique ou
haute ne franchit la chaîne** (§18.2 point 1). Quand une exception est inévitable, elle
est écrite avec une échéance, et l'échéance est tenue.

---

## 6. Hygiène des secrets

Aucun secret en clair dans le dépôt (§18.2 point 3). `.gitignore` exclut les `*.env`,
`*.pem`, `*.key`, `*.p12`, `*.jks`, les `application-local.yml` et
`application-prod.yml`. Le chart n'embarque aucun identifiant : il référence des
`Secret` Kubernetes par nom, matérialisés depuis le coffre. La procédure, y compris la
rotation et ce qu'il faut faire quand un secret a fuité, est dans
[`docs/runbooks/secrets-hygiene.md`](../../runbooks/secrets-hygiene.md).

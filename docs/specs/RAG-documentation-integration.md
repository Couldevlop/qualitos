# Étude — Intégrer la documentation au RAG de QualitOS

- **Statut** : étude, pour décision. Aucune décision n'est appliquée tant qu'elle n'est pas gravée dans un ADR.
- **Date** : 2026-11-11
- **Portée** : `apps/ai-service` (RAG), `apps/api-quality-engine` (`aigateway`, `docs`, `standards`), `apps/web`, `infra/k8s`
- **S'inscrit dans** : CLAUDE.md §8.7 (moteur d'alignement), §12.2 (architecture IA), §12.3 (explicabilité), §18.2 n° 2 (client lu dans le jeton)
- **S'appuie sur** : ADR 0014, 0017, 0046, 0047, 0048, 0049, 0086, 0094, 0099, 0102, 0109

**Objectif produit.** Donner à l'assistant l'accès à la *documentation* — le wiki
technique et utilisateur de la plateforme, les procédures du client dans la GED, et
ce que le Standards Hub détient légalement des normes — pour qu'il réponde en citant
des sources, et non de mémoire.

**Résumé exécutable.** Le chemin de *requête* du RAG est écrit et complet. Le chemin
d'*alimentation* existe depuis l'ADR 0046 mais **n'a aucun appelant dans la
plateforme** : ni l'engine, ni le SPA n'appellent `/v1/ai/rag/*`. Le corpus est donc
vide en production, et personne ne peut le remplir par l'application. Trois défauts
bloquants s'y ajoutent (identifiant de point Qdrant invalide, fragments périmés
jamais retirés, aucun filtre de confidentialité intra-client). Le travail à faire
n'est pas « construire un RAG » : c'est **brancher, corriger et élargir celui qui
existe**, en sept étapes dont les trois premières sont des corrections.

---

## 1. État des lieux — ce qui est réellement là

### 1.1 Chemin de requête : complet

`apps/ai-service/application/usecase/rag_query.py` enchaîne, dans cet ordre :

1. bouclier anti-injection sur la question (`PromptInjectionFilter`, rejet → 422) ;
2. caviardage des données personnelles de la question (`PiiFilter`) ;
3. embedding de la question ;
4. recherche **bornée au client** (`tenant_id` du `UserContext`, jamais du corps) ;
5. si zéro résultat : réponse explicite « No relevant documents found », confiance 0,1 —
   pas de complétion à vide ;
6. complétion LLM sur un contexte tronqué à 1 000 caractères par document, invite
   système imposant la citation `[doc_id]` ;
7. caviardage de la **sortie** ;
8. journal de prompt caviardé (`PromptAuditLogger`) ;
9. confiance = moyenne des scores de similarité, méthode déclarée `heuristic`.

Exposé par `POST /v1/ai/rag/query` (`presentation/routers/rag_router.py`).

### 1.2 Chemin d'alimentation : écrit, jamais appelé

`application/usecase/rag_ingest.py` (ADR 0046) :

- refuse toute source dont la licence n'est pas dans `ALLOWED_LICENCES`
  (`tenant-owned`, `cc-by-4.0`, `etalab-2.0`, `public-domain`) ;
- refuse une source sans `origin` — « un fragment qu'on ne peut pas remonter n'est pas
  indexé » ;
- découpe par paragraphes, en paquets de `CHUNK_CHARS = 1200`, recouvrement
  `CHUNK_OVERLAP = 150` pour les paragraphes plus longs qu'un fragment ;
- **caviarde avant stockage**, pas à la sortie : ce qui n'est jamais écrit ne peut pas
  fuir ;
- compte les documents entièrement caviardés (`skipped_empty`) plutôt que de les faire
  passer pour indexés ;
- `forget_tenant()` supprime la collection (RGPD art. 17).

Exposé par `POST /v1/ai/rag/ingest`.

### 1.3 Socle technique : réel et déployé

| Brique | État | Référence |
| --- | --- | --- |
| Embeddings `bge-m3`, 1024 dimensions, servis par Ollama | Réel, **sans repli silencieux** (503 si indisponible) | ADR 0049, `infrastructure/vector/ollama_embedder.py` |
| `EMBEDDINGS_PROVIDER` explicite (`ollama` \| `local` \| `deterministic`) | Réel ; refus de démarrer sans configuration exploitable | ADR 0049 |
| Qdrant v1.11.0 | Déployé : `docker-compose.dev.yml`, `infra/k8s/deps/30-qdrant.yaml` | — |
| `QDRANT_URL` + `EMBEDDINGS_PROVIDER=ollama` | Positionnés en préprod, prod et on-premise | `infra/k8s/qualitos/values-{preprod,prod,onprem}.yaml` |
| Propagation du client engine → ai-service | Réel : jeton de service + `X-Tenant-Id` cru d'un `azp` de confiance seulement | ADR 0048 |
| Fournisseur de modèle épinglé par client, échec fermé | Réel | ADR 0102 |
| Quotas, débit, disjoncteur par client (LLM04) | Réel, côté engine | ADR 0017 |
| Journal chaîné des actions | Réel, par client et au niveau plateforme | §11.5, ADR 0109 |

Le socle n'est donc pas à construire. C'est la **chaîne documentaire** qui manque.

---

## 2. Ce qui manque — constats vérifiés

### 2.1 Bloquant — le RAG n'est branché nulle part

`grep` sur `/v1/ai/rag`, `RagQuery`, `rag/query` dans `apps/` et `libs/` (Java **et**
TypeScript) : **zéro occurrence**. `AiGatewayClient` expose treize points de la
passerelle (`complete`, `nlq/ask`, `spc/analyze`, `spc/forecast`, `anomaly/detect`,
`anomaly/explain`, `predict/kpi`, `predict/nc-clusters`, `predict/supplier-risk`,
`complaints/analyze`, `standards/mock-audit`, `transcribe`, `providers`) — **aucun
point RAG**.

Conséquence : l'ai-service peut répondre, mais rien dans la plateforme ne l'interroge
et rien ne remplit son index. L'écran « Assistant IA » documenté
(`docs/wiki/modules/assistant-ia.md`) est le **NLQ** (texte→SQL), pas le RAG : il lit
les *tables*, pas les *documents*. Ce sont deux moteurs distincts, et le second n'a
pas de porte d'entrée.

### 2.2 Bloquant — l'identifiant de point Qdrant est invalide

`QdrantVectorStore.upsert` construit `PointStruct(id=d.document_id)` où
`document_id = f"{source_id}#{index}"`. Qdrant n'accepte comme identifiant de point
qu'un **entier non signé 64 bits ou un UUID** ; une chaîne arbitraire est rejetée.
L'adaptateur Qdrant n'a donc **jamais été exercé contre un vrai Qdrant** — les tests
passent sur l'adaptateur en mémoire, qui n'a pas cette contrainte. Correction :
`uuid5(namespace, f"{tenant}:{source_id}#{index}")`, déterministe, qui conserve la
propriété de ré-indexation idempotente, et le `document_id` lisible reste dans le
payload pour la citation.

### 2.3 Bloquant — les fragments périmés ne sont jamais retirés

L'identifiant d'un fragment dépend de son rang. Si une procédure passe de dix
fragments à six, les fragments 6 à 9 de l'ancienne version **restent indexés** et
restent citables. Sur un corpus qualité, c'est la panne la plus grave possible :
l'assistant cite, avec son format d'autorité `[doc_id]`, un texte **retiré**. Il faut
une suppression par filtre `source_id` **avant** chaque ré-ingestion, dans la même
unité de travail.

### 2.4 Majeur — aucun filtre de confidentialité à l'intérieur d'un client

Le cloisonnement entre clients est correct (collection par client, `tenant_id` lu du
jeton). Mais à l'intérieur d'un client, **tous les fragments sont visibles de tous les
porteurs d'un jeton**. Or la plateforme applique déjà deux règles plus fines :
l'ADR 0081 (« chacun voit ce qui le concerne, ou tout ») et la RLS (ADR 0086). Un
document GED en brouillon, une version rejetée, un rapport d'audit restreint
deviendraient accessibles par le RAG à quelqu'un qui n'y a pas droit dans l'écran.
**Le RAG ne doit pas être un chemin détourné vers des données que l'écran refuse** —
c'est exactement la portée de l'ADR 0081, et c'est le même oubli que celui déjà
constaté sur un écran de synthèse.

### 2.5 Majeur — la GED ne fournit pas de texte

L'ADR 0094 stocke le fichier déposé comme un objet binaire (dédup SHA-256, gzip
sélectif), servi par lien présigné : **le serveur ne relit jamais le fichier**.
`DocumentVersion` porte bien une colonne `content` (texte saisi dans l'application) à
côté du `blob` (fichier joint). Seul `content` est indexable aujourd'hui. Un PDF ou un
`.docx` déposé en pièce jointe — c'est-à-dire la majorité d'un référentiel documentaire
réel — n'a **aucune représentation textuelle** dans la plateforme.

### 2.6 Majeur — pas de fraîcheur d'index

Aucun écouteur sur la publication d'une version, aucun événement, aucun ordonnanceur.
Un index alimenté une fois dérive dès la publication suivante.

### 2.7 Majeur — la documentation de la plateforme n'est pas un corpus

`docs/wiki/` contient 73 fichiers Markdown (modules, rôles, secteurs, technique). Rien
ne les indexe, et surtout : **aucune licence de `ALLOWED_LICENCES` ne les couvre**. Ce
ne sont ni des données du client (`tenant-owned`), ni un texte public. Il manque une
classe `platform-owned` et un corpus **partagé** — le même pour tous les clients.

### 2.8 Moyen — récupération purement dense, sans reclassement

- Pas de recherche lexicale : une question contenant un code de document (`PR-QUA-012`),
  un numéro de lot ou une référence de clause (`§8.5.1`) est mal servie par un vecteur
  dense seul.
- Pas de reclassement : les 5 premiers résultats cosinus partent tels quels au modèle.
- `min_score = 0.5` est un seuil **fixe** sur une similarité cosinus. Pour `bge-m3`,
  cette valeur n'a pas de signification universelle : trop haut, le corpus paraît vide ;
  trop bas, on injecte du bruit dans le contexte. C'est un paramètre à étalonner, pas à
  poser.

### 2.9 Moyen — citation pauvre et non vérifiable

La réponse de l'API rend `document_id`, `score` et un extrait de 200 caractères. Il
manque le titre, la version, la date de publication et le lien vers la fiche. §12.3
exige des citations de sources ; une citation qu'un auditeur ne peut pas ouvrir n'en
est pas une.

### 2.10 Moyen — aucune mesure de pertinence

L'ADR 0049 le dit lui-même : les tests tournent en `EMBEDDINGS_PROVIDER=deterministic`,
donc « la pertinence sémantique du RAG ne s'évalue pas en test unitaire, et ne doit pas
y être affirmée ». Il n'existe à ce jour **aucun jeu d'évaluation**. On ne peut donc ni
prouver une amélioration, ni détecter une régression.

### 2.11 À arbitrer — collection par client ou partition par payload

L'adaptateur crée une collection par client (`qos_tenant_<uuid>`). La documentation de
Qdrant recommande, pour un grand nombre de clients, **une collection unique avec
partition par payload** (champ `group_id`, index `keyword` avec `is_tenant: true`,
Qdrant ≥ 1.11), et réserve le *sharding* dédié aux gros clients ; la version 1.16
introduit un mode mixte. Le modèle actuel est le plus sûr par construction (aucun filtre
à oublier) mais multiplie les collections et le coût fixe d'un index HNSW par client.

---

## 3. Architecture cible

### 3.1 Trois corpus, trois régimes

| Corpus | Contenu | Licence | Portée | Fraîcheur |
| --- | --- | --- | --- | --- |
| **Client** | `document_versions.content` + texte extrait des pièces jointes, au statut publié ; enregistrements qualité (NC, CAPA, audits, revues, comptes rendus) | `tenant-owned` | Collection du client | À la publication d'une version |
| **Plateforme** | `docs/wiki/` (73 fichiers), aide en ligne par module et par rôle | `platform-owned` (nouvelle classe) | Collection **partagée**, lue par tous | À chaque livraison (construite dans la CI) |
| **Public** | EUR-Lex (CC BY 4.0), Légifrance/PISTE (Etalab 2.0), FDA (domaine public) | déjà autorisées | Collection partagée, par source | Par connecteur, cadencé |

**Les normes ISO/IATF/AS9100 restent hors corpus** (ADR 0046) : ni texte, ni
reformulation. Le Standards Hub conserve la *structure* — codes et intitulés de
clauses, liens vers les preuves — qui n'est pas un corpus vectoriel mais une
**récupération structurée**, par requête SQL sur `standard_requirements` et
`requirement_evidence`. Une question du type « suis-je couvert sur §8.5 ? » se répond en
mêlant les deux : clauses et preuves par le SQL, texte des procédures du client par le
vecteur.

### 3.2 Ce qu'une réponse mobilise

```
Question utilisateur
        │
        ├─► bouclier anti-injection ──► caviardage PII            (existant)
        │
        ├─► Récupération structurée : clauses + preuves du client  (à écrire, SQL)
        │
        └─► Récupération documentaire
               ├── dense  : bge-m3 → Qdrant (cosinus)             (existant)
               ├── lexical: BM25 sparse dans Qdrant               (à ajouter)
               ├── fusion : RRF (rangs, pas de normalisation)     (à ajouter)
               └── reclassement : bge-reranker-v2-m3 (Apache-2.0) (à ajouter, optionnel)
                        │
                        ▼
          Filtres obligatoires sur chaque requête :
            tenant_id (jeton) · corpus · statut=publié · visibilité ⊆ droits du lecteur
                        │
                        ▼
          Contexte borné → LLM du fournisseur épinglé au client (ADR 0102)
                        │
                        ▼
          Réponse + citations ouvrables + confiance + journal
```

### 3.3 Métadonnées d'un fragment (contrat d'indexation)

Au-delà de ce que l'ADR 0046 impose déjà (`origin`, `licence`, `version`, `url`,
`title`, `fragment`, `fragments_total`), tout fragment porte :

| Champ | Rôle | Obligatoire |
| --- | --- | --- |
| `tenant_id` | Cloisonnement — filtre dur sur chaque requête | oui (sauf corpus partagés) |
| `corpus` | `client` \| `platform` \| `public` | oui |
| `source_id` | Clé de suppression avant ré-indexation | oui |
| `source_type` | `document` \| `wiki` \| `nc` \| `capa` \| `audit` \| `regulation` | oui |
| `entity_id` | Clé métier pour construire le lien d'ouverture | oui pour `client` |
| `version_label`, `published_at` | Fraîcheur, affichée dans la citation | oui |
| `visibility` | Étiquette de confidentialité confrontée aux droits du lecteur | oui pour `client` |
| `language` | Filtrage et cohérence de réponse (6 langues, §15.1) | oui |
| `clause_refs` | Codes de clause couverts, quand connus | non |

### 3.4 Invariants non négociables

1. `tenant_id` vient **toujours** du jeton validé, jamais du corps (§18.2 n° 2).
   Déjà respecté côté ingestion et requête ; à maintenir sur toute nouvelle route.
2. **Aucune requête sans filtre de cloisonnement.** Si le modèle passe à la collection
   unique partitionnée par payload, le filtre `must` devient la seule barrière : il doit
   alors être posé dans l'adaptateur, pas dans l'appelant, et couvert par un test qui
   échoue si on l'enlève.
3. **Aucun fragment sans provenance** (ADR 0046) ni sans étiquette de visibilité.
4. **Le caviardage PII reste à l'ingestion**, pas seulement à la sortie.
5. **Aucune réponse sans citation ouvrable.** Un contexte non vide qui produit une
   réponse sans citation est une anomalie à journaliser, pas à afficher.
6. **Échec franc** : service d'embeddings indisponible → 503, jamais de repli
   déterministe (ADR 0049), jamais de bascule de fournisseur (ADR 0102).
7. **L'oubli est complet** : supprimer un client supprime sa collection ; purger une
   version retire ses fragments.

---

## 4. Extraction de texte des pièces jointes

C'est le seul chantier véritablement nouveau. Contraintes : l'ADR 0094 interdit de
faire transiter les fichiers par l'API, et l'image de l'ai-service suit l'ADR 0047
(base Chainguard, surface minimale).

Proposition :

- l'extraction vit **dans l'ai-service**, qui est déjà le seul service Python et le seul
  à parler à Qdrant et à Ollama ;
- elle lit l'objet par **lien présigné** (même mécanisme que l'aperçu du navigateur) :
  l'engine fournit le lien, pas les octets ;
- formats de la première version : PDF (texte natif), `.docx`, `.txt`, `.md`, `.csv`.
  **Pas d'OCR** en V1 : un PDF scanné est signalé « non indexable, aperçu seulement »
  plutôt que traité en silence — même principe que le refus du repli déterministe ;
- le texte extrait **n'est pas stocké** ailleurs que sous forme de fragments caviardés :
  une seconde copie en clair du référentiel documentaire serait une surface de fuite
  nouvelle, et une donnée à purger de plus ;
- l'empreinte SHA-256 du blob (déjà calculée, ADR 0094) sert de clé de cache : deux
  versions partageant le même fichier ne sont extraites qu'une fois — la déduplication
  de la GED se propage gratuitement à l'index.

Toute dépendance d'extraction ajoutée est soumise à la règle §18.2 n° 1 (aucune CVE
critique ou haute) et doit être justifiée dans l'ADR correspondant.

---

## 5. Fraîcheur de l'index

Déclencheur : **la publication d'une version** (`VersionStatus` → publiée), pas le dépôt
du fichier ni l'enregistrement du brouillon. C'est la même frontière que celle que
l'ADR 0099 pose pour les signataires : ce qui n'est pas approuvé n'est pas opposable,
donc n'est pas citable.

Mécanisme recommandé : **réutiliser le relais `outbox` existant** (`audit_events`
utilisé comme boîte d'envoi, socle Kafka) plutôt que d'ajouter une file. Une ligne
d'événement `document.version.published` est déjà produite par le journal chaîné ; un
consommateur la transforme en tâche d'indexation.

Règles :

| Événement | Action sur l'index |
| --- | --- |
| Version publiée | Supprimer tous les fragments `source_id` du document, puis ré-indexer |
| Version rendue obsolète | Supprimer ses fragments ; la version précédente reste citable si elle est en vigueur |
| Fichier purgé (rétention, ADR 0094 §5) | Supprimer les fragments ; l'empreinte et l'ancrage restent |
| Document archivé | Supprimer les fragments ou basculer `visibility=archive` selon le réglage du client |
| Client supprimé | `forget_tenant()` |
| Livraison de la plateforme | Reconstruire la collection `platform` depuis `docs/wiki/`, dans la CI |

Un **recollement périodique** (nocturne) compare le nombre de fragments par `source_id`
à l'état attendu et répare les écarts : une file perd des messages, une base de vecteurs
non réconciliée ment.

---

## 6. Explicabilité et opposabilité

§12.3 : « l'IA suggère, l'humain décide ». Appliqué au RAG :

- **Citation ouvrable** : titre, version, date de publication, lien vers la fiche du
  document ou la page de wiki. Un auditeur doit pouvoir cliquer.
- **Le passage cité est affiché**, pas seulement référencé : l'extrait de 200 caractères
  actuel ne suffit pas à vérifier.
- **Confiance honnête** : la moyenne des scores de similarité mesure la *proximité du
  corpus*, pas la *justesse de la réponse*. Elle doit être étiquetée comme telle dans
  l'interface — la méthode est déjà rendue par l'API (`confidence_method`), l'écran doit
  l'afficher.
- **Réponse sans citation = anomalie** : si le modèle répond sans `[doc_id]` alors que le
  contexte n'était pas vide, la réponse est marquée « non étayée » et journalisée. C'est
  le même garde-fou que le bloc « chiffres absents des données source » du storyboard,
  qui est aujourd'hui le meilleur dispositif anti-hallucination de la plateforme.
- **Le corpus vide se dit** : « aucun document pertinent » est déjà la réponse correcte.
  L'écran doit en plus indiquer *pourquoi* (corpus non alimenté, droits insuffisants,
  filtre trop restrictif) — trois causes qui demandent trois actions différentes.
- **Journal** : chaque interrogation inscrit question caviardée, fragments retournés,
  fournisseur, latence et jetons (déjà fait) ; s'y ajoute la **liste des `source_id`
  cités**, pour qu'on puisse reconstituer sur quoi une décision s'est appuyée.
- **Les normes sous droit d'auteur** : quand la question porte sur un texte ISO,
  l'interface dit explicitement que le texte n'est pas restituable et renvoie aux
  preuves rattachées à la clause (ADR 0046). C'est une limite à **afficher**, pas à
  contourner.

---

## 7. Coût et quotas

Trois postes, à mesurer et non à estimer :

1. **Embeddings.** Un calcul par fragment à l'ingestion, un par question. `bge-m3`
   compte environ 568 millions de paramètres et accepte des entrées jusqu'à
   8 192 jetons ; nos fragments de 1 200 caractères restent très en deçà, donc aucune
   troncature. Attention : la fenêtre effective servie par Ollama dépend du `num_ctx` du
   Modelfile livré — à vérifier par `ollama show bge-m3` sur la machine d'inférence
   plutôt qu'à supposer.
2. **Reclassement.** `bge-reranker-v2-m3` (Apache-2.0, environ 568 M de paramètres)
   s'applique à ~30 candidats par question. C'est un coût par *requête*, non amorti ;
   il doit donc être activable par réglage et mesuré avant d'être imposé.
3. **Complétion.** Inchangée : elle passe par le fournisseur épinglé au client
   (ADR 0102) et par le garde-fou LLM04 (ADR 0017).

Le garde-fou actuel compte des **appels**, pas des jetons ni un budget. L'ingestion
documentaire change l'échelle : ré-indexer un référentiel complet est une opération
longue et massive qui ne doit pas consommer le quota conversationnel du client. Il faut
donc **deux compteurs distincts** — interactif et ingestion — et une file d'ingestion
bornée par client. L'ADR 0017 anticipe déjà cette évolution (« quota en nombre d'appels,
pas en jetons »).

Aucun chiffre de coût monétaire n'est avancé ici : il dépend du fournisseur épinglé et
du matériel d'inférence, et ne peut être posé qu'après mesure sur la préproduction.

---

## 8. Évaluation — comment on saura que c'est mieux

Sans mesure, on ne distingue pas un RAG qui cite juste d'un RAG qui cite joliment. On
constitue donc un **jeu d'évaluation versionné** : 60 à 100 questions réelles, chacune
avec les fragments attendus et une réponse de référence, tirées du wiki et d'un corpus
documentaire de démonstration (jamais des données d'un client réel).

Quatre mesures, de la famille Ragas, et une propre au produit :

| Mesure | Ce qu'elle attrape |
| --- | --- |
| **Fidélité** (faithfulness) | Une affirmation de la réponse non étayée par le contexte |
| **Pertinence de la réponse** | Une réponse exacte mais hors sujet |
| **Précision du contexte** | Du bruit dans les fragments retenus |
| **Rappel du contexte** | Un passage nécessaire non retrouvé |
| **Exactitude de citation** (propre à QualitOS) | Un `[doc_id]` cité qui ne figure pas dans le contexte, ou qui pointe une version périmée |

La dernière est la plus importante pour un QMS : c'est elle qui sépare une aide d'une
preuve. Elle se calcule sans modèle juge, par simple comparaison d'ensembles, et peut
donc tourner dans la CI.

**Garde-fou de test** : l'évaluation ne tourne pas en `EMBEDDINGS_PROVIDER=deterministic`
(ADR 0049). Elle s'exécute comme un travail nocturne contre la préproduction, et son
résultat est publié ; la CI, elle, ne garde que les tests de contrat (cloisonnement,
suppression avant ré-indexation, filtre de visibilité, exactitude de citation).

---

## 9. Plan d'exécution

| # | Étape | Contenu | Dépend de |
| --- | --- | --- | --- |
| **1** | **Réparer l'adaptateur Qdrant** | `uuid5` déterministe pour l'identifiant de point ; suppression par filtre `source_id` avant ré-indexation ; test d'intégration contre un vrai Qdrant (Testcontainers) | — |
| **2** | **Visibilité dans l'index** | Étiquette `visibility` sur chaque fragment, confrontée aux droits du lecteur à la requête ; test de non-fuite intra-client | 1 |
| **3** | **Brancher la passerelle** | `AiGatewayClient.ragQuery` + route engine + écran « Rechercher dans la documentation » avec citations ouvrables ; corpus alimenté à la main pour la recette | 1, 2 |
| **4** | **Corpus plateforme** | Classe de licence `platform-owned`, collection partagée, construction depuis `docs/wiki/` dans la CI à chaque livraison | 3 |
| **5** | **Corpus client — texte saisi** | Indexation de `document_versions.content` publié, déclenchée par l'événement de publication, avec recollement nocturne | 3 |
| **6** | **Corpus client — pièces jointes** | Extraction PDF/docx/txt par lien présigné, cache par SHA-256, refus explicite des scans | 5 |
| **7** | **Qualité de récupération** | BM25 sparse + fusion RRF, reclassement optionnel, étalonnage de `min_score`, jeu d'évaluation et tableau de bord de pertinence | 4, 5 |

Les étapes 1 à 3 rendent utilisable ce qui est déjà écrit. Les étapes 4 et 5 donnent sa
valeur au module. Les étapes 6 et 7 font la différence entre une démonstration et un
produit.

**Hors de cette étude, à ne pas glisser dedans** : l'intégration du corpus réglementaire
public (EUR-Lex, Légifrance, FDA) — déjà tranchée par l'ADR 0046 et dépendante de
connecteurs à écrire ; et l'usage agentique du RAG, traité par l'étude d'architecture V2.

---

## 10. ADR à rédiger

Numérotation à la suite des 112 existantes (dernier : ADR 0112). Les numéros sont
indicatifs et doivent être réattribués à l'intégration, plusieurs chantiers étant menés
en parallèle.

| N° proposé | Objet | Ce qu'il tranche |
| --- | --- | --- |
| **0113** | *Un fragment indexé s'identifie par un UUID dérivé, et disparaît avec sa version* | Identifiant `uuid5`, suppression par `source_id` avant ré-indexation, recollement nocturne. Corrige §2.2 et §2.3. |
| **0114** | *Le RAG ne voit pas plus que l'écran* | Étiquette de visibilité sur chaque fragment, filtre à la requête, articulation avec ADR 0081 et 0086. |
| **0115** | *Trois corpus, trois régimes de licence* | Classe `platform-owned`, collections partagées plateforme et publique, frontière avec le structuré du Standards Hub (ADR 0046 confirmé, non rouvert). |
| **0116** | *L'index suit la publication, pas le dépôt* | Déclencheur sur la publication de version, réutilisation de la boîte d'envoi existante, table des événements → actions d'index. |
| **0117** | *Le texte d'une pièce jointe s'extrait par lien présigné, et jamais ne se stocke deux fois* | Où vit l'extraction, formats couverts, refus explicite des scans, cache par SHA-256. |
| **0118** | *Récupération hybride et reclassement* | BM25 sparse + RRF, reclassement activable, étalonnage du seuil ; et arbitrage collection-par-client contre partition par payload. |
| **0119** | *Une réponse sans citation ouvrable n'est pas une réponse* | Contrat de citation, marquage « non étayée », journal des sources citées, affichage de la méthode de confiance. |
| **0120** | *Le quota d'ingestion est distinct du quota conversationnel* | Deux compteurs, file d'ingestion bornée par client ; fait évoluer l'ADR 0017. |

---

## Sources externes

- Qdrant — *Points* (identifiants : entier non signé 64 bits ou UUID) :
  <https://qdrant.tech/documentation/concepts/points/>
- Qdrant — *Choose a Multitenancy Design* (collection unique + partition par payload,
  `is_tenant`, sharding dédié) :
  <https://qdrant.tech/documentation/deploy-and-scale/multitenant-search/>
- Qdrant — *Configure Multitenancy* : <https://qdrant.tech/documentation/guides/multitenancy/>
- Qdrant — *Hybrid Search* (vecteurs creux BM25, fusion RRF/DBSF) :
  <https://qdrant.tech/documentation/search-tuning/hybrid-search/>
- Ollama — bibliothèque de modèles, `bge-m3` (embedding 567 M, documents jusqu'à
  8 192 jetons) : <https://registry.ollama.ai/library/bge-m3>
- Hugging Face — `BAAI/bge-reranker-v2-m3` (licence Apache-2.0) :
  <https://huggingface.co/BAAI/bge-reranker-v2-m3>
- Ragas — référence des mesures (fidélité, pertinence, précision et rappel du contexte) :
  <https://docs.ragas.io/en/v0.1.21/references/metrics.html>
- ISO — droits d'auteur sur les normes (rappel du fondement de l'ADR 0046) :
  <https://www.iso.org/copyright.html>

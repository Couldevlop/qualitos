# Architecture — vues C4

[← Retour à l'espace technique](README.md)

Quatre niveaux, du plus large au plus précis. Chaque niveau nomme les fichiers qui font
foi : quand cette page et le dépôt divergent, c'est le dépôt qui a raison, et cette page
qui est à corriger.

---

## Niveau 1 — Contexte

QualitOS est une plateforme de management de la qualité multi-client. Autour d'elle :

| Acteur ou système | Relation |
| --- | --- |
| Les membres d'un client (direction, qualité, terrain, auditeur) | Navigateur, application web et PWA |
| Un consultant d'un cabinet partenaire | Même application, troisième surface d'autorisation, sans `tenant_id` (ADR 0106) |
| L'éditeur | Console de plateforme : clients, modules, tarifs, cabinets, piste d'audit (ADR 0109) |
| Keycloak | Fournisseur d'identité : jetons OIDC, rôles, annuaire source (ADR 0091, 0093) |
| Modèles de langage | Ollama en local, Anthropic ou Mistral à distance, **toujours** derrière la passerelle IA (ADR 0008, 0102) |
| Équipements de terrain | Capteurs et systèmes industriels ou de santé via l'IoT Hub (ADR 0026 à 0028) |
| Réseau Hyperledger Fabric | Ancrage de preuves, phase B, optionnel et désactivé par défaut (ADR 0012) |

---

## Niveau 2 — Conteneurs

Un bloc du tableau = une image, un `Deployment` et un `Service` générés par le chart
`infra/k8s/qualitos`. Les ports sont ceux de `values.yaml` ; ce sont aussi ceux des
`application.yml` et des `Dockerfile`, et ils ne se devinent pas.

| Conteneur | Technologie | Port | Rôle | Activé par défaut |
| --- | --- | --- | --- | --- |
| `web` | Angular 20, Material 20, nginx non privilégié (UID 101) | 8080 | Application web et PWA | oui, 2 instances |
| `api-core` | Java 21, Spring Boot 4.1 | 8081 | Clients, modules, abonnements, annuaire, cabinets, piste d'audit de plateforme | oui, 2 instances |
| `api-quality-engine` | Java 21, Spring Boot 4.1 | 8082 | Tout le métier qualité (voir niveau 3) | oui, 2 instances |
| `api-iot-hub` | Java 21, Spring Boot 4.1 | 8083 | Registre d'équipements, ingestion de télémétrie, agrégats | oui, 1 instance |
| `ai-service` | Python, FastAPI | 8085 | Passerelle de modèles, RAG, NLQ, calculs statistiques et ML | oui, 1 instance |
| `blockchain-service` | Java 21, Spring Boot 4.1, SDK Fabric | 8090 | Ancrage phase B | **non** : exige un réseau Fabric joignable |
| `ai-vision-5s` | Python, FastAPI | 8090 | Inférence de vision pour les audits 5S | **non présent dans le chart** |
| `site`, `site-contact` | Site public et formulaire de contact | — | Vitrine `qualitos.fr`, hors plateforme | déployés par `infra/k8s/site` |

Dépendances d'état, hors du chart applicatif, décrites par `infra/k8s/deps/` :
PostgreSQL, Keycloak, Qdrant (vecteurs), Ollama (modèles locaux), MinIO (pièces jointes,
désactivé par défaut), et les deux tâches de sauvegarde — locale et hors serveur
(ADR 0085).

```
                      Navigateur / PWA
                             │  HTTPS
                    ┌────────▼────────┐
                    │  ingress nginx  │  (règle par surface ; routes de dépôt
                    └────────┬────────┘   et de stockage objet à part)
         ┌──────────────┬────┴─────┬───────────────┐
         ▼              ▼          ▼               ▼
      web:8080   api-core:8081  engine:8082   iot-hub:8083
                        │            │              │
                        │            ├──────────────┘ (seuil franchi → CAPA)
                        │            ▼
                        │      ai-service:8085 ──► Ollama / Anthropic / Mistral
                        │            │
                        │            └──► Qdrant (RAG)
                        └──► engine:8082 (souscrire un module ouvre le module)
         Keycloak ◄── validation des jetons, de tous les services
         PostgreSQL ◄── une base par service : core, quality, iot
```

Trois arêtes de ce schéma méritent d'être connues :

- **`api-core` → `api-quality-engine`.** Souscrire un module fait appliquer la décision
  par le moteur. L'adaptateur échoue fermé : sans cette adresse, la souscription rend
  502 plutôt que d'enregistrer un abonnement dont le module resterait fermé.
- **`api-iot-hub` → `api-quality-engine`.** Un seuil franchi n'ouvre pas une CAPA
  lui-même ; il la fait ouvrir par le moteur (ADR 0016).
- **`api-quality-engine` → `ai-service`.** Aucun appel de modèle ne part d'ailleurs
  (§18.2 point 4). La passerelle porte les garde-fous de débit, de quota et de taille
  (ADR 0017) et le routage du fournisseur imposé par le client (ADR 0102).

---

## Niveau 3 — Composants du moteur de qualité

`apps/api-quality-engine` est un **modulith** : un déploiement, un paquet Java par
domaine métier, sous `com.openlab.qualitos.quality`. Au relevé du 2026-11-11, il porte
quatre-vingt-huit paquets de ce genre. Les principaux, par famille :

| Famille | Paquets |
| --- | --- |
| Les cinq méthodes | `pdca`, `fives`, `dmaic`, `circle`, `ishikawa`, `fivewhys` |
| Écarts et actions | `nonconformity`, `capa`, `complaints`, `supplier`, `nccluster` |
| Maîtrise des procédés | `spc`, `product`, `controlplan`, `fmeascale`, `apqp`, `calibration` |
| Système de management | `standards`, `ims`, `smi`, `stakeholders`, `riskregister`, `risk`, `audit`, `change`, `docs`, `training`, `academy`, `ehs`, `costofquality` |
| Pilotage | `dashboards`, `kpi`, `storyboard`, `forecast`, `anomaly`, `nlq` |
| Gouvernance réglementaire | `gdpr`, `ropa`, `consent`, `dpia`, `retention`, `aiact`, `aiconformity`, `aiincidents`, `nis2measures`, `cyberincidents`, `breach` |
| Socle transverse | `authz`, `settings`, `config`, `tenantmodules`, `auditlog`, `notifications`, `webhooks`, `apikeys`, `ratelimit`, `rls`, `crypto`, `blockchain`, `aigateway`, `airouting`, `edition`, `industry`, `marketplace` |

Chaque module suit la même découpe hexagonale — `domain` sans framework, `application`
sur des ports, `infrastructure` pour les adaptateurs, `web` pour les contrôleurs — et
celle-ci est une exigence permanente du projet, pas une préférence de style.

L'`ai-service` suit la même discipline en Python (ADR 0008) : `domain/` sans import de
framework, ports abstraits (`AIProvider`, `VectorStore`, `PiiFilter`),
`application/usecase/`, `infrastructure/`, `presentation/`. C'est ce qui permet de
calculer les règles de Nelson, Holt-Winters, l'Isolation Forest, Kernel SHAP ou le
DBSCAN **en NumPy pur** dans le domaine, sans dépendance lourde, et de reléguer
Prophet, LSTM, HDBSCAN, les transformeurs et Whisper à des backends optionnels à
import paresseux (ADR 0031).

---

## Niveau 4 — Le code

Le niveau 4 du modèle C4 descend aux classes. Il n'est pas recopié ici, et c'est
délibéré : une liste de classes dans un fichier Markdown est fausse au premier
renommage. Elle est lisible là où elle est juste :

- la Javadoc et les commentaires du code, en français comme le reste du projet ;
- le champ **Portée** en tête de chaque ADR, qui énumère les classes, migrations et
  écrans touchés par la décision — c'est la carte la plus fiable du dépôt ;
- `/v3/api-docs` et `/swagger-ui.html` pour les contrats exposés, voir
  [API et contrats](api-et-contrats.md).

## Décisions d'architecture à connaître avant de contribuer

| ADR | Ce qu'elle impose |
| --- | --- |
| [0001](../../adr/0001-multi-tenant-via-jwt-claim.md) | Le client vient du jeton, jamais du corps de la requête |
| [0002](../../adr/0002-angular-ngmodules-no-standalone.md) | Angular en NgModules, HTML et SCSS dans des fichiers séparés |
| [0065](../../adr/0065-autorisation-decidee-avant-la-lecture-du-corps.md) | L'autorisation se décide avant la lecture du corps |
| [0086](../../adr/0086-isoler-les-clients-dans-la-base.md) | L'isolation est aussi dans la base, pas seulement dans le code |
| [0092](../../adr/0092-une-personne-est-un-utilisateur-de-l-annuaire.md) | Une personne est un utilisateur de l'annuaire, jamais un texte libre |
| [0100](../../adr/0100-passage-a-spring-boot-4.md) | Spring Boot 4, Jackson 2 conservé par configuration explicite |

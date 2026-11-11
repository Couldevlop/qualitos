# Feuille de route vers 100 % — QualitOS

> **Relevé du 2026-11-11.** Révise la feuille de route du 2026-06-19, qui partait d'une base
> de ~87 % et proposait trois vagues d'exécution. **Les trois vagues sont jouées.** Ce qui
> suit distingue ce qui est fait, ce qui reste codable, et ce qui attend une ressource
> extérieure.
>
> Chaque ligne « fait » nomme sa décision d'architecture ou son fichier : une feuille de
> route qui s'auto-déclare avancée sans preuve ne sert qu'à se rassurer.

---

## 1. Ce que les trois vagues ont livré

### Vague A — différenciateurs (niveau 1 de la feuille précédente)

| Item | État | Preuve |
| --- | --- | --- |
| Standards Hub — génération de documents normatifs par l'IA (§8.8) | **fait** | ADR 0032, puis ADR 0037 pour le dossier en lot ; écran `/standards-doc-gen` ; aucun document en dur, signature humaine obligatoire |
| Standards Hub — audit blanc avancé (§8.4 onglet 7) | **fait** | ADR 0033 ; matière calculée depuis les preuves réelles du client, criticité par la règle ISO/IEC 17021-1 |
| Tableaux de bord interactifs (§7.3) | **fait** | ADR 0034 (filtrage croisé, descente, annotations), ADR 0038 (glisser-déposer) |
| Tableaux de bord — état à une date (§7.3) | **fait** | ADR 0035 ; état *as-of* calculé en SQL, « n/d » quand la donnée est absente plutôt qu'un zéro |
| Export PDF d'un tableau de bord, signé et ancré | **fait** (non prévu alors) | ADR 0043 ; vérification publique par code |

### Vague B — complétude fonctionnelle (niveau 2)

| Item | État | Preuve |
| --- | --- | --- |
| Packs sectoriels (§5) | **fait, avec une dette** | quatorze manifestes servis (`apps/api-quality-engine/src/main/resources/industry-packs/*.yml`) ; sept manifestes **enrichis** vivent dans `libs/industry-packs/packs/*.yaml` et ne sont **pas** sur le chemin de classe du moteur, qui ne dépend pas de cette bibliothèque. Deux sources, une seule servie : à réunir |
| Formation, Academy et progression (§19.3) | **fait** | ADR 0036 ; ceintures, badges, parcours par rôle, export SCORM et xAPI côté serveur |
| IoT — connecteur DICOM (§9.4) | **non fait** | `Protocol.java` propose la valeur DICOM, **aucun connecteur d'ingestion ne la sert**. C'est le seul protocole annoncé sans implémentation |
| IoT — service Edge en veille longue (§9.5) | **partiel** | ADR 0030 : domaine (tampon de report, repli déterministe) et infrastructure (ONNX, stockage fichier) livrés et testés ; manifestes K3s et courtier local présents ; **la boucle de service qui souscrit au courtier et appelle l'orchestrateur n'existe pas** |

### Vague C — durcissement (niveau 3)

| Item | État | Preuve |
| --- | --- | --- |
| Analyse dynamique de sécurité en intégration | **fait** | ADR 0039 ; travail « DAST (OWASP ZAP) » de `ci.yml` |
| Tests de charge avec budgets | **fait** | ADR 0040 ; `.github/workflows/perf-k6.yml`, appelé par la chaîne |
| Ingénierie du chaos | **fait** | ADR 0042 ; `.github/workflows/chaos.yml`, `infra/chaos` |
| Couverture des composants du front | **fait** | 399 fichiers de spécification sous `apps/web/src`, travail « Frontend build & tests » bloquant |

---

## 2. Ce qui a été livré en plus, et qui n'était pas dans la feuille de route

Cinq mois de travail que la feuille précédente n'anticipait pas, parce qu'ils viennent du
terrain et non du plan. Groupés par ce qu'ils apportent :

| Famille | Apport | Décisions |
| --- | --- | --- |
| Documentation qualité | GED réelle : déduplication par empreinte, signataires désignés, circuit activable | 0094, 0095, 0099 |
| Maîtrise des procédés | Études de capabilité (X̄–R et X̄–S), Gage R&R, règles de Nelson sur la carte, prévision de sortie de limites | 0096, 0097, 0107, 0110, 0112 |
| Système de management | Tableau de bord SMI, ajout de norme par le Standards Hub, registre des parties intéressées, registre des risques | 0077, 0090, 0104, 0076 |
| Changements et produits | MOC par analyse d'impact, produit / PFMEA / plan de surveillance, APQP par projet, rapport 8D | 0087, 0059, 0060, 0063, 0064, 0066 à 0072, 0071 |
| Isolation et traçabilité | Cloisonnement des clients dans la base, piste d'audit de la plateforme avec ajout seul garanti | 0086, 0109 |
| Exploitation chez le client | Édition on-premise, licence hors ligne, mode léger sans Kubernetes, paquet signé, copie hors serveur | 0082 à 0085 |
| Identité et personnes | Annuaire en lecture restreinte, une personne est un utilisateur, remplissage à la première connexion | 0091, 0092, 0093 |
| Souveraineté de l'IA | Fournisseur épinglé par client, sans repli silencieux | 0102 |
| Adaptabilité | Mécanisme unique des listes déroulantes du client, natures d'écart appartenant au client | 0111, 0103 |
| Socle | Passage à Spring Boot 4 pour corriger deux vulnérabilités sans correctif en 6.2.x | 0100 |
| Commercialisation | Cabinets partenaires, facturation et abonnements, site de présentation | 0106 |

---

## 3. Ce qui reste codable

| # | Item | Pourquoi ce n'est pas fait | Périmètre |
| --- | --- | --- | --- |
| C.1 | **Connecteur d'ingestion DICOM** | Le patron existe (Modbus, LoRaWAN, Sparkplug) ; le travail n'a pas été fait. Une valeur d'énumération sans connecteur est une promesse à l'écran | `apps/api-iot-hub/.../infrastructure` |
| C.2 | **Service Edge en veille longue** | La logique est livrée et testée ; il manque la boucle qui souscrit au courtier local | `infra/edge/inference` |
| C.3 | **Réunir les deux sources de packs sectoriels** | Les sept manifestes enrichis ne sont pas servis ; le moteur sert quatorze manifestes simples | `libs/industry-packs`, `apps/api-quality-engine` |
| C.4 | **Description AsyncAPI des événements** | `CLAUDE.md` §19.1 l'annonce, aucun fichier n'existe. Sans urgence tant que le relais Kafka est désactivé ; **bloquant avant d'ouvrir le flux à un tiers** | `docs/`, `apps/api-quality-engine/.../webhooks` |
| C.5 | **Trancher le sort du service `api-connectors`** | L'ADR 0007 décrit un service qui n'est **pas dans la construction** (module commenté dans `pom.xml`). Les connecteurs livrés sont des paquets du moteur. Il faut soit l'intégrer, soit un ADR de supersession | `pom.xml`, `docs/adr` |
| C.6 | **Le diagramme du designer de workflow n'est jamais exécuté** | Les circuits réels (GED, MOC, CAPA) ne lisent pas ces diagrammes. Soit on les branche, soit l'écran annonce ce qu'il est | `apps/web/.../workflow-designer`, `apps/api-quality-engine/.../workflow` |
| C.7 | **Écran de prédiction du risque fournisseur** | L'appel serveur existe et fonctionne ; aucun écran ne l'affiche | `apps/web/.../features/suppliers` |

---

## 4. Ce qui attend une ressource extérieure

Inchangé depuis juin, et c'est normal : aucune de ces lignes ne se débloque par du code.
Le code d'accueil est en place dans les cinq cas.

| Item | Prérequis | Code d'accueil |
| --- | --- | --- |
| Vision 5S — modèle de production | Carte graphique et jeu d'images 5S annoté | chemin ONNX réel, exercé en test par un modèle jouet (ADR 0029) |
| Modèles lourds (Prophet, LSTM, HDBSCAN, transformeurs, Whisper) | Carte graphique | backends optionnels à import paresseux ; indisponible rend 422 ou 501, jamais un faux résultat (ADR 0031) |
| Ancrage phase B | Réseau Hyperledger Fabric joignable | profil `fabric`, service livré, `enabled: false` dans le chart (ADR 0012) |
| Cluster TimescaleDB | Instance managée | migration gardée, inerte sur PostgreSQL simple |
| Cryptographie bc-fips post-quantique | Publication amont de BC-FIPS avec les algorithmes post-quantiques | agilité cryptographique en place, CBOM tenu (ADR 0011) |
| Test d'intrusion manuel, vidéos de formation | Budget et prestataire | — |

---

## 5. Définition de « fait »

Inchangée, et elle s'applique à chaque étape, pas seulement à l'item :

1. **Code complet** : zéro ébauche, zéro `TODO`, tout câblé de bout en bout.
2. **Architecture hexagonale** : domaine sans framework, application sur des ports,
   infrastructure, présentation.
3. **OWASP** : client issu du jeton (§18.2 #2), autorisation avant lecture du corps,
   validation des entrées, aucune donnée personnelle en clair dans les journaux.
4. **Tests unitaires** sur chaque unité de logique.
5. **Test de référence** figeant le comportement de bout en bout de l'étape.
6. **Couverture ≥ 98 %** en lignes sur le code de l'étape — pas 85 %.
7. **Validation séquentielle** : l'étape est verte avant d'entamer la suivante. Aucune dette
   de test reportée.
8. Chaîne d'intégration verte, ADR si la décision est structurante, messages de commit en
   français suivant les *Conventional Commits*.
9. **Documentation simultanée** : page du wiki technique **et** page du wiki utilisateur
   (§18.2 #7). Une fonctionnalité sans ses deux pages n'est pas livrée.

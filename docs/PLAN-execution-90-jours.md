# QualitOS — Plan de mise en exécution

> Dérivé de `docs/QualitOS_Etude_Strategique_Approfondie_Nov2026.docx`.
> Ce document ne répète pas la stratégie : il dit **ce qui doit être fait dans
> le produit** pour que cette stratégie soit tenable, et **dans quel ordre**.

---

## 0. Ce que ce plan promet, et ce qu'aucun plan ne peut promettre

L'étude demande « un plan de mise en exécution 100 % de réussite ». Disons-le
franchement : **aucun plan ne garantit 5 à 8 contrats en 90 jours**, parce que
la signature dépend du budget, du calendrier et de l'arbitrage d'un client —
trois variables hors de notre main.

Ce qui peut être rendu certain, en revanche, c'est que **rien dans le produit
ne fasse perdre une affaire**. C'est la promesse de ce plan : à J+90, aucun
« non » ne doit s'expliquer par une démo qui tombe, une donnée qu'on ne peut pas
importer, un pack qu'on ne peut pas vendre, ou un module qu'on ne peut pas
éteindre. Le reste est du commerce, et l'étude le couvre déjà.

Les objectifs produit ci-dessous sont donc formulés en **conditions
éliminatoires** : chacun se vérifie par oui ou non, et chacun bloque une vente
s'il n'est pas rempli.

---

## 1. Réponse à la question posée : QualitOS peut-il être « core + modules complémentaires » ?

**Oui — l'ossature existe déjà, et elle est plus avancée que ce que l'étude
suppose. Elle est en revanche incomplète sur trois points précis, et c'est là
que se joue la vendabilité des trois packs.**

### 1.1 Ce qui existe (vérifié dans le code)

| Mécanisme | Où | Ce qu'il fait déjà |
| --- | --- | --- |
| Catalogue de modules | `ModuleCatalog` (api-quality-engine, `tenantmodules/domain`) | 26 modules déclarés, chacun avec un **code**, une **catégorie**, un **palier minimum**, ses **dépendances** et un indicateur **`coreModule`** |
| Notion de module cœur | `ModuleCatalogEntry.coreModule` | 6 modules marqués cœur : `pdca`, `ishikawa`, `fives`, `capa`, `docs`, `audit`, `auditlog` — non désactivables |
| Paliers | `BillingTier` | `FREE`, `STANDARD`, `PRO`, `ENTERPRISE` ; une activation est refusée si le palier du client est insuffisant |
| Dépendances | `ModuleCatalog` | p. ex. `controlplan` exige `risk` **et** `product` ; `standards` exige `docs` et `audit` |
| Activation par client | table `tenant_modules` (api-core, RLS activée) + `ModuleActivationService` | activation/désactivation à chaud, avec statut `TRIAL / ACTIVE / SUSPENDED / EXPIRED / DISABLED` et date de fin d'essai |
| Barrière à l'exécution | annotation `@RequiresModule` | un appel sur un module éteint est refusé côté serveur |
| Chargement paresseux | modules Angular (`loadChildren`) | un client sans le module ne télécharge pas son bundle |
| Tarification par module | `ModulePrice` (api-core, billing) | prix par **module × palier × période**, et facturation sur l'abonnement signé, pas sur le catalogue courant |

**Conclusion d'architecture : il n'y a rien à inventer.** Le modèle
« core + complémentaires + paliers + dépendances + essai + prix » est déjà en
place, testé, et isolé par client.

### 1.2 Les trois écarts à combler — et ce qu'ils coûtent commercialement

#### Écart 1 — Le catalogue connaît 26 modules ; la plateforme en expose bien plus

Le dossier `apps/web/src/app/features` compte **75 surfaces**. Le catalogue en
déclare 26. Des modules centraux **n'ont aucun code de catalogue**, et ne
peuvent donc être ni vendus à l'unité, ni éteints, ni facturés :

- `nc` (non-conformités) — le point d'entrée commercial de l'étude ;
- `spc` (cartes de contrôle, capabilité, Gage R&R) — l'argument du pack
  Industrial Excellence ;
- `apqp`, `products` / PFMEA, `moc`, `documents` (GED), `smi`, `risk-register`,
  `cost-of-quality`, `academy`, `five-whys`, `ideas`, `workflow-designer` ;
- toute la famille GRC / données personnelles (`dpia`, `ropa`, `consents`,
  `breach`, `transfers`, `ai-systems`, `nis2-measures`…) ;
- toute la famille IA (`anomaly`, `forecast`, `nc-cluster`, `complaint-nlp`,
  `nlq`, `storyboard`) — l'argument du pack Enterprise & AI.

**Conséquence commerciale directe :** les trois packs de l'étude ne sont pas
exprimables aujourd'hui. On ne peut pas vendre « Professional » sans savoir ce
qu'il contient au sens de la plateforme, ni démontrer à un prospect
« Professional » qu'il n'a pas accès au SPC — puisque rien ne l'en empêche.

#### Écart 2 — La barrière n'est posée que sur 4 modules

`@RequiresModule` est utilisé **8 fois**, sur **4 codes** (`dmaic`,
`controlplan`, `product`, `risk`), pour **117 contrôleurs**. Autrement dit :
désactiver un module le retire du menu, mais **son API reste ouverte**.

**Conséquence :** la modularité est aujourd'hui cosmétique. Elle ne soutient ni
la facturation (rien n'empêche d'utiliser ce qu'on ne paie pas), ni l'argument
de simplicité (« vous n'activez que ce dont vous avez besoin »), ni un audit de
sécurité sérieux chez un prospect grand compte.

#### Écart 3 — Les 4 paliers techniques ne sont pas les 3 packs commerciaux

Le code raisonne en `FREE / STANDARD / PRO / ENTERPRISE`. L'étude vend
`Professional / Industrial Excellence / Enterprise & AI Predictive`. Le **pack
n'existe pas comme objet** : il faudrait aujourd'hui activer les modules un à
un, à la main, pour chaque nouveau client — opération longue, non reproductible,
et donc source d'erreur le jour d'un onboarding.

**Conséquence :** chaque signature se paie en temps de paramétrage manuel, et
deux clients du même pack risquent de ne pas avoir le même périmètre.

### 1.3 La cible proposée

Garder les quatre paliers techniques — ils servent les dépendances et les
règles d'activation — et **ajouter au-dessus la notion de pack** : un nom
commercial, une liste de modules, un palier. Un pack s'applique en une
opération, et un client affiche à tout moment le pack dont il relève.

| Pack commercial | Palier technique | Modules (codes de catalogue) |
| --- | --- | --- |
| **Professional** | `STANDARD` | cœur (`pdca`, `ishikawa`, `fives`, `capa`, `docs`, `audit`, `auditlog`) + `nc`, `risk`, `ehs`, `training`, `complaints`, `calibration`, `supplier`, `change`, `kpi`, `moc`, `ged` |
| **Industrial Excellence** | `PRO` | Professional + `dmaic`, `circle`, `spc`, `apqp`, `product`, `controlplan`, `fivewhys`, `ideas`, `standards`, `workflow` |
| **Enterprise & AI Predictive** | `ENTERPRISE` | Industrial Excellence + `ai-anomaly`, `ai-forecast`, `ai-cluster`, `ai-complaints`, `nlq`, `storyboard`, `dashboards`, `tv`, `iot`, `industry`, `blockchain`, `grc` |
| **Console Cabinet** | `PRO` (multi-clients) | Professional + `standards` + bascule entre espaces clients |

Ce tableau est une **proposition**, pas une décision : il se tranche avec le
commerce. Ce qui n'est pas négociable, c'est qu'il existe **quelque part dans le
code** et non dans un tableur.

---

## 2. Plan produit, calé sur les 90 jours de l'étude

L'étude fixe trois mois : *amorce et preuve*, *outbound et vidéos*, *closing et
partenaires*. Le produit doit être prêt **un cran avant** chaque phase, jamais
pendant.

### Phase A — Avant J+15 : « la démo ne doit jamais tomber »

L'étude prévoit 15 à 20 Cold Audit Videos dès les semaines 1-2, et toute la
mécanique commerciale repose sur une démonstration enregistrée ou live. Une démo
qui tombe à l'écran coûte le prospect **et** la vidéo.

| # | Action | Condition éliminatoire | Charge estimée |
| --- | --- | --- | --- |
| A1 | **Jeu de données de démonstration réaliste et rejouable** : un script qui remonte un client complet (NC, CAPA, audits, cartes SPC hors limites, AMDEC, projet DMAIC en phase Analyser, réclamations). `scripts/donnees-demo.py` existe — le compléter et le rendre idempotent. | Un `--reset` puis un `--seed` rendent l'environnement identique, en moins de 5 minutes | 3 j |
| A2 | **Recette visuelle des 10 écrans de démo**, sur Chrome, en 1280×800 et en 390×844 : registre NC, fiche NC, CAPA, audit, carte de contrôle, capabilité, DMAIC, Ishikawa, SMI, tableau de bord. | Zéro écran cassé, zéro « Chargement… » figé, zéro débordement | 2 j |
| A3 | **Clore la dette de densité** : la passe faite sur les tokens globaux couvre tous les écrans par héritage, mais les écrans à grille propre (tableau de bord, SMI, APQP) doivent être relus. | Les 10 écrans de démo tiennent une ligne de flottaison de 1080 px sans défilement pour l'essentiel | 3 j |
| A4 | **Import contrôlé depuis Excel** pour NC, CAPA, fournisseurs, équipements. L'étude en fait un argument central (« migration progressive et non big bang »). | Un fichier client de 500 lignes s'importe avec un rapport d'erreurs ligne à ligne, sans donnée partiellement écrite | 5 j |

### Phase B — Avant J+45 : « le pack doit être vendable »

L'étude signe ses premiers Design Partners en semaines 5-8. On ne peut pas
signer un pack qui n'existe pas.

| # | Action | Condition éliminatoire | Charge estimée |
| --- | --- | --- | --- |
| B1 | **Compléter le catalogue de modules** : un code pour chaque surface vendable (≈ 35 entrées à ajouter), avec son palier, ses dépendances et son indicateur cœur. | `ModuleCatalog` couvre toute surface facturable ; un test d'architecture échoue si un module Angular paresseux n'a pas de code | 4 j |
| B2 | **Poser `@RequiresModule` sur tous les contrôleurs concernés** (≈ 110 à annoter). | Un test parcourt les contrôleurs et échoue si l'un, rattaché à un module, n'est pas gardé | 5 j |
| B3 | **Introduire le pack** : objet, API, et application en une opération à la création d'un client. | Créer un client « Industrial Excellence » active exactement les modules du pack, sans intervention manuelle | 4 j |
| B4 | **Écran « Mon périmètre »** côté client : ce qui est activé, ce qui ne l'est pas, et ce que débloquerait le pack supérieur. | Un prospect voit, à l'écran, ce qu'il n'a pas — c'est le moteur de l'upsell décrit par l'étude | 3 j |
| B5 | **Mesurer le ROI dans le produit** : le module « coût de la non-qualité » existe ; il doit produire la page que l'étude veut partager en fin de démo (temps d'audit, NC récurrentes, coûts évités). | Une page exportable en PDF signé, chiffrée sur les données du client | 4 j |

### Phase C — Avant J+75 : « l'onboarding doit tenir sans les fondateurs »

L'étude identifie la dépendance aux fondateurs comme un risque de plafonnement.
Cinq onboardings simultanés en semaines 9-12 ne passent pas à deux personnes.

| # | Action | Condition éliminatoire | Charge estimée |
| --- | --- | --- | --- |
| C1 | **Parcours d'onboarding guidé** dans l'application : 8 étapes (sites, utilisateurs, catégories, natures d'écart, circuits, premiers imports, premier audit, premier tableau de bord), avec un indicateur d'avancement. | Un client va de zéro à sa première NC sans assistance | 6 j |
| C2 | **Console Cabinet, niveau 1** : vue multi-clients, indicateurs de santé, bascule d'espace. L'étude en fait un canal, pas un gadget. | Un consultant voit 5 clients et bascule sans se déconnecter | 8 j |
| C3 | **Trames réutilisables** (niveau 2 de la Console) : modèles d'audit, d'AMDEC, de DMAIC, exportables et co-brandés. | Un consultant instancie une trame chez un client en moins de 2 minutes | 5 j |
| C4 | **Preuves d'adoption** : les KPI produit que l'étude veut suivre (activation des modules à 30 jours, utilisateurs actifs, volumes NC/CAPA/audits, taux de clôture, temps de préparation d'audit avant/après). | Un tableau de bord interne les affiche par client | 3 j |

### Phase D — Continu : la discipline qui rend le reste vrai

| # | Action | Condition éliminatoire |
| --- | --- | --- |
| D1 | Un commit = un run de CI, et la préprod ne reçoit que du vert (déjà en place). | Aucun déploiement sur un rouge |
| D2 | Recette visuelle sur navigateur avant chaque mise en préprod des écrans de démo. | Capture jointe à la PR |
| D3 | Revue produit bi-mensuelle alignée sur les blocages d'adoption des Design Partners (l'étude la demande). | Compte-rendu avec décisions et responsables |
| D4 | Toute fonctionnalité IA reste explicable et validée par un humain (§12.3, ADR 0102). | Aucune décision automatique non expliquée |

---

## 3. Charge totale et ce qu'elle implique

| Phase | Charge | Fenêtre |
| --- | --- | --- |
| A — la démo ne tombe pas | 13 j | J+1 → J+15 |
| B — le pack est vendable | 20 j | J+15 → J+45 |
| C — l'onboarding tient sans les fondateurs | 22 j | J+45 → J+75 |
| **Total** | **55 jours-homme** | 75 jours calendaires |

À une personne à temps plein sur le produit, 55 jours-homme sur 75 jours
calendaires ne laissent **aucune marge** pour le support des premiers clients,
qui tombera précisément en phase C. Deux conclusions, à trancher :

1. soit une seconde paire de bras sur la phase C (Console Cabinet et onboarding
   sont les deux chantiers les plus volumineux et les plus séparables) ;
2. soit on repousse la Console Cabinet au trimestre suivant, en assumant que les
   5 cabinets partenaires de l'étude seront accompagnés **à la main** sur le
   premier trimestre — ce qui est faisable à 5, et ne l'est plus à 15.

Ne pas trancher revient à prendre la première option sans la financer, et à
livrer les trois phases en retard.

---

## 4. Les risques produit de l'étude, et ce qu'ils deviennent ici

| Risque (étude §14) | Traduction produit | Parade |
| --- | --- | --- |
| Complexité produit → adoption lente | 75 surfaces, dont beaucoup hors du quotidien d'un client | Packs (B3) + écran « Mon périmètre » (B4) + onboarding guidé (C1) : un client ne voit que son périmètre |
| Qualité des données client → IA moins pertinente | Les modèles IA tournent sur ce que le client importe | Import contrôlé (A4) avec rapport d'erreurs ; aucune suggestion IA sans validation humaine |
| Dépendance aux fondateurs | La démo et l'onboarding reposent sur eux | Jeu de données rejouable (A1), onboarding guidé (C1), trames (C3) |
| Perception d'un prix élevé | Rien dans le produit ne chiffre la valeur | Page ROI exportable (B5), construite sur les données réelles du client |
| Partenaires peu actifs | Rien ne leur fait gagner du temps | Console Cabinet niveaux 1-2 (C2, C3) |

---

## 5. Ce qui n'est pas dans ce plan, et pourquoi

- **Les actions commerciales** (vidéos, livre blanc, LinkedIn, clusters) : elles
  sont décrites dans l'étude et ne dépendent pas du produit. Les répéter ici
  diluerait la responsabilité.
- **L'IA prédictive avancée** : elle est livrée (anomalies, prévision,
  clustering, NLP réclamations, SHAP). Ce qui manque n'est pas du modèle, c'est
  la **preuve** — d'où C4 plutôt qu'un chantier IA.
- **Les Industry Packs sectoriels** : utiles à l'expansion, inutiles aux 90
  premiers jours, où la cible est l'industrie et où les valeurs par défaut de la
  plateforme suffisent.

---

## 6. Les cinq oui/non qui disent si le plan a réussi

À J+90, on répond par oui ou par non. Rien d'autre ne compte.

1. L'environnement de démonstration se remonte en moins de 5 minutes et les 10
   écrans de démo sont propres sur portable **et** sur téléphone.
2. Un fichier Excel client de 500 lignes s'importe avec un rapport d'erreurs,
   sans donnée à moitié écrite.
3. Créer un client sur un pack active exactement les modules de ce pack, et
   l'API refuse ce qui n'est pas activé.
4. Un client va de zéro à sa première non-conformité sans qu'un fondateur
   intervienne.
5. Chaque Design Partner dispose d'une page de ROI chiffrée sur ses propres
   données.

Si ces cinq réponses sont « oui », aucune affaire perdue ne s'expliquera par le
produit. C'est la seule garantie qu'un plan d'exécution puisse honnêtement
donner.

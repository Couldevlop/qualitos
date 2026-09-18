# Passation — reprendre QualitOS et le faire évoluer seul

> **But de ce document.** Vous donner de quoi tenir l'architecture, savoir où vit
> chaque classe et pourquoi elle existe, et continuer les développements sans
> personne. Il ne remplace pas `CLAUDE.md`, qui reste la **spécification** et les
> invariants ; il explique comment le code les applique.
>
> Rédigé le 16 septembre 2026. Remis à jour le 18 septembre, sur `main` à `355e74c`.

---

## 0. Reprendre la main : six capacités, et où chacune se trouve

Tenir ce projet seul, c'est pouvoir faire six choses. Le reste de ce document est
rangé pour y répondre — cette table est la porte d'entrée, à lire en premier.

| | Capacité | Où | État |
| --- | --- | --- | --- |
| 1 | **Lancer** — la plateforme tourne sur votre poste | `README.md` § Démarrage rapide, puis §6.1 ici | Une commande Docker, 15 services |
| 2 | **Comprendre** — ouvrir n'importe quel fichier et savoir ce qu'il fait | **§2 à §5** — c'est le cœur de ce document | Six motifs expliquent la quasi-totalité du code |
| 3 | **Modifier sans casser** — changer quelque chose et le savoir | **§6** (lancer les suites, les pièges) et **§8** (les invariants) | Tests + CI + 8 règles non négociables |
| 4 | **Livrer** — une modification arrive en ligne | **§7** | Branche → PR → CI → fusion → le cluster tire |
| 5 | **Exploiter** — voir ce qui tourne, et pourquoi | **§7.3** et `docs/runbooks/README.md` | Douze runbooks, indexés |
| 6 | **Réparer** — revenir en arrière, restaurer | `docs/runbooks/README.md` § Réparer | Retour arrière + sauvegardes, vérifiées |

**Si vous ne lisez qu'une chose**, lisez le **§2** : il donne les six motifs qui
rendent les 1 700 fichiers Java lisibles sans les ouvrir un par un. Le reste se
consulte au besoin.

**L'ordre conseillé pour une prise en main complète** : §0 → §6.1 (faire tourner,
pour voir ce dont on parle) → §2 (les motifs) → §3 (tracer une requête de bout en
bout) → §6.2 et §6.3 (vérifier, et les pièges) → §7 (livrer) → les runbooks.

---

## 1. Ce que vous reprenez, en chiffres

**Ne recopiez pas ces chiffres, remesurez-les.** La première version de ce
document a menti en UN JOUR : elle annonçait « migrations jusqu'à V131 » et
« vérification CAPA : non commencé » alors que V132 était déjà appliquée en
préproduction. Un tableau écrit à la main ne vieillit pas, il pourrit — et un
document de reprise qui se trompe sur l'état du code est pire que pas de
document, parce qu'on le croit.

```bash
bash scripts/passation-chiffres.sh
```

Ce que ce script rendait le 18 septembre 2026, sur `main` à `355e74c` :

| | |
| --- | --- |
| Fichiers Java — moteur qualité | **1 547** |
| Fichiers Java — tous services et bibliothèques | **1 743** |
| Fichiers TypeScript front (hors tests) | **555** |
| Fichiers de test front | **262** |
| Fichiers de test end-to-end | **3** |
| Migrations Flyway | jusqu'à **V133** |
| Décisions d'architecture | **72** ADR, numérotés jusqu'à **0074** (0004 et 0005 n'existent pas), indexés dans `docs/adr/README.md` |
| Langues servies | 6 — `fr` (source, dans les gabarits) + 5 traductions |

Le **nombre de tests** n'est volontairement pas dans ce tableau : il ne s'obtient
qu'en lançant les suites (§6.2). Un `grep @Test` donnerait un chiffre faux — tests
paramétrés, tests désactivés, classes imbriquées — et un chiffre faux sur la
couverture est exactement le genre de chiffre qu'on ne rattrape plus.

Sept applications dans `apps/`, quatre bibliothèques dans `libs/`. Le gros du
métier est dans **`api-quality-engine`** ; `api-core` porte l'authentification,
les tenants et les utilisateurs.

---

## 2. Comprendre 1 547 classes sans les lire une à une

C'est l'objectif principal, alors commençons par là. **Six motifs expliquent la
quasi-totalité du code.** Qui les tient peut ouvrir n'importe quel fichier et
savoir en dix secondes ce qu'il fait et où sont ses voisins.

### 2.1 Deux dispositions de module coexistent — et c'est délibéré

**Disposition plate** (modules anciens : `capa`, `nonconformity`, `apqp`, `risk`…)
Tout le module dans un seul paquet, nommé par convention :

```
capa/
  CapaCase.java            ← entité JPA (l'état)
  CapaStatus.java          ← énumération d'état
  CapaCaseRepository.java  ← accès données (Spring Data)
  CapaService.java         ← règles métier
  CapaController.java      ← surface HTTP
  CapaDto.java             ← ce qui entre et sort (records imbriqués)
  CapaStateException.java  ← refus métier → 409
  CapaValidationException.java ← refus de forme → 422
```

**Disposition hexagonale** (modules récents : `controlplan`, `product`,
`nonconformity/eightd`, `dashboards`, `export`, `marketplace`) :

```
eightd/
  domain/          ← l'état et les règles. AUCUNE dépendance framework.
  application/     ← les cas d'usage + les PORTS (interfaces)
  infrastructure/  ← les ADAPTATEURS : JPA, PDF, signature, sources
  web/             ← contrôleurs et DTO de transport
```

La règle « `domain` et `application` n'importent aucun framework » est **vérifiée
par la CI** — `HexagonalArchitectureTest` (ArchUnit) échoue si quelqu'un importe
Spring ou JPA dans le domaine. Ne cherchez pas à uniformiser les anciens modules :
la migration se fait module par module, quand on y retouche.

### 2.2 Le motif du service

Tout service métier fait, dans cet ordre :

1. lit le **tenant du JWT** (`TenantContext.getTenantId()`), jamais du corps ;
2. **charge** l'agrégat en filtrant par tenant (`findByIdAndTenantId`) ;
3. **vérifie l'état** (transition permise ?) → `…StateException` (409) ;
4. **vérifie la forme** → `…ValidationException` (422) ;
5. **écrit**, puis **journalise** dans le journal d'audit.

Si vous ouvrez un service et que l'un de ces cinq temps manque, c'est soit un
oubli, soit une raison documentée juste au-dessus en commentaire.

### 2.3 Le motif de la pièce jointe

Six modules stockent des fichiers (preuves PDCA, CAPA, NC, livrables APQP…), tous
sur le même patron :

- **métadonnées en base** (nom, type, taille, empreinte, auteur, date) ;
- **octets dans MinIO**, sous une clé préfixée par le tenant ;
- **URL présignée** pour le téléchargement, jamais le fichier via l'API ;
- un `StoredObjectOwner` déclare les objets encore référencés, pour que le
  balayeur d'orphelins ne supprime pas un fichier vivant ;
- garde d'entrée commune : `UploadedBinaryGuard` (liste blanche de types,
  vérification des octets de tête, nettoyage du nom de fichier).

Exemple de référence : `ApqpDeliverableEvidenceService`.

### 2.4 Le motif du document scellé

Control plans, rapports d'audit, certificats de formation, **rapport 8D** :

1. le contenu est **figé** dans un instantané de TEXTE (jamais des identifiants à
   résoudre : un document remis à un client ne doit pas changer parce qu'une
   source a bougé depuis) ;
2. le rendu PDF est une **fonction pure** de cet instantané ;
3. son **empreinte** est signée (ML-DSA, `HybridSignatureService`) puis ancrée ;
4. au téléchargement, on **re-rend et on compare** — discordance ⇒ 409, pas de
   document.

Piège rencontré et résolu : PDFBox écrit un `/ID` aléatoire à chaque `save`, donc
deux rendus du même contenu n'avaient pas la même empreinte. Il est dérivé du
contenu — voir `PdfBoxEightDRenderAdapter`.

### 2.5 Le motif de l'agrégation

Le référentiel transverse (`CLAUDE.md` §3.6) veut qu'on **agrège** plutôt qu'on
recopie. Deux écrans le font :

- **Dossier PPAP** — agrège les livrables du cycle APQP marqués « requis » ;
- **Rapport 8D** — agrège la NC, l'Ishikawa, les 5 pourquoi, la CAPA, le PFMEA et
  les plans de surveillance.

Dans les deux cas : un **port unique** côté `application`, un **adaptateur**
côté `infrastructure` qui interroge les dépôts des autres modules, et une règle
d'or — **une source absente le DIT**, elle ne laisse jamais un vide muet.

### 2.6 Le motif de la traduction

Deux mondes, à ne jamais confondre :

- **Ce que la plateforme fournit se traduit.** Le texte d'amorçage du référentiel
  APQP suit la langue demandée tant que **ce texte-là** est resté celui du
  référentiel (comparaison au texte source, pas à une date — ADR 0070).
- **Ce que le client écrit lui appartient.** Dès qu'un utilisateur reformule un
  libellé, sa formulation gagne, dans toutes les langues.
- **Exception assumée : le rapport 8D est en anglais**, titres et corps compris.
  C'est un document remis à un donneur d'ordre, la pratique est anglophone, et un
  document opposable ne change pas de langue selon qui l'affiche.

---

## 3. Suivre une requête de bout en bout

Le meilleur moyen de tenir l'architecture est de tracer **un** cas complet. Prenez
le 8D, le plus récent et le plus représentatif.

```
Navigateur
  └─ nc-eightd.component.ts          apps/web/src/app/features/nc/pages/nc-eightd/
     └─ NcService.issueEightDReport() apps/web/src/app/features/nc/nc.service.ts
        └─ ApiInterceptor              core/http/api.interceptor.ts
           (pose Authorization + Accept-Language)
              ↓ HTTP POST /api/v1/nc/{ncId}/8d/issue
EightDController.emettre()             …/eightd/web/
  @PreAuthorize AVANT la lecture du corps (ADR 0065)
  StepUpGuard : second facteur exigé (§18.2 #5)
  └─ EightDService.emettre()           …/eightd/application/
     ├─ EightDSourcePort               (interface)
     │   └─ EightDSourceAdapter        …/infrastructure/ ← interroge 9 dépôts
     ├─ EightDSnapshotAssembler        ← met les sources en texte, 8 sections
     ├─ EightDSealPort → EightDSealAdapter  ← signe l'empreinte, l'ancre
     └─ EightDReportRepository (port)
         └─ EightDReportRepositoryAdapter → EightDReportJpaRepository
                                          → table nc_eightd_reports (V130)
```

Refaites l'exercice une fois sur un module plat (`CapaController` →
`CapaService` → `CapaCaseRepository`) et vous aurez vu les deux dispositions.

---

## 4. Retrouver le code derrière n'importe quoi

**Derrière un écran.** Le fil d'Ariane est la route. `/fr/apqp/:id/ppap` →
`features/apqp/apqp-routing.module.ts` → le composant → son service →
l'`endpoint` du service, qui vous donne l'URL de l'API.

**Derrière un appel d'API.** Cherchez le chemin dans les contrôleurs :

```bash
grep -rn '"/api/v1/nc' apps/api-quality-engine/src/main/java --include=*Controller.java
```

**Derrière une colonne de base.** Cherchez-la dans les migrations : elles sont
datées, numérotées, et **chacune explique en commentaire pourquoi elle existe**.

```bash
grep -rln 'verification_required' apps/api-quality-engine/src/main/resources/db/migration/
```

**Derrière une décision.** `docs/adr/README.md` est un tableau : une ligne par
décision, avec son résumé. C'est le document à lire quand un choix vous surprend —
il y a presque toujours une raison écrite, et les alternatives écartées avec.

**Derrière un texte affiché.** Les traductions sont **générées**. Ne modifiez
jamais `apps/web/src/locale/messages.*.xlf` : les textes vivent dans
`apps/web/scripts/i18n/*.py`, et `python apps/web/scripts/gen-i18n-xlf.py` les
régénère. La CI refuse une divergence.

---

## 5. Générer les références qui ne périment pas

Un catalogue de classes écrit à la main serait faux en une semaine. Préférez des
références **générées depuis le code**, qui restent vraies :

| Quoi | Comment |
| --- | --- |
| API HTTP | Déjà en place : springdoc. Démarrez le moteur, ouvrez `/swagger-ui.html` |
| Classes Java | `mvn javadoc:javadoc` → **`target/reports/apidocs/index.html`** (5 288 pages sur le moteur ; ce n'est plus `target/site/apidocs`, le chemin a changé avec le greffon 3.x) |
| Composants Angular | `npx @compodoc/compodoc -p tsconfig.app.json` |
| Schéma de base | `pg_dump --schema-only`, ou lire les migrations dans l'ordre |

Le code porte **beaucoup** de commentaires, et ils disent le *pourquoi*, pas le
*quoi*. C'est là que se trouve la connaissance qu'aucun diagramme ne donne.

> **`doclint` est coupé dans le `pom.xml` racine, et c'est délibéré.** Les
> commentaires de ce dépôt sont écrits pour être *lus*, pas pour satisfaire un
> vérificateur de balises. Avec `doclint` actif, un `@param` manquant sur une
> méthode privée fait échouer la génération **entière** : on perdrait 5 288 pages
> utiles pour une balise absente. Aucune exécution n'est liée à une phase — la
> génération n'alourdit pas `verify`, elle marche simplement quand on la demande.

---

## 6. Faire tourner et vérifier

### 6.1 Démarrer

```bash
docker compose up -d                 # PostgreSQL, Keycloak, MinIO
cd apps/api-quality-engine && mvn spring-boot:run
cd apps/web && npx ng serve
```

Comptes de démonstration : `demo/demo` (manager qualité, sans second facteur),
`admin/admin` et `superadmin` (second facteur TOTP exigé).

### 6.2 Vérifier

```bash
# Backend — TEMP sur D: obligatoire, C: est saturé
TEMP=D:/tmp TMP=D:/tmp TESTCONTAINERS_RYUK_DISABLED=true mvn clean verify

# Front
cd apps/web && npx ng test --watch=false --browsers=ChromeHeadless

# End-to-end — quelques minutes. Sert la SPA lui-même (ng serve en
# configuration `e2e`) : ni backend ni Keycloak à démarrer avant.
# Le nombre de cas n'est pas écrit ici : c'est une mesure d'EXÉCUTION, et un
# chiffre recopié aurait la même espérance de vie que ceux du §1.
cd apps/web && npx playwright test

# Sécurité, mêmes règles que la CI
semgrep scan --config p/security-audit --config p/owasp-top-ten \
             --config p/java --config p/secrets --severity ERROR --error
```

### 6.3 Les pièges qui coûtent une demi-journée

Tous rencontrés, tous vérifiés :

- **`TESTCONTAINERS_RYUK_DISABLED=true` supprime le ramasseur de conteneurs.**
  Après une exécution interrompue, les conteneurs survivent, mangent la mémoire et
  font échouer les runs suivants **en cascade**. Réflexe :
  `docker container prune -f`.
- **Karma peut sortir en code 0 sans avoir exécuté un seul test** (« Found 1 load
  error »). Ne vous fiez jamais au code de sortie : cherchez la ligne
  `TOTAL: N SUCCESS`.
- **Le compilateur Maven est incrémental** et masque des échecs. « Unresolved
  compilation problems » ⇒ supprimez `target/test-classes/…` du paquet concerné.
- **`ng test --include` est ignoré** sur ce poste : lancez la suite entière.
- **PostgreSQL local sur 5434** (un PostgreSQL natif occupe déjà 5432).
- **L'application est une PWA.** Après un déploiement, un bandeau « nouvelle
  version disponible » apparaît et **la page continue de tourner sur l'ancien
  code** tant qu'on ne clique pas « Recharger ». Erreur commise : croire qu'un
  correctif n'était pas déployé alors qu'il l'était.
- **Chrome auto-traduit la préproduction.** Vous lirez « Maison » pour *Home* et
  « Effondrement » pour *Collapse* — ce n'est pas l'interface, c'est le
  traducteur. Désactivez-le sur le domaine avant tout contrôle d'i18n.

---

## 7. Livrer

Le flux est décrit dans `docs/git-workflow.md`. En pratique :

1. branche depuis `main` ;
2. commits **en français**, sujet court, corps qui dit le **pourquoi** ;
3. `mvn clean verify` + suite front verts **avant** de pousser ;
4. pull request → la CI décide ;
5. fusion dans `main` → la mise en ligne se fait seule, **mais pas comme on croit** :
   voir juste en dessous.

### 7.1 Le déploiement est TIRÉ par le cluster, pas poussé par GitHub

C'est le point le plus contre-intuitif de la chaîne, et celui qui fait perdre le
plus de temps quand on l'ignore.

Le workflow `CD préproduction` **ne déploie rien**. Il construit les images, les
scanne (Trivy, bloquant) et les publie. C'est tout.

C'est le **cluster** qui déploie : un minuteur systemd sur le nœud exécute
`infra/k8s/auto-deploy.sh`, qui surveille `main` et, dès que les images d'un
commit sont publiées, rejoue `infra/k8s/deploy.sh`.

**Pourquoi ce sens** : déployer depuis GitHub imposerait d'y déposer une clé
d'accès au cluster ; quiconque l'obtiendrait obtiendrait l'environnement. En
inversant, aucun secret ne quitte l'infrastructure.

**Ce que ça change pour vous** : entre la fusion et la mise en ligne, il y a le
délai du minuteur. Un `CD préproduction` vert ne signifie pas « c'est en ligne »,
mais « les images sont prêtes à être prises ». Pour savoir ce qui tourne
vraiment, il faut demander au cluster (§7.3).

### 7.2 Retour arrière, sauvegardes

`docs/runbooks/retour-arriere-production.md` — et lisez-le **avant** d'en avoir
besoin : il contient un piège de fond (un `helm rollback` seul ne suffit pas).
`docs/runbooks/sauvegarde-et-restauration.md` pour les bases.

### 7.3 Voir ce qui tourne

```bash
ssh root@62.238.11.20
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
NS=qualitos-preprod

kubectl -n "$NS" get pods,deploy,ingress
# Quelle VERSION est servie — la question à poser en premier :
kubectl -n "$NS" get deploy \
  -o custom-columns=NOM:.metadata.name,IMAGE:'.spec.template.spec.containers[0].image'
```

L'étiquette d'image porte le commit. Si elle correspond à votre fusion, le code
est déployé — et si l'écran montre encore l'ancien comportement, c'est le
navigateur (§6.3, le piège PWA), pas le déploiement.

Le reste de l'exploitation — observabilité, stockage objet, annuaire, Edge,
vision — est indexé dans **`docs/runbooks/README.md`**.

La CI bloque sur : tests, **Semgrep (ERROR)**, Trivy, et un contrôle que les
fichiers i18n générés sont à jour (`--check` + `git diff --exit-code`).

Retour arrière : `docs/runbooks/retour-arriere-production.md`.
Sauvegardes : `docs/runbooks/sauvegarde-et-restauration.md`.

> **Il n'existe pas de production.** La préproduction EST l'environnement
> (`preprod.qualitos.openlabconsulting.com`), et son espace de noms est
> **`qualitos-preprod`**. Ne lancez jamais `deploy.sh prod`.
>
> Certains runbooks, écrits quand un espace « production » était envisagé,
> prennent encore `NS=qualitos` par défaut. Vérifiez l'espace avant toute
> commande destructrice : une restauration jouée au mauvais endroit ne se
> rattrape pas.

---

## 8. Les invariants auxquels on ne touche pas

Repris de `CLAUDE.md` §18.2, ce sont les règles dont la violation est un défaut,
pas un choix :

1. `tenant_id` **toujours** issu du JWT validé, **jamais** du corps de la requête.
2. `@PreAuthorize` **avant** la lecture du corps (ADR 0065) — sinon le point
   d'entrée fermé sert d'oracle de validation.
3. Aucun secret en clair.
4. Aucune action critique sans second facteur **et** ancrage.
5. Aucune dépendance portant une CVE critique ou haute.
6. Aucune logique sectorielle codée en dur : tout passe par les Industry Packs.
7. **Aucune trace d'assistant IA** dans les artefacts — commits, PR, code,
   commentaires, manifestes, documentation, fichiers générés.

---

## 9. Ce qui reste ouvert

À vous de trancher, rien n'est engagé :

| Sujet | État |
| --- | --- |
| **Tests end-to-end** | Trois fichiers, tous verts. **Couvert** : la navigation sans erreur JS sur dix routes, le Standards Hub, et la vérification CAPA par ses **deux** portes — le dialogue d'édition et le popup d'action d'une NC. **Non couvert : l'APQP et le 8D**, les deux plus gros lots récents, et donc la dette la plus rentable à combler. Le patron à recopier est `capa-verification.spec.ts` : des accroches `data-test` dans le gabarit, aucun compteur codé en dur, et `pageerror` écouté pour attraper les `NG0100`. (Le nombre de cas se lit en lançant la suite, pas ici — §6.2.) |
| **Couverture front des fonctions** | Le seuil global est à 93 % et la marge est mince (mesurée à 93,07 % après le lot 8D). Une fonctionnalité peu testée fera échouer la CI. |
| **Journal d'audit hors transaction** | L'émission d'un 8D et l'approbation d'un control plan écrivent le journal dans une transaction séparée. Cohérent entre eux, mais un incident entre les deux laisserait un acte non journalisé. |
| **Modules anciens en disposition plate** | `capa`, `nonconformity`, `apqp`, `risk` n'ont pas la découpe hexagonale. À migrer quand on y retouche, pas avant. |

---

## 10. Les sept derniers lots, pour le contexte

Ce qui vient d'être livré, avec la décision qui l'explique :

| Lot | ADR | Ce qu'il change |
| --- | --- | --- |
| Livrables APQP cochables et prouvables | 0068 | Un livrable se coche, se prouve, et le dossier PPAP agrège le cycle |
| Rejet d'une réclamation externe | 0069 | Rejeter ≠ annuler ; motif obligatoire, statut réel |
| Le référentiel APQP suit la langue | 0070 | Révise 0068 : la traduction se décide sur le TEXTE, pas sur la ligne |
| Rapport 8D | 0071 | Agrège le dossier, fige, signe, ancre ; vérification publique par QR |
| Projets APQP | 0072 | Révise 0068 : plusieurs projets, un seul formulaire de livrable |
| Vérification d'efficacité CAPA | 0073 | On EXIGE avant de constater ; `NULL` n'est pas `false` — trois états, pas deux |
| L'action avant le dossier | 0074 | Révise 0072 : « Ajouter une action » remplace « Escalader CAPA », le type dit un travail |
| La vérification dans le popup d'action | 0073 (amendé) | Depuis une NC, la question est posée là — et **obligatoire** : c'est le seul chemin où elle se pose |

Lisez-les dans cet ordre : chacun explique pourquoi le précédent ne suffisait pas.
C'est le meilleur résumé de la manière dont ce code évolue.

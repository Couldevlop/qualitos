# Revue manuelle — relire le code, et le tenir

> **Ce document est fait pour vous, pas pour la machine.** Il répond à deux
> besoins qui n'en font qu'un : **relire une modification à la main** en sachant
> quoi chercher, et **lire n'importe quel fichier du dépôt** en sachant ce qu'il
> fait, ce qu'il garantit et qui en dépend. Les deux reposent sur la même
> compétence : savoir ce que ce code promet.
>
> **Il ne remplace rien.** [`CLAUDE.md`](../CLAUDE.md) est la **spécification** et
> les invariants. [`PASSATION.md`](./PASSATION.md) explique **l'architecture** —
> les six motifs, le trajet d'une requête, où vit quoi. Celui-ci donne la
> **méthode de lecture et la grille de contrôle** : ce qu'on regarde, dans quel
> ordre, avec quelles commandes, et ce qui a déjà cassé ici.
>
> Mesures de ce document : **2026-11-13**, sur `5db707f`. Les chiffres se
> remesurent (`bash scripts/passation-chiffres.sh`) ; les **méthodes**, non.

---

## 0. Par où commencer

| Vous voulez… | Lisez |
| --- | --- |
| Relire une PR aujourd'hui | §1 (ce qui est déjà vérifié) → §2 (préparer) → §5 (les grilles) → §7 (la fiche) |
| Comprendre un fichier qu'on vous montre | §3 (décoder un nom) → §4 (la méthode des cinq questions) |
| Savoir ce qui casse dans ce dépôt | §5.5 — le catalogue des familles de défauts. **La section la plus rentable.** |
| Former quelqu'un à relire | §1, §3, §5.5, puis faites-lui relire une PR déjà fusionnée et comparez |

**Si vous ne lisez qu'une chose** : le **§1.2** (les sept classes de défaut que
rien d'automatique n'attrape) et le **§5.5** (celles qui sont déjà arrivées).
Tout le reste, la CI le fait mieux que vous.

---

## 1. Ce que la machine vérifie déjà

Relire à la main ce qu'un programme vérifie mieux est du temps perdu deux fois :
on le dépense, et on le prend sur la lecture qui compte. Donc d'abord, ce qui est
**déjà couvert**.

### 1.1 Les contrôles de la chaîne d'intégration

Déclarés dans `.github/workflows/ci.yml`. Une PR les déclenche tous sauf la mise
en ligne, réservée à `main`.

| Travail | Ce qu'il attrape | Ce qui le fait échouer |
| --- | --- | --- |
| `build-test` | Compilation + tests Java + **couverture JaCoCo** | Un test rouge, ou la couverture sous **0,85 lignes / 0,75 branches** |
| `frontend-build` | Build de production + suite Karma + **cohérence i18n** | Un test rouge, un seuil de couverture manqué, ou un fichier `.xlf` qui diverge des tables |
| `ai-service-tests`, `ai-vision-5s-tests`, `site-contact-tests` | Les suites Python des trois services | Un test rouge, ou la couverture du service de contact sous **98 %** |
| `sast-semgrep` | Motifs de vulnérabilité dans le code | Un seul résultat de niveau **ERROR** sur `p/security-audit`, `p/owasp-top-ten`, `p/java`, `p/secrets` |
| `secrets-scan` | Secrets, **sur tout l'historique** | Un secret trouvé, même dans un commit ancien |
| `sca-trivy-deps`, `sca-owasp` | Dépendances vulnérables | Une CVE **critique ou haute** |
| `image-scan` | Les images construites | Idem, dans les couches de l'image |
| `dast-zap` | Le moteur en marche, sondé par son OpenAPI | Une alerte **haute** |
| `e2e-smoke` | Dix routes dans un vrai navigateur, erreurs JS écoutées | Une page qui casse, un `NG0100` |
| `infra-onprem` | Les scripts et le chart Helm | ShellCheck, un rendu de chart invalide, `tenant_id` absent du profil Keycloak |
| `perf-k6` | Les temps de réponse | Un SLO dépassé |

S'y ajoute, **à chaque exécution de tests Java**, `HexagonalArchitectureTest` :
vingt et une règles ArchUnit qui interdisent au domaine et à la couche
application d'importer Spring, JPA, Hibernate, Jackson ou POI. **Lisez bien le
§5.3 avant de vous reposer dessus** : ces règles sont écrites module par module,
à la main, et **la majorité des modules n'en a aucune**.

### 1.2 Les sept classes de défaut qu'aucun de ces travaux ne verra

C'est pour celles-là que vous relisez. Aucune n'est détectable par un outil, et
les sept ont déjà produit un défaut réel dans ce dépôt.

| # | Classe | Pourquoi la machine est aveugle |
| --- | --- | --- |
| 1 | **Le code fait autre chose que ce que le message de commit promet** | Les deux sont cohérents avec eux-mêmes |
| 2 | **Une protection qui se retourne en moyen de nuire** | Elle fonctionne : elle refuse. La question est *qui* elle refuse |
| 3 | **Un état inconnu traité comme un état connu** | `null` est une valeur valide ; le compilateur est content |
| 4 | **Un refus qui enseigne comment le contourner** | Le refus est correct ; c'est le message qui fuit |
| 5 | **Une information affirmée alors qu'on ne la sait pas encore** | L'affichage est syntaxiquement juste ; il est faux une seconde, ou pour toujours |
| 6 | **Un commentaire ou un ADR devenu faux** | Rien ne compile un commentaire |
| 7 | **Une hypothèse d'exploitation non écrite** (un seul exemplaire, un ordre, une horloge) | Le code marche — sur la machine où il a été écrit |

Les sept sont dépliées au **§5.5**, avec l'endroit où chacune est arrivée.

---

## 2. Préparer la revue — vingt minutes avant de lire une ligne

### 2.1 Obtenir le travail, et le mesurer

```bash
# Le périmètre exact, sur la branche de la PR
git fetch origin && git log --oneline origin/main..HEAD
git diff --stat origin/main...HEAD

# Ce qui a bougé, par nature — décide l'ordre de lecture
git diff --name-only origin/main...HEAD | sed 's|.*\.||' | sort | uniq -c | sort -rn
```

Trois nombres à regarder tout de suite : **combien de commits**, **combien de
fichiers**, **combien de migrations**. Une PR qui touche une migration Flyway et
quarante fichiers n'est pas la même revue qu'une PR de dix lignes dans un
gabarit.

> **Une PR trop grosse n'est pas relisible, et une revue qui prétend le
> contraire est un tampon, pas une revue.** Au-delà d'une trentaine de fichiers,
> le constat à écrire n'est pas sur le code : c'est « découpez ». Sauf si le
> gros du volume est **mécanique** (traductions générées, renommage) — alors
> isolez-le et relisez le reste.

### 2.2 Lire la promesse avant le code

Les messages de commit de ce dépôt sont longs et disent le **pourquoi**. Ce n'est
pas de la décoration : **c'est la promesse que la revue vérifie.** Lisez-les
d'abord, en entier, et notez-en les affirmations vérifiables — « le plafond se
compte avant l'envoi », « cinq lignes restent », « la couverture est à 100 % ».
Chacune devient un point à contrôler. Un commit qui ne promet rien de vérifiable
est déjà un constat à écrire.

### 2.3 L'ordre de lecture

Du plus difficile à rattraper vers le plus facile. On lit **l'irréversible
d'abord**.

1. **Les migrations** (`db/migration/V*.sql`) — une migration partie en ligne ne
   se retire pas, elle se compense. Lisez-les en premier, toujours.
2. **L'ADR** s'il y en a un, ou son absence pour une décision qui en méritait un.
3. **Le domaine** (`*/domain/`) — les règles. Si elles sont justes, le reste est
   du câblage ; si elles sont fausses, le reste est du câblage de l'erreur.
4. **L'application** (`*/application/`) — l'enchaînement, les ports.
5. **Le web** (`*/web/`, contrôleurs) — autorisation, formes d'entrée, codes de
   retour.
6. **L'infrastructure** (`*/infrastructure/`) — adaptateurs, requêtes, rendu.
7. **Le front** — services, puis composants, puis gabarits.
8. **Les tests** — en dernier, mais **jamais sautés** : voir §5.6.

### 2.4 Faire tourner ce que vous relisez

Une revue entièrement sur écran laisse passer tout le §5.5. Le minimum :

```bash
# La suite du périmètre touché, pas tout le dépôt. Le profil `dev` saute JaCoCo
# et les tests de contexte web : trois à cinq secondes au lieu de dix minutes.
# Le `verify` complet reste obligatoire avant de pousser — c'est la CI qui tranche.
TEMP=D:/tmp TMP=D:/tmp TESTCONTAINERS_RYUK_DISABLED=true \
  mvn -P dev -Dtest='com.openlab.qualitos.quality.capa.**' test

cd apps/web && npx ng test --watch=false --browsers=ChromeHeadless \
  > /d/tmp/karma.log 2>&1; grep -a 'TOTAL' /d/tmp/karma.log
```

Puis **ouvrez l'écran** et faites le geste que la PR décrit. Les pièges qui
coûtent une demi-journée sont listés dans `PASSATION.md` §6.3 — le plus cher
étant le cache de la PWA, qui fait croire qu'un correctif n'est pas déployé.

---

## 3. Décoder un nom — savoir ce qu'une classe fait sans l'ouvrir

Ce dépôt est **nommé par convention**, sans exception notable. Un suffixe est un
contrat : il dit ce que la classe a le droit de faire, et surtout ce qu'elle n'a
pas le droit de faire. Tenir ce tableau, c'est pouvoir lire une arborescence
comme un sommaire.

### 3.1 Java — les suffixes et leur contrat

Comptes mesurés le 2026-11-13 sur l'ensemble `apps/*/src/main/java` et `libs/`.

| Suffixe | Nb | Ce qu'il contient | Ce qu'il ne doit **jamais** contenir |
| --- | --- | --- | --- |
| `*Controller` | 141 | La surface HTTP : chemin, autorisation, forme d'entrée, code de retour | Une règle métier. Un `if` sur un statut est un signal d'alerte |
| `*Service` | 144 | Les règles métier, dans l'ordre du motif de service (`PASSATION.md` §2.2) | Du rendu (PDF, Excel), du SQL écrit à la main, un `HttpServletRequest` |
| `*Repository` | 241 | L'accès aux données, Spring Data | Une décision. Un dépôt ne choisit pas, il rend |
| `*Dto` | 158 | Ce qui entre et ce qui sort, en `record`s imbriqués | Un comportement. Un DTO qui a une méthode métier est une entité déguisée |
| `*Port` | 26 | Une **interface** déclarée côté `application` | Une annotation de framework |
| `*Adapter` | 75 | L'implémentation d'un port, côté `infrastructure` | Une règle que le port ne décrit pas |
| `*Exception` | 259 | Un refus. Trois familles, et le suffixe **dicte le code HTTP** | — |
| ↳ `*StateException` | 55 | « La transition demandée n'est pas permise dans cet état » → **409** | |
| ↳ `*ValidationException` | 22 | « La forme de ce que vous envoyez est invalide » → **422** | |
| ↳ `*NotFoundException` | 117 | « Pas trouvé **pour ce tenant** » → **404** | |
| `*Guard` | 7 | Une garde d'entrée transverse (fichiers envoyés, second facteur, débit IA) | |
| `*Assembler` | 2 | La mise en **texte** d'un instantané de document scellé | Une lecture de dépôt : elle se fait dans l'adaptateur de source |
| `*Mapper` | 37 | Entité ⇄ DTO, sans perte et sans décision | |
| `*Properties` | 14 | Configuration liée (`@ConfigurationProperties`) | Une valeur par défaut qui change le comportement en silence |
| `*Config` | 21 | Le câblage Spring | |

**Le fait à retenir sur les exceptions** : le nom porte le code de retour. Une
exception mal nommée répond 409 là où il fallait 422 — et un client qui distingue
« réessayez autrement » de « réessayez plus tard » prend la mauvaise décision.
Quand vous relisez un `throw`, demandez-vous **quel code HTTP l'appelant va
recevoir**, pas si le message est joli.

### 3.2 TypeScript — les fichiers et les variables

| Forme | Nb | Contrat |
| --- | --- | --- |
| `*.component.ts` + `.html` + `.scss` | 376 | Un écran ou un morceau d'écran. **Jamais `standalone: true`**, jamais de gabarit en ligne |
| `*.service.ts` | 101 | Un appel HTTP et sa mise en forme. C'est là que vit l'URL de l'API |
| `*.module.ts` | 100 | Le découpage NgModule, et le chargement différé des routes |
| `*.guard.ts`, `*.interceptor.ts` | 3 | Autorisation de route, en-têtes (`Authorization`, `Accept-Language`) |
| `*.spec.ts` | 411 | La spécification exécutable du voisin. Sur un composant lourd, **découpée par thème** : `capa-detail.lifecycle.spec.ts`, `…evidence…`, `…fiche…` |

**Le suffixe `$`** désigne un observable, sans exception : `report$`, `loading$`,
`error$`, `reload$`. Il se lit dans le gabarit avec `| async`. Un nom sans `$`
qui porte un observable est un défaut de lecture pour tous ceux qui suivront.

**`data-test="…"`** dans un gabarit (présent dans 120 gabarits) est une accroche
pour les tests de bout en bout. Elle fait partie du contrat : **la renommer casse
un test que votre PR ne contient pas.**

**`qos-user-picker`** (44 usages) est le seul moyen de désigner une personne. Un
champ de texte libre ou un UUID affiché à l'écran est un défaut, jamais un choix
— voir §5.5, famille G.

### 3.3 Ce qu'un nom ne garantit pas

Un suffixe dit l'**intention**, pas le fait. Les trois écarts les plus fréquents,
tous rencontrés :

- un `*Service` qui rend un PDF (la règle et le rendu dans la même classe) ;
- un `*Adapter` qui décide (une règle écrite dans l'infrastructure, donc
  intestable sans base) ;
- un `*Dto` qui porte une méthode de calcul (une entité qui a traversé la
  frontière).

Chacun est une violation de SRP qu'aucun outil ne signale. Le **§5.4** donne la
question à poser pour chacun.

---

## 4. Lire une classe en dix minutes — les cinq questions

À appliquer sur n'importe quel fichier, dans cet ordre. On ne lit pas le corps
des méthodes avant la question 4.

**1. Que garantit cette classe ?** Lisez le nom, le paquet, le commentaire de
classe. Ce dépôt commente le *pourquoi* : le commentaire d'en-tête vaut souvent
dix minutes de lecture de corps.

**2. Quel est son état ?** Les champs, et seulement eux. `final` partout = sans
état, donc sûre à appeler de n'importe où. Un champ mutable dans un `@Service`
Spring est **partagé entre toutes les requêtes et tous les tenants** : c'est un
défaut, pas un choix (§5.5, famille G).

**3. Qui l'appelle ?** C'est la question qui donne le sens.

```bash
grep -rn 'CapaService' apps/api-quality-engine/src/main/java --include=*.java | grep -v 'CapaService.java'
```

Si personne ne l'appelle, c'est du code mort — un constat à écrire. Si tout
l'appelle, toute modification est à haut risque.

**4. Qu'est-ce qui la casse ?** Ouvrez son test, pas son code. Un `*Test.java` ou
un `*.spec.ts` est la **spécification exécutable** : les noms de cas disent les
règles, y compris celles que personne n'a documentées ailleurs.

```bash
find apps -name 'CapaServiceTest.java'
ls apps/web/src/app/features/capa/pages/capa-detail/*.spec.ts
```

Côté front, les bancs d'un composant lourd sont **découpés par thème** et non en
un fichier unique : `capa-detail.lifecycle.spec.ts`, `…closure-blockers…`,
`…evidence…`, `…fiche…`. Le nom du fichier dit déjà quelle règle il garde — et un
composant dont tous les bancs tiennent dans un seul fichier de mille lignes est,
en soi, un constat à écrire.

**5. Qu'est-ce qu'elle appelle ?** Les imports. Dans un paquet `domain/`, un
import de `org.springframework` ou `jakarta.persistence` est une violation
d'architecture — même quand aucune règle ArchUnit ne couvre ce module (§5.3).

### 4.1 Les références générées, plutôt qu'un catalogue écrit

Un catalogue de classes écrit à la main serait faux en une semaine. `PASSATION.md`
§5 donne les quatre générateurs ; le plus utile pour relire est la Javadoc, qui
rend **toutes** les classes du moteur avec leurs commentaires :

```bash
cd apps/api-quality-engine && mvn javadoc:javadoc   # → target/reports/apidocs/index.html
cd apps/web && npx @compodoc/compodoc -p tsconfig.app.json
```

Et pour l'API vue de l'extérieur : démarrez le moteur, ouvrez `/swagger-ui.html`.
C'est la seule description de l'API qui ne peut pas mentir, puisqu'elle est
dérivée des contrôleurs.

---

## 5. Les grilles

Six grilles, dans l'ordre où elles se passent. Chacune donne la **question**, le
**moyen de la vérifier ici**, et à quoi ressemble l'échec.

### 5.1 Périmètre et intention

| Question | Comment |
| --- | --- |
| Le code fait-il ce que le message promet, **et rien de plus** ? | Relisez le message (§2.2), cochez ses affirmations une par une |
| Y a-t-il du changement **non demandé** dans le diff ? | `git diff --stat` : un fichier qui n'a rien à voir avec le sujet est à questionner, même s'il est amélioré |
| Une **décision structurante** est-elle prise sans ADR ? | Si la réponse à « pourquoi comme ça ? » n'est écrite nulle part, il manque un ADR (`CLAUDE.md` §22-7) |
| Un ADR existant devient-il **faux** ? | `grep -rln '<le sujet>' docs/adr/` — un ADR qu'on contredit se **révise**, il ne se contourne pas |
| Un reformatage noie-t-il le fond ? | Demandez à le séparer. Un diff illisible n'est pas relu, il est approuvé |

### 5.2 Sécurité — la grille la plus courte, et la plus chère à manquer

Les invariants de `CLAUDE.md` §18.2 ne sont pas des recommandations : leur
violation est un défaut. Six d'entre eux se vérifient en deux minutes.

| Invariant | Vérification |
| --- | --- |
| **`tenant_id` vient du jeton, jamais du corps** (§18.2 #2) | Dans le diff : `grep -n 'tenantId' <fichiers>`. Toute lecture hors de `TenantContext` est suspecte. Côté dépôt, la lecture doit être `findByIdAndTenantId` (148 usages) — un `findById` nu sur une entité multi-tenant est une fuite entre clients |
| **`@PreAuthorize` avant la lecture du corps** (ADR 0065) | L'annotation est sur la méthode du contrôleur, et la validation du corps ne la précède pas. Sinon un point d'entrée fermé devient un **oracle de validation** : il dit si la donnée existe à qui n'a pas le droit de demander |
| **Second facteur + ancrage sur acte critique** (#5) | Signature, émission, approbation : `grep -n 'StepUp' <fichier>` |
| **Aucun secret en clair** (#3) | `gitleaks` le voit. Ce que vous voyez et pas lui : un secret **dérivé** d'une valeur en clair, ou journalisé |
| **Pas de donnée personnelle dans les journaux** (§22-9) | Chaque `log.` du diff : un identifiant technique, un compte, un statut — jamais un nom, une adresse, un contenu saisi |
| **Pas de donnée personnelle sur la chaîne** (§11.3) | Seules des empreintes sont ancrées |

Et les quatre questions OWASP que Semgrep ne pose pas :

1. **Le refus dit-il trop ?** Un message d'erreur qui nomme le contrôle qui a
   mordu apprend à l'attaquant quoi corriger. Règle du dépôt (ADR 0118) : *on
   répond une raison quand elle dit au visiteur quoi faire ; on la journalise
   quand elle ne sert qu'à l'attaquant.*
2. **La protection peut-elle servir d'arme ?** Un plafond, un verrou, un blocage
   de compte : qui peut le déclencher **contre quelqu'un d'autre**, et à quel
   coût ? Voir §5.5, famille B — c'est arrivé.
3. **L'autorisation est-elle vérifiée sur l'objet, ou seulement sur le rôle ?**
   Avoir le rôle ne donne pas accès à *cet* enregistrement : le filtre par
   tenant, le site, le service.
4. **Un nouvel appel sortant est-il ajouté ?** Destination en liste blanche
   (§11.1 A10), et jamais une URL venue du corps de la requête.

### 5.3 Clean Architecture — et le trou qu'il faut connaître

Les deux dispositions de module coexistent exprès (`PASSATION.md` §2.1). Sur un
module **hexagonal**, la grille est :

| Question | Échec typique |
| --- | --- |
| `domain/` importe-t-il un framework ? | `org.springframework`, `jakarta.persistence`, `org.hibernate`, `com.fasterxml.jackson`, `org.apache.poi` |
| `application/` dépend-il de `infrastructure/` ou de `web/` ? | Un cas d'usage qui construit son adaptateur lui-même |
| Un module en appelle-t-il un autre **autrement que par un port** ? | Un dépôt JPA d'un autre module importé directement dans un service |
| La dépendance pointe-t-elle **vers l'intérieur** ? | Le domaine qui connaît le DTO de transport |

**Le trou, et c'est le point le plus important de cette section.** Les règles
ArchUnit sont écrites **module par module, à la main**. Mesuré le 2026-11-13 :

- **54 paquets `domain/`** dans le moteur ;
- **11 d'entre eux** sont protégés par une règle.

Donc pour **quarante-trois modules sur cinquante-quatre, vous êtes la seule
garde.** Et le corollaire, à vérifier systématiquement :

> **Une PR qui crée un module hexagonal doit ajouter sa règle dans
> `HexagonalArchitectureTest`.** Sans elle, la discipline tient le temps que son
> auteur s'en souvienne. L'absence de règle est un constat à écrire, à chaque
> fois.

```bash
# Les paquets domain sans règle — à lancer sur une PR qui ajoute un module
cd apps/api-quality-engine/src/main/java
find . -type d -name domain | sed 's|.*/quality/||; s|/domain$||' | sort
grep -o '"\.\.[a-z0-9.]*\.domain\.\."' \
  ../../test/java/com/openlab/qualitos/quality/arch/HexagonalArchitectureTest.java | sort -u
```

Sur un module en **disposition plate** (`capa`, `nonconformity`, `apqp`,
`risk`…), la grille est plus simple : les cinq temps du motif de service sont-ils
là, dans l'ordre ? Et **n'exigez pas la migration vers l'hexagonal** : la règle
du dépôt est de migrer un module quand on y retouche en profondeur, pas à
l'occasion d'un correctif.

### 5.4 SOLID — en questions, pas en principes

Réciter les cinq lettres ne trouve rien. Les voici retournées en questions qui
tombent sur ce code précis.

**S — une seule raison de changer.** « Si demain on change le format du PDF,
combien de fichiers j'ouvre ? » Si la réponse inclut un `*Service`, le rendu et
la règle sont mélangés. C'est exactement ce que la disposition hexagonale sépare :
la règle dans `domain`, le rendu dans un adaptateur. **Signal** : un import de
`org.apache.pdfbox` ou `org.apache.poi` dans un service.

**O — ouvert à l'extension, fermé à la modification.** La forme que ça prend ici
est l'invariant #9 : **aucune logique sectorielle codée en dur**. « Ajouter le
secteur viticole, ça touche du code ou de la donnée ? » Si c'est du code, c'est
un défaut. **Signal** : un `switch` ou une suite de `if` sur un nom de secteur,
une norme, un type de tenant.

**L — un adaptateur tient la promesse de son port.** Pas seulement la signature :
le **comportement aux bords**. « Que fait l'adaptateur quand la source est
vide ? » La règle du dépôt (motif de l'agrégation) est qu'**une source absente le
DIT** — jamais un vide muet. Un adaptateur qui rend une liste vide là où un autre
rend une mention « source indisponible » casse l'écran qui les traite
indifféremment.

**I — un port ne demande pas plus que ce qu'on utilise.** « Qui implémente ce
port, et combien de ses méthodes sont réellement appelées ? » Un port de neuf
méthodes dont les appelants utilisent deux force chaque adaptateur — et chaque
double de test — à écrire sept méthodes pour rien.

**D — on dépend de l'interface, pas de l'implémentation.** C'est la grille §5.3.
La question pratique : « ce cas d'usage, je peux l'instancier dans un test **sans
base de données ni contexte Spring** ? » Si non, l'inversion n'est pas faite, que
les paquets s'appellent `domain` ou pas.

### 5.5 Le catalogue — ce qui a déjà cassé ici

**La section la plus rentable du document.** Les mêmes familles reviennent ;
chacune est arrivée, avec son correctif.

#### A — Un état inconnu traité comme un état connu

Un booléen a deux valeurs ; un fait en a trois : vrai, faux, **on ne sait pas
encore**. Les écraser en deux fait affirmer au logiciel ce qu'il ignore.

- Vécu : le verdict d'efficacité d'une CAPA — `NULL` n'est pas `false`, un
  critère non encore évalué n'est pas un échec (ADR 0073, ADR 0108).
- Vécu : `capaCaseId$` partait de `null` dans le rapport 8D ; trois disciplines
  affirmaient « aucun dossier CAPA » pendant le chargement, et **définitivement**
  si la requête échouait.

**Question de revue** : pour chaque champ nullable et chaque `BehaviorSubject`,
*que veut dire l'absence de valeur, et l'écran la distingue-t-il du faux ?*

#### B — Une protection qui devient un moyen de nuire

Un plafond, un verrou, un blocage : la bonne question n'est pas « protège-t-il ? »
mais « **qui peut le déclencher contre quelqu'un d'autre, et pour quel prix ?** »

- Vécu : le plafond global du formulaire public se comptait **à l'entrée de la
  route**, donc sur des requêtes qui n'envoyaient aucun courriel. Douze adresses
  et soixante requêtes de rebut, chacune sous la borne par adresse, fermaient le
  formulaire pour tout le monde pendant une heure. Correctif : il se compte juste
  avant l'envoi — *un plafond qui dit ce que la boîte accepte compte des
  courriels, pas des requêtes HTTP* (ADR 0118).
- Vécu : un durcissement d'autorisation avait enfermé dehors l'administrateur de
  tenant lui-même.

**Question de revue** : *où exactement le compteur s'incrémente-t-il, et un
appelant malveillant peut-il l'épuiser sans rien obtenir ?*

#### C — Un refus qui enseigne comment le contourner

- Vécu : le formulaire répondait « soumis trop vite ». Aucun humain ne déclenche
  ce refus, et le nommer dit à l'automate quoi corriger : attendre trois
  secondes. La raison reste journalisée, elle n'est plus répondue — alors que
  « jeton périmé » le reste, parce qu'elle dit au visiteur de recharger.

**Question de revue** : *ce message, un attaquant en apprend-il quelque chose
qu'un utilisateur légitime n'a pas besoin de savoir ?*

#### D — Le faux positif silencieux

Un contrôle qui **refuse** doit être relu à l'envers : non pas « laisse-t-il
passer le mauvais ? » mais « **écarte-t-il le bon, et le saurait-on ?** »

- Vécu : le filtre de balisage du formulaire lisait `a<b>c` — une comparaison
  écrite en toutes lettres — comme une balise `<b>`. Sur un formulaire de
  prospection, un faux positif écarte un client **et personne ne l'apprend
  jamais**.

**Question de revue** : *quel texte légitime ce motif refuse-t-il, et qu'est-ce
qui le signalerait ?* Asymétrie assumée du dépôt : le doute **marque**, il ne
refuse pas.

#### E — L'affichage qui parle avant de savoir (front)

- `*ngIf="!x"` est faux quand `x` vaut `undefined` parce que la donnée n'est pas
  arrivée. Trois états, trois branches : `=== null` pour « il n'y en a pas »,
  `undefined` pour « on ne sait pas », la valeur sinon.
- **`NG0100`** (une valeur change après la vérification) : rencontré dans une
  trentaine de listes, cause toujours la même — un `loading$.next(true)`
  synchrone dans le constructeur. Parade du dépôt : `queueMicrotask` (12
  fichiers). Voir `docs/web-ng0100.md`. Le travail `e2e-smoke` écoute
  `pageerror` et l'attrape — mais seulement sur les dix routes couvertes.
- **Spinner infini, scintillement, « Chargement… » figé** : trois défauts RxJS
  récurrents — un `shareReplay({refCount: true})` sur un flux repris, un
  `loading` jamais remis à `false` sur la branche d'erreur, un `reload$` déclaré
  `Subject` au lieu de `BehaviorSubject` (donc rien ne part au premier
  abonnement). Convention du dépôt : les lectures mises en cache utilisent
  `refCount: false`, **avec la raison écrite au-dessus**.

**Question de revue** : *que montre cet écran pendant la seconde de chargement,
et que montre-t-il si la requête échoue ?* Si la réponse est « la même chose que
quand il n'y a rien », c'est un défaut.

#### F — Le commentaire, l'ADR ou le texte devenu faux

- Vécu : deux blocs Javadoc empilés sur la même méthode, le premier affirmant
  l'inverse du second — et c'était exactement la croyance périmée qui venait de
  faire échouer un test de bout en bout.
- Les traductions sont **générées** : ne modifiez jamais
  `apps/web/src/locale/messages.*.xlf`. Les textes vivent dans
  `apps/web/scripts/i18n/*.py`. La CI refuse une divergence — mais elle ne voit
  pas un texte français juste et une traduction qui dit autre chose.

**Question de revue** : *le diff change un comportement décrit ailleurs ?* Si oui,
l'ailleurs est dans le périmètre de la PR. `grep -rln '<le sujet>' docs/`.

#### G — L'hypothèse d'exploitation non écrite

Du code qui marche sur la machine où il a été écrit.

- Vécu : le service de contact tient son secret de signature **en mémoire** et
  ses compteurs de débit aussi. Il exige donc `replicas: 1` — et ce n'est pas un
  choix de capacité mais une **exigence de correction**, désormais écrite dans le
  manifeste, à l'endroit où quelqu'un changerait ce nombre. À deux exemplaires,
  un visiteur sur deux verrait « jeton non reconnu » et les plafonds doubleraient
  en silence.
- Un champ mutable dans un `@Service` Spring est partagé entre toutes les
  requêtes — donc entre tous les tenants.
- Le journal d'audit de l'émission 8D est écrit dans une transaction séparée : un
  incident entre les deux laisse un acte non journalisé. Dette connue, assumée,
  écrite (`PASSATION.md` §9).

**Question de revue** : *qu'est-ce que ce code suppose du monde — un seul
exemplaire, un ordre d'exécution, une horloge, un système de fichiers
inscriptible — et où est-ce écrit ?*

#### H — La personne réduite à un identifiant

Règle du dépôt : un responsable, un pilote, un propriétaire se **choisit dans
l'annuaire** (`qos-user-picker`, 44 usages) et s'affiche **par son nom**. Jamais
un UUID à l'écran, jamais un champ de texte libre pour désigner quelqu'un.

### 5.6 Tests, textes, accessibilité, documentation

**Les tests.** La couverture est contrôlée par la CI ; ce qu'elle ne contrôle pas,
c'est **ce qui est couvert**.

| Question | Échec typique |
| --- | --- |
| Le test couvre-t-il le chemin **malheureux** ? | Trois cas, tous dans le chemin heureux : la couverture monte, le risque ne baisse pas |
| Un test **encode-t-il le défaut** plutôt que la règle ? | Un cas réécrit pour passer. Si une PR modifie un test existant, **demandez pourquoi** : c'est le seul endroit où un défaut peut être consacré |
| Un compteur est-il codé en dur ? | `expect(items.length).toBe(7)` casse au huitième élément et n'apprend rien |
| Le cas porte-t-il le nom de la **règle** ? | « devrait marcher » ne documente rien. Ici les noms de cas sont la spécification (§4, question 4) |
| Une correction de défaut a-t-elle son **banc de non-régression** ? | Un correctif sans test se re-cassera |

**Les textes.** Toute chaîne visible porte un `i18n="@@clé"`. Une clé nouvelle
existe dans les tables Python, pas directement dans le `.xlf`.

**L'accessibilité.** Un bouton sans libellé lisible, un `mat-icon-button` sans
`matTooltip` ni `aria-label`, un contraste porté par la couleur seule : la cible
est WCAG 2.2 AA (`CLAUDE.md` §15.1), et aucun travail de CI ne la mesure
aujourd'hui.

**La documentation.** Une fonctionnalité sans entrée wiki n'est pas *Done*
(`CLAUDE.md` §20). Un nouvel indicateur sans définition formelle — formule, seuil,
source, propriétaire — non plus (§18.2 #8).

---

## 6. Écrire la revue

Un constat utile a **trois** parties. Sans la troisième, il se discute au lieu de
se corriger.

1. **Où** : fichier et ligne.
2. **Ce qui est faux** : une phrase, affirmative.
3. **Le scénario qui casse** : des entrées concrètes, et ce qui se produit alors.

> `apps/site-contact/app/api.py`, entrée de la route `/api/contact` — le plafond
> global se consomme avant tout envoi. Douze adresses × 5 requêtes au corps
> illisible, chacune sous la borne par adresse, ferment le formulaire une heure
> pour tout le monde.

(Constat réel du 2026-11-13, cité ici dans sa forme d'origine ; le plafond se
compte désormais juste avant l'envoi.)

Sans le scénario, ça reste une opinion. Avec, ça se vérifie — ou se réfute, ce
qui est aussi bien.

**Trois niveaux, et un seul bloque.**

| Niveau | Définition | Suite |
| --- | --- | --- |
| **Bloquant** | Violation d'un invariant §18.2, perte ou fuite de donnée, défaut atteignable par un utilisateur | Ne fusionne pas |
| **À corriger** | Défaut réel, chemin rare ou dégradation sans perte | Corrigé dans la PR, ou une suite **écrite** avec son échéance |
| **Remarque** | Lisibilité, nommage, dette signalée | Dit une fois, jamais imposé |

**Ne faites pas tout dans une seule revue.** Mieux vaut cinq constats vérifiés
que vingt remarques dont trois comptent : au-delà, l'auteur trie à votre place, et
il triera mal.

**Quand vous ne savez pas, dites-le comme ça.** « Je ne vois pas où le tenant est
filtré ici — est-ce ailleurs ? » est une bonne revue. Une affirmation fausse coûte
plus cher qu'une question : elle se défend, elle fait perdre un aller-retour, et
elle abîme la confiance dans les constats suivants.

**Et relisez votre propre travail avec la même grille.** Les sept écarts du
2026-11-13 ont été trouvés par une relecture à la main de commits déjà verts en
CI, déjà testés, déjà couverts à 100 %. Les quinze travaux de la chaîne n'en
avaient vu aucun — ils n'en voient jamais aucun de cette nature (§1.2).

---

## 7. La fiche d'une page

À garder ouverte pendant la lecture.

**Avant**
- [ ] Lu les messages de commit ; noté leurs affirmations vérifiables
- [ ] `git diff --stat` ; taille relisible, ou constat « découpez »
- [ ] Migrations Flyway repérées et lues **en premier**
- [ ] La CI est verte (sinon la revue attend : on ne relit pas deux fois)

**Sécurité**
- [ ] `tenant_id` du jeton, jamais du corps ; lectures en `…AndTenantId`
- [ ] `@PreAuthorize` avant la lecture du corps
- [ ] Second facteur + ancrage sur acte critique
- [ ] Aucun secret, aucune donnée personnelle dans les journaux
- [ ] Les messages de refus n'enseignent rien
- [ ] Les protections ajoutées ne peuvent pas être retournées (famille B)

**Architecture**
- [ ] `domain/` sans framework ; `application/` sans infrastructure
- [ ] Les modules se parlent par des ports
- [ ] **Nouveau module hexagonal ⇒ règle ArchUnit ajoutée**
- [ ] Pas de logique sectorielle codée en dur
- [ ] Le cas d'usage s'instancie sans base ni contexte Spring

**Les familles maison**
- [ ] Nullable : « on ne sait pas » distingué de « faux » (A)
- [ ] Faux positifs du contrôle envisagés, et signalés quelque part (D)
- [ ] Écrans : état de chargement, état d'erreur, état vide — trois branches (E)
- [ ] Commentaires, ADR, textes mis à jour avec le comportement (F)
- [ ] Hypothèses d'exploitation écrites là où on les changerait (G)
- [ ] Personne = utilisateur de l'annuaire, affiché par son nom (H)

**Tests et finitions**
- [ ] Chemin malheureux couvert ; aucun test affaibli sans raison écrite
- [ ] Non-régression sur chaque défaut corrigé
- [ ] Textes en `i18n`, tables Python et non `.xlf`
- [ ] Clavier et lecteur d'écran pour tout nouveau composant
- [ ] ADR si décision structurante ; wiki si fonctionnalité

**Après**
- [ ] Ouvert l'écran et fait le geste décrit
- [ ] Constats écrits avec leur scénario, et classés

---

## 8. Ce que ce guide ne couvre pas

- **L'exploitation** — voir `docs/runbooks/README.md` : observabilité, retour
  arrière, sauvegardes, stockage objet, annuaire, Edge.
- **La répartition des zones dans une équipe** — `PASSATION-EQUIPE.md`.
- **La revue de l'infrastructure** (chart Helm, manifestes, Terraform). Le travail
  `infra-onprem` en couvre la forme ; le fond — qui peut joindre quoi, quelles
  ressources, quels secrets — demande une grille propre qui reste à écrire.
- **La revue de performance.** `perf-k6` tient les SLO d'API. Une requête N+1
  introduite dans un chemin peu sollicité passera : elle se verra plus tard, sur
  les traces.
- **Le modèle de menace par module** (`CLAUDE.md` §11.1 A04). Il existe pour
  certains modules ; l'étendre est un travail à part, pas une étape de revue.

---

_Maintenu avec le code. Si une grille de ce document se révèle fausse ou
inopérante, la corriger fait partie du travail : un guide de revue qui se trompe
est pire que pas de guide, parce qu'on le croit._

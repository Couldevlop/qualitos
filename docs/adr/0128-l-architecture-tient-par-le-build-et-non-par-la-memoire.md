# ADR 0128 — L'architecture tient par le `build`, et non par la mémoire

- **Statut** : accepté
- **Date** : 2026-11-18
- **Portée** :
  - `apps/api-quality-engine/.../common/GlobalExceptionHandler.java` (2 551 → 335 lignes)
  - `apps/api-quality-engine/.../common/ProblemDetails.java` (nouveau)
  - 69 conseils `*ExceptionHandler` nouveaux, un par module
  - `apps/api-quality-engine/.../arch/HexagonalArchitectureTest.java` (4 règles universelles)
  - 22 services applicatifs : retrait d'un `@Autowired` sans effet
  - `ims` : `GetCoverageMatrixUseCase` et `ImsConfig`
  - `GlobalExceptionHandlerTest` et 4 bancs de conseil nouveaux
  - `scripts/arch/releve.py` (nouveau), `docs/architecture/etat-des-lieux.md` (nouveau)

---

## Le constat

Un relevé mesuré du moteur qualité ([état des lieux](../architecture/etat-des-lieux.md)) a
montré trois choses que la documentation ne disait pas.

**Le socle dépendait de tout ce qui dépendait de lui.** `common` porte ce que tous les modules
utilisent — le client du jeton, l'utilisateur courant, le filtre, le second facteur — et
quatre-vingts modules en dépendent. Il dépendait lui-même de **soixante-six modules**, à cause
d'un seul fichier : `GlobalExceptionHandler`, 2 551 lignes, 225 `@ExceptionHandler`, 205
imports de classes d'exception. Il devait importer l'exception de chaque module pour savoir
quel statut HTTP lui répondre. **Soixante-cinq des soixante-douze cycles** entre modules du
moteur n'existaient que par ce fichier.

**La garde d'architecture était une liste blanche tenue à la main.** `HexagonalArchitectureTest`
énonçait vingt-deux règles, chacune nommant un paquet précis. Sur les cinquante-quatre paquets
`domain` du moteur, **onze** étaient tenus. Un module qui oubliait d'ajouter ses deux règles
n'était jamais vérifié.

**Et l'invariant tenait moins bien que les imports ne le laissaient croire.** Vingt-deux
services applicatifs portaient `@org.springframework.beans.factory.annotation.Autowired` écrit
en **nom pleinement qualifié dans le corps** — invisible à toute mesure qui ne lit que les
imports. ArchUnit, qui lit le bytecode, les voyait tous. C'est le fait le plus instructif du
relevé : une convention qu'on vérifie à la main se contourne sans même le vouloir.

---

## La décision

### 1. Chaque module dit ses propres refus

Soixante-neuf conseils `@RestControllerAdvice`, un par module, remplacent les 206 gestionnaires
de module du socle. Ce n'est pas un patron inventé pour l'occasion : huit modules récents le
suivaient déjà, et sept d'entre eux déclaraient même l'ordre nécessaire. Le socle était
simplement le legs non migré.

`common` ne garde que ce qui ne vient d'aucun module : le client absent du jeton, le second
facteur exigé, les refus que Spring et Jakarta lèvent avant tout code métier, le stockage objet
injoignable — et le filet de sécurité.

**L'ordre est la seule subtilité.** Spring consulte les conseils dans l'ordre et retient le
PREMIER qui possède un gestionnaire applicable. Le socle garde un
`@ExceptionHandler(Exception.class)` qui accepte tout : il est donc `@Order(LOWEST_PRECEDENCE)`
et les conseils de module `@Order(HIGHEST_PRECEDENCE)`. Sans cela, le filet répondrait 500 à la
place de chacun d'eux.

**La portée des conseils reste globale, sans `assignableTypes`.** C'est le point où nous
divergeons du patron existant, et c'est délibéré : une exception CAPA peut être levée pendant
une requête adressée aux écarts — un écart qui ouvre un dossier — et elle doit garder son
statut. Limiter le conseil aux contrôleurs de son module ferait retomber ce cas dans le filet,
en 500.

### 2. La forme d'un refus est écrite une fois

Les 225 gestionnaires répétaient les six mêmes lignes pour ne faire varier que trois valeurs.
`common/ProblemDetails.of(statut, détail, type, titre)` les remplace, et le préfixe
`https://qualitos.io/errors/` ne vit plus qu'à un seul endroit. Des clients routent leur
traitement sur ce champ : qu'il soit toujours bâti pareil est un contrat, pas une élégance.

### 3. Quatre règles universelles, et des dérogations nommées

Aux onze règles nommées — conservées, car **plus strictes** : elles interdisent en plus PDFBox
dans l'instantané 8D, ZXing dans l'export, un module voisin pour les parties intéressées —
s'ajoutent quatre règles qui portent sur `..domain..` et `..application..` **en général** :

1. aucun domaine ne connaît de cadre technique ;
2. aucun domaine ne regarde vers `infrastructure` ni `web` ;
3. aucune couche applicative ne touche un cadre technique ni le web ;
4. **le socle ne dépend d'aucun module**.

La quatrième garde la décision ci-dessus, parce que c'est la plus facile à casser sans y
penser : il suffit d'ajouter un gestionnaire « pour aller plus vite ».

**La portée est le code de production.** Un banc assemble légitimement un adaptateur réel pour
l'éprouver, et peut emprunter le contrôleur d'un autre module pour vérifier un filtre
transverse — `StepUpEndpointTest` se sert du contrôleur des plans de surveillance. Mesurer les
bancs ferait échouer les règles sur des dépendances qui sont précisément le travail d'un banc,
et la seule issue serait d'affaiblir les règles.

### 4. Une exception raisonnée, deux dérogations datées

`org.springframework.transaction.annotation` est **admis** en couche applicative, et nulle part
ailleurs. Décider qu'une opération doit être atteinte ou défaite entièrement est une décision
du **cas d'usage** : `ApiKeyService.rotate` le dit en toutes lettres — émettre une clé peut
échouer après avoir révoqué l'ancienne, et laisser le client sans aucune clé valide. Pousser
cette frontière dans l'adaptateur la retirerait à celui qui sait où elle doit tomber. Ce qui
reste interdit, c'est que le **domaine** la connaisse.

Deux dérogations subsistent, nommées dans la règle avec leur raison : `academy` (ses entités de
domaine *sont* des entités JPA) et `marketplace` (`ManifestScanner` lit un manifeste avec
Jackson). Une dérogation sans raison écrite est une règle qu'on a renoncé à tenir.

### 5. Vingt-deux annotations retirées avec preuve

Les vingt-deux `@Autowired` en nom pleinement qualifié ne servaient à rien : la configuration de
chaque module construit déjà son service d'un `new` explicite, donc Spring n'autocâblait aucun
de ces constructeurs. Chacune n'a été retirée qu'après avoir vérifié qu'une méthode `@Bean` du
dépôt construisait la classe.

Ce garde-fou a servi : deux services d'`academy` ont deux constructeurs, dont un **sans
modificateur** réservé aux bancs. Là, l'annotation désigne celui que Spring doit prendre, et la
retirer empêchait le contexte de démarrer. Elle a été remise, avec le commentaire qui le dit.

---

## Comment on sait que le contrat n'a pas bougé

Un découpage de 225 gestionnaires peut changer silencieusement ce qu'un client reçoit. La
vérification n'a donc pas été laissée aux bancs seuls : le triplet **(statut, fragment d'URL,
titre)** a été extrait du fichier d'origine pour chacune des 197 exceptions, puis réextrait du
code produit, et les deux tables comparées. **Zéro différence.**

Ensuite seulement : `7 218 tests, 0 échec` sur le moteur, dont les 25 règles d'architecture et
les bancs d'intégration qui démarrent un contexte Spring complet — c'est eux qui éprouvent
réellement l'ordre des conseils.

Cinq bancs ont suivi leurs gestionnaires : `GlobalExceptionHandlerTest` appelait directement
les méthodes déplacées, et ne mesure plus que les refus de la plateforme. Un banc de refus de
module placé dans `common` réintroduirait la dépendance que le découpage a supprimée.

---

## Ce que cela change, mesuré

| | Avant | Après |
| --- | --- | --- |
| `GlobalExceptionHandler` | 2 551 lignes, 225 gestionnaires | 335 lignes, 17 gestionnaires |
| Modules dont le socle dépend | 66 | **0** |
| Cycles entre modules | 72 | **7** |
| Arêtes entre modules | 315 | 251 |
| Paquets `domain` tenus par une règle | 11 / 54 | **54 / 54** |
| Fichiers impurs en `domain`/`application` | 18 | 17 (2 dérogations nommées) |

Et surtout : **un module neuf n'a plus de fichier partagé à modifier** pour que ses refus
sortent avec le bon statut. Il écrit son conseil, et c'est tout.

---

## Les alternatives écartées

**Un registre de correspondances dans le socle**, où chaque module déclarerait ses statuts.
Cela aurait aussi inversé la dépendance, mais en remplaçant la résolution de Spring — qui gère
l'héritage d'exceptions, du plus spécifique au plus général — par une table à parcourir à la
main. Remplacer un mécanisme du cadre par le nôtre coûte plus qu'il ne rapporte quand le cadre
fait déjà ce qu'il faut.

**Déplacer les gestionnaires sans toucher aux corps.** C'était le choix le plus sûr, et il
laissait la répétition de six lignes intacte. Le triplet extrait et comparé a rendu la fabrique
aussi vérifiable que le déplacement nu : la sécurité ne tenait pas à l'immobilité du code, elle
tenait à la table.

**Lister les modules déviants dans la règle universelle plutôt que les corriger.** C'était
recréer la liste blanche sous un autre nom. Les vingt-deux `@Autowired` et le `@Service` d'IMS
ont donc été corrigés ; seuls `academy` et `marketplace` restent, parce que ce sont de vrais
chantiers et non des oublis.

**Ajouter Spring Modulith**, que `CLAUDE.md` §10.2 inscrit dans la pile imposée et qui
n'apparaît dans aucun `pom.xml`. C'est une décision distincte — soit on l'ajoute et il vérifie
les frontières de modules que ces règles ne couvrent pas encore, soit on retire la ligne de la
spécification. Laisser l'écart, c'est laisser la spécification mentir ; le trancher ici
mélangerait deux sujets.

---

## La dette assumée

- **Les quarante-sept modules plats restent plats**, et c'est le cœur du produit : les cinq
  méthodes, les écarts, les CAPA, le Standards Hub. 113 des 182 entités JPA vivent hors d'une
  couche `infrastructure`, donc l'objet que JPA charge *est* l'objet qui porte les règles.
  Aucune règle automatique ne les en sortira : il faut, module par module, séparer entité de
  persistance, entité de domaine et convertisseur. Ce qui est acquis, c'est qu'un module
  **neuf** ne peut plus y entrer sans que le `build` le dise.
- **Les sept cycles restants** sont de vrais allers-retours du métier, cinq passant par
  `nonconformity`. Les rompre demande un port de chaque côté, et c'est une décision à prendre
  paire par paire : toutes ne méritent pas d'être rompues.
- **`api-core` porte le même défaut, en plus petit, et n'est pas traité.** Son
  `GlobalExceptionHandler` fait 435 lignes, 28 gestionnaires et 11 imports de modules, et cette
  application n'a *aucune* règle d'architecture. Le même découpage y a été tenté dans la foulée :
  les corps de ses gestionnaires ont d'autres formes, la table (statut, URL, titre) extraite avant
  et après ne concordait pas, et le socle gardait ses imports. Le travail a donc été défait plutôt
  que livré à côté d'une transformation vérifiée. C'est un chantier à part, de la taille d'une
  demande.
- **Soixante-neuf fichiers de plus** dans le dépôt. C'est le prix de la décision, et il est
  assumé : un fichier de vingt lignes par module coûte moins à lire qu'un fichier de 2 551
  lignes que tout le monde doit modifier.

L'ordre dans lequel prendre la suite est en [§7 de l'état des
lieux](../architecture/etat-des-lieux.md).

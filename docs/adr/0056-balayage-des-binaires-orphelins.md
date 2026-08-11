# ADR 0056 — Balayage des binaires orphelins du stockage objet

- **Statut** : Accepté
- **Date** : 2026-08-11
- **Owners** : @Couldevlop

## Contexte

Les preuves CAPA (ADR 0050) et les photos de non-conformité déposent des binaires dans
le stockage objet. Les deux services suivent le même ordre, délibérément : **la ligne
de métadonnées d'abord, l'objet ensuite** au dépôt ; l'inverse au retrait. Cet ordre
garantit qu'aucune ligne ne pointe jamais vers un objet absent — le cas qui casserait
un écran, et le plus visible.

Il laisse ouverte la situation symétrique. L'objet est écrit, puis la transaction
échoue : validation refusée en aval, journal d'audit indisponible, pod tué entre les
deux. La ligne disparaît, **l'objet reste**. Rien, dans la base, ne dit alors qu'il
existe : il est introuvable par l'application, invisible à tout écran — et facturé
indéfiniment. Sur des photos prises au téléphone en atelier, quelques milliers
d'échecs finissent par peser.

Aucun mécanisme ne les rattrapait. Le port `ObjectStorage` ne savait même pas
énumérer : il exposait `put`, `presignGet`, `delete`. Un orphelin était donc, au sens
strict, **indétectable**.

## Décision

1. **`ObjectStorage.list(prefix, limit)`** — énumération bornée, renvoyant
   `{key, lastModified, sizeBytes}`. La limite est **exigée**, pas suggérée.
2. **Point d'extension `StoredObjectOwner`** — chaque module qui dépose des binaires
   déclare ce qu'il revendique encore (`isReferenced(key)`). Deux implémentations :
   preuves CAPA, photos de NC.
3. **`OrphanObjectSweeper`** — réconciliation périodique : ce que le stockage contient
   sous `tenants/`, moins ce que les propriétaires revendiquent, moins ce qui est trop
   récent, est supprimé.
4. **Trois garde-fous, non négociables** :
   - **OFF par défaut** (`qualitos.storage.orphan-sweep.enabled=false`) ;
   - **délai de grâce** de 24 h par défaut, **refusé au démarrage sous 1 h** ;
   - **aucune suppression en cas de doute** : si un propriétaire lève une exception,
     l'objet est laissé en place et l'échec est compté.
5. **Aucun propriétaire déclaré ⇒ balayage annulé**, avec un avertissement.
6. **Balayage limité au préfixe `tenants/`** — le bucket peut être partagé.
7. **Ordonnanceur quotidien**, désactivé en profil `test`, exceptions rattrapées.
8. **Chaque suppression est journalisée** avec sa clé et sa taille.

## Justification

**Pourquoi une réconciliation et non une compensation.** Supprimer l'objet dans un
`catch` supposerait que le processus survive à l'incident — ce qu'un pod tué ne fait
précisément pas, et c'est l'un des cas les plus fréquents. Un `try/finally` ne couvre
pas davantage la panne d'infrastructure. Le seul mécanisme qui rattrape *tous* les cas
est une comparaison périodique entre ce que le stockage contient et ce que la base
revendique.

**Pourquoi un point d'extension plutôt que deux dépôts câblés en dur.** Câbler le
balayeur sur `capa_evidences` et `nc_photos` fonctionnerait aujourd'hui. Le jour où un
troisième module déposerait un binaire, le balayeur **effacerait ses fichiers en
silence**, faute de savoir qu'ils existent — un oubli qui ne se voit qu'après coup,
quand la pièce a disparu. Déclarer son appartenance devient donc la condition
d'entrée : qui ne se déclare pas se fait effacer, et c'est une règle qu'un développeur
rencontre en écrivant son module, pas en lisant le journal d'un incident.

**Pourquoi un délai de grâce, et pourquoi 24 h.** Entre le `put` et la validation de
la transaction, l'objet existe sans sa ligne : il est indiscernable d'un orphelin. Le
supprimer effacerait une preuve **au moment même où on la verse**. Quelques minutes
suffiraient en théorie ; 24 h ne coûtent rien (un orphelin ne bouge pas) et couvrent
les transactions longues, les horloges décalées entre le serveur d'application et le
stockage, et les rejeux. Le plancher d'une heure est refusé **au démarrage** plutôt
qu'ignoré : une valeur dangereuse découverte en production l'est une fois les octets
perdus.

**Pourquoi OFF par défaut.** C'est le seul défaut défendable pour une opération qui
efface des octets qu'aucune sauvegarde applicative ne rappellera. Le même principe
gouverne déjà le stockage objet lui-même, le relais Kafka et la brique courriel.
L'exploitant doit l'allumer sciemment, après avoir vérifié que son bucket ne sert
qu'à cette plateforme.

**Pourquoi ne rien supprimer quand un propriétaire échoue.** Une panne de base se
traduirait sinon par « personne ne revendique cet objet » — c'est-à-dire par la
destruction de preuves au pire moment possible. L'exception est donc laissée remonter,
comptée, et l'objet conservé. Le balayage se rattrape tout seul au passage suivant.

**Pourquoi un plafond par passage.** Un balayage est une réconciliation, pas une
urgence. Plusieurs petits passages valent mieux qu'un seul qui énumère un bucket
entier et tient une connexion ouverte pendant ce temps.

**Pourquoi aucun verrou entre répliques** — contrairement au rappel d'audit
(ADR 0053). Supprimer un objet déjà supprimé est sans effet : le `DELETE` S3 est
idempotent. Deux répliques qui balaient en même temps ne produisent qu'un décompte un
peu généreux dans le journal. Ajouter un verrou coûterait une dépendance pour un
risque inexistant.

**Pourquoi journaliser la clé.** C'est un chemin de stockage, pas une donnée
personnelle (§22-9). Sans elle, une suppression injustifiée serait impossible à
instruire après coup — or c'est exactement la question qu'on se posera le jour où elle
surviendra.

## Conséquences

- ✅ Les binaires orphelins cessent de s'accumuler, et cessent d'être invisibles.
- ✅ Le port de stockage sait énumérer — capacité réutilisable (inventaire, migration).
- ✅ Un nouveau module qui dépose des binaires a un endroit évident où se déclarer.
- ⚠ **Un module qui oublie d'implémenter `StoredObjectOwner` verra ses binaires
  effacés** dès que le balayage est activé. C'est le prix du point d'extension, et
  c'est pourquoi l'interface le dit explicitement dans sa documentation.
- ⚠ **Rien n'est actif par défaut** : sans activation explicite, le problème d'origine
  demeure. Assumé — mieux vaut un dispositif éteint qu'un dispositif qui efface sans
  qu'on l'ait voulu.
- ⚠ **Le balayage ne distingue pas les tenants** : il s'exécute hors requête, sans
  `TenantContext`, comme l'ordonnanceur de rappel. La clé porte le tenant ; aucune
  décision ne dépend d'un contexte ambiant.
- ⚠ `S3ObjectStorage.list` déroule la pagination à la main. Le paginateur du SDK
  parcourt le bucket de façon paresseuse et une interruption laisserait une connexion
  ouverte ; ici on sait exactement combien de pages on demande.

## Tests d'invariant

`OrphanObjectSweeperTest` — les tests portent d'abord sur ce que le balayeur **refuse**
de supprimer : balayage éteint, stockage éteint, aucun propriétaire déclaré, objet trop
récent, objet revendiqué, propriétaire en panne, objet hors du préfixe `tenants/`. Puis
sur ce qu'il supprime : orphelin âgé, décompte des octets récupérés, arrêt au plafond
de lot, et le fait qu'un échec n'interrompe pas le passage. Un test vérifie enfin que
les propriétaires ne sont **pas** interrogés pour un objet trop récent — la décision
est déjà prise par la date.

`OrphanSweepPropertiesTest` — OFF par défaut, grâce de 24 h par défaut, plancher d'une
heure accepté, cinq minutes refusées, `null` refusé, lot non positif refusé, lot plafonné.

## Références

- CLAUDE.md §4.3 (Non-conformance — photos), §18.2-3, §22-9
- [ADR 0050](./0050-preuves-jointes-capa.md) — preuves jointes CAPA (ordre ligne/objet)
- [ADR 0053](./0053-rappel-echeance-audit-et-brique-courriel.md) — même forme
  d'ordonnanceur mince ; la comparaison sur le verrou entre répliques y est explicitée

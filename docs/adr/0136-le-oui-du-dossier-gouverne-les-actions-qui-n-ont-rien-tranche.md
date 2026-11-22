# ADR 0136 — Le « oui » du dossier gouverne les actions qui n'ont rien tranché

- **Statut** : accepté
- **Date** : 2026-11-22
- **Révise** : ADR 0131 §filtre du plan d'efficacité (précise, ne renverse pas)
- **S'appuie sur** : ADR 0073, ADR 0108 (la vérification du dossier), ADR 0131 (la vérification de l'action), ADR 0081 (la portée)

## Contexte

La décision de vérifier l'efficacité vit à **deux niveaux** :

- sur le **dossier** — `capa_cases.verification_required`, posé par l'ADR 0073 puis
  repris par l'ADR 0108 pour le verdict ;
- sur l'**action** — `capa_actions.verification_required`, ajouté par l'ADR 0131
  et la migration V177.

L'ADR 0131 a voulu que le plan d'efficacité travaille au niveau de l'action : il
est juste qu'une action close sans effet mesurable n'encombre pas la file de
celui qui doit juger. Le filtre écrit pour cela n'admettait un dossier que si
**une de ses actions** portait `verification_required = true` :

```
and exists (select a.id from CapaAction a
            where a.capa = c and a.verificationRequired = true)
```

Deux chemins d'écriture courants n'alimentent pourtant **que** le niveau du
dossier :

1. **La fiche CAPA.** `CapaService.updateCase` n'appelle `appliquerVerification`
   que sur le dossier et ne touche aucune action. Répondre « oui » là laisse
   toutes les actions à `NULL`.
2. **Tout l'historique.** La migration V177 a créé les colonnes de l'action
   **sans reprise**, et elle le dit : « `NULL` veut dire « la question n'a pas
   été tranchée pour cette action », ce qui est l'état exact de toutes les lignes
   existantes ». Aucune migration ultérieure ne les reprend.

Conséquence observée en production : on répond « oui » à la vérification
d'efficacité, le dossier reste visible au registre, et **il ne paraît jamais au
plan d'efficacité**. L'écart est muet — rien n'indique que la ligne a été
écartée, ni pourquoi.

Le banc d'essai ne pouvait pas l'attraper : son fabricant de dossiers pose le
drapeau **à la main sur chaque action**, et tous les cas antérieurs ont été
migrés vers ce fabricant. Le banc éprouvait donc exactement le chemin qui
fonctionne, et aucun de ceux qui cassent. Un banc qui pose lui-même la condition
qu'il vérifie ne vérifie rien.

## Décision

**`NULL` sur une action signifie « la question n'a pas été tranchée pour cette
action », et non « non ».** Quand **aucune** action n'a tranché, c'est la
décision **du dossier** qui gouverne. C'est la lettre de l'ADR 0131, appliquée à
la lecture et non seulement à l'écriture.

Le prédicat du plan devient, dans les deux requêtes — celle qui voit tout et
celle que borne la portée (ADR 0081) :

```
and (exists (select a.id from CapaAction a
             where a.capa = c and a.verificationRequired = true)
     or (c.verificationRequired = true
         and not exists (select a.id from CapaAction a
                         where a.capa = c and a.verificationRequired is not null)
         and exists (select a.id from CapaAction a
                     where a.capa = c and a.actionType <> CONTAINMENT)))
```

**Les deux requêtes, et non une seule** : un plan qui changerait selon le droit
du lecteur serait pire que le défaut qu'on corrige.

Trois bornes empêchent ce repli de trop ramener.

1. **Un « non » explicite sur une action prime.** Dès qu'une action porte une
   réponse — même « non » —, c'est elle qui vaut : quelqu'un a jugé, pour cette
   action, qu'il n'y avait rien à mesurer. Faire remonter le dossier malgré ce
   « non » rendrait la réponse de l'action sans effet, et on ne saurait plus à
   quoi sert de la poser.
2. **Il faut une action qui ne soit pas un endiguement.** Le confinement arrête
   l'effet sans supprimer la cause (ISO 9001 §10.2) : il ne promet rien à
   mesurer, et le domaine lui refuse déjà toute vérification. Un dossier qui n'a
   que cela reste dehors, comme avant.
3. **Le dossier doit lui-même dire « oui ».** Sans « oui » nulle part, il n'y a
   rien à juger.

## Ce qui a été écarté

**Une migration de reprise** qui descendrait `capa_cases.verification_required`
sur les actions non-confinement restées à `NULL`, et une propagation de
`updateCase` vers les actions. C'est plus propre en lecture — un seul niveau à
interroger — mais l'ADR 0131 **refuse explicitement** cette reprise : recopier la
décision du dossier sur une action affirmerait qu'elle a été tranchée pour cette
action, alors qu'elle ne l'a pas été. Renverser ce point demande un arbitrage
métier, pas un correctif. Le repli en lecture, lui, ne prétend rien : il dit que
faute de décision locale, la décision globale s'applique — ce qui est vrai.

## Conséquences

- Les dossiers dont le « oui » n'était écrit que sur la fiche **reparaissent** au
  plan, y compris tout l'historique antérieur à V177. Sur un client au long
  passé, le plan peut s'allonger d'un coup : c'est le rattrapage d'un oubli, pas
  une régression.
- Le niveau de l'action reste **prioritaire** : l'ADR 0131 n'est pas renversé,
  il est précisé. Dès qu'une action tranche, le repli ne s'applique plus.
- **Règle de banc, à tenir** : les bancs du plan d'efficacité ne doivent plus
  poser `verification_required` à la main sur une action pour fabriquer un cas
  d'entrée. Ils doivent partir des états que les **vrais chemins d'écriture**
  produisent. C'est ce manquement, et non le filtre, qui a laissé le défaut
  passer en production.

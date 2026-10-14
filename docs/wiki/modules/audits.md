# Module Audits

[← Retour à l'index](../README.md) · Route : **`/audits`** · Menu : *Qualité opérationnelle › Audits*

## À quoi sert ce module

Le module **Audits** permet de programmer et conduire des audits (internes, fournisseurs,
préparation de certification), de consigner les **constats** (findings) et de produire des
**rapports**.

## Parcours pas à pas

1. **Ouvrir** `/audits` : la liste des audits planifiés et réalisés.
2. **Créer / programmer un audit** : périmètre, référentiel visé, **auditeur responsable**,
   date, et — facultatif — l'adresse à prévenir avant l'échéance (voir *Planning et
   rappels* ci-dessous). L'auditeur responsable est celui qui conduit l'audit et signe
   le rapport : il se choisit dans l'annuaire de votre organisation, **vous par défaut**.
   Si vos droits ne permettent pas de lire l'annuaire, l'audit vous est confié et l'écran
   le signale. Son nom est enregistré avec l'audit et s'affiche sur la fiche.
   L'audit reçoit à sa création une **référence** de la forme `AUD-2026-0001` : c'est
   par elle qu'on le cite en revue de direction, dans une convocation ou devant un
   organisme certificateur. Elle est propre à votre organisation, repart à `0001`
   chaque année, et **ne change jamais** — contrairement au titre, qui se corrige.
3. **Suivre les échéances** sur `/audits/planning` (menu *Qualité opérationnelle ›
   Planning audits*).
4. **Conduire l'audit** : ajouter au moins une question à la checklist, **démarrer**
   l'audit, puis répondre aux questions et consigner les constats et leur criticité.
   Un constat ne se relève que **pendant** l'audit : tant qu'il est planifié, le bouton
   *Ajouter un constat* est grisé, la raison est écrite juste en dessous, avec un bouton
   *Démarrer l'audit*. L'auteur du constat est l'utilisateur connecté.
5. **Rédiger et émettre le rapport** (section *Rapport*, sous les constats) : synthèse
   — que l'IA peut proposer, à relire —, points forts, axes d'amélioration et conclusion
   (*certification recommandée / avec réserves / non recommandée* pour un audit de
   certification ; *favorable / avec réserves / défavorable* sinon). Les chiffres clés
   (constats par type, taux de conformité) se calculent tout seuls. Le brouillon se
   retouche à volonté ; **Émettre le rapport** n'est possible qu'une fois l'audit
   clôturé, demande votre code à usage unique, et **fige** le rapport : texte,
   conclusion, chiffres, date et émetteur. L'émission et l'empreinte du contenu sont
   inscrites au journal d'audit.
6. **Joindre des documents** (section *Documents*, bouton *Ajouter un document*) :
   convocation, plan d'audit, photos, relevés… PDF, image, Word, Excel ou PowerPoint,
   10 Mo par fichier, 20 documents par audit. Un clic sur le nom ouvre le fichier.
   Le retrait demande confirmation et reste tracé ; il est réservé au manager qualité
   et aux administrateurs. Une fois le rapport émis, les documents sont figés avec lui.
7. **Traiter les écarts** : chaque constat peut donner lieu à une [CAPA](capa.md) ou une
   [non-conformité](non-conformites.md).

## Planning et rappels

L'écran **Planning audits** (`/audits/planning`) montre les audits *planifiés* du plus
proche au plus lointain, **retards compris** : un audit dont la date est passée sans qu'il
ait été lancé reste en tête de liste, avec son nombre de jours de retard. Deux filtres :
le **type** d'audit et l'**horizon** (30, 90, 180 ou 365 jours). Un clic sur une ligne
ouvre l'audit.

Le décompte affiché (« Dans 12 jours », « 3 jours de retard ») est calculé par le serveur.
Il ne dépend donc ni de l'heure ni du fuseau de votre poste : tous les utilisateurs d'un
même espace voient la même échéance.

**Rappel automatique — 30 jours avant l'échéance :**

- une **notification dans l'application** part vers le pilote de l'audit et vers l'audité ;
- un **courriel** part en plus, *si* une adresse a été renseignée sur le plan d'audit *et*
  si votre administrateur a configuré l'envoi de courriels. Sans adresse, le rappel reste
  dans l'application — il fonctionne quand même.

Le rappel ne part **qu'une fois** par audit ; la colonne *Rappel* indique s'il est déjà
parti. Repousser la date d'un audit ne le réarme pas : si l'échéance change après le
rappel, prévenez les participants vous-même.

> Le délai de 30 jours, la configuration du serveur de courriel et l'adresse d'expédition
> sont réglés par l'administrateur de la plateforme, sans redéploiement.

## Liens avec les autres modules

- En préparation de certification, l'**audit blanc** du [Standards Hub](standards-hub.md) aide à
  cibler les clauses à risque avant l'audit officiel.
- Les écarts non levés alimentent les [CAPA](capa.md) et les indicateurs (`/kpis`).

## Bonnes pratiques

- **Indépendance** : l'auditeur constate, le responsable du périmètre corrige.
- **Criticité constante** : qualifiez les écarts (mineur / majeur) selon des critères stables.
- **Bouclez la levée** : un constat n'est clos que lorsque sa preuve de levée est enregistrée.

## Pour aller plus loin

- Rôle dédié : [Auditeur](../roles/auditeur.md).

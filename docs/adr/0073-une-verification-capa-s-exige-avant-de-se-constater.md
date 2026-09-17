# ADR 0073 — Une vérification CAPA s'EXIGE avant de se constater ; ne rien dire n'est pas dire « non »

- **Statut** : Accepté
- **Date** : 2026-09-17
- **Owners** : @Couldevlop
- **Portée** : Module CAPA (modèle du dossier, règles de mise à jour, dialogue
  d'édition, fiche)
- **S'inscrit dans** : la vérification d'efficacité CAPA (PR #153, migration V132)

## Contexte

Le dossier CAPA portait déjà `effectiveness_verified` : le **constat** qu'une
vérification a eu lieu. Il lui manquait ce qui vient avant — l'**exigence**.

Sans elle, deux dossiers très différents se ressemblaient à l'écran :

- celui dont on a **délibérément décidé** qu'il ne demandait pas de vérification
  (action évidente, effet immédiat, mesure déjà couverte ailleurs) ;
- celui qu'on a simplement **oublié** de vérifier.

Les deux affichaient « non vérifié ». C'est pourtant la première question d'un
auditeur — *« comment savez-vous que cette action a marché ? »* — et la réponse
« on a jugé que ce n'était pas nécessaire » n'est défendable que si elle a été
écrite au moment où on l'a prise. Reconstituée après coup, elle ne vaut rien.

Il manquait aussi le **qui** : une vérification qu'on exige sans la confier à
quelqu'un n'aura pas lieu.

## Décisions

### 1. `NULL` n'est pas `false` — l'état est à trois valeurs, pas à deux

`verification_required` est un `BOOLEAN` **nullable**, et les trois états sont
porteurs de sens :

| Valeur | Ce que ça dit |
| --- | --- |
| `NULL` | la question n'a pas été tranchée |
| `TRUE` | une vérification est exigée |
| `FALSE` | on a décidé qu'il n'en fallait pas — **c'est une décision, pas un vide** |

**Alternative écartée : `BOOLEAN NOT NULL DEFAULT FALSE`.** C'était plus simple,
et c'est exactement ce qui aurait ruiné la fonctionnalité. Les milliers de
dossiers ouverts avant ce lot n'ont pris **aucune** décision sur le sujet ; leur
en prêter une par défaut aurait rempli la base d'un « non » que personne n'a
prononcé — et aurait rendu indistinguables, dès la migration, les deux cas que
cette fonctionnalité existe précisément pour distinguer.

Conséquence assumée à l'écran : la fiche n'affiche la ligne **que si** la
question a été tranchée. Un dossier sans décision n'annonce rien.

### 2. La règle « exigée ⇒ assignée » est au SERVICE, pas dans la base

Une contrainte `CHECK` aurait été tentante. Elle ne tient pas : `NULL` y signifie
« pas encore décidé », et une contrainte devrait alors distinguer trois cas dont
l'un est une absence — on obtient une expression que personne ne relit
correctement six mois après.

`CapaService.appliquerVerification` porte donc les trois gardes, toutes en **422** :

1. **Exiger sans désigner** — refusé. Une vérification que personne ne doit faire
   n'aura pas lieu ; enregistrer l'intention sans titulaire produirait un dossier
   qui se croit couvert.
2. **Désigner sans exiger** — refusé. C'est presque toujours une case oubliée, et
   l'accepter en silence donnerait le même dossier faussement couvert.
3. **Ne plus exiger efface le reste** — passer à « non » retire le vérificateur et
   les consignes. Les laisser traîner laisserait croire qu'une vérification est
   attendue alors qu'on a décidé le contraire.

La mise à jour reste **partielle** : `verificationRequired` absent de la requête
laisse tout en l'état. Ne rien dire n'est pas dire « non » — au serveur comme à
l'auditeur.

### 3. Le nom du vérificateur est RECOPIÉ à côté de son identifiant

`verification_assignee_id` (UUID de l'annuaire `api-core`, `/api/v1/users`) **et**
`verification_assignee_name` (libellé figé).

**Alternative écartée : ne garder que l'identifiant et résoudre le nom à
l'affichage.** C'est la forme normalisée, et elle était le premier réflexe. Mais
un dossier CAPA se relit des années plus tard, souvent dans un audit, et le compte
du vérificateur aura pu être désactivé ou supprimé entre-temps. La fiche aurait
alors affiché un UUID — ou pire, rien. Même parti que l'instantané de texte des
documents scellés (ADR 0071) : ce qui doit rester lisible se fige.

### 4. Un annuaire muet se DIT, il ne se déguise pas en liste vide

Si `/api/v1/users` ne répond pas, le dialogue affiche un message explicite
(`annuaire-indisponible`) au lieu d'une liste déroulante vide. Une liste vide
ferait croire à l'utilisateur que son organisation ne compte personne, et il
chercherait le problème du mauvais côté. Même règle d'or que l'agrégation
(PASSATION §2.5) : **une source absente le dit**.

### 5. Un index PARTIEL, parce que la seule lecture fréquente est une liste de travail

```sql
CREATE INDEX idx_capa_verification_a_faire
    ON capa_cases (tenant_id, verification_assignee_id)
    WHERE verification_required = TRUE
      AND effectiveness_verified IS NOT TRUE;
```

L'index n'existe que pour « les dossiers dont la vérification m'incombe et reste
à faire ». Il n'indexe pas les dossiers pour lesquels la question ne se pose pas —
c'est-à-dire la grande majorité.

## Conséquences

- ✅ Migration **V132** : quatre colonnes facultatives, l'index partiel, et les
  commentaires de colonnes qui portent la sémantique de `NULL`.
- ✅ La distinction « pas vérifié » / « décidé non vérifiable » est enfin dans la
  donnée, donc dans l'export et le journal d'audit, pas seulement à l'écran.
- ⚠ Un bloc qui **apparaît conditionnellement** attache ses validateurs pendant le
  cycle de rendu : ce lot a coûté quatre passes de `NG0100`. Le remède retenu est
  celui déjà en place ailleurs dans le dépôt — `deferredView`, et **toute** mutation
  du formulaire hors du cycle de rendu. La cause finale n'était d'ailleurs pas le
  `mat-error` soupçonné, mais `[submitDisabled]="form.invalid"`, qui bascule au
  moment même où le validateur s'attache.
- ⚠ La règle vivant au service, un futur accès direct à la base (import, script de
  reprise) peut écrire un état incohérent. C'est le prix de la lisibilité ; tout
  passage par l'API est couvert.

## Tests d'invariant

- `CapaServiceTest` — les trois gardes, chacune sur son cas de refus, plus la mise
  à jour partielle qui ne décide rien.
- `capa-edit-dialog.component.spec.ts` — apparition et disparition du bloc,
  annuaire indisponible annoncé.
- `apps/web/e2e/capa-verification.spec.ts` — le parcours réel : les deux réponses
  offertes, « oui » qui demande à qui et quoi, le retour à « non » qui referme, et
  un « non » enregistré qui se relit sur la fiche. Le banc écoute `pageerror` :
  `NG0100` est une erreur en mode développement, une régression le referait échouer.

## Références

- `CLAUDE.md` §4.2 (CAPA), §18.2 #2 (tenant depuis le JWT), §20 (Definition of Done)
- ADR 0071 — le principe « ce qui doit rester lisible se fige »
- `docs/PASSATION.md` §2.2 (le motif du service), §2.5 (une source absente le dit)

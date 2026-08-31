# ADR 0065 — L'autorisation se décide avant que le corps de la requête ne soit lu

- **Statut** : Accepté
- **Date** : 2026-08-31
- **Owners** : @Couldevlop
- **Portée** : couche web du moteur qualité (tous les contrôleurs portant `@PreAuthorize`)
- **Réfs** : OWASP A01 (Broken Access Control), ASVS V4, CLAUDE.md §11.1, §22.1

## Contexte

Constaté sur la préproduction le 2026-08-31, en confrontant les réponses réelles
aux règles déclarées.

Un appelant authentifié **sans habilitation d'écriture** qui envoyait un corps
malformé sur `PUT /api/v1/fmea/rating-scales/{kind}` recevait **400**, non 403 :

```
PUT … {"rows":[]}     par quality_manager → 400 Bad Request
PUT … <corps complet> par quality_manager → 403 Forbidden
```

La règle était juste — c'est son **moment d'application** qui ne l'était pas.
Spring MVC résout et valide les arguments d'une méthode de contrôleur, dont
`@Valid @RequestBody`, dans l'adaptateur de handler : **avant** d'invoquer la
méthode, donc avant l'advice qui porte `@PreAuthorize`.

Trois conséquences, par ordre de gravité :

1. **La réponse varie selon ce que l'appelant envoie.** Un point d'entrée qui lui
   est fermé devient un oracle : en faisant varier sa charge utile, il apprend la
   forme attendue, les champs obligatoires, les bornes. C'est de l'information sur
   un endroit où il n'a rien à apprendre.
2. **Un corps hostile est désérialisé et parcouru** par une requête qui n'aurait
   jamais dû être lue. Tout ce qui se passe avant l'autorisation est du travail
   offert à quelqu'un qui n'y a pas droit.
3. Le refus lui-même est moins clair pour qui débogue de bonne foi : un 400 fait
   chercher l'erreur dans la charge utile, jamais dans les droits.

Le comportement n'était pas uniforme : `POST /api/v1/tenant-modules` répondait
bien 403, parce qu'il est **aussi** déclaré dans la chaîne de filtres, laquelle
s'exécute bien avant MVC. Le contraste dit exactement où est la cause.

## Décisions

### 1. On ne duplique pas les règles dans la chaîne de filtres

La correction évidente — déclarer chaque endpoint sensible dans
`SecurityConfig` — remet l'ordre d'aplomb, au prix d'une **seconde source de
vérité** : soixante-dix règles de rôle recopiées, deux endroits à tenir
d'accord. Elles divergeraient au premier endpoint ajouté, et **un écart entre
les deux copies est précisément la faille qu'on prétend fermer**.

La chaîne de filtres garde donc ce pour quoi elle est faite : les règles
**transverses** (méthode HTTP, familles de chemins, suppressions génériques).
Les règles **propres à un point d'entrée** restent là où on les lit, sur la
méthode.

### 2. La règle existante est évaluée plus tôt, pas réécrite

Un `HandlerInterceptor` (`MethodAuthorizationPreCheckInterceptor`) lit le
`@PreAuthorize` de la méthode visée et l'évalue dans `preHandle`, qui s'exécute
**avant la résolution des arguments**. Une seule déclaration, deux moments
d'application.

L'évaluation est déléguée à `WebExpressionAuthorizationManager`, le moteur
d'expressions de Spring Security lui-même. Réécrire `hasAnyRole` à la main aurait
rendu possible un désaccord sur le préfixe `ROLE_` ou sur les alias de rôle
(`quality_director` / `DIRECTOR_QUALITY`, cf. ADR 0020) — c'est-à-dire **deux
verdicts d'autorisation différents pour un même jeton**, ce qui serait pire que
le défaut corrigé.

### 3. L'intercepteur ne peut que refuser plus tôt — invariant de conception

`@PreAuthorize` **reste en place et s'exécute ensuite**. L'intercepteur ne sait
que jeter `AccessDeniedException` ; il n'a aucun chemin qui autorise. Une erreur
dans son évaluation ne peut donc pas ouvrir un accès : au pire elle en ferme un,
et le banc de test le voit immédiatement.

C'est cette propriété qui permet de l'ajouter à un système **en service** sans en
refaire l'audit d'autorisation.

Trois abstentions explicites, toutes du côté sûr :

- **Aucune authentification** → il se tait. « Je ne sais pas qui tu es » n'est pas
  « tu n'as pas le droit » : c'est à la chaîne de filtres de rendre 401, et se
  prononcer ici rendrait 403 à sa place.
- **Expression liée aux arguments** (`#id`, `returnObject`, `filterObject`) → non
  pré-évaluable, puisque ses arguments n'existent pas encore et que les faire
  exister demanderait de lier le corps — exactement ce qu'on évite.
  `@PreAuthorize` l'applique comme avant. Le moteur qualité n'en compte aucune
  aujourd'hui ; la garde existe pour que la première qui apparaîtra échoue en
  « pas de pré-contrôle », jamais en « autorisé ».
- **Expression que le moteur web ne sait pas compiler** → même traitement.

### 4. Placement et coût

Web, donc couche web : `config`, à côté de `SecurityConfig`, sans aucune
dépendance vers le domaine ni l'application — l'invariant hexagonal du dépôt est
inchangé, et il est vérifié par `HexagonalArchitectureTest`.

L'intercepteur ne s'applique qu'à `/api/**`. Les expressions compilées sont
mémorisées par méthode de contrôleur : recompiler la même SpEL à chaque requête
coûterait sur un chemin chaud (SLO §20 : p95 < 300 ms) ce que l'annotation, elle,
ne change jamais.

## Conséquences

- ✅ Un appelant sans habilitation reçoit **403 quel que soit son corps** : la
  réponse ne dépend plus de ce qu'il envoie.
- ✅ Aucun corps non autorisé n'est plus désérialisé ni validé.
- ✅ **Zéro règle dupliquée** : `@PreAuthorize` reste l'unique déclaration.
- ✅ Le correctif porte sur les **soixante-dix** points d'entrée annotés du
  moteur, pas seulement sur celui où le défaut a été constaté.
- ✅ Pour qui a le droit d'écrire, **rien ne change** : un corps invalide reste un
  400. Sans quoi le pré-contrôle masquerait les erreurs de saisie derrière un
  refus d'accès.
- ⚠ Un `@PreAuthorize` qui parlerait des arguments de sa méthode ne serait pas
  pré-évalué et retrouverait l'ancien ordre. La règle à tenir : **une règle
  d'autorisation qui dépend du corps de la requête ne peut pas protéger la
  lecture de ce corps.** Ce qui doit être décidé avant la lecture doit se
  décider sur le jeton seul.
- ⚠ `api-core` n'est pas couvert : il a sa propre couche web. À reprendre si le
  même écart s'y constate.

## Vérifications

- `MethodAuthorizationPreCheckInterceptorTest` — refus avant lecture, laissez-passer
  des rôles admis, abstention sans authentification, abstention sur expression
  liée aux arguments, précédence méthode > classe, et le balayage qui tient
  l'invariant : aucune porte ouverte à un simple utilisateur.
- `FmeaScaleControllerTest` — sur l'endpoint réel : corps malformé + rôle
  insuffisant ⇒ **403** (et le service n'est jamais appelé) ; corps malformé +
  rôle suffisant ⇒ **400**, inchangé.

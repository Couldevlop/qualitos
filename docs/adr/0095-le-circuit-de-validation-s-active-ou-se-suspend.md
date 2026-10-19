# ADR 0095 — Le circuit de validation s'active ou se suspend

- **Statut** : Accepté
- **Date** : 2026-10-20
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`circuit`, migration V144), écrans
  `/admin/circuits` et panneau de la GED
- **Complète** : ADR 0080 (circuits de validation par client), ADR 0094 (GED)

## Contexte

L'ADR 0080 fait dépendre le mode de validation de la présence d'étapes : sans
étape, l'approbation est simple ; avec des étapes, la version suit le circuit.
Revenir à l'approbation simple, même pour un temps (un audit, une
réorganisation), obligeait donc à effacer les étapes, puis à les ressaisir.

Le réglage n'était pas non plus lisible par les membres du client : seul un
administrateur pouvait le lire. Or l'auteur d'une version veut savoir, avant de
la soumettre, par quelles étapes elle passera ; la GED affiche ce circuit sur la
fiche du document (Validateur 1, 2… puis diffusion).

## Décisions

### 1. Un interrupteur explicite, distinct des étapes

Le circuit d'un type d'objet porte un état **actif** ou **suspendu**
(`circuit_settings.enabled`). Suspendu, il garde ses étapes, mais aucune
soumission nouvelle n'ouvre de passage : le port `ApprovalCircuits` rend
`Optional.empty()` et l'approbation simple s'applique (toute personne ayant le
droit d'approuver, sauf l'auteur). Réactivé, il reprend ses étapes.

Sans ligne de réglage, le circuit est actif : les clients réglés avant cette
décision ne voient rien changer. Le corps de `PUT /api/v1/circuits/{subject}`
accepte `enabled` ; absent, il vaut vrai.

### 2. Les passages ouverts vont à leur terme

Suspendre ne touche pas les versions déjà soumises, comme changer les étapes ne
les touche pas (ADR 0080, décision 4) : un passage ouvert se poursuit avec ses
étapes figées.

### 3. Le circuit se lit par tout membre du client

`GET /api/v1/circuits/{subject}` n'exige plus `authz.manage` : les étapes, leurs
rôles et l'état actif ne sont pas sensibles, et l'auteur doit les connaître. Le
régler l'exige toujours. L'isolation par client reste celle de la base
(ADR 0086).

### 4. La trace dit l'état

L'événement d'audit `circuit.replaced` porte `enabled` à côté des étapes : qui a
suspendu ou réactivé le circuit, et quand, se lit dans le journal.

## Conséquences

- Une table de plus (`circuit_settings`, clé client + type d'objet), sans
  donnée personnelle.
- L'écran d'administration offre le choix « Approbation simple » ou « Validation
  par étapes » sans perdre les étapes réglées.
- La fiche GED peut annoncer le parcours d'une version encore en brouillon.

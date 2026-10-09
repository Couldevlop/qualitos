# ADR 0080 — Les circuits de validation se règlent par client

- **Statut** : Accepté
- **Date** : 2026-10-08
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`circuit`, `docs`, `change`, migration V137),
  écrans `/admin/circuits` et fiche document
- **S'inscrit dans** : CLAUDE.md §4.1 (workflow d'approbation configurable),
  §5.4 (workflows paramétrables), §18.2 (acteur issu du jeton), ADR 0078 (droits
  par client)

## Contexte

L'approbation d'une version de document tenait en un clic : toute personne
ayant le droit « Approuver » faisait passer la version de « en revue » à
« approuvée ». Aucun client ne pouvait exiger une relecture par deux managers
puis une signature de la direction, ni désigner un rôle créé chez lui. Il
n'existait pas non plus de refus : une version en revue ne pouvait que
s'approuver ou attendre.

Plus grave, l'approbateur, le lecteur qui acquitte et l'auteur d'une version
venaient du **corps** de la requête. N'importe qui pouvait approuver « au nom »
d'un autre, et la règle « l'auteur n'approuve pas son propre travail » se
contournait en déclarant un autre identifiant.

## Décisions

### 1. L'acteur vient du jeton

Approbateur, lecteur, auteur d'une version et décideur d'une demande de
changement sont l'utilisateur du jeton. Un identifiant encore présent dans le
corps n'est gardé que pour vérifier qu'il concorde : agir au nom d'un autre est
refusé en 403, pas corrigé en silence — la tentative doit se voir.

### 2. Un moteur léger, pas un moteur BPMN

Un circuit est une suite d'au plus dix étapes. Chacune nomme le rôle qui
l'approuve — rôle de la plateforme ou rôle créé par le client (ADR 0078) — et
le nombre d'approbations distinctes qu'elle exige (1 à 10). C'est ce que les
clients demandent pour la maîtrise documentaire, et cela tient dans un écran
qu'un responsable qualité règle seul. Un moteur BPMN reste possible plus tard
pour des processus à branches ; il n'est pas nécessaire ici.

Le circuit se règle par type d'objet (`CircuitSubject`). Le premier est la
version de document ; un nouveau type s'ajoute à l'énumération, puis se branche
dans son service (soumission → départ, approbation → décision).

### 3. Sans circuit, rien ne change

Un client qui n'a rien réglé garde l'approbation simple. Le port
`ApprovalCircuits` rend `Optional.empty()` et le module métier applique sa
règle d'avant. Retirer toutes les étapes rend l'approbation simple.

### 4. Les étapes sont figées au départ

Soumettre une version ouvre un **passage** qui COPIE les étapes du moment.
Changer le circuit ensuite ne touche pas les versions déjà soumises : sinon un
réglage pourrait faire approuver un document par moins de monde que prévu.
Une version soumise avant le réglage d'un circuit garde l'approbation simple.

### 5. Quatre yeux, refus motivé

- L'auteur ne décide jamais, même s'il porte le rôle de l'étape.
- Une même personne ne décide qu'une fois par passage, à n'importe quelle étape.
- Seul un porteur du rôle de l'étape en cours décide (403 sinon).
- Un refus exige une raison et clôt le passage ; la version revient en
  brouillon avec `rejected_by`, `rejected_at`, `rejection_reason`. Une nouvelle
  soumission ouvre un nouveau passage, depuis la première étape.

La base tend les mêmes filets : un seul passage ouvert par objet (index unique
partiel), une décision par personne et par passage, un refus sans raison
refusé, un statut et sa date de fin cohérents. Une version de ligne (`@Version`)
refuse la seconde de deux décisions simultanées (409).

### 6. Une étape ne désigne qu'un rôle qui peut décider

Régler une étape sur un rôle qui n'a pas le droit d'approuver ce type d'objet
est refusé (422) : ses porteurs ne verraient jamais le bouton, et le circuit
resterait bloqué. Si le droit est retiré plus tard au rôle, l'écran de réglage
signale l'étape orpheline.

### 7. Ce que garde le journal

Le réglage d'un circuit et chaque décision sont tracés avec des codes et des
identifiants. Ni noms d'étapes ni commentaires — du texte libre — dans la trace
opposable ; les commentaires restent au passage.

## Conséquences

- `POST/PATCH` d'approbation et d'acquittement n'exigent plus l'identifiant de
  l'utilisateur dans le corps. Les clients d'API qui l'envoient encore doivent
  envoyer le leur.
- `PATCH /api/v1/documents/{id}/versions/{vid}/reject` (droit « Approuver »).
- `GET|PUT /api/v1/circuits/{subject}` (droit « Administrer les droits ») et
  `GET /api/v1/circuits/{subject}/runs/{subjectId}` (tout membre du client).
- Écran « Circuits de validation » : on glisse un rôle sur le parcours pour
  créer une étape, on glisse les étapes pour les réordonner ; les mêmes gestes
  existent au clavier. La fiche document montre l'avancement et à qui revient
  la décision.
- Reste à brancher les autres objets qui s'approuvent (CAPA, demandes de
  changement, plans de contrôle) ; le moteur ne change pas pour eux.

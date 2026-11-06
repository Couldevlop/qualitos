# ADR 0105 — L'identité visuelle d'un client : quatre thèmes fermés, des couleurs vérifiées au serveur, et une préférence de personne qui l'emporte

- **Statut** : Accepté
- **Date** : 2026-11-05
- **Owners** : @Couldevlop
- **Portée** : api-core (`core.tenant` — `TenantIdentity`, `TenantTheme`,
  `ContrastGuard`, `TenantIdentityService`, `BrandingController`,
  `TenantLogoController`, `LogoPolicy`), web (`core/branding` —
  `BrandingService`, `brand-color`, `styles/_client-themes.scss`,
  `core/theme/ThemeService.applyOrganisationDefault`)
- **Complète** : ADR 0082 (une route SaaS répond 404 en on-premise), ADR 0094
  (dépôt de fichier : le type se lit dans les octets)
- **Met en œuvre** : `docs/specs/2026-11-05-console-super-admin.md` §D4

## Contexte

Un client doit pouvoir poser **sa** marque sur son espace : logo, slogan,
mentions légales, couleurs, et l'allure générale de l'application. La demande est
explicite — un client « totalement paramétrable dès sa création ». Elle arrive
avec la console de l'éditeur, mais elle ne lui appartient pas : un client
on-premise installe le produit chez lui et n'a aucun éditeur à qui demander son
slogan, et en SaaS tout paramétrage revient à l'administrateur du client, pas à
nous.

Trois questions se posaient, chacune avec une réponse facile et coûteuse.

**1. Jusqu'où va la personnalisation ?** Le réflexe du marché est l'éditeur de
jetons libre : chaque couleur du système de design devient un champ. Il se vend
bien en démonstration et il se paie deux fois — d'abord en accessibilité, parce
que rien n'empêche alors de composer un espace illisible et qu'aucun écran ne
permet de revenir en arrière ; ensuite en vérification, parce que chaque
combinaison devient une variante d'interface à éprouver, et qu'il y en a une
infinité.

**2. Où se vérifie le contraste ?** Le contrôler dans l'écran de saisie est une
politesse, pas une garantie : l'appel HTTP se forge. Et le fond contre lequel se
mesure la lisibilité dépend du thème — mesurer toujours sur blanc interdirait
tout thème sombre, dont les accents sont clairs par construction.

**3. Que se passe-t-il quand le client choisit le sombre et que quelqu'un a
besoin du clair ?** Le thème est un réglage d'entreprise ; le mode clair/sombre
est, pour certaines personnes, un réglage d'accessibilité. Les confondre fait
d'un choix de la console de l'éditeur une décision prise à distance sur le
confort visuel d'un utilisateur.

## Décision

### 1. Une liste fermée de quatre thèmes, pas un éditeur de jetons

`TenantTheme` : `DENSE_LIGHT` (le poste de travail d'un service qualité),
`AIRY_LIGHT` (le défaut), `DARK`, `HIGH_CONTRAST` (atelier, extérieur). Un code
inconnu est refusé au serveur ; côté écran, il retombe sur le défaut plutôt que
de laisser la page sans thème.

Chaque thème porte une classe sur `<html>` (`qos-client-dense`, `qos-client-airy`,
`qos-client-dark`, `qos-client-contrast`) qui **redéfinit des jetons existants** —
`--qos-space-*`, `--qos-lh-*`, `--qos-border-default`, `--qos-ring-focus` — et
rien d'autre. Aucune règle par composant : les écrans consomment déjà ces jetons,
les resserrer d'un cran agit partout d'un coup et ne crée pas une seconde famille
de règles à maintenir.

### 2. Deux couleurs seulement, vérifiées au serveur contre le fond du thème

Le client choisit une couleur principale et une secondaire, en `#RRGGBB`.
`ContrastGuard` mesure la luminance relative de WCAG 2.1 et **refuse
l'enregistrement** sous 4,5:1 (niveau AA, texte normal, `CLAUDE.md` §15.1), contre
le fond du thème choisi — chaque thème déclare le sien, ce qui rend le sombre
possible sans assouplir le seuil.

L'écran traduit ensuite ces deux couleurs en une **liste fermée de neuf jetons**
(`--qos-accent-bg`, son survol, son libellé, ses variantes douces, le lien, l'anneau
de focus, les deux premières séries de graphique). Tout le reste du système de
design est hors d'atteinte du client.

### 3. Le libellé posé SUR la couleur se calcule dans l'écran

Le serveur garantit que la couleur se lit **sur la page**. Il ne dit rien du
texte posé **sur elle** : un bouton plein a un fond accentué et un libellé
dessus, et un jaune validé par le serveur porterait un libellé blanc illisible.
`readableForeground` mesure le contraste du blanc et celui de l'encre et garde le
meilleur — au lieu d'un seuil de luminance, qui se trompe sur les couleurs
saturées. La formule est celle du serveur, au chiffre près : deux formules
donneraient deux verdicts sur la même couleur.

### 4. Le thème du client est un défaut, la préférence de la personne l'emporte

`ThemeService.applyOrganisationDefault(mode)` n'applique le mode du client qu'à
qui **n'a rien enregistré**. Le thème du client remplace donc le système
d'exploitation, rien de plus. Deux conséquences tenues explicitement :

- « rien d'enregistré » et « j'ai choisi de suivre le système » ont la même valeur
  en mémoire ; seule la présence en stockage les distingue, et c'est elle qui est
  lue — suivre le système **est** un choix ;
- le mode venu du client n'est **pas** persisté : le jour où le client change de
  thème, l'écran suit, au lieu de garder une décision périmée.

### 5. La marque se charge dans la coquille, et son échec n'arrête rien

`/api/v1/branding` lit le client **dans le jeton** (`CLAUDE.md` §18.2.2) : un
administrateur ne peut pas écrire la marque d'un autre client en forgeant sa
requête. La lecture est déclenchée par la coquille applicative et non par un
initializer, parce qu'elle exige un jeton qui n'existe pas encore au démarrage.

Marque absente, serveur muet, couleur mal formée : on garde les jetons du socle,
lisibles par construction. Une application qui refuserait de s'afficher faute de
logo serait un défaut bien pire que l'absence de logo.

## Conséquences

**Ce que cela donne.** Un client pose son identité à sa création, en SaaS comme
en on-premise, et son administrateur la modifie ensuite sans passer par nous. La
surface d'écran à éprouver reste finie : quatre thèmes × deux modes, et des
couleurs dont la lisibilité est garantie par une mesure, pas par une relecture.

**Ce que cela coûte.** Un client qui voudrait une couleur hors seuil se voit
refuser son choix, et le message doit dire pourquoi — un refus sans explication
serait pris pour une panne. La marque arrive après le premier rendu, donc l'accent
du socle est visible une fraction de seconde au chargement ; bloquer l'amorçage
sur un appel réseau coûterait plus cher à tout le monde que ce scintillement ne
coûte à la marque.

**Ce qui reste ouvert.** Le logo n'habille pour l'instant que la console ; sa
pose dans l'en-tête applicatif et dans les exports PDF signés suivra. Les lots 2
à 10 de la spécification (cabinets, paramétrage central, statistiques,
chiffrement par client, habilitations, abonnement, reprise de données, mots de
passe) ne sont pas couverts ici.

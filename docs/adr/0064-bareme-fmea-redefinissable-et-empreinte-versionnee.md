# ADR 0064 — Le barème FMEA redéfinissable par le tenant, l'empreinte du control plan versionnée

- **Statut** : Accepté
- **Date** : 2026-08-31
- **Owners** : @Couldevlop
- **Portée** : FMEA (référentiel de cotation), Control Plan (trame et scellement)
- **Remplace** : ADR 0063 §6 sur le point « constante du code, pas donnée de tenant »

## Contexte

Deux constats, tirés de la confrontation du livré à la trame réelle.

1. L'ADR 0063 avait affiché le barème de cotation FMEA à l'écran, en constante
   du code, au motif qu'« un barème modifié silencieusement invalide toutes les
   cotations passées ». Le raisonnement était juste ; la conclusion, non. Un
   barème de sévérité rédigé pour un atelier de sertissage — « arrêt de ligne
   client », « rebut du lot » — ne veut rien dire dans un laboratoire d'analyses
   ni dans un centre d'appels. Imposé, il produit des cotations que personne ne
   croit, et l'organisation tient son vrai barème sur un tableur à côté. C'est
   aussi ce qu'interdit l'invariant §22.11 : aucune logique sectorielle en dur.

   Ce qu'il fallait retenir de 0063 n'était donc pas « le barème ne bouge pas »,
   mais « **un barème qui bouge en silence** invalide les cotations passées ».
   C'est le silence qu'on supprime, pas la possibilité de redéfinir.

2. L'empreinte de scellement d'un control plan se voulait celle de « chaque
   ligne dans son intégralité ». Elle ne l'était plus : cinq colonnes arrivées
   avec la V116 n'y entraient pas. On pouvait déplacer le lieu d'enregistrement
   d'un contrôle — la colonne même que l'auditeur suit pour retrouver la
   preuve — sans que l'empreinte du document ne bouge.

   Par ailleurs la trame tient deux colonnes là où le modèle n'en avait qu'une :
   ce qu'on surveille et la **grandeur spécifiée** qui porte la tolérance.

## Décisions

### 1. Le barème est un référentiel de tenant, servi par défaut et jamais copié

Un tenant qui n'a rien redéfini n'a **aucune ligne** en base et reçoit le barème
de référence. Semer les trente lignes à la création de chaque tenant l'aurait
figé au jour de l'inscription et aurait rendu impossible de distinguer « jamais
touché » de « redéfini à l'identique » — la distinction même qu'un auditeur
vient chercher. L'écran dit lequel des deux cas s'applique : deux RPN issus de
barèmes différents ne se comparent pas, et le lecteur doit le savoir sans
enquêter.

### 2. Une échelle se remplace d'un bloc, de 1 à 10, ou pas du tout

Autoriser la modification d'une ligne isolée permettrait de laisser un trou, et
un score sans définition fait coter au jugé exactement là où le barème existe
pour l'éviter. La complétude est vérifiée dans le service — pas seulement à la
frontière HTTP — et doublée d'une contrainte d'unicité `(tenant, kind, score)`
en base : un barème incomplet ne se voit pas à l'écran, il se découvre le jour
où quelqu'un cote un 7 qui ne veut rien dire.

### 3. Redéfinir relève de la direction qualité, et s'inscrit au journal

L'écriture est réservée à la direction qualité et à l'administration du tenant.
Le manager qualité en est volontairement exclu : il cote, il ne redéfinit pas
l'échelle sur laquelle il cote.

Chaque redéfinition et chaque retour à la référence sont inscrits au journal
chaîné du tenant (`fmea.rating_scale.redefined`, `fmea.rating_scale.reverted`),
avec l'acteur lu du jeton. C'est ce qui répond à l'objection de 0063 : la ligne
en base porte bien `updated_by`, mais elle est écrasée au remplacement suivant
et ne dit jamais qu'il a existé un barème avant. Le journal, lui, est en ajout
seul et ancré périodiquement (§11.5). Devant deux RPN de 120 cotés à six mois
d'écart, l'auditeur peut établir si l'échelle a bougé entre les deux.

### 4. Le control plan gagne la caractéristique spécifiée

Colonne **facultative** : un control plan se remplit par passes successives, et
l'exiger empêcherait simplement d'ouvrir une ligne.

### 5. Le calcul d'empreinte est versionné, les preuves passées ne se réécrivent pas

Le calcul complet devient la version 2 et s'annonce dans son propre texte
canonique (`control-plan/2`) — deux versions ne doivent jamais pouvoir produire
le même texte. Chaque plan porte la version avec laquelle il a été scellé, et
la version 1 reste calculable pour toujours.

Compléter le calcul sans le versionner aurait rendu **invérifiables** tous les
plans déjà scellés : rejouer le hachage d'un document ancien avec le nouveau
calcul donne une autre valeur, c'est-à-dire un verdict de falsification sur un
document intact. Le défaut `0` de `seal_version` signifie « pas encore scellé » ;
les plans déjà scellés reçoivent `1`, la seule version qui existait alors. Aucune
empreinte n'est recalculée.

## Conséquences

- ✅ Une organisation cote sur SON échelle, sans quitter la plateforme.
- ✅ L'écran distingue un barème de référence d'un barème redéfini ; les RPN de
  deux tenants ne se lisent plus comme s'ils sortaient de la même règle.
- ✅ Un changement de barème est attribuable et horodaté de façon infalsifiable.
- ✅ Déplacer un lieu d'enregistrement change désormais l'empreinte du plan.
- ✅ Les plans scellés avant ce jour restent vérifiables à l'identique.
- ⚠ Deux RPN d'un même tenant cotés de part et d'autre d'une redéfinition ne se
  comparent pas. La plateforme le rend **constatable** (journal), elle ne
  recote pas rétroactivement : recoter serait réécrire un jugement qualité que
  personne n'a porté.
- ⚠ Toute colonne ajoutée plus tard à une ligne de control plan devra entrer
  dans une version **3** du calcul, jamais dans la version 2.

## Vérifications

- `FmeaScaleServiceTest` — complétude de 1 à 10, refus des trous et des doublons,
  défaut servi sans écriture, journalisation de la redéfinition et du retour,
  aucune trace laissée par un barème refusé.
- `FmeaScaleControllerTest` — lecture ouverte à l'authentifié, écriture refusée
  au manager qualité, isolation par tenant.
- `ControlPlanFingerprintTest` — la version 1 rejouée à l'identique, la version 2
  sensible aux six colonnes, deux versions jamais confondues.
- `ControlPlanSealVersionBackfillOnPostgresTest` (tag `migration`, PostgreSQL
  réel) — les plans déjà scellés reçoivent la version 1, les autres 0. Sur un
  vrai moteur : une doublure de dépôt rend l'objet d'origine et masquerait
  exactement le défaut que cette bascule peut introduire.

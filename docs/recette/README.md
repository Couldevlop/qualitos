# Recette de la plateforme

## Les livrables

Cinq fichiers, produits par `scripts/recette/construire.py` :

| Fichier | Pour qui | Ce qu'il contient |
| --- | --- | --- |
| `QualitOS-cahier-de-recette-v1.0.xlsx` | Les testeurs | 170 cas, 5 scénarios bout-en-bout, 28 jeux de données, le journal des anomalies, la synthèse calculée |
| `QualitOS-plan-de-recette-v1.0.docx` | Le pilote de recette | Périmètre, organisation, méthode, échelle de gravité, critères d'entrée et de sortie, risques |
| `QualitOS-recette-direction-v1.0.pptx` | Directeurs de site et de la qualité | Ce qui a été vérifié, ce qui ne l'a pas été, la décision attendue |
| `QualitOS-recette-managers-v1.0.pptx` | Managers qualité | Rôles, planning, suivi quotidien, gestion des anomalies |
| `QualitOS-recette-ingenieurs-v1.0.pptx` | Ingénieurs et acteurs terrain | Comment jouer un cas, les conventions, les pièges |

```bash
python scripts/recette/construire.py
```

Dépendances : `openpyxl`, `python-docx`, `python-pptx`.

## Pourquoi le code est versionné et pas les documents

Un classeur Excel ne se relit pas dans une *pull request* : le diff d'un `.xlsx`
est illisible, et personne ne peut donc contester un cas de test avant qu'il ne
soit joué. Une liste de cas écrite en Python, si.

Les cas vivent donc dans `scripts/recette/recette_cas_*.py`, la charte et les
jeux de données dans `recette_socle.py`.

Quatre fichiers de cas, et le quatrième n'est pas un découpage arbitraire :

| Fichier | Contenu |
| --- | --- |
| `recette_cas_a.py` | Pilotage, méthodes, analyses IA, SPC, APQP, écarts |
| `recette_cas_b.py` | Opérations, référentiels, risques, administration, et les 5 scénarios bout-en-bout |
| `recette_cas_c.py` | Conformité réglementaire (RGPD, IA Act, NIS2) et les cas transverses |
| `recette_cas_d.py` | Le **delta** : ce qui est entré dans le produit après la campagne précédente |

Tenir le delta à part répond à la seule question qui se pose en relecture d'une
campagne à la suivante : *qu'est-ce qui n'avait jamais été recetté ?* Mélangé
dans les trois premiers fichiers, il deviendrait introuvable au bout de deux
itérations. Les documents se régénèrent en une
commande, et ne sont pas suivis par git — d'autant que le classeur est **rempli
à la main pendant la campagne** : le versionner inviterait des conflits binaires
à chaque journée de recette.

Si une campagne doit être archivée telle qu'elle a été jouée, c'est le classeur
rempli qu'on archive, hors dépôt, avec la version recettée dans son nom.

## Comment la couverture a été établie

L'inventaire n'est pas écrit de mémoire : il est tiré de la navigation réelle de
l'application (`apps/web/src/app/layout/main-shell/main-shell.component.ts`),
soit onze sections et environ quatre-vingts entrées, croisées avec les routes de
`app-routing.module.ts`.

C'est le reproche habituel fait aux cahiers de recette — ils couvrent ce dont on
se souvient. L'onglet « Couverture » du classeur rend l'écart visible : un
domaine sans aucun cas de type « Droits » ou « Négatif » doit alerter, parce que
ce sont les deux colonnes où se cachent les défauts coûteux.

## Le fil rouge

Un seul scénario industriel traverse toute la campagne : la pompe **P-204**
vibre, le capteur le voit, la carte de contrôle lève une règle de Nelson, une
non-conformité naît, l'Ishikawa trouve la cause, la CAPA la traite contre un
critère posé à l'avance, l'audit en fait une preuve de clause, et un MOC finit
par remplacer la pompe.

Des données éparpillées testeraient les écrans un par un. Un fil rouge teste ce
que le produit promet — que ces objets soient liés.

## Ce que le delta du 2026-11-11 ajoute

Vingt-cinq cas, pour les fonctionnalités entrées après la première édition du
cahier. Ils ne répètent aucun cas existant — la vérification s'est faite en
relisant les intitulés des 145 cas précédents, pas de mémoire.

| Ce qui est couvert | Décision de référence |
| --- | --- |
| Règles de Nelson posées sur la carte de l'étude de capabilité, carte de dispersion exclue, panne d'analyse sans effet sur le Cpk | ADR 0112 |
| Prévision de sortie des limites de contrôle, et CAPA **préventive** distincte du dossier correctif | ADR 0110 |
| Refus explicite au-delà de vingt-cinq mesures par prélèvement | ADR 0107 |
| Un écart devient le cycle PDCA ou le projet DMAIC qui le traitera, la gravité proposant sans décider | Référentiel transverse §3.6 |
| Un constat d'audit non conforme entre au registre des écarts, et ne s'y inscrit pas deux fois | §3.6, ADR 0088 |
| Un pilier 5S faible porté au cercle de qualité ; la photo d'un constat jointe même hors ligne | §3.2, §3.6 |
| Une action DMAIC inscrite comme détrompeur à la bibliothèque du client, et son étanchéité entre clients | §3.4, ADR 0086 |
| Le risque énoncé sur une exigence de partie intéressée inscrit au registre | ADR 0076, ADR 0104 |
| Les douze listes déroulantes du client : mode ouvert, mode « renommer seulement », oubli à la déconnexion | ADR 0111 |
| La piste d'audit de la plateforme, et l'impossibilité de retoucher une ligne même en base | ADR 0109 |
| Un client et un cabinet qui se corrigent sans toucher au repère ni à l'état | ADR 0106 |
| Échéance au calendrier et responsable de l'annuaire sur une étape de certification | ADR 0092, §8.5 |

Deux de ces cas se jouent avec un accès `psql` et le compte de plateforme : ils
vérifient un refus **de la base**, pas un refus de l'écran. Un cahier qui
n'éprouverait que l'interface déclarerait conforme une piste d'audit qu'un
`DELETE` suffit à réécrire.

## Campagnes précédentes

- `recette-moc-capa-audits-smi.md` — gestion des changements, CAPA, audits, SMI
  (version `main-f01c0c4b21a8`). Ses comptes de recette sont repris ici pour que
  les deux campagnes parlent des mêmes personnes.

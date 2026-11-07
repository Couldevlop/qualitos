# Recette de la plateforme

## Les livrables

Cinq fichiers, produits par `scripts/recette/construire.py` :

| Fichier | Pour qui | Ce qu'il contient |
| --- | --- | --- |
| `QualitOS-cahier-de-recette-v1.0.xlsx` | Les testeurs | 145 cas, 5 scénarios bout-en-bout, 28 jeux de données, le journal des anomalies, la synthèse calculée |
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
jeux de données dans `recette_socle.py`. Les documents se régénèrent en une
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

## Campagnes précédentes

- `recette-moc-capa-audits-smi.md` — gestion des changements, CAPA, audits, SMI
  (version `main-f01c0c4b21a8`). Ses comptes de recette sont repris ici pour que
  les deux campagnes parlent des mêmes personnes.

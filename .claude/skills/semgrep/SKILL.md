---
name: semgrep
description: Lancer Semgrep en local sur QualitOS, lire les findings et les corriger, avec exactement les règles du job « SAST (Semgrep) » de la CI. À utiliser dès qu'il s'agit d'analyse statique de sécurité : avant d'ouvrir une PR touchant du Java, du TypeScript ou du Python, quand le job SAST de la CI échoue et qu'il faut le reproduire, quand on demande « scanne le code », « y a-t-il une faille ici », « passe Semgrep », « vérifie la sécurité de ce que je viens d'écrire », ou avant de fusionner un correctif de sécurité. Utile même quand Semgrep n'est pas nommé : toute demande de revue de sécurité du code modifié passe par ici.
---

# Semgrep sur QualitOS

## Pourquoi cette skill

La CI porte un job **« SAST (Semgrep) »** (`.github/workflows/ci.yml`) qui **bloque** la
fusion. Découvrir ses findings après avoir poussé coûte un aller-retour de CI de
plusieurs minutes ; les découvrir en local coûte une minute. Tout l'objet de cette
skill est de rejouer *exactement* ce que la CI fait, pas une approximation : un scan
local avec d'autres règles donne une fausse assurance, ce qui est pire que pas de scan.

## Ce que la CI vérifie exactement

Deux passes, mêmes règles, sévérités différentes :

| Passe | Sévérités | Bloquante ? | Rôle |
| --- | --- | --- | --- |
| Rapport complet | toutes | non (`continue-on-error`) | fond de tableau INFO/WARNING |
| Findings ERROR | ERROR seulement | **oui** (`--error`) | vulnérabilités à forte confiance |

Les quatre paquets de règles sont `p/security-audit`, `p/owasp-top-ten`, `p/java` et
`p/secrets`, avec `--metrics=off`. La version est **épinglée à `semgrep==1.145.0`** :
une version différente ne voit pas les mêmes règles, donc un « c'est vert chez moi »
ne vaut rien si la version diverge. Vérifier d'abord :

```bash
semgrep --version          # doit afficher 1.145.0
python -m pip install --quiet semgrep==1.145.0   # si absent ou divergent
```

Sur ce poste Windows, `pip` seul n'est pas dans le PATH — passer par `python -m pip`.

## La boucle locale : scanner le diff, pas le dépôt

Scanner tout QualitOS prend des dizaines de minutes pour rien. Ce qui compte est ce
que la branche a changé :

```bash
bash .claude/skills/semgrep/scripts/scan-diff.sh          # vs main, passe bloquante
bash .claude/skills/semgrep/scripts/scan-diff.sh --all    # toutes sévérités (rapport)
bash .claude/skills/semgrep/scripts/scan-diff.sh --base develop
```

Le script liste les fichiers modifiés par rapport à la base, écarte ceux qui ont été
supprimés, et applique les quatre `--config` de la CI. Compter ~40 s au premier appel
(téléchargement des règles depuis le registre Semgrep, donc **réseau requis**), puis
quelques secondes.

Pour rejouer la CI à l'identique, dépôt entier, quand un finding vient d'un fichier
non modifié :

```bash
semgrep scan --config p/security-audit --config p/owasp-top-ten \
             --config p/java --config p/secrets \
             --severity ERROR --error --metrics=off
```

## Lire un finding sans se tromper

`--severity ERROR` filtre l'affichage : ce que la CI bloque, ce sont ces lignes-là.
Pour obtenir la règle, le fichier et la ligne exacte de façon exploitable :

```bash
semgrep scan --config p/security-audit --config p/owasp-top-ten \
             --config p/java --config p/secrets --severity ERROR \
             --json --metrics=off <fichiers> \
  | python -c "import json,sys; [print(f\"{r['path']}:{r['start']['line']} {r['check_id']}\n  {r['extra']['message'][:200]}\") for r in json.load(sys.stdin)['results']]"
```

Deux pièges de lecture qui font conclure trop vite :

- **« Rules run: 0 » ou « Targets scanned: 0 » ne veut pas dire « code sain »** : cela
  veut dire qu'aucune règle ne couvre ces fichiers. Le SCSS, le HTML et les YAML
  n'ont quasiment pas de règles dans ces paquets — un correctif de mise en page qui
  « passe Semgrep » n'a en réalité rien fait vérifier. Le dire tel quel plutôt que
  d'annoncer un scan vert.
- Le résumé de fin (`Findings: N (M blocking)`) est la seule ligne à recopier quand
  on rend compte. `M` est ce qui casse la CI.

## Corriger, dans cet ordre

1. **Corriger le code.** La quasi-totalité des findings ERROR de ces paquets sont de
   vraies faiblesses : requête concaténée, désérialisation ouverte, aléa non
   cryptographique, secret en littéral, chemin construit depuis une entrée. Les
   invariants du dépôt (`CLAUDE.md` §18.2) disent déjà quoi faire : requêtes
   paramétrées, secrets par Vault, `tenant_id` issu du JWT.
2. **Réduire la portée** si le motif est légitime mais trop large : extraire la
   partie sensible, typer l'entrée, valider avant usage. Souvent le finding disparaît
   parce que le code est devenu plus clair.
3. **Écarter en dernier recours seulement**, et jamais silencieusement. Le dépôt a une
   règle de forme, tenue par `.trivyignore`, `.gitleaks.toml` et `.gitguardian.yaml` :
   une exception dit **ce qu'elle écarte, pourquoi ce n'est pas une faille, et une
   date de revue**. Pour Semgrep, cela se pose au plus près du code :

   ```java
   // nosemgrep: java.lang.security.audit.crypto.weak-random — graine d'affichage
   // d'une démo, aucune valeur de sécurité. Revue : 2027-02-15.
   ```

   Un `nosemgrep` nu, sans identifiant de règle ni raison, est une fuite en sursis :
   il masque aussi les futurs findings de la même ligne.

## Quand c'est la CI qui a échoué

Le job affiche la règle et le fichier. Récupérer le journal sans quitter le terminal :

```bash
gh run view <run-id> --log-failed | grep -A 20 "findings ERROR"
```

Puis rejouer **le même fichier** en local avec la commande de parité ci-dessus. Si le
finding n'apparaît pas localement, la cause est presque toujours la version de
Semgrep ou un `--config` oublié — vérifier ces deux points avant de conclure à un
faux positif de la CI.

## Avant d'annoncer que c'est réglé

Relancer la passe bloquante et recopier sa ligne de résumé dans la réponse. « Semgrep
est vert » sans la ligne `Findings: 0 (0 blocking)` et sans le nombre de fichiers
réellement scannés n'est pas une vérification, c'est une impression.

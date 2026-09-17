#!/usr/bin/env bash
#
# Les chiffres de docs/PASSATION.md, remesurés depuis le dépôt.
#
# Pourquoi un script plutôt qu'un tableau écrit à la main : la première version
# de PASSATION.md a menti en UN JOUR. Elle annonçait « migrations jusqu'à V131 »
# et « CAPA vérification : non commencé » alors que V132 était déjà appliquée en
# préproduction. Un chiffre recopié ne vieillit pas, il pourrit — et un document
# de reprise qui se trompe sur l'état du code est pire que pas de document, parce
# qu'on le croit.
#
# Ce script ne rend QUE ce qui se compte sans rien exécuter. Le nombre de tests
# n'en fait pas partie : il ne s'obtient qu'en lançant les suites, et l'inventer
# depuis un `grep @Test` donnerait un chiffre faux (tests paramétrés, tests
# désactivés, classes imbriquées). La section « à mesurer en lançant » rappelle
# les commandes qui, elles, disent la vérité.
#
# Usage :  bash scripts/passation-chiffres.sh
#
set -euo pipefail

cd "$(dirname "$0")/.."

java_moteur=$(find apps/api-quality-engine/src/main/java -name '*.java' | wc -l)
java_total=$(find apps/*/src/main/java libs -name '*.java' 2>/dev/null | wc -l)
ts_front=$(find apps/web/src -name '*.ts' ! -name '*.spec.ts' | wc -l)
specs_front=$(find apps/web/src -name '*.spec.ts' | wc -l)
e2e=$(find apps/web/e2e -name '*.spec.ts' | wc -l)

migration_max=$(ls apps/api-quality-engine/src/main/resources/db/migration/ \
                | sed -n 's/^V\([0-9]\+\)__.*/\1/p' | sort -n | tail -1)
adr_nb=$(ls docs/adr/*.md | grep -vc README)
adr_max=$(ls docs/adr/*.md | grep -v README | sed -n 's|.*/\([0-9]\{4\}\)-.*|\1|p' | sort -n | tail -1)
# `fr` est la langue SOURCE : les textes vivent dans les gabarits, il n'existe
# donc pas de `messages.fr.xlf`. Compter les seuls fichiers annoncerait une
# langue de moins que ce que la plateforme sert réellement.
traductions=$(ls apps/web/src/locale/messages.*.xlf | wc -l)
langues=$((traductions + 1))

apps_nb=$(find apps -maxdepth 1 -mindepth 1 -type d | wc -l)
libs_nb=$(find libs -maxdepth 1 -mindepth 1 -type d | wc -l)

# `main` avance ; le document doit dire sur quel commit il a été mesuré.
commit=$(git rev-parse --short=7 HEAD)
branche=$(git rev-parse --abbrev-ref HEAD)
date_jour=$(date +%Y-%m-%d)

cat <<TABLEAU
Mesuré le $date_jour sur $branche à $commit.

| | |
| --- | --- |
| Fichiers Java — moteur qualité | **$java_moteur** |
| Fichiers Java — tous services et bibliothèques | **$java_total** |
| Fichiers TypeScript front (hors tests) | **$ts_front** |
| Fichiers de test front | **$specs_front** |
| Fichiers de test end-to-end | **$e2e** |
| Migrations Flyway | jusqu'à **V$migration_max** |
| Décisions d'architecture | **$adr_nb** ADR, numérotés jusqu'à **$adr_max** |
| Langues servies | **$langues** — \`fr\` (source, dans les gabarits) + $traductions traductions |
| Applications / bibliothèques | **$apps_nb** / **$libs_nb** |

À mesurer en LANÇANT (aucun grep ne le donne juste) :

  backend   TEMP=D:/tmp TMP=D:/tmp TESTCONTAINERS_RYUK_DISABLED=true mvn clean verify
            → lire la ligne « Tests run: » du récapitulatif

  front     cd apps/web && npx ng test --watch=false --browsers=ChromeHeadless
            → lire « TOTAL: N SUCCESS », JAMAIS le code de sortie (§6.3)

  e2e       cd apps/web && npx playwright test
TABLEAU

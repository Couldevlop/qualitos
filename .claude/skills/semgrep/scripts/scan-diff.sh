#!/usr/bin/env bash
# Scanne avec Semgrep les fichiers modifies par rapport a une base, avec EXACTEMENT
# les regles du job « SAST (Semgrep) » de la CI (.github/workflows/ci.yml).
#
#   scan-diff.sh                 # vs main, passe bloquante (ERROR seulement)
#   scan-diff.sh --all           # toutes severites, ne bloque pas
#   scan-diff.sh --base develop  # autre base de comparaison
#
# Sortie : code 1 si la CI bloquerait (un finding ERROR au moins).
set -uo pipefail

BASE="main"
SEVERITY_ARGS=(--severity ERROR --error)
EXPECTED_VERSION="1.145.0"

while [ $# -gt 0 ]; do
  case "$1" in
    --all)  SEVERITY_ARGS=() ;;
    --base) shift; BASE="${1:?--base attend une reference}" ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) echo "Option inconnue : $1" >&2; exit 2 ;;
  esac
  shift
done

if ! command -v semgrep >/dev/null 2>&1; then
  echo "Semgrep absent. Installer la version de la CI :" >&2
  echo "  python -m pip install --quiet semgrep==$EXPECTED_VERSION" >&2
  exit 2
fi

VERSION="$(semgrep --version 2>/dev/null | tail -1)"
if [ "$VERSION" != "$EXPECTED_VERSION" ]; then
  echo "ATTENTION : semgrep $VERSION en local, $EXPECTED_VERSION en CI." >&2
  echo "Les regles different d'une version a l'autre : un scan vert ici ne dit rien" >&2
  echo "de la CI. Aligner avec  python -m pip install semgrep==$EXPECTED_VERSION" >&2
fi

# --diff-filter=d : un fichier supprime n'est plus scannable.
mapfile -t FILES < <(git diff --name-only --diff-filter=d "$BASE"...HEAD)

if [ "${#FILES[@]}" -eq 0 ]; then
  echo "Aucun fichier modifie par rapport a $BASE — rien a scanner."
  exit 0
fi

echo "Base       : $BASE"
echo "Fichiers   : ${#FILES[@]}"
echo "Passe      : ${SEVERITY_ARGS[*]:-toutes severites (informative)}"
echo

# Le premier appel telecharge les paquets de regles depuis le registre Semgrep :
# compter ~40 s et une connexion reseau.
semgrep scan \
  --config p/security-audit \
  --config p/owasp-top-ten \
  --config p/java \
  --config p/secrets \
  --metrics=off \
  "${SEVERITY_ARGS[@]}" \
  "${FILES[@]}"
STATUS=$?

echo
if [ "$STATUS" -eq 0 ]; then
  echo "Aucun finding bloquant sur le diff. Attention : les fichiers sans regle"
  echo "associee (SCSS, HTML, YAML) ne sont pas 'verifies' pour autant."
else
  echo "La CI bloquerait sur ce diff (code $STATUS)."
fi
exit "$STATUS"

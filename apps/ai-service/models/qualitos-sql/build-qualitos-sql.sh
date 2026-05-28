#!/usr/bin/env bash
# Crée/recrée le modèle NLQ spécialisé `qualitos-sql` dans le conteneur Ollama.
# 100% local : FROM qwen2.5-coder:3b doit déjà être pull (ollama pull qwen2.5-coder:3b).
#
# Usage : ./build-qualitos-sql.sh [nom_conteneur_ollama]   (défaut: qualitos-ollama)
set -euo pipefail

OLLAMA_CONTAINER="${1:-qualitos-ollama}"
MODELFILE="$(dirname "$0")/Modelfile"

echo "→ Copie du Modelfile dans ${OLLAMA_CONTAINER}…"
docker cp "${MODELFILE}" "${OLLAMA_CONTAINER}:/tmp/qualitos-sql.Modelfile"

echo "→ ollama create qualitos-sql…"
docker exec "${OLLAMA_CONTAINER}" ollama create qualitos-sql -f /tmp/qualitos-sql.Modelfile

echo "✓ Modèle 'qualitos-sql' prêt. Pointer ai-service dessus : OLLAMA_MODEL=qualitos-sql"

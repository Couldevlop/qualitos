"""Provider LLM par défaut, piloté par l'environnement (extension d'ADR 0014).

Quand une requête IA ne précise pas de ``provider``, le moteur utilisé est résolu
depuis la variable d'environnement ``AI_DEFAULT_PROVIDER`` (``ollama`` | ``mistral``
| ``anthropic``). Défaut historique : ``ollama`` (local). Pour basculer toute la
plateforme sur Mistral, il suffit de configurer (sans toucher au code) :

    AI_DEFAULT_PROVIDER=mistral
    MISTRAL_API_KEY=...            # jamais committé (Vault/ESO en prod, §18.2 #3)
    MISTRAL_MODEL=mistral-large-latest   # optionnel (cf. container)

Le secret n'est jamais ici : seul le *choix* du provider l'est. Une valeur inconnue
ou absente retombe sur ``ollama`` (repli sûr, comportement antérieur inchangé).
"""
from __future__ import annotations

import os

from domain.model.completion import ProviderName


def resolve_default_provider(raw: str | None) -> ProviderName:
    """Résout le provider par défaut depuis une valeur d'environnement.

    Insensible à la casse et aux espaces. Valeur absente/vide/inconnue → ``OLLAMA``.
    """
    if not raw:
        return ProviderName.OLLAMA
    try:
        return ProviderName(raw.strip().lower())
    except ValueError:
        return ProviderName.OLLAMA


# Évalué une fois au démarrage du processus ; les schémas Pydantic l'utilisent comme
# valeur par défaut de leur champ ``provider``.
DEFAULT_PROVIDER: ProviderName = resolve_default_provider(os.environ.get("AI_DEFAULT_PROVIDER"))

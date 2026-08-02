"""Allow-listed AI provider endpoints (OWASP A10 SSRF / LLM07)."""
from __future__ import annotations

import os
from urllib.parse import urlparse

# Hôtes toujours autorisés : fournisseurs hébergés officiels et boucle locale.
# `ollama` est le nom de service du docker-compose de développement.
_BUILTIN_HOSTS: frozenset[str] = frozenset(
    {
        "api.anthropic.com",
        "api.mistral.ai",
        "ollama",  # docker hostname
        "localhost",
        "127.0.0.1",
    }
)

# Variable d'environnement permettant d'ÉTENDRE la liste (hôtes séparés par des
# virgules).
#
# Pourquoi cette extension. La liste était entièrement codée en dur, ce qui la
# rendait inapplicable hors du docker-compose : sur Kubernetes le service porte un
# nom pleinement qualifié (`ollama.<namespace>.svc.cluster.local`) que rien ne
# permettait de déclarer. Le service refusait alors tout appel et redémarrait en
# boucle. Un contrôle de sécurité qu'on ne peut pas configurer finit par être
# désactivé ; mieux vaut le rendre paramétrable et le garder actif.
#
# L'extension est ADDITIVE : elle ne peut pas retirer un hôte de la liste
# intégrée. Une valeur absente ou vide laisse le comportement d'origine inchangé.
ALLOWED_HOSTS_ENV = "QOS_AI_ALLOWED_HOSTS"


def _extra_hosts() -> frozenset[str]:
    raw = os.environ.get(ALLOWED_HOSTS_ENV, "")
    return frozenset(h.strip().lower() for h in raw.split(",") if h.strip())


def allowed_hosts() -> frozenset[str]:
    """Liste effective. Relue à chaque appel, donc testable sans réimport."""
    return _BUILTIN_HOSTS | _extra_hosts()


# Conservé pour compatibilité : d'autres modules et des tests importent ce nom.
# Il ne reflète QUE les hôtes intégrés ; la vérification ci-dessous s'appuie sur
# `allowed_hosts()` et prend donc bien l'extension en compte.
ALLOWED_HOSTS: frozenset[str] = _BUILTIN_HOSTS


def assert_host_allowed(url: str) -> None:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower()
    if host not in allowed_hosts():
        raise PermissionError(
            f"AI provider host '{host}' not in allow-list — refusing call"
        )

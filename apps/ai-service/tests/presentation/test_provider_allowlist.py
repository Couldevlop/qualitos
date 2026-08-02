"""AI provider host allow-list — OWASP A10 + LLM07."""
from __future__ import annotations

import pytest

from infrastructure.providers._allowlist import (
    ALLOWED_HOSTS,
    ALLOWED_HOSTS_ENV,
    allowed_hosts,
    assert_host_allowed,
)


@pytest.mark.parametrize(
    "url",
    [
        "https://api.anthropic.com/v1/messages",
        "https://api.mistral.ai/v1/chat/completions",
        "http://ollama:11434/api/generate",
        "http://localhost:11434/api/generate",
    ],
)
def test_allowed_hosts_pass(url):
    assert_host_allowed(url)


@pytest.mark.parametrize(
    "url",
    [
        "http://attacker.com/llm",
        "https://169.254.169.254/latest/meta-data/",  # AWS metadata SSRF
        "https://internal-admin.local/secret",
        "http://10.0.0.1/exfil",
    ],
)
def test_blocked_hosts_raise(url):
    with pytest.raises(PermissionError):
        assert_host_allowed(url)


# --- Extension par l'environnement (déploiement hors docker-compose) ---------
# La liste ne contenait que le nom court `ollama`. Sur Kubernetes, le service
# porte un nom pleinement qualifié : le service refusait tout appel et
# redémarrait en boucle. L'extension doit autoriser ce nom SANS ouvrir la liste.

K8S_HOST = "ollama.qualitos-preprod.svc.cluster.local"


def test_fqdn_kubernetes_refuse_sans_declaration(monkeypatch):
    monkeypatch.delenv(ALLOWED_HOSTS_ENV, raising=False)
    with pytest.raises(PermissionError):
        assert_host_allowed(f"http://{K8S_HOST}:11434/api/generate")


def test_fqdn_kubernetes_accepte_une_fois_declare(monkeypatch):
    monkeypatch.setenv(ALLOWED_HOSTS_ENV, K8S_HOST)
    assert_host_allowed(f"http://{K8S_HOST}:11434/api/generate")


def test_extension_accepte_plusieurs_hotes_et_ignore_les_espaces(monkeypatch):
    monkeypatch.setenv(ALLOWED_HOSTS_ENV, f" {K8S_HOST} , qdrant.svc , ")
    assert_host_allowed(f"http://{K8S_HOST}:11434/api/generate")
    assert_host_allowed("http://qdrant.svc:6333/collections")


def test_extension_est_insensible_a_la_casse(monkeypatch):
    monkeypatch.setenv(ALLOWED_HOSTS_ENV, K8S_HOST.upper())
    assert_host_allowed(f"http://{K8S_HOST}:11434/api/generate")


def test_extension_vide_laisse_le_comportement_dorigine(monkeypatch):
    monkeypatch.setenv(ALLOWED_HOSTS_ENV, "   ")
    assert allowed_hosts() == ALLOWED_HOSTS
    with pytest.raises(PermissionError):
        assert_host_allowed("http://attacker.com/llm")


def test_extension_ne_peut_pas_retirer_un_hote_integre(monkeypatch):
    """L'extension est ADDITIVE : déclarer un seul hôte n'en retire aucun."""
    monkeypatch.setenv(ALLOWED_HOSTS_ENV, K8S_HOST)
    assert_host_allowed("https://api.anthropic.com/v1/messages")
    assert ALLOWED_HOSTS <= allowed_hosts()


def test_extension_nouvre_pas_la_liste_aux_autres_hotes(monkeypatch):
    monkeypatch.setenv(ALLOWED_HOSTS_ENV, K8S_HOST)
    with pytest.raises(PermissionError):
        assert_host_allowed("https://169.254.169.254/latest/meta-data/")

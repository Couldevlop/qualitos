"""Shared test fixtures."""
from __future__ import annotations

import os
import sys
from pathlib import Path
from uuid import UUID, uuid4

import pytest

# Ensure the ai-service root is importable when tests are run from CI/CD.
_AI_SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(_AI_SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(_AI_SERVICE_ROOT))

# Embeddings du RAG : les tests déclarent explicitement travailler avec des
# vecteurs de hachage (ADR 0049). Le conteneur refuse désormais de démarrer sans
# ce choix — c'est voulu : c'est ce silence qui laissait la production indexer
# avec un embedder de repli sans que personne ne le sache.
os.environ.setdefault("EMBEDDINGS_PROVIDER", "deterministic")

from domain.model.tenant import TenantContext, UserContext  # noqa: E402


@pytest.fixture()
def tenant_id() -> UUID:
    return UUID("11111111-1111-1111-1111-111111111111")


@pytest.fixture()
def user_context(tenant_id: UUID) -> UserContext:
    return UserContext(
        user_id=UUID("22222222-2222-2222-2222-222222222222"),
        tenant=TenantContext(tenant_id=tenant_id, issuer="test"),
        roles=frozenset({"analyst"}),
        correlation_id="test-corr-1",
    )


@pytest.fixture()
def other_tenant_user() -> UserContext:
    return UserContext(
        user_id=uuid4(),
        tenant=TenantContext(
            tenant_id=UUID("99999999-9999-9999-9999-999999999999"),
            issuer="test",
        ),
        roles=frozenset({"analyst"}),
        correlation_id="test-corr-2",
    )

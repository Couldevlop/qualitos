"""Jeton de service (client_credentials) → résolution du tenant par en-tête.

Un jeton `client_credentials` représente une machine, pas un utilisateur : il ne
porte aucun tenant. Jusqu'ici le validateur exigeait la claim `tid` dans TOUT
jeton, si bien que l'engine — pourtant porteur d'un jeton irréprochable — se
faisait répondre 401 sur chaque appel, et toutes les fonctions d'IA tombaient.

Contrat vérifié ici (transposé de l'ADR 0021, déjà appliqué à ai-vision-5s) :

1. la claim `tid` gagne toujours ;
2. à défaut, l'en-tête `X-Tenant-Id` n'est accepté QUE si l'`azp` du jeton
   (client Keycloak émetteur) figure dans la liste de confiance ;
3. tout le reste échoue — fail-closed.

Sans la restriction sur `azp`, l'en-tête rouvrirait la faille de `X-Dev-Claims` :
n'importe quel porteur d'un jeton valide se déclarerait de n'importe quel tenant.
"""
from __future__ import annotations

import pytest

from domain.model.errors import CrossTenantAccessError
from infrastructure.auth.keycloak_jwks_validator import _build_context

TRUSTED = frozenset({"api-quality-engine-ai"})
SUB = "0000000a-0000-0000-0000-000000000a01"
TENANT = "22222222-2222-2222-2222-222222222222"


def test_claim_tid_gagne_meme_si_un_en_tete_est_present():
    # Un jeton utilisateur porte son tenant : l'en-tête ne doit jamais primer,
    # sinon un utilisateur légitime pourrait sortir de son tenant.
    ctx = _build_context(
        {"sub": SUB, "tid": TENANT, "azp": "api-quality-engine-ai"},
        "iss",
        "corr",
        tenant_header="33333333-3333-3333-3333-333333333333",
        trusted_azp=TRUSTED,
    )
    assert str(ctx.tenant.tenant_id) == TENANT


def test_jeton_de_service_de_confiance_prend_le_tenant_de_l_en_tete():
    ctx = _build_context(
        {"sub": SUB, "azp": "api-quality-engine-ai"},
        "iss",
        "corr",
        tenant_header=TENANT,
        trusted_azp=TRUSTED,
    )
    assert str(ctx.tenant.tenant_id) == TENANT
    assert str(ctx.user_id) == SUB


def test_jeton_de_service_refuse_si_azp_non_de_confiance():
    with pytest.raises(CrossTenantAccessError):
        _build_context(
            {"sub": SUB, "azp": "client-quelconque"},
            "iss",
            "corr",
            tenant_header=TENANT,
            trusted_azp=TRUSTED,
        )


def test_jeton_de_service_refuse_si_la_liste_de_confiance_est_vide():
    # Défaut de configuration : aucun client déclaré ⇒ l'en-tête n'est JAMAIS cru.
    with pytest.raises(CrossTenantAccessError):
        _build_context(
            {"sub": SUB, "azp": "api-quality-engine-ai"},
            "iss",
            "corr",
            tenant_header=TENANT,
            trusted_azp=frozenset(),
        )


def test_jeton_de_service_refuse_sans_en_tete_de_tenant():
    with pytest.raises(CrossTenantAccessError):
        _build_context(
            {"sub": SUB, "azp": "api-quality-engine-ai"},
            "iss",
            "corr",
            tenant_header=None,
            trusted_azp=TRUSTED,
        )


def test_jeton_de_service_refuse_si_en_tete_pas_un_uuid():
    with pytest.raises(CrossTenantAccessError):
        _build_context(
            {"sub": SUB, "azp": "api-quality-engine-ai"},
            "iss",
            "corr",
            tenant_header="pas-un-uuid",
            trusted_azp=TRUSTED,
        )


def test_client_id_accepte_comme_variante_de_azp():
    # Selon la configuration du mapper Keycloak, le client émetteur se lit dans
    # `azp` ou dans `client_id` ; les deux doivent ouvrir le même chemin.
    ctx = _build_context(
        {"sub": SUB, "client_id": "api-quality-engine-ai"},
        "iss",
        "corr",
        tenant_header=TENANT,
        trusted_azp=TRUSTED,
    )
    assert str(ctx.tenant.tenant_id) == TENANT


def test_jeton_utilisateur_sans_tid_reste_refuse():
    # Aucun azp de confiance : le comportement historique ne bouge pas.
    with pytest.raises(CrossTenantAccessError):
        _build_context({"sub": SUB}, "iss", "corr")

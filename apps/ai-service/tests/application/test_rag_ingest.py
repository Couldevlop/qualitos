"""Ingestion du corpus RAG — ADR 0046.

Deux invariants portent l'essentiel de la valeur du module et sont vérifiés ici :
un fragment sans provenance n'entre pas dans le corpus, et une source dont la
licence n'autorise pas l'indexation est refusée — ce qui vise en premier lieu les
normes ISO, dont les éditeurs interdisent à la fois la redistribution et
l'introduction dans un système d'IA.
"""
from __future__ import annotations

from uuid import UUID, uuid4

import pytest

from application.usecase.rag_ingest import (
    ALLOWED_LICENCES,
    CHUNK_CHARS,
    RagIngestUseCase,
    SourceDocument,
    UnlicensedSourceError,
)
from domain.model.tenant import TenantContext, UserContext
from domain.port.pii_filter import PiiFilter, PiiScanResult
from infrastructure.vector.in_memory_vector_store import InMemoryVectorStore

TENANT = UUID("11111111-1111-1111-1111-111111111111")
OTHER_TENANT = UUID("22222222-2222-2222-2222-222222222222")


class FakeEmbedder:
    """Vecteur déterministe : la similarité reflète le contenu, sans modèle."""

    def __init__(self) -> None:
        self.calls: list[list[str]] = []

    def embed(self, texts: list[str]) -> list[list[float]]:
        self.calls.append(texts)
        return [self._vector(t) for t in texts]

    def dimension(self) -> int:
        return 3

    @staticmethod
    def _vector(text: str) -> list[float]:
        lowered = text.lower()
        return [
            float(lowered.count("audit")),
            float(lowered.count("soudure")),
            float(len(lowered) % 7) + 1.0,
        ]


class PassthroughPii(PiiFilter):
    def redact(self, text: str) -> PiiScanResult:
        return PiiScanResult(redacted_text=text, findings=(), had_pii=False)


class NameRedactingPii(PiiFilter):
    """Redaction minimale, suffisante pour prouver qu'elle a lieu à l'écriture."""

    def redact(self, text: str) -> PiiScanResult:
        redacted = text.replace("Jean Dupont", "<PERSON>")
        return PiiScanResult(
            redacted_text=redacted,
            findings=("PERSON",) if redacted != text else (),
            had_pii=redacted != text,
        )


class StrippingPii(PiiFilter):
    """Retire l'identité au lieu de la remplacer — un document qui n'est qu'un
    nom ne laisse alors rien à indexer."""

    def redact(self, text: str) -> PiiScanResult:
        redacted = text.replace("Jean Dupont", "")
        return PiiScanResult(
            redacted_text=redacted,
            findings=("PERSON",) if redacted != text else (),
            had_pii=redacted != text,
        )


def user_for(tenant_id: UUID = TENANT) -> UserContext:
    return UserContext(
        user_id=uuid4(),
        tenant=TenantContext(tenant_id=tenant_id, issuer="test"),
        roles=frozenset({"analyst"}),
        correlation_id="test-corr",
    )


def source(**over) -> SourceDocument:
    base = {
        "source_id": "doc-proc-001",
        "title": "Procédure de contrôle en réception",
        "content": "Contrôle en réception des matières premières.",
        "origin": "qualitos.documents",
        "licence": "tenant-owned",
        "version": "3",
    }
    base.update(over)
    return SourceDocument(**base)


@pytest.fixture()
def store() -> InMemoryVectorStore:
    return InMemoryVectorStore()


@pytest.fixture()
def embedder() -> FakeEmbedder:
    return FakeEmbedder()


@pytest.fixture()
def usecase(store: InMemoryVectorStore, embedder: FakeEmbedder) -> RagIngestUseCase:
    return RagIngestUseCase(store, embedder, PassthroughPii())


# ---------------------------------------------------------------------------
# Provenance et licence
# ---------------------------------------------------------------------------


def test_refuse_une_source_sans_origine() -> None:
    with pytest.raises(ValueError, match="origin required"):
        source(origin="")


def test_refuse_une_source_sans_contenu() -> None:
    with pytest.raises(ValueError, match="content required"):
        source(content="   ")


def test_refuse_une_norme_iso_meme_presentee_comme_un_document_du_tenant() -> None:
    # Le cas concret que l'ADR 0046 interdit : recopier une clause ISO dans le
    # corpus. La licence est le seul point de contrôle qui tienne.
    with pytest.raises(UnlicensedSourceError) as exc:
        source(origin="iso.org", licence="iso-copyright")

    assert "not cleared for indexing" in str(exc.value)


def test_accepte_les_licences_ouvertes_du_benchmark() -> None:
    for licence in ("tenant-owned", "cc-by-4.0", "etalab-2.0", "public-domain"):
        assert source(licence=licence).licence in ALLOWED_LICENCES


# ---------------------------------------------------------------------------
# Découpage
# ---------------------------------------------------------------------------


def test_un_document_court_donne_un_seul_fragment(
    usecase: RagIngestUseCase, store: InMemoryVectorStore
) -> None:
    result = usecase.execute(user_for(), [source()])

    assert result.documents_read == 1
    assert result.fragments_indexed == 1
    assert result.skipped_empty == 0


def test_les_paragraphes_restent_entiers_tant_qu_ils_tiennent(
    usecase: RagIngestUseCase, embedder: FakeEmbedder
) -> None:
    contenu = "\n\n".join([f"Paragraphe {i} du mode opératoire." for i in range(4)])

    usecase.execute(user_for(), [source(content=contenu)])

    # Quatre paragraphes courts tiennent dans un fragment : on ne découpe pas
    # pour découper, un fragment éclaté ne répond plus à rien.
    assert len(embedder.calls[0]) == 1
    assert "Paragraphe 0" in embedder.calls[0][0]
    assert "Paragraphe 3" in embedder.calls[0][0]


def test_un_document_long_est_decoupe_sans_perdre_de_texte(
    usecase: RagIngestUseCase, embedder: FakeEmbedder
) -> None:
    paragraphes = [f"Exigence {i}. " + "détail " * 60 for i in range(6)]

    usecase.execute(user_for(), [source(content="\n\n".join(paragraphes))])

    fragments = embedder.calls[0]
    assert len(fragments) > 1
    assert all(len(f) <= CHUNK_CHARS for f in fragments)
    # Chaque exigence reste retrouvable après découpage.
    joint = " ".join(fragments)
    for i in range(6):
        assert f"Exigence {i}." in joint


def test_un_paragraphe_plus_long_qu_un_fragment_est_recoupe_avec_recouvrement(
    usecase: RagIngestUseCase, embedder: FakeEmbedder
) -> None:
    pave = "a" * (CHUNK_CHARS * 2)

    usecase.execute(user_for(), [source(content=pave)])

    fragments = embedder.calls[0]
    assert len(fragments) >= 2
    assert all(len(f) <= CHUNK_CHARS for f in fragments)
    # Recouvrement : la somme des fragments dépasse la longueur d'origine.
    assert sum(len(f) for f in fragments) > len(pave)


def test_un_document_entierement_caviarde_est_signale_comme_ignore(
    store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    # Un document qui n'est QUE de la donnée personnelle ne laisse rien à
    # indexer. L'appel réussit, mais le corpus ne contient rien : le compter
    # comme indexé donnerait à l'utilisateur une fausse assurance.
    usecase = RagIngestUseCase(store, embedder, StrippingPii())

    result = usecase.execute(user_for(), [source(content="Jean Dupont")])

    assert result.documents_read == 1
    assert result.fragments_indexed == 0
    assert result.skipped_empty == 1


# ---------------------------------------------------------------------------
# Provenance portée par chaque fragment
# ---------------------------------------------------------------------------


def test_chaque_fragment_porte_sa_source_sa_licence_et_sa_version(
    usecase: RagIngestUseCase, store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    usecase.execute(
        user_for(),
        [source(url="https://intranet/proc-001", version="3")],
    )

    hits = store.search(TENANT, embedder.embed(["contrôle en réception"])[0], 5, 0.0)
    doc = hits[0][0]

    assert doc.metadata["origin"] == "qualitos.documents"
    assert doc.metadata["licence"] == "tenant-owned"
    assert doc.metadata["version"] == "3"
    assert doc.metadata["url"] == "https://intranet/proc-001"
    assert doc.metadata["title"] == "Procédure de contrôle en réception"


def test_l_identifiant_du_fragment_designe_le_document_et_son_rang(
    usecase: RagIngestUseCase, store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    paragraphes = [f"Exigence {i}. " + "détail " * 60 for i in range(6)]

    usecase.execute(user_for(), [source(content="\n\n".join(paragraphes))])

    hits = store.search(TENANT, embedder.embed(["exigence"])[0], 20, 0.0)
    ids = sorted(d.document_id for d, _ in hits)
    assert ids[0].startswith("doc-proc-001#")
    # La citation reste traçable jusqu'au fragment exact.
    assert all("#" in i for i in ids)


# ---------------------------------------------------------------------------
# Redaction et isolation
# ---------------------------------------------------------------------------


def test_les_donnees_personnelles_sont_retirees_avant_stockage(
    store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    # Redaction à l'écriture et non à la lecture : ce qui n'est jamais stocké ne
    # peut pas fuiter, y compris via une requête ultérieure (LLM06).
    usecase = RagIngestUseCase(store, embedder, NameRedactingPii())

    usecase.execute(
        user_for(),
        [source(content="Audit réalisé par Jean Dupont le 12 mars.")],
    )

    hits = store.search(TENANT, embedder.embed(["audit"])[0], 5, 0.0)
    assert "Jean Dupont" not in hits[0][0].content
    assert "<PERSON>" in hits[0][0].content


def test_le_corpus_est_cloisonne_par_tenant(
    usecase: RagIngestUseCase, store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    usecase.execute(user_for(TENANT), [source(content="Soudure poste 3.")])

    vector = embedder.embed(["soudure"])[0]
    assert store.search(TENANT, vector, 5, 0.0)
    assert store.search(OTHER_TENANT, vector, 5, 0.0) == []


def test_le_droit_a_l_effacement_vide_le_corpus_du_tenant(
    usecase: RagIngestUseCase, store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    usecase.execute(user_for(TENANT), [source()])
    usecase.execute(user_for(OTHER_TENANT), [source()])

    usecase.forget_tenant(user_for(TENANT))

    vector = embedder.embed(["contrôle"])[0]
    assert store.search(TENANT, vector, 5, 0.0) == []
    # L'effacement d'un tenant ne touche pas les autres.
    assert store.search(OTHER_TENANT, vector, 5, 0.0)


# ---------------------------------------------------------------------------
# Réindexation
# ---------------------------------------------------------------------------


def test_reindexer_un_document_remplace_l_ancienne_version(
    usecase: RagIngestUseCase, store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    usecase.execute(user_for(), [source(content="Version initiale du contrôle.")])
    usecase.execute(user_for(), [source(content="Version révisée du contrôle.")])

    hits = store.search(TENANT, embedder.embed(["contrôle"])[0], 10, 0.0)

    # Une procédure révisée ne doit pas rester interrogeable sous son ancienne
    # rédaction : sinon l'assistant cite un texte qui n'est plus en vigueur.
    contents = [d.content for d, _ in hits]
    assert any("révisée" in c for c in contents)
    assert not any("initiale" in c for c in contents)


def test_un_document_indexe_est_effectivement_retrouvable(
    usecase: RagIngestUseCase, store: InMemoryVectorStore, embedder: FakeEmbedder
) -> None:
    # Régression directe du défaut d'origine : l'ancien magasin rangeait `[0.0]`
    # comme vecteur, donc tout document indexé restait introuvable.
    usecase.execute(user_for(), [source(content="Audit interne du procédé.")])

    hits = store.search(TENANT, embedder.embed(["audit"])[0], 5, 0.1)

    assert hits
    assert hits[0][1] > 0.0


# ---------------------------------------------------------------------------
# Contrat du magasin
# ---------------------------------------------------------------------------


def test_le_magasin_refuse_un_nombre_de_vecteurs_incoherent(
    store: InMemoryVectorStore,
) -> None:
    from datetime import datetime, timezone

    from domain.model.rag import RagDocument

    doc = RagDocument(
        document_id="d1",
        tenant_id=TENANT,
        content="texte",
        metadata={},
        indexed_at=datetime.now(timezone.utc),
    )

    with pytest.raises(ValueError, match="length mismatch"):
        store.upsert(TENANT, [doc], [])

"""Use case: ingest REAL tenant documents into the tenant's RAG corpus.

Governed by ADR 0046. Two rules carry the whole design:

1. **No provenance, no indexing.** Every fragment records where it comes from and
   under which licence. A citation the user cannot trace back is worse than no
   answer, because it looks authoritative.
2. **Nothing is authored here.** This use case indexes text it receives; it never
   generates, completes or paraphrases. Copyrighted normative text (ISO & co.)
   never reaches it — that is enforced upstream, and re-checked here by refusing
   any source whose licence is not among the ones cleared in ADR 0046.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone

from domain.model.rag import RagDocument
from domain.model.tenant import UserContext
from domain.port.embedder import Embedder
from domain.port.pii_filter import PiiFilter
from domain.port.vector_store import VectorStore

#: Licences cleared for indexing (ADR 0046). `tenant-owned` covers the client's
#: own quality records; the others are public texts whose reuse is granted.
ALLOWED_LICENCES = frozenset(
    {
        "tenant-owned",
        "cc-by-4.0",  # EUR-Lex — Commission Decision 2011/833/EU
        "etalab-2.0",  # Légifrance / DILA
        "public-domain",  # US federal works (17 U.S.C. §105), openFDA CC0
    }
)

#: Characters per fragment. BGE-M3 handles longer inputs, but retrieval quality
#: collapses when a fragment mixes several unrelated requirements — and a
#: citation pointing at ten pages is not a citation.
CHUNK_CHARS = 1200
CHUNK_OVERLAP = 150


class UnlicensedSourceError(ValueError):
    """Raised when a source's licence is not cleared for indexing."""


@dataclass(frozen=True, slots=True)
class SourceDocument:
    """A real document to index, with the provenance that makes it citable."""

    source_id: str
    title: str
    content: str
    #: Where it comes from — e.g. `qualitos.documents`, `eur-lex`, `legifrance`.
    origin: str
    licence: str
    #: Version or revision of the source, so a citation stays reproducible.
    version: str = ""
    #: Public URL, when the source has one.
    url: str = ""

    def __post_init__(self) -> None:
        if not self.source_id:
            raise ValueError("source_id required")
        if not self.content or not self.content.strip():
            raise ValueError("content required")
        if not self.origin:
            raise ValueError("origin required — an untraceable fragment is not indexed")
        if self.licence not in ALLOWED_LICENCES:
            raise UnlicensedSourceError(
                f"licence '{self.licence}' is not cleared for indexing (ADR 0046); "
                f"allowed: {sorted(ALLOWED_LICENCES)}"
            )


@dataclass(frozen=True, slots=True)
class IngestResult:
    documents_read: int
    fragments_indexed: int
    #: Documents that yielded no indexable fragment — in practice, those whose
    #: whole content was personal data and disappeared under redaction. The
    #: caller must be told: such a document is accepted, stored nowhere, and
    #: would otherwise look indexed.
    skipped_empty: int


class RagIngestUseCase:
    """Chunk, embed and store real documents in the tenant's collection."""

    def __init__(
        self,
        vector_store: VectorStore,
        embedder: Embedder,
        pii_filter: PiiFilter,
    ) -> None:
        self._store = vector_store
        self._embedder = embedder
        self._pii = pii_filter

    def execute(self, user: UserContext, sources: list[SourceDocument]) -> IngestResult:
        fragments: list[RagDocument] = []
        skipped = 0
        now = datetime.now(timezone.utc)

        for source in sources:
            chunks = _chunk(source.content)
            kept_for_source = 0
            for index, chunk in enumerate(chunks):
                # A quality record routinely names people (auditor, owner,
                # complainant). The corpus is queried by an LLM, so the redaction
                # happens BEFORE storage, not on the way out: what is never
                # stored cannot leak (LLM06).
                redacted = self._pii.redact(chunk).redacted_text
                if not redacted.strip():
                    continue
                kept_for_source += 1
                fragments.append(
                    RagDocument(
                        document_id=f"{source.source_id}#{index}",
                        tenant_id=user.tenant.tenant_id,
                        content=redacted,
                        metadata={
                            "title": source.title,
                            "origin": source.origin,
                            "licence": source.licence,
                            "version": source.version,
                            "url": source.url,
                            "fragment": str(index),
                            "fragments_total": str(len(chunks)),
                        },
                        indexed_at=now,
                    )
                )
            if kept_for_source == 0:
                skipped += 1

        if not fragments:
            return IngestResult(len(sources), 0, skipped)

        embeddings = self._embedder.embed([f.content for f in fragments])
        indexed = self._store.upsert(user.tenant.tenant_id, fragments, embeddings)
        return IngestResult(len(sources), indexed, skipped)

    def forget_tenant(self, user: UserContext) -> None:
        """RGPD art. 17 — drop the whole tenant collection."""
        self._store.delete_collection(user.tenant.tenant_id)


def _chunk(text: str) -> list[str]:
    """Split on paragraph boundaries, packing up to CHUNK_CHARS.

    Splitting on a fixed character count alone would cut mid-sentence and produce
    fragments that answer nothing. Paragraphs are the natural unit of a procedure
    or a regulation article, so they are kept whole whenever they fit.
    """
    normalized = text.replace("\r\n", "\n").strip()
    if not normalized:
        return []

    paragraphs = [p.strip() for p in normalized.split("\n\n") if p.strip()]
    if not paragraphs:
        return []

    chunks: list[str] = []
    current = ""
    for paragraph in paragraphs:
        # A paragraph longer than a whole fragment is split on its own, with an
        # overlap so a requirement straddling the cut stays retrievable.
        if len(paragraph) > CHUNK_CHARS:
            if current:
                chunks.append(current)
                current = ""
            chunks.extend(_split_long(paragraph))
            continue
        candidate = f"{current}\n\n{paragraph}" if current else paragraph
        if len(candidate) > CHUNK_CHARS:
            chunks.append(current)
            current = paragraph
        else:
            current = candidate
    if current:
        chunks.append(current)
    return chunks


def _split_long(paragraph: str) -> list[str]:
    step = CHUNK_CHARS - CHUNK_OVERLAP
    return [
        paragraph[start : start + CHUNK_CHARS]
        for start in range(0, len(paragraph), step)
        if paragraph[start : start + CHUNK_CHARS].strip()
    ]

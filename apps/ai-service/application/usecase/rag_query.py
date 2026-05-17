"""Use case: RAG query over the tenant's quality corpus."""
from __future__ import annotations

from dataclasses import dataclass

from domain.model.completion import (
    Citation,
    CompletionRequest,
    Confidence,
    ProviderName,
)
from domain.model.errors import PromptInjectionError
from domain.model.rag import RagQuery, RagResult
from domain.model.tenant import UserContext
from domain.port.ai_provider import AIProvider
from domain.port.embedder import Embedder
from domain.port.pii_filter import PiiFilter
from domain.port.prompt_audit_logger import (
    PromptAuditEntry,
    PromptAuditLogger,
)
from domain.port.prompt_injection_filter import PromptInjectionFilter
from domain.port.vector_store import VectorStore

_SYSTEM_PROMPT = (
    "You are a QualitOS quality assistant. Answer ONLY using the provided "
    "context. If the context is insufficient, say so. Always cite the "
    "document_ids you used as [doc_id]. Do not invent facts."
)


@dataclass(frozen=True, slots=True)
class RagQueryRequest:
    question: str
    top_k: int = 5
    min_score: float = 0.5
    provider: ProviderName = ProviderName.OLLAMA


@dataclass(frozen=True, slots=True)
class RagQueryResult:
    rag: RagResult
    confidence: Confidence


class RagQueryUseCase:
    """RAG over a tenant-scoped vector collection."""

    def __init__(
        self,
        vector_store: VectorStore,
        embedder: Embedder,
        providers: dict[ProviderName, AIProvider],
        pii_filter: PiiFilter,
        injection_filter: PromptInjectionFilter,
        audit_logger: PromptAuditLogger,
    ) -> None:
        self._store = vector_store
        self._embedder = embedder
        self._providers = providers
        self._pii = pii_filter
        self._injection = injection_filter
        self._audit = audit_logger

    def execute(self, user: UserContext, req: RagQueryRequest) -> RagQueryResult:
        injection = self._injection.scan(req.question)
        if injection.suspicious:
            raise PromptInjectionError(
                f"Prompt-injection suspected (score={injection.score:.2f})"
            )

        sanitized = self._pii.redact(req.question).redacted_text

        query = RagQuery(
            question=sanitized,
            tenant=user.tenant,
            top_k=req.top_k,
            min_score=req.min_score,
        )

        embedding = self._embedder.embed([sanitized])[0]
        hits = self._store.search(
            tenant_id=user.tenant.tenant_id,
            query_embedding=embedding,
            top_k=query.top_k,
            min_score=query.min_score,
        )

        if not hits:
            empty = RagResult(
                answer="No relevant documents found in the tenant corpus.",
                documents=(),
                scores=(),
                explanation="vector-store returned 0 hits",
            )
            return RagQueryResult(rag=empty, confidence=Confidence(0.1, "heuristic"))

        docs, scores = zip(*hits)
        context = "\n\n---\n".join(
            f"[{d.document_id}] {d.content[:1000]}" for d in docs
        )
        user_prompt = (
            f"Context (use only this):\n{context}\n\n"
            f"Question: {sanitized}\n\n"
            "Answer with citations like [doc_id]."
        )

        provider = self._providers[req.provider]
        completion = provider.complete(
            CompletionRequest(
                system_prompt=_SYSTEM_PROMPT,
                user_prompt=user_prompt,
                provider=req.provider,
                max_tokens=1024,
                temperature=0.1,
            )
        )

        scan_out = self._pii.redact(completion.text)
        citations = tuple(
            Citation(document_id=d.document_id, score=s, excerpt=d.content[:200])
            for d, s in hits
        )

        rag = RagResult(
            answer=scan_out.redacted_text,
            documents=docs,
            scores=scores,
            explanation=f"top-{len(docs)} from tenant collection, avg score "
            f"{sum(scores) / len(scores):.2f}",
        )

        # Confidence is the mean retrieval score (heuristic).
        conf = Confidence(value=min(0.99, sum(scores) / len(scores)), method="heuristic")

        self._audit.log(
            PromptAuditEntry(
                tenant_id=user.tenant.tenant_id,
                user_id=user.user_id,
                correlation_id=user.correlation_id,
                operation="rag.query",
                provider=req.provider.value,
                redacted_prompt=sanitized[:1000],
                redacted_response=scan_out.redacted_text[:1000],
                latency_ms=completion.latency_ms,
                tokens_used=completion.tokens_used,
            )
        )

        return RagQueryResult(rag=rag, confidence=conf)

"""POST /v1/ai/rag/query."""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from application.usecase.rag_ingest import SourceDocument, UnlicensedSourceError
from application.usecase.rag_query import RagQueryRequest
from domain.model.errors import (
    PromptInjectionError,
    ProviderUnavailableError,
)
from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.rag import (
    RagDocumentSchema,
    RagIngestRequestSchema,
    RagIngestResponseSchema,
    RagQueryRequestSchema,
    RagQueryResponseSchema,
)
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/rag", tags=["rag"])
_container = Container.build_default()


@router.post(
    "/query",
    response_model=RagQueryResponseSchema,
    summary="RAG over the tenant's quality corpus",
)
async def query(
    payload: RagQueryRequestSchema,
    user: UserContext = Depends(current_user),
) -> RagQueryResponseSchema:
    try:
        result = _container.rag_query().execute(
            user,
            RagQueryRequest(
                question=payload.question,
                top_k=payload.top_k,
                min_score=payload.min_score,
                provider=payload.provider,
            ),
        )
    except PromptInjectionError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ProviderUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    docs = [
        RagDocumentSchema(document_id=d.document_id, score=s, excerpt=d.content[:200])
        for d, s in zip(result.rag.documents, result.rag.scores)
    ]
    return RagQueryResponseSchema(
        answer=result.rag.answer,
        documents=docs,
        confidence=result.confidence.value,
        confidence_method=result.confidence.method,
        explanation=result.rag.explanation,
    )


@router.post(
    "/ingest",
    response_model=RagIngestResponseSchema,
    summary="Index real tenant documents into the tenant's corpus",
)
async def ingest(
    payload: RagIngestRequestSchema,
    user: UserContext = Depends(current_user),
) -> RagIngestResponseSchema:
    """Index documents the tenant already owns.

    The collection is derived from the JWT tenant, never from the payload
    (§18.2 #2) — a caller cannot write into another tenant's corpus.
    """
    try:
        sources = [
            SourceDocument(
                source_id=s.source_id,
                title=s.title,
                content=s.content,
                origin=s.origin,
                licence=s.licence,
                version=s.version,
                url=s.url,
            )
            for s in payload.sources
        ]
    except UnlicensedSourceError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    result = _container.rag_ingest().execute(user, sources)
    return RagIngestResponseSchema(
        documents_read=result.documents_read,
        fragments_indexed=result.fragments_indexed,
        skipped_empty=result.skipped_empty,
    )

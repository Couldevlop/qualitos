"""POST /v1/ai/rag/query."""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from application.usecase.rag_query import RagQueryRequest
from domain.model.errors import (
    PromptInjectionError,
    ProviderUnavailableError,
)
from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.rag import (
    RagDocumentSchema,
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

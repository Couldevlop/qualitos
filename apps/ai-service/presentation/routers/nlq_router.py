"""POST /v1/ai/nlq/ask."""
from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException

from application.usecase.nlq_ask import NlqAskRequest
from domain.model.errors import (
    PromptInjectionError,
    ProviderUnavailableError,
    UnsafeSqlError,
)
from domain.model.tenant import UserContext
from presentation.container import Container
from presentation.schemas.nlq import (
    ChartSpecSchema,
    GeneratedSqlSchema,
    NlqRequestSchema,
    NlqResponseSchema,
)
from presentation.security import current_user

router = APIRouter(prefix="/v1/ai/nlq", tags=["nlq"])
_container = Container.build_default()


@router.post(
    "/ask",
    response_model=NlqResponseSchema,
    summary="Natural Language Query â€” generates a validated, read-only SELECT",
)
async def ask(
    payload: NlqRequestSchema,
    user: UserContext = Depends(current_user),
) -> NlqResponseSchema:
    try:
        answer = _container.nlq_ask().execute(
            user,
            NlqAskRequest(
                question=payload.question,
                provider=payload.provider,
                max_rows=payload.max_rows,
            ),
        )
    except PromptInjectionError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except UnsafeSqlError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ProviderUnavailableError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    return NlqResponseSchema(
        question=answer.question,
        sql=GeneratedSqlSchema(
            sql=answer.sql.sql,
            parameters=answer.sql.parameters,
            tables_used=list(answer.sql.tables_used),
            functions_used=list(answer.sql.functions_used),
            tenant_filter_applied=answer.sql.tenant_filter_applied,
        ),
        rows=list(answer.rows),
        chart=ChartSpecSchema(
            chart_type=answer.chart.chart_type,
            title=answer.chart.title,
            x_axis=list(answer.chart.x_axis),
            series=list(answer.chart.series),
        ),
        narrative=answer.narrative,
        confidence=answer.confidence,
        row_count=answer.row_count,
    )

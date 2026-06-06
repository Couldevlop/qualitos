"""FastAPI application factory."""
from __future__ import annotations

import logging
import os

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from domain.model.errors import DomainError
from presentation.routers import (
    completion_router,
    federated_router,
    health_router,
    nlq_router,
    predict_router,
    rag_router,
    spc_router,
)

logging.basicConfig(
    level=os.environ.get("QOS_LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)


def create_app() -> FastAPI:
    is_prod = os.environ.get("QOS_ENV", "dev").lower() == "prod"
    app = FastAPI(
        title="QualitOS AI Service",
        version="0.1.0",
        description=(
            "OWASP-hardened AI gateway for QualitOS â€” completion, RAG, NLQ, "
            "federated learning scaffold."
        ),
        # OWASP A05: hide /docs in prod.
        docs_url=None if is_prod else "/docs",
        redoc_url=None if is_prod else "/redoc",
        openapi_url=None if is_prod else "/openapi.json",
    )

    app.include_router(health_router)
    app.include_router(completion_router)
    app.include_router(rag_router)
    app.include_router(nlq_router)
    app.include_router(federated_router)
    app.include_router(spc_router)
    app.include_router(predict_router)

    @app.exception_handler(DomainError)
    async def _domain_error_handler(_: Request, exc: DomainError) -> JSONResponse:
        return JSONResponse(
            status_code=exc.http_status,
            content={
                "type": f"https://qualitos.io/errors/{exc.code}",
                "title": exc.__class__.__name__,
                "status": exc.http_status,
                "detail": str(exc),
                "code": exc.code,
            },
        )

    return app


app = create_app()

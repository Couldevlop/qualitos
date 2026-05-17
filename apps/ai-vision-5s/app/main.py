"""FastAPI entrypoint for the QualitOS AI Vision 5S service.

CLAUDE.md §9 + §3.2 — vision-driven 5S scoring with on-prem inference.
Default port: 8090.
"""

from __future__ import annotations

import logging
import os

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import Response

from app.infrastructure.auth import assert_production_safe
from app.presentation.routes import limiter, router

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="ts=%(asctime)s level=%(levelname)s logger=%(name)s msg=%(message)s",
)


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    """OWASP A05 — secure default HTTP headers."""

    async def dispatch(self, request, call_next) -> Response:
        response: Response = await call_next(request)
        response.headers.setdefault("X-Content-Type-Options", "nosniff")
        response.headers.setdefault("X-Frame-Options", "DENY")
        response.headers.setdefault("Referrer-Policy", "no-referrer")
        response.headers.setdefault(
            "Content-Security-Policy",
            "default-src 'none'; frame-ancestors 'none'",
        )
        response.headers.setdefault(
            "Strict-Transport-Security",
            "max-age=31536000; includeSubDomains",
        )
        return response


def create_app() -> FastAPI:
    assert_production_safe()
    app = FastAPI(
        title="QualitOS — Vision 5S",
        version="0.1.0",
        openapi_url="/v1/openapi.json",
        docs_url="/v1/docs",
        redoc_url=None,
    )
    app.state.limiter = limiter
    app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
    app.add_middleware(SecurityHeadersMiddleware)
    app.include_router(router)

    @app.get("/health", include_in_schema=False)
    async def health() -> dict[str, str]:
        return {"status": "UP"}

    @app.exception_handler(RequestValidationError)
    async def validation_handler(_request, exc):
        return JSONResponse(
            status_code=400,
            media_type="application/problem+json",
            content={
                "type": "https://qualitos.local/errors/validation",
                "title": "Validation failed",
                "status": 400,
                "detail": str(exc.errors()),
            },
        )

    return app


app = create_app()

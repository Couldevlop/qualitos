"""Structured JSON audit logger for prompts (OWASP A09).

Writes to stdout via the `logging` module so the platform's log pipeline
(OpenTelemetry / Loki / OpenSearch) ingests it. Never logs raw PII â€” the
entries reaching this logger are already redacted by Presidio.
"""
from __future__ import annotations

import json
import logging
from datetime import datetime, timezone

from domain.port.prompt_audit_logger import PromptAuditEntry, PromptAuditLogger


class JsonPromptAuditLogger(PromptAuditLogger):
    """One JSON line per audit entry."""

    def __init__(self, logger_name: str = "qos.ai.audit") -> None:
        self._logger = logging.getLogger(logger_name)

    def log(self, entry: PromptAuditEntry) -> None:
        payload = {
            "ts": datetime.now(timezone.utc).isoformat(),
            "tenant_id": str(entry.tenant_id),
            "user_id": str(entry.user_id),
            "correlation_id": entry.correlation_id,
            "operation": entry.operation,
            "provider": entry.provider,
            "prompt": entry.redacted_prompt,
            "response": entry.redacted_response,
            "latency_ms": entry.latency_ms,
            "tokens_used": entry.tokens_used,
        }
        self._logger.info(json.dumps(payload, ensure_ascii=False))

"""JsonPromptAuditLogger smoke test."""
from __future__ import annotations

import logging
from uuid import UUID

from domain.port.prompt_audit_logger import PromptAuditEntry
from infrastructure.persistence import JsonPromptAuditLogger


def test_audit_logger_writes_one_json_line(caplog):
    entry = PromptAuditEntry(
        tenant_id=UUID(int=1),
        user_id=UUID(int=2),
        correlation_id="cid",
        operation="complete",
        provider="ollama",
        redacted_prompt="hello",
        redacted_response="world",
        latency_ms=10,
        tokens_used=5,
    )
    logger_name = "qos.ai.audit.test"
    logger = JsonPromptAuditLogger(logger_name)
    with caplog.at_level(logging.INFO, logger=logger_name):
        logger.log(entry)
    assert any("complete" in r.message for r in caplog.records)

"""Microsoft Presidio adapter (LLM06 + A09).

Imports presidio lazily â€” if it isn't installed, we fall back to heuristics.
"""
from __future__ import annotations

from typing import Any

from domain.port.pii_filter import PiiFilter, PiiScanResult
from .heuristic_pii_filter import HeuristicPiiFilter


class PresidioPiiFilter(PiiFilter):
    """Wraps `presidio-analyzer` + `presidio-anonymizer`."""

    def __init__(self, analyzer: Any | None = None, anonymizer: Any | None = None) -> None:
        self._fallback = HeuristicPiiFilter()
        self._analyzer = analyzer
        self._anonymizer = anonymizer
        if analyzer is None or anonymizer is None:
            try:
                from presidio_analyzer import AnalyzerEngine
                from presidio_anonymizer import AnonymizerEngine

                self._analyzer = analyzer or AnalyzerEngine()
                self._anonymizer = anonymizer or AnonymizerEngine()
            except Exception:  # pragma: no cover - import-time degradation
                self._analyzer = None
                self._anonymizer = None

    def redact(self, text: str, language: str = "en") -> PiiScanResult:
        if not text:
            return PiiScanResult(redacted_text=text or "", findings=(), had_pii=False)
        if self._analyzer is None or self._anonymizer is None:
            return self._fallback.redact(text, language)
        try:
            results = self._analyzer.analyze(text=text, language=language)
            anonymized = self._anonymizer.anonymize(text=text, analyzer_results=results)
            categories = tuple(sorted({r.entity_type for r in results}))
            return PiiScanResult(
                redacted_text=anonymized.text,
                findings=categories,
                had_pii=bool(categories),
            )
        except Exception:
            # Never let PII detection crash the request â€” fall back to heuristic.
            return self._fallback.redact(text, language)

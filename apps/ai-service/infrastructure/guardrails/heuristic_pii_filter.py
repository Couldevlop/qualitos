"""Pure-Python PII redactor.

Used as a fallback when Presidio is unavailable (tests, lean containers).
Detects the most common categories: EMAIL, PHONE, IBAN, IP, SSN/CC numeric.
"""
from __future__ import annotations

import re

from domain.port.pii_filter import PiiFilter, PiiScanResult

# Patterns are simple by design - Presidio is preferred in prod.
# IMPORTANT: order matters - the more specific patterns must run first so the
# generic PHONE matcher does not eat SSN/CARD digits.
_PATTERNS: tuple[tuple[str, re.Pattern[str], str], ...] = (
    ("EMAIL", re.compile(r"[\w.+-]+@[\w-]+\.[\w.-]+"), "<EMAIL>"),
    ("IBAN", re.compile(r"\b[A-Z]{2}\d{2}[A-Z0-9]{10,30}\b"), "<IBAN>"),
    ("IPV4", re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b"), "<IP>"),
    ("SSN_US", re.compile(r"\b\d{3}-\d{2}-\d{4}\b"), "<SSN>"),
    ("CARD16", re.compile(r"\b(?:\d{4}[\s-]?){3}\d{4}\b"), "<CARD>"),
    (
        "PHONE",
        re.compile(
            r"(?<!\d)(?:\+\d{1,3}[\s.-]?)?(?:\(?\d{2,4}\)?[\s.-]?){2,4}\d{2,4}(?!\d)"
        ),
        "<PHONE>",
    ),
)


class HeuristicPiiFilter(PiiFilter):
    """Cheap deterministic PII redactor."""

    def redact(self, text: str, language: str = "en") -> PiiScanResult:
        if not text:
            return PiiScanResult(redacted_text=text or "", findings=(), had_pii=False)
        findings: list[str] = []
        out = text
        for label, pattern, replacement in _PATTERNS:
            new = pattern.sub(replacement, out)
            if new != out:
                findings.append(label)
                out = new
        return PiiScanResult(
            redacted_text=out,
            findings=tuple(findings),
            had_pii=bool(findings),
        )

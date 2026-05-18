"""Heuristic prompt-injection detector (LLM01).

Pure-Python pattern bank, no ML dependency. Score = max(category weight).
Production hardening: add a small classifier (Rebuff/LlamaGuard) behind this.
"""
from __future__ import annotations

import re

from domain.port.prompt_injection_filter import (
    InjectionScanResult,
    PromptInjectionFilter,
)

# Each pattern carries a score + rationale.
_PATTERNS: tuple[tuple[float, re.Pattern[str], str], ...] = (
    (0.95, re.compile(r"ignore (all |previous |the )?(instructions|prompts)", re.I), "ignore-instructions"),
    (0.95, re.compile(r"disregard (all |previous )?(instructions|rules)", re.I), "disregard-instructions"),
    (0.90, re.compile(r"forget (everything|all previous)", re.I), "forget-everything"),
    (0.90, re.compile(r"you are (now |actually )?(an? )?(unrestricted|dan|jailbreak)", re.I), "role-jailbreak"),
    (0.85, re.compile(r"system\s*:\s*you\s+are", re.I), "fake-system-prompt"),
    (0.85, re.compile(r"(?:reveal|print|show|leak)\s+(?:your\s+)?(?:system\s+)?(?:prompt|instructions)", re.I), "system-prompt-leak"),
    (0.80, re.compile(r"\[\s*(?:end|start|stop)\s+of\s+system\s*\]", re.I), "delimiter-spoof"),
    (0.75, re.compile(r"```\s*system", re.I), "system-codefence"),
    (0.70, re.compile(r"developer\s+mode|debug\s+mode", re.I), "developer-mode"),
    (0.70, re.compile(r"act as (?:if you (?:were|are) )?(?:not bound|free)", re.I), "free-actor"),
    (0.60, re.compile(r"<\s*(?:script|iframe|img)\b", re.I), "html-injection"),
)


class HeuristicInjectionFilter(PromptInjectionFilter):
    """Pattern bank + score aggregation."""

    SUSPICIOUS_THRESHOLD = 0.6

    def scan(self, user_prompt: str) -> InjectionScanResult:
        if not user_prompt:
            return InjectionScanResult(suspicious=False, score=0.0, rationale="empty")
        best_score = 0.0
        best_label = ""
        for score, pat, label in _PATTERNS:
            if pat.search(user_prompt):
                if score > best_score:
                    best_score = score
                    best_label = label
        return InjectionScanResult(
            suspicious=best_score >= self.SUSPICIOUS_THRESHOLD,
            score=best_score,
            rationale=best_label or "no-match",
        )

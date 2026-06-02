"""Domain model for SPC (Statistical Process Control) anomaly detection.

Univariate control-chart analysis for individuals (I-chart): control limits +
the 8 Nelson rules (a superset of the classic Western Electric rules). All
value objects are immutable; the detection logic lives in
``domain.service.spc_rules`` as pure functions (CLAUDE.md §3.4, §12.1).
"""
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class SpcLimits:
    """Control limits of a process (centre line ± 3σ) and the 1σ/2σ zones."""

    center_line: float
    sigma: float
    ucl: float  # Upper Control Limit (center + 3σ)
    lcl: float  # Lower Control Limit (center - 3σ)
    # Whether the limits were estimated from the data (vs supplied by the caller).
    estimated: bool = True


@dataclass(frozen=True)
class SpcViolation:
    """A single rule firing over a contiguous window of points."""

    rule: str            # e.g. "NELSON_1"
    title: str           # short human-readable name
    description: str     # why it fired
    point_indices: list[int]
    severity: str        # "high" | "medium"


@dataclass(frozen=True)
class SpcAnalysis:
    """Result of an SPC analysis over a single numeric series."""

    n: int
    limits: SpcLimits
    violations: list[SpcViolation] = field(default_factory=list)

    @property
    def out_of_control(self) -> bool:
        return bool(self.violations)

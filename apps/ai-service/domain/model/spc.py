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


@dataclass(frozen=True)
class SpcBreachPoint:
    """One future period: where the process is heading, and the risk it exits."""

    step: int               # 1..horizon (periods after the last observation)
    value: float            # point forecast
    low: float              # 95 % prediction interval
    high: float
    p_above_ucl: float      # P(y > UCL) at this step
    p_below_lcl: float      # P(y < LCL) at this step

    @property
    def p_breach(self) -> float:
        """Risk of exiting the limits on EITHER side.

        The two tails are mutually exclusive (UCL > LCL), so they add up — no
        correction to apply.
        """
        return min(1.0, self.p_above_ucl + self.p_below_lcl)


@dataclass(frozen=True)
class SpcBreachForecast:
    """When a process in control is heading out of its limits.

    The control chart says whether a process IS out of control. This says when
    it WILL be — which is the only version an operator can still act on. The
    answer is a probability per future period, read against the same limits the
    chart draws: a forecast judged against anything else would not be the same
    process.
    """

    n: int
    limits: SpcLimits
    horizon: int
    # Risk from which a period is reported as a breach (0.5 by default: more
    # likely out than in).
    threshold: float
    points: list[SpcBreachPoint] = field(default_factory=list)
    # First period whose risk reaches the threshold, or ``None`` while the
    # process stays inside its limits over the whole horizon.
    first_breach_step: int | None = None
    # Which limit that first period crosses: "upper" | "lower" | None.
    side: str | None = None
    trend: float = 0.0          # per-period drift of the fitted level
    residual_sigma: float = 0.0  # one-step residual σ of the fit
    r2: float = 0.0
    confidence: str = "low"      # "low" | "medium" | "high"
    model: str = "holt_linear"

    @property
    def will_breach(self) -> bool:
        return self.first_breach_step is not None

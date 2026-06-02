"""Domain service: SPC control limits + the 8 Nelson rules (pure functions).

No I/O, no framework, fully deterministic — the statistical backbone of the
DMAIC/SPC capability (CLAUDE.md §3.4 "règles WECO/Nelson classiques", §12.1
"détection anomalies SPC"). Univariate individuals chart (I-chart).

Sigma is estimated the SPC-correct way for individuals: the average moving
range divided by the d2 constant for n=2 (1.128). This is more robust to
out-of-control points than the raw sample standard deviation. The caller may
also supply a known process centre/sigma (the recommended mode once a baseline
is established).
"""
from __future__ import annotations

import numpy as np

from domain.model.spc import SpcAnalysis, SpcLimits, SpcViolation

# d2 anti-biasing constant for a moving range of n=2 successive observations.
_D2_N2 = 1.128

# Default Nelson rule thresholds (run lengths). Standard textbook values.
_RUN_SAME_SIDE = 9       # rule 2
_RUN_TREND = 6           # rule 3
_RUN_ALTERNATING = 14    # rule 4
_RUN_WITHIN_1S = 15      # rule 7
_RUN_BEYOND_1S = 8       # rule 8


def estimate_limits(values: list[float]) -> SpcLimits:
    """Estimate control limits from data (centre = mean, σ = MR̄ / d2)."""
    arr = np.asarray(values, dtype=float)
    if arr.size == 0:
        # No data → degenerate limits (no spread); analyze() returns no findings.
        return SpcLimits(center_line=0.0, sigma=0.0, ucl=0.0, lcl=0.0, estimated=True)
    center = float(arr.mean())
    if arr.size >= 2:
        moving_range = np.abs(np.diff(arr))
        mr_bar = float(moving_range.mean())
        sigma = mr_bar / _D2_N2
    else:
        sigma = 0.0
    # Degenerate series (flat or single point): fall back to sample std so the
    # chart still has non-zero limits when there is real spread.
    if sigma == 0.0 and arr.size >= 2:
        sigma = float(arr.std(ddof=1))
    return SpcLimits(
        center_line=center,
        sigma=sigma,
        ucl=center + 3.0 * sigma,
        lcl=center - 3.0 * sigma,
        estimated=True,
    )


def analyze(
    values: list[float],
    *,
    center: float | None = None,
    sigma: float | None = None,
) -> SpcAnalysis:
    """Run the 8 Nelson rules over ``values`` against control limits.

    If ``center``/``sigma`` are both provided they define the limits (known
    process baseline); otherwise limits are estimated from the data.
    """
    n = len(values)
    if center is not None and sigma is not None:
        limits = SpcLimits(
            center_line=float(center),
            sigma=float(sigma),
            ucl=float(center) + 3.0 * float(sigma),
            lcl=float(center) - 3.0 * float(sigma),
            estimated=False,
        )
    else:
        limits = estimate_limits(values)

    violations: list[SpcViolation] = []
    if n == 0 or limits.sigma <= 0.0:
        # No spread → no meaningful control chart; return limits with no findings.
        return SpcAnalysis(n=n, limits=limits, violations=violations)

    arr = np.asarray(values, dtype=float)
    # z = signed distance from centre in sigma units.
    z = (arr - limits.center_line) / limits.sigma
    side = np.sign(z)  # +1 above, -1 below, 0 on the line

    violations.extend(_rule1_beyond_3s(z))
    violations.extend(_rule2_same_side(side))
    violations.extend(_rule3_trend(arr))
    violations.extend(_rule4_alternating(arr))
    violations.extend(_rule5_two_of_three_beyond_2s(z, side))
    violations.extend(_rule6_four_of_five_beyond_1s(z, side))
    violations.extend(_rule7_fifteen_within_1s(z))
    violations.extend(_rule8_eight_beyond_1s(z))
    return SpcAnalysis(n=n, limits=limits, violations=violations)


# --- Individual rules (each returns the list of windows that fired) ---


def _rule1_beyond_3s(z: np.ndarray) -> list[SpcViolation]:
    out = []
    for i in range(z.size):
        if abs(z[i]) > 3.0:
            out.append(SpcViolation(
                rule="NELSON_1",
                title="Point hors limites (>3σ)",
                description=f"Le point {i} est au-delà de 3σ — cause spéciale probable.",
                point_indices=[i],
                severity="high",
            ))
    return out


def _rule2_same_side(side: np.ndarray) -> list[SpcViolation]:
    return _runs_same_value(
        side, _RUN_SAME_SIDE, nonzero_only=True,
        rule="NELSON_2", title=f"{_RUN_SAME_SIDE} points du même côté",
        desc=f"{_RUN_SAME_SIDE} points consécutifs du même côté de la ligne centrale (décentrage).",
    )


def _rule3_trend(arr: np.ndarray) -> list[SpcViolation]:
    out = []
    direction = 0  # +1 increasing, -1 decreasing
    start = 0
    for i in range(1, arr.size):
        if arr[i] > arr[i - 1]:
            step = 1
        elif arr[i] < arr[i - 1]:
            step = -1
        else:
            step = 0
        if step != 0 and step == direction:
            continue
        # direction changed (or equal value broke the run) → close previous run
        run_len = i - start  # number of points in the monotone run ending at i-1
        if direction != 0 and run_len >= _RUN_TREND:
            out.append(_trend_violation(start, i - 1, direction))
        direction = step
        start = i - 1 if step != 0 else i
    # tail
    if direction != 0 and (arr.size - start) >= _RUN_TREND:
        out.append(_trend_violation(start, arr.size - 1, direction))
    return out


def _trend_violation(start: int, end: int, direction: int) -> SpcViolation:
    word = "croissante" if direction > 0 else "décroissante"
    return SpcViolation(
        rule="NELSON_3",
        title=f"Tendance {word}",
        description=f"{end - start + 1} points en tendance {word} continue (dérive).",
        point_indices=list(range(start, end + 1)),
        severity="medium",
    )


def _rule4_alternating(arr: np.ndarray) -> list[SpcViolation]:
    out = []
    if arr.size < _RUN_ALTERNATING:
        return out
    signs = np.sign(np.diff(arr))
    # find runs where signs strictly alternate and are non-zero
    start = 0
    for i in range(1, signs.size):
        if signs[i] != 0 and signs[i] == -signs[i - 1]:
            continue
        run_points = (i - start) + 1
        if run_points >= _RUN_ALTERNATING:
            out.append(_alternating_violation(start, i))
        start = i
    run_points = (signs.size - start) + 1
    if run_points >= _RUN_ALTERNATING:
        out.append(_alternating_violation(start, signs.size))
    return out


def _alternating_violation(start: int, end: int) -> SpcViolation:
    # diff index i corresponds to point i+1; the window of points is start..end inclusive.
    return SpcViolation(
        rule="NELSON_4",
        title="Sciage (alternance)",
        description=f"{end - start + 1} points alternant systématiquement (sur-réglage probable).",
        point_indices=list(range(start, end + 1)),
        severity="medium",
    )


def _rule5_two_of_three_beyond_2s(z: np.ndarray, side: np.ndarray) -> list[SpcViolation]:
    out = []
    for i in range(z.size - 2):
        window = range(i, i + 3)
        for s in (1.0, -1.0):
            idx = [j for j in window if side[j] == s and abs(z[j]) > 2.0]
            if len(idx) >= 2:
                out.append(SpcViolation(
                    rule="NELSON_5",
                    title="2 sur 3 au-delà de 2σ",
                    description="2 points sur 3 consécutifs au-delà de 2σ du même côté (zone A).",
                    point_indices=idx,
                    severity="medium",
                ))
                break
    return out


def _rule6_four_of_five_beyond_1s(z: np.ndarray, side: np.ndarray) -> list[SpcViolation]:
    out = []
    for i in range(z.size - 4):
        window = range(i, i + 5)
        for s in (1.0, -1.0):
            idx = [j for j in window if side[j] == s and abs(z[j]) > 1.0]
            if len(idx) >= 4:
                out.append(SpcViolation(
                    rule="NELSON_6",
                    title="4 sur 5 au-delà de 1σ",
                    description="4 points sur 5 consécutifs au-delà de 1σ du même côté (zone B).",
                    point_indices=idx,
                    severity="medium",
                ))
                break
    return out


def _rule7_fifteen_within_1s(z: np.ndarray) -> list[SpcViolation]:
    out = []
    within = np.abs(z) < 1.0
    for start, end in _true_runs(within, _RUN_WITHIN_1S):
        out.append(SpcViolation(
            rule="NELSON_7",
            title=f"{_RUN_WITHIN_1S} points dans 1σ",
            description=f"{end - start + 1} points consécutifs dans 1σ (variation anormalement faible — stratification).",
            point_indices=list(range(start, end + 1)),
            severity="medium",
        ))
    return out


def _rule8_eight_beyond_1s(z: np.ndarray) -> list[SpcViolation]:
    out = []
    beyond = np.abs(z) > 1.0
    for start, end in _true_runs(beyond, _RUN_BEYOND_1S):
        out.append(SpcViolation(
            rule="NELSON_8",
            title=f"{_RUN_BEYOND_1S} points hors 1σ",
            description=f"{end - start + 1} points consécutifs hors de 1σ des deux côtés (mélange/bimodalité).",
            point_indices=list(range(start, end + 1)),
            severity="medium",
        ))
    return out


# --- run-length helpers ---


def _runs_same_value(
    values: np.ndarray, min_len: int, *, nonzero_only: bool,
    rule: str, title: str, desc: str,
) -> list[SpcViolation]:
    """Maximal runs where ``values`` holds a constant (optionally non-zero) value."""
    out = []
    start = 0
    for i in range(1, values.size + 1):
        if i < values.size and values[i] == values[start]:
            continue
        run_val = values[start]
        run_len = i - start
        if run_len >= min_len and (not nonzero_only or run_val != 0):
            out.append(SpcViolation(
                rule=rule, title=title, description=desc,
                point_indices=list(range(start, i)), severity="medium",
            ))
        start = i
    return out


def _true_runs(mask: np.ndarray, min_len: int) -> list[tuple[int, int]]:
    """Return (start, end) inclusive index pairs of True-runs of length ≥ min_len."""
    runs = []
    start = None
    for i in range(mask.size):
        if mask[i]:
            if start is None:
                start = i
        else:
            if start is not None and (i - start) >= min_len:
                runs.append((start, i - 1))
            start = None
    if start is not None and (mask.size - start) >= min_len:
        runs.append((start, mask.size - 1))
    return runs

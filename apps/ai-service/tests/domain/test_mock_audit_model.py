"""Domain object tests — audit blanc IA (Standards Hub §8.4 onglet 7)."""
from __future__ import annotations

import pytest

from domain.model.mock_audit import (
    AuditClause,
    AuditQuestion,
    ClauseGap,
    GapCriticality,
    MockAuditReport,
    MockAuditSpec,
    ObligationLevel,
    RiskLevel,
)


def _clause(**kw) -> AuditClause:
    base = dict(
        clause_code="8.1",
        title="Planification et maîtrise opérationnelles",
        obligation=ObligationLevel.MUST,
        risk=RiskLevel.HIGH,
        total_requirements=4,
        covered_requirements=1,
    )
    base.update(kw)
    return AuditClause(**base)


# ---- enums ------------------------------------------------------------------


@pytest.mark.parametrize(
    ("raw", "expected"),
    [("must", ObligationLevel.MUST), ("SHOULD", ObligationLevel.SHOULD), ("May", ObligationLevel.MAY)],
)
def test_obligation_from_value(raw, expected):
    assert ObligationLevel.from_value(raw) is expected


@pytest.mark.parametrize("bad", ["mandatory", "", None, 7])
def test_obligation_from_value_rejects(bad):
    with pytest.raises(ValueError, match="unknown obligation"):
        ObligationLevel.from_value(bad)


@pytest.mark.parametrize(
    ("raw", "expected"),
    [("low", RiskLevel.LOW), ("HIGH", RiskLevel.HIGH), ("Critical", RiskLevel.CRITICAL)],
)
def test_risk_from_value(raw, expected):
    assert RiskLevel.from_value(raw) is expected


@pytest.mark.parametrize("bad", ["severe", "", None])
def test_risk_from_value_rejects(bad):
    with pytest.raises(ValueError, match="unknown risk"):
        RiskLevel.from_value(bad)


# ---- AuditClause invariants -------------------------------------------------


def test_clause_coverage_ratio_and_full():
    c = _clause(total_requirements=4, covered_requirements=1)
    assert c.coverage_ratio == 0.25
    assert not c.fully_covered

    full = _clause(total_requirements=4, covered_requirements=4)
    assert full.coverage_ratio == 1.0
    assert full.fully_covered


def test_clause_coverage_ratio_zero_total():
    c = _clause(total_requirements=0, covered_requirements=0)
    assert c.coverage_ratio == 0.0
    assert not c.fully_covered  # 0/0 n'est pas "couvert"


@pytest.mark.parametrize("field", ["clause_code", "title"])
def test_clause_blank_field_rejected(field):
    with pytest.raises(ValueError):
        _clause(**{field: "  "})


def test_clause_negative_total_rejected():
    with pytest.raises(ValueError, match="total_requirements"):
        _clause(total_requirements=-1, covered_requirements=0)


def test_clause_covered_exceeds_total_rejected():
    with pytest.raises(ValueError, match="covered_requirements"):
        _clause(total_requirements=2, covered_requirements=3)


def test_risk_score_orders_must_critical_uncovered_first():
    high = _clause(
        obligation=ObligationLevel.MUST, risk=RiskLevel.CRITICAL,
        total_requirements=4, covered_requirements=0,
    )
    low = _clause(
        clause_code="9.9", obligation=ObligationLevel.MAY, risk=RiskLevel.LOW,
        total_requirements=4, covered_requirements=4,
    )
    assert high.risk_score() > low.risk_score()


def test_risk_score_penalises_uncovered_over_covered():
    uncovered = _clause(total_requirements=4, covered_requirements=0)
    covered = _clause(total_requirements=4, covered_requirements=4)
    assert uncovered.risk_score() > covered.risk_score()


def test_criticality_major_for_must_high_uncovered():
    assert _clause(
        obligation=ObligationLevel.MUST, risk=RiskLevel.HIGH,
        total_requirements=2, covered_requirements=0,
    ).criticality() is GapCriticality.MAJOR


def test_criticality_major_for_must_critical():
    assert _clause(
        obligation=ObligationLevel.MUST, risk=RiskLevel.CRITICAL,
        total_requirements=2, covered_requirements=1,
    ).criticality() is GapCriticality.MAJOR


def test_criticality_minor_for_must_medium_uncovered():
    assert _clause(
        obligation=ObligationLevel.MUST, risk=RiskLevel.MEDIUM,
        total_requirements=2, covered_requirements=0,
    ).criticality() is GapCriticality.MINOR


def test_criticality_observation_for_should():
    assert _clause(
        obligation=ObligationLevel.SHOULD, risk=RiskLevel.CRITICAL,
        total_requirements=2, covered_requirements=0,
    ).criticality() is GapCriticality.OBSERVATION


def test_criticality_observation_when_fully_covered():
    assert _clause(
        obligation=ObligationLevel.MUST, risk=RiskLevel.CRITICAL,
        total_requirements=2, covered_requirements=2,
    ).criticality() is GapCriticality.OBSERVATION


# ---- MockAuditSpec ----------------------------------------------------------


def test_spec_valid():
    spec = MockAuditSpec(
        standard_code="iso-9001",
        standard_name="ISO 9001:2015",
        industry="manufacturing",
        clauses=(_clause(),),
    )
    assert spec.min_questions == 30
    assert spec.max_questions == 100


@pytest.mark.parametrize("field", ["standard_code", "standard_name", "industry"])
def test_spec_blank_field_rejected(field):
    kw = dict(
        standard_code="iso-9001", standard_name="ISO 9001", industry="it",
        clauses=(_clause(),),
    )
    kw[field] = " "
    with pytest.raises(ValueError):
        MockAuditSpec(**kw)


def test_spec_empty_clauses_rejected():
    with pytest.raises(ValueError, match="at least one clause"):
        MockAuditSpec(
            standard_code="iso-9001", standard_name="ISO 9001",
            industry="it", clauses=(),
        )


@pytest.mark.parametrize(
    ("mn", "mx"),
    [(0, 100), (50, 40), (1, 300)],
)
def test_spec_question_bounds_rejected(mn, mx):
    with pytest.raises(ValueError, match="min_questions"):
        MockAuditSpec(
            standard_code="iso-9001", standard_name="ISO 9001",
            industry="it", clauses=(_clause(),),
            min_questions=mn, max_questions=mx,
        )


# ---- value objects: question / gap / report --------------------------------


def test_question_requires_code_and_text():
    with pytest.raises(ValueError, match="clause_code"):
        AuditQuestion(clause_code=" ", question="q?")
    with pytest.raises(ValueError, match="question required"):
        AuditQuestion(clause_code="4.1", question="  ")


def test_clause_gap_validation():
    gap = ClauseGap(
        clause_code="4.1", title="Contexte",
        criticality=GapCriticality.MINOR, coverage_ratio=0.5,
        finding="Couverture partielle.",
    )
    assert gap.questions == ()
    with pytest.raises(ValueError, match="coverage_ratio"):
        ClauseGap(
            clause_code="4.1", title="t", criticality=GapCriticality.MINOR,
            coverage_ratio=1.5, finding="x",
        )
    with pytest.raises(ValueError, match="finding required"):
        ClauseGap(
            clause_code="4.1", title="t", criticality=GapCriticality.MINOR,
            coverage_ratio=0.5, finding=" ",
        )
    with pytest.raises(ValueError, match="clause_code"):
        ClauseGap(
            clause_code="", title="t", criticality=GapCriticality.MINOR,
            coverage_ratio=0.5, finding="x",
        )


def test_report_readiness_bounds():
    with pytest.raises(ValueError, match="readiness"):
        MockAuditReport(
            standard_code="iso-9001", standard_name="ISO 9001",
            questions=(), gaps=(), readiness=120.0,
            major_count=0, minor_count=0, observation_count=0,
            provider="ollama", tokens_used=0, latency_ms=0,
        )

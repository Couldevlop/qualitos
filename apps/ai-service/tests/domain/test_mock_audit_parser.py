"""Parser tests — projection défensive de la sortie LLM (audit blanc §8.4)."""
from __future__ import annotations

import pytest

from domain.model.mock_audit import (
    AuditClause,
    GapCriticality,
    MockAuditSpec,
    ObligationLevel,
    RiskLevel,
)
from domain.service import mock_audit_parser as parser


def _spec() -> MockAuditSpec:
    return MockAuditSpec(
        standard_code="iso-9001",
        standard_name="ISO 9001:2015",
        industry="manufacturing",
        clauses=(
            AuditClause(
                clause_code="8.1", title="Maîtrise opérationnelle",
                obligation=ObligationLevel.MUST, risk=RiskLevel.CRITICAL,
                total_requirements=4, covered_requirements=0,
            ),
            AuditClause(
                clause_code="7.1", title="Ressources",
                obligation=ObligationLevel.MUST, risk=RiskLevel.MEDIUM,
                total_requirements=2, covered_requirements=1,
            ),
            AuditClause(
                clause_code="4.1", title="Contexte",
                obligation=ObligationLevel.SHOULD, risk=RiskLevel.LOW,
                total_requirements=2, covered_requirements=2,
            ),
        ),
        max_questions=100,
    )


def _known(spec: MockAuditSpec) -> dict:
    return {c.clause_code: c for c in spec.clauses}


# ---- _extract_json_object ---------------------------------------------------


def test_extract_json_plain():
    assert parser._extract_json_object('{"a": 1}') == {"a": 1}


def test_extract_json_with_surrounding_prose():
    text = 'Voici le résultat : ```json\n{"questions": []}\n``` fin.'
    assert parser._extract_json_object(text) == {"questions": []}


def test_extract_json_handles_braces_in_strings():
    text = '{"q": "utilise {accolades} ici"}'
    assert parser._extract_json_object(text) == {"q": "utilise {accolades} ici"}


def test_extract_json_handles_escaped_quote():
    text = r'{"q": "il a dit \"oui\""}'
    assert parser._extract_json_object(text)["q"] == 'il a dit "oui"'


@pytest.mark.parametrize("bad", ["", "   ", "pas de json", "{unbalanced"])
def test_extract_json_invalid(bad):
    with pytest.raises(ValueError):
        parser._extract_json_object(bad)


@pytest.mark.parametrize("bad", ["", "pas de json", "{unbalanced"])
def test_safe_payload_degrades_to_empty(bad):
    # _safe_payload absorbe l'inexploitable → dict vide (anti-fragilité).
    assert parser._safe_payload(bad) == {}


def test_safe_payload_parses_valid():
    assert parser._safe_payload('{"questions": []}') == {"questions": []}


# ---- parse_questions --------------------------------------------------------


def test_parse_questions_keeps_known_drops_unknown():
    spec = _spec()
    text = (
        '{"questions": ['
        '{"clause_code": "8.1", "question": "Comment maîtrisez-vous ?", "rationale": "risque"},'
        '{"clause_code": "99.9", "question": "clause hallucinée"},'
        '{"clause_code": "7.1", "question": ""}'  # vide → ignorée
        "]}"
    )
    qs = parser.parse_questions(text, spec, _known(spec))
    assert [q.clause_code for q in qs] == ["8.1"]
    assert qs[0].rationale == "risque"


def test_parse_questions_truncates_to_max():
    spec = MockAuditSpec(
        standard_code="iso-9001", standard_name="ISO 9001", industry="it",
        clauses=(_spec().clauses[0],), min_questions=1, max_questions=2,
    )
    items = ",".join(
        f'{{"clause_code": "8.1", "question": "q{i}"}}' for i in range(5)
    )
    qs = parser.parse_questions(f'{{"questions": [{items}]}}', spec, _known(spec))
    assert len(qs) == 2


def test_parse_questions_non_list_and_non_dict_items():
    spec = _spec()
    assert parser.parse_questions('{"questions": "oops"}', spec, _known(spec)) == ()
    assert parser.parse_questions('{"questions": [1, null]}', spec, _known(spec)) == ()


def test_parse_questions_missing_key():
    spec = _spec()
    assert parser.parse_questions('{"findings": []}', spec, _known(spec)) == ()


# ---- parse_findings ---------------------------------------------------------


def test_parse_findings_known_only():
    spec = _spec()
    text = (
        '{"findings": ['
        '{"clause_code": "8.1", "finding": "Aucune preuve."},'
        '{"clause_code": "zzz", "finding": "hallucination"},'
        '{"clause_code": "7.1", "finding": ""}'  # vide → ignorée
        "]}"
    )
    out = parser.parse_findings(text, _known(spec))
    assert out == {"8.1": "Aucune preuve."}


def test_parse_findings_non_list():
    spec = _spec()
    assert parser.parse_findings('{"findings": 3}', _known(spec)) == {}
    assert parser.parse_findings('{"findings": [42]}', _known(spec)) == {}


# ---- build_gaps + deterministic fallback ------------------------------------


def test_build_gaps_orders_by_risk_and_uses_ai_finding():
    spec = _spec()
    qs = parser.parse_questions(
        '{"questions": [{"clause_code": "8.1", "question": "Q ?"}]}',
        spec, _known(spec),
    )
    gaps = parser.build_gaps(spec, qs, {"8.1": "Constat IA pour 8.1."})
    # 8.1 (MUST/critical/uncovered) en tête.
    assert gaps[0].clause_code == "8.1"
    assert gaps[0].finding == "Constat IA pour 8.1."
    assert gaps[0].criticality is GapCriticality.MAJOR
    assert gaps[0].questions[0].question == "Q ?"
    # 4.1 (SHOULD, couvert) en queue → observation.
    assert gaps[-1].clause_code == "4.1"
    assert gaps[-1].criticality is GapCriticality.OBSERVATION


def test_build_gaps_deterministic_fallback_when_no_ai_finding():
    spec = _spec()
    gaps = parser.build_gaps(spec, (), {})
    by_code = {g.clause_code: g for g in gaps}
    # 8.1 : 0 preuve → message "aucune preuve".
    assert "Aucune preuve" in by_code["8.1"].finding
    # 7.1 : 1/2 → couverture partielle.
    assert "partielle" in by_code["7.1"].finding
    # 4.1 : 2/2 → couverte.
    assert "couverte" in by_code["4.1"].finding


# ---- counts + readiness -----------------------------------------------------


def test_count_by_criticality():
    spec = _spec()
    gaps = parser.build_gaps(spec, (), {})
    major, minor, observation = parser.count_by_criticality(gaps)
    assert major == 1  # 8.1
    assert minor == 1  # 7.1
    assert observation == 1  # 4.1


def test_readiness_is_must_coverage():
    spec = _spec()
    # MUST : 8.1 (0/4) + 7.1 (1/2) → 1/6 ≈ 16.67 %.
    assert parser.readiness_score(spec) == pytest.approx(16.67, abs=0.01)


def test_readiness_zero_when_no_must():
    spec = MockAuditSpec(
        standard_code="x", standard_name="X", industry="it",
        clauses=(
            AuditClause(
                clause_code="a.1", title="t", obligation=ObligationLevel.SHOULD,
                risk=RiskLevel.LOW, total_requirements=2, covered_requirements=1,
            ),
        ),
    )
    assert parser.readiness_score(spec) == 0.0

"""Pure parser of the LLM mock-audit output (Standards Hub §8.4 onglet 7).

L'IA répond en JSON : une liste de *questions ciblées* (par clause) et une liste
de *constats* (gap analysis, par clause). Ce module — domaine PUR, sans framework
— extrait ce JSON de façon défensive et le projette sur les value objects, en
n'acceptant QUE les clauses réellement présentes dans la matière fournie par
l'engine (anti-hallucination, LLM09 Overreliance) :

  * une question/un constat portant sur un code de clause inconnu est ignoré ;
  * une clause à risque sans constat IA reçoit un constat *déterministe* dérivé
    de son état de preuve (jamais de trou silencieux dans le rapport) ;
  * la criticité finale est celle de la grille déterministe (ISO/IEC 17021-1),
    pas un libellé libre du LLM (sécurité : l'IA propose, la règle tranche).

Aucune E/S, aucune dépendance framework : le JSON est parsé via ``json`` stdlib.
"""
from __future__ import annotations

import json

from domain.model.mock_audit import (
    AuditClause,
    AuditQuestion,
    ClauseGap,
    GapCriticality,
    MockAuditSpec,
)


def _safe_payload(text: str) -> dict:
    """Parse le JSON LLM en tolérant l'inexploitable (→ dict vide).

    Une sortie LLM sans JSON ne doit PAS faire échouer l'audit : on dégrade
    proprement vers les constats déterministes (anti-fragilité, LLM09).
    """
    try:
        return _extract_json_object(text)
    except ValueError:
        return {}


def _extract_json_object(text: str) -> dict:
    """Extrait le 1er objet JSON d'un texte LLM (tolère le bavardage autour).

    Le LLM peut entourer le JSON de prose ou de balises ```json```. On isole la
    portion {...} équilibrée puis on la parse. Lève ValueError si rien d'exploitable.
    """
    if not text or not text.strip():
        raise ValueError("empty LLM output")
    start = text.find("{")
    if start < 0:
        raise ValueError("no JSON object in LLM output")
    depth = 0
    in_string = False
    escaped = False
    for i in range(start, len(text)):
        ch = text[i]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                candidate = text[start : i + 1]
                return json.loads(candidate)
    raise ValueError("unbalanced JSON object in LLM output")


def _clean(value: object, max_len: int) -> str:
    """Normalise une valeur LLM en chaîne bornée (anti-output-injection LLM02)."""
    if value is None:
        return ""
    return str(value).strip()[:max_len]


def parse_questions(
    text: str, spec: MockAuditSpec, known: dict[str, AuditClause]
) -> tuple[AuditQuestion, ...]:
    """Projette les questions du JSON LLM sur les clauses connues."""
    payload = _safe_payload(text)
    raw_questions = payload.get("questions", [])
    if not isinstance(raw_questions, list):
        raw_questions = []
    questions: list[AuditQuestion] = []
    for item in raw_questions:
        if not isinstance(item, dict):
            continue
        code = _clean(item.get("clause_code"), 30)
        text_q = _clean(item.get("question"), 1000)
        if code not in known or not text_q:
            continue  # ignore les clauses hallucinées / questions vides
        questions.append(
            AuditQuestion(
                clause_code=code,
                question=text_q,
                rationale=_clean(item.get("rationale"), 600),
            )
        )
        if len(questions) >= spec.max_questions:
            break
    return tuple(questions)


def parse_findings(
    text: str, known: dict[str, AuditClause]
) -> dict[str, str]:
    """Extrait les constats IA par clause connue (code → texte du constat)."""
    payload = _safe_payload(text)
    raw = payload.get("findings", [])
    if not isinstance(raw, list):
        raw = []
    findings: dict[str, str] = {}
    for item in raw:
        if not isinstance(item, dict):
            continue
        code = _clean(item.get("clause_code"), 30)
        finding = _clean(item.get("finding"), 1500)
        if code in known and finding:
            findings[code] = finding
    return findings


def _deterministic_finding(clause: AuditClause) -> str:
    """Constat de repli, dérivé du seul état de preuve (jamais inventé)."""
    if clause.fully_covered:
        return (
            f"Clause {clause.clause_code} couverte par des preuves "
            f"({clause.covered_requirements}/{clause.total_requirements}) — "
            "vérifier la fraîcheur et la pertinence des preuves liées."
        )
    if clause.covered_requirements == 0:
        return (
            f"Aucune preuve liée à la clause {clause.clause_code} "
            f"({clause.title}). Exigences non démontrées : "
            f"{clause.total_requirements}."
        )
    return (
        f"Couverture partielle de la clause {clause.clause_code} : "
        f"{clause.covered_requirements}/{clause.total_requirements} "
        "exigences démontrées par une preuve."
    )


def build_gaps(
    spec: MockAuditSpec,
    questions: tuple[AuditQuestion, ...],
    ai_findings: dict[str, str],
) -> tuple[ClauseGap, ...]:
    """Assemble le rapport d'écarts par clause (constat + questions + criticité).

    La criticité est TOUJOURS celle de la grille déterministe du domaine — l'IA
    ne rédige que le texte du constat. Les clauses sont triées par priorité de
    risque décroissante (les écarts majeurs d'abord).
    """
    by_clause: dict[str, list[AuditQuestion]] = {}
    for q in questions:
        by_clause.setdefault(q.clause_code, []).append(q)

    ordered = sorted(spec.clauses, key=lambda c: c.risk_score(), reverse=True)
    gaps: list[ClauseGap] = []
    for clause in ordered:
        finding = ai_findings.get(clause.clause_code) or _deterministic_finding(clause)
        gaps.append(
            ClauseGap(
                clause_code=clause.clause_code,
                title=clause.title,
                criticality=clause.criticality(),
                coverage_ratio=clause.coverage_ratio,
                finding=finding,
                questions=tuple(by_clause.get(clause.clause_code, [])),
            )
        )
    return tuple(gaps)


def count_by_criticality(
    gaps: tuple[ClauseGap, ...]
) -> tuple[int, int, int]:
    """Décompte (major, minor, observation) sur l'ensemble des écarts."""
    major = sum(1 for g in gaps if g.criticality is GapCriticality.MAJOR)
    minor = sum(1 for g in gaps if g.criticality is GapCriticality.MINOR)
    observation = sum(
        1 for g in gaps if g.criticality is GapCriticality.OBSERVATION
    )
    return major, minor, observation


def readiness_score(spec: MockAuditSpec) -> float:
    """Score de préparation = couverture des exigences MUST (en %).

    Aligné sur l'engine (``StandardsService.computeAuditBlanc``) : seules les
    exigences obligatoires comptent pour la décision de certification.
    """
    must_total = sum(
        c.total_requirements for c in spec.clauses
        if c.obligation.value == "must"
    )
    must_covered = sum(
        c.covered_requirements for c in spec.clauses
        if c.obligation.value == "must"
    )
    if must_total == 0:
        return 0.0
    return round(must_covered * 100.0 / must_total, 2)

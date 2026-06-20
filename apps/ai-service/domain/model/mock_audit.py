"""Audit blanc IA avancé — domain objects (Standards Hub §8.4 onglet 7).

Avant l'audit de certification officiel, l'IA simule un audit : elle génère
30 à 100 *questions ciblées* sur les clauses à risque, puis confronte chaque
clause aux *preuves disponibles* du tenant et restitue un *constat d'écart*
(gap analysis) par clause, avec sa criticité.

Domaine PUR : aucun framework, aucune E/S. Les value objects portent leurs
invariants (CLAUDE.md §18.2). Les questions et constats sont rédigés par le
``AIProvider`` au niveau application ; ici on ne modélise que les données et
les règles (sélection des clauses à risque, criticité).

La matière à risque (clauses + état de preuve) est fournie par le Standards
Hub côté engine : aucune donnée tenant n'est inventée par l'IA.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum


class ObligationLevel(str, Enum):
    """Caractère d'une exigence (liste fermée, pas de texte libre)."""

    MUST = "must"
    SHOULD = "should"
    MAY = "may"

    @classmethod
    def from_value(cls, value: str) -> "ObligationLevel":
        try:
            return cls(value.lower())
        except (ValueError, AttributeError) as exc:
            raise ValueError(
                f"unknown obligation level: {value!r} "
                f"(expected one of {[o.value for o in cls]})"
            ) from exc


class RiskLevel(str, Enum):
    """Risque si l'exigence n'est pas satisfaite (liste fermée)."""

    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

    @classmethod
    def from_value(cls, value: str) -> "RiskLevel":
        try:
            return cls(value.lower())
        except (ValueError, AttributeError) as exc:
            raise ValueError(
                f"unknown risk level: {value!r} "
                f"(expected one of {[r.value for r in cls]})"
            ) from exc


class GapCriticality(str, Enum):
    """Criticité d'un écart de clause restitué par l'audit blanc."""

    MAJOR = "major"  # MUST à risque élevé/critique non démontré
    MINOR = "minor"  # MUST à risque faible/moyen, ou couverture partielle
    OBSERVATION = "observation"  # SHOULD/MAY, ou clause couverte (piste)


@dataclass(frozen=True, slots=True)
class AuditClause:
    """Une clause candidate à l'audit, avec son état de preuve côté tenant.

    Aucune PII : code/titre de clause, caractère, risque, et un *décompte* de
    preuves (jamais le contenu des preuves). ``coverage_ratio`` ∈ [0,1] est la
    part d'exigences de la clause démontrées par au moins une preuve tenant.
    """

    clause_code: str
    title: str
    obligation: ObligationLevel
    risk: RiskLevel
    total_requirements: int
    covered_requirements: int
    evidence_types: tuple[str, ...] = field(default_factory=tuple)

    def __post_init__(self) -> None:
        if not self.clause_code or not self.clause_code.strip():
            raise ValueError("clause_code required")
        if not self.title or not self.title.strip():
            raise ValueError("title required")
        if self.total_requirements < 0:
            raise ValueError("total_requirements must be >= 0")
        if not 0 <= self.covered_requirements <= self.total_requirements:
            raise ValueError("covered_requirements must be in [0, total_requirements]")

    @property
    def coverage_ratio(self) -> float:
        if self.total_requirements == 0:
            return 0.0
        return self.covered_requirements / self.total_requirements

    @property
    def fully_covered(self) -> bool:
        return (
            self.total_requirements > 0
            and self.covered_requirements == self.total_requirements
        )

    def risk_score(self) -> float:
        """Score de priorité d'audit (élevé = clause la plus à risque).

        Combine : caractère obligatoire, gravité du risque, et *défaut* de
        couverture. Une clause MUST critique non couverte domine ; une clause
        pleinement couverte est reléguée en bas de liste.
        """
        obligation_weight = {
            ObligationLevel.MUST: 3.0,
            ObligationLevel.SHOULD: 1.5,
            ObligationLevel.MAY: 0.5,
        }[self.obligation]
        risk_weight = {
            RiskLevel.LOW: 1.0,
            RiskLevel.MEDIUM: 2.0,
            RiskLevel.HIGH: 3.0,
            RiskLevel.CRITICAL: 4.0,
        }[self.risk]
        # Défaut de couverture : 1 si rien n'est démontré, 0 si tout l'est.
        gap = 1.0 - self.coverage_ratio
        return obligation_weight * risk_weight * (0.25 + 0.75 * gap)

    def criticality(self) -> GapCriticality:
        """Criticité de l'écart (ISO/IEC 17021-1, même grille que l'engine)."""
        if self.fully_covered:
            return GapCriticality.OBSERVATION
        if self.obligation is not ObligationLevel.MUST:
            return GapCriticality.OBSERVATION
        if self.risk in (RiskLevel.HIGH, RiskLevel.CRITICAL):
            return GapCriticality.MAJOR
        return GapCriticality.MINOR


@dataclass(frozen=True, slots=True)
class MockAuditSpec:
    """Matière d'un audit blanc : norme adoptée + clauses candidates.

    ``standard_code``/``standard_name`` identifient la norme ; ``clauses`` est
    l'ensemble des clauses (avec état de preuve) calculé par l'engine. Le tenant
    technique n'est PAS porté ici (il vient du JWT, côté gateway).
    """

    standard_code: str
    standard_name: str
    industry: str
    clauses: tuple[AuditClause, ...]
    language: str = "fr"
    min_questions: int = 30
    max_questions: int = 100

    def __post_init__(self) -> None:
        if not self.standard_code or not self.standard_code.strip():
            raise ValueError("standard_code required")
        if not self.standard_name or not self.standard_name.strip():
            raise ValueError("standard_name required")
        if not self.industry or not self.industry.strip():
            raise ValueError("industry required")
        if not self.clauses:
            raise ValueError("at least one clause required")
        if not 1 <= self.min_questions <= self.max_questions <= 200:
            raise ValueError("require 1 <= min_questions <= max_questions <= 200")


@dataclass(frozen=True, slots=True)
class AuditQuestion:
    """Une question d'audit ciblée sur une clause à risque (rédigée par l'IA)."""

    clause_code: str
    question: str
    rationale: str = ""

    def __post_init__(self) -> None:
        if not self.clause_code or not self.clause_code.strip():
            raise ValueError("clause_code required")
        if not self.question or not self.question.strip():
            raise ValueError("question required")


@dataclass(frozen=True, slots=True)
class ClauseGap:
    """Constat d'écart pour une clause (confrontation question ↔ preuves)."""

    clause_code: str
    title: str
    criticality: GapCriticality
    coverage_ratio: float
    finding: str
    questions: tuple[AuditQuestion, ...] = field(default_factory=tuple)

    def __post_init__(self) -> None:
        if not self.clause_code or not self.clause_code.strip():
            raise ValueError("clause_code required")
        if not 0.0 <= self.coverage_ratio <= 1.0:
            raise ValueError("coverage_ratio must be in [0, 1]")
        if not self.finding or not self.finding.strip():
            raise ValueError("finding required")


@dataclass(frozen=True, slots=True)
class MockAuditReport:
    """Rapport d'audit blanc complet (gap analysis), rédigé par l'IA."""

    standard_code: str
    standard_name: str
    questions: tuple[AuditQuestion, ...]
    gaps: tuple[ClauseGap, ...]
    readiness: float
    major_count: int
    minor_count: int
    observation_count: int
    provider: str
    tokens_used: int
    latency_ms: int

    def __post_init__(self) -> None:
        if not 0.0 <= self.readiness <= 100.0:
            raise ValueError("readiness must be in [0, 100]")

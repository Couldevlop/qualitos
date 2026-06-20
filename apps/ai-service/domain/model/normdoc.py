"""Normative-document generation domain objects (Standards Hub §8.8).

Génération assistée d'un document normatif complet et multi-sections (Manuel
Qualité, Politique Qualité, Procédure documentée) à partir du *contexte tenant*
(secteur, taille, nom, processus connus) et de la *structure de sections*
fournie par le Standards Hub.

Domaine pur : aucun framework, aucune E/S. Les value objects portent leurs
invariants (CLAUDE.md §18.2). Le texte de chaque section est rédigé par le
``AIProvider`` au niveau application — ici on ne modélise que les données.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum


class NormDocKind(str, Enum):
    """Type de document normatif demandé (liste fermée — pas de texte libre)."""

    MANUAL = "manual"  # Manuel Qualité
    POLICY = "policy"  # Politique Qualité
    PROCEDURE = "procedure"  # Procédure documentée

    @classmethod
    def from_value(cls, value: str) -> "NormDocKind":
        try:
            return cls(value)
        except ValueError as exc:
            raise ValueError(
                f"unknown document kind: {value!r} "
                f"(expected one of {[k.value for k in cls]})"
            ) from exc


@dataclass(frozen=True, slots=True)
class TenantProfile:
    """Contexte tenant non sensible utilisé pour pré-remplir le document.

    Aucune PII : nom d'organisation, secteur, taille, langue et processus connus.
    Le tenant_id technique n'est PAS porté ici (il vient du JWT, côté gateway).
    """

    organization_name: str
    industry: str
    size: str
    language: str = "fr"
    known_processes: tuple[str, ...] = field(default_factory=tuple)

    def __post_init__(self) -> None:
        if not self.organization_name or not self.organization_name.strip():
            raise ValueError("organization_name required")
        if not self.industry or not self.industry.strip():
            raise ValueError("industry required")
        if not self.size or not self.size.strip():
            raise ValueError("size required")
        if not self.language or not self.language.strip():
            raise ValueError("language required")


@dataclass(frozen=True, slots=True)
class SectionSpec:
    """Spécification d'une section à rédiger (titre + clauses couvertes)."""

    key: str
    title: str
    clauses: tuple[str, ...] = field(default_factory=tuple)
    guidance: str = ""

    def __post_init__(self) -> None:
        if not self.key or not self.key.strip():
            raise ValueError("section key required")
        if not self.title or not self.title.strip():
            raise ValueError("section title required")


@dataclass(frozen=True, slots=True)
class NormDocSpec:
    """Spécification complète d'un document normatif à générer."""

    kind: NormDocKind
    standard_code: str
    standard_name: str
    tenant_profile: TenantProfile
    sections: tuple[SectionSpec, ...]

    def __post_init__(self) -> None:
        if not self.standard_code or not self.standard_code.strip():
            raise ValueError("standard_code required")
        if not self.standard_name or not self.standard_name.strip():
            raise ValueError("standard_name required")
        if not self.sections:
            raise ValueError("at least one section required")
        keys = [s.key for s in self.sections]
        if len(keys) != len(set(keys)):
            raise ValueError("section keys must be unique")


@dataclass(frozen=True, slots=True)
class GeneratedSection:
    """Une section rédigée par l'IA (Markdown), traçant ses clauses."""

    key: str
    title: str
    clauses: tuple[str, ...]
    body_markdown: str


@dataclass(frozen=True, slots=True)
class NormDocDraft:
    """Document normatif complet généré (brouillon IA, multi-sections)."""

    kind: NormDocKind
    standard_code: str
    standard_name: str
    title: str
    sections: tuple[GeneratedSection, ...]
    provider: str
    tokens_used: int
    latency_ms: int

    def to_markdown(self) -> str:
        """Sérialise le document complet (titre H1 + sections H2)."""
        parts = [f"# {self.title}", ""]
        for section in self.sections:
            parts.append(f"## {section.title}")
            if section.clauses:
                parts.append(f"*Clauses : {', '.join(section.clauses)}*")
            parts.append("")
            parts.append(section.body_markdown.strip())
            parts.append("")
        return "\n".join(parts).strip() + "\n"

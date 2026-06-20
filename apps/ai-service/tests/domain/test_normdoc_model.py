"""Domain tests for the normative-document value objects (§8.8)."""
from __future__ import annotations

import pytest

from domain.model.normdoc import (
    GeneratedSection,
    NormDocDraft,
    NormDocKind,
    NormDocSpec,
    SectionSpec,
    TenantProfile,
)


def _profile() -> TenantProfile:
    return TenantProfile(
        organization_name="ACME",
        industry="manufacturing",
        size="PME",
        language="fr",
        known_processes=("achats", "production"),
    )


def test_kind_from_value_valid():
    assert NormDocKind.from_value("manual") is NormDocKind.MANUAL
    assert NormDocKind.from_value("policy") is NormDocKind.POLICY
    assert NormDocKind.from_value("procedure") is NormDocKind.PROCEDURE


def test_kind_from_value_invalid_raises():
    with pytest.raises(ValueError, match="unknown document kind"):
        NormDocKind.from_value("manifesto")


@pytest.mark.parametrize(
    "field",
    ["organization_name", "industry", "size", "language"],
)
def test_tenant_profile_requires_non_blank(field):
    kwargs = {
        "organization_name": "ACME",
        "industry": "manufacturing",
        "size": "PME",
        "language": "fr",
    }
    kwargs[field] = "  "
    with pytest.raises(ValueError):
        TenantProfile(**kwargs)


def test_tenant_profile_defaults():
    p = TenantProfile(organization_name="A", industry="i", size="s")
    assert p.language == "fr"
    assert p.known_processes == ()


def test_section_spec_requires_key_and_title():
    with pytest.raises(ValueError, match="section key required"):
        SectionSpec(key="", title="T")
    with pytest.raises(ValueError, match="section title required"):
        SectionSpec(key="k", title="")


def test_section_spec_defaults():
    s = SectionSpec(key="k", title="T")
    assert s.clauses == ()
    assert s.guidance == ""


def test_normdoc_spec_requires_fields():
    sections = (SectionSpec(key="k", title="T"),)
    with pytest.raises(ValueError, match="standard_code required"):
        NormDocSpec(NormDocKind.MANUAL, " ", "ISO", _profile(), sections)
    with pytest.raises(ValueError, match="standard_name required"):
        NormDocSpec(NormDocKind.MANUAL, "iso-9001", " ", _profile(), sections)


def test_normdoc_spec_requires_sections():
    with pytest.raises(ValueError, match="at least one section"):
        NormDocSpec(NormDocKind.MANUAL, "iso-9001", "ISO 9001", _profile(), ())


def test_normdoc_spec_unique_section_keys():
    sections = (
        SectionSpec(key="dup", title="A"),
        SectionSpec(key="dup", title="B"),
    )
    with pytest.raises(ValueError, match="unique"):
        NormDocSpec(NormDocKind.MANUAL, "iso-9001", "ISO 9001", _profile(), sections)


def test_draft_to_markdown_with_and_without_clauses():
    draft = NormDocDraft(
        kind=NormDocKind.POLICY,
        standard_code="iso-9001",
        standard_name="ISO 9001:2015",
        title="Politique Qualité — ACME (iso-9001)",
        sections=(
            GeneratedSection("s1", "Engagement", ("5.2",), "Corps **gras**."),
            GeneratedSection("s2", "Périmètre", (), "Tout le site."),
        ),
        provider="ollama",
        tokens_used=42,
        latency_ms=1000,
    )
    md = draft.to_markdown()
    assert md.startswith("# Politique Qualité — ACME (iso-9001)")
    assert "## Engagement" in md
    assert "*Clauses : 5.2*" in md
    assert "## Périmètre" in md
    # La section sans clauses n'a pas de ligne *Clauses : *.
    assert "*Clauses : *" not in md
    assert md.endswith("\n")

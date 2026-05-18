"""HeuristicPiiFilter tests."""
from __future__ import annotations

from infrastructure.guardrails import HeuristicPiiFilter


def test_redacts_email():
    f = HeuristicPiiFilter()
    r = f.redact("Contact me at alice@example.com asap")
    assert "<EMAIL>" in r.redacted_text
    assert "EMAIL" in r.findings
    assert r.had_pii


def test_redacts_iban():
    f = HeuristicPiiFilter()
    r = f.redact("Pay to FR7630001007941234567890185 please")
    assert "<IBAN>" in r.redacted_text


def test_redacts_card():
    f = HeuristicPiiFilter()
    r = f.redact("My card 4111 1111 1111 1111 is here")
    assert "<CARD>" in r.redacted_text


def test_redacts_ssn():
    f = HeuristicPiiFilter()
    r = f.redact("SSN 123-45-6789 attached")
    assert "<SSN>" in r.redacted_text


def test_no_pii_no_redaction():
    f = HeuristicPiiFilter()
    r = f.redact("This is a generic sentence about quality.")
    assert not r.had_pii
    assert r.redacted_text == "This is a generic sentence about quality."

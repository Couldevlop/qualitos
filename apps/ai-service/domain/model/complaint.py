"""Modèle de domaine : analyse NLP des réclamations clients (§4.9, §12.1).

Sentiment lexical + classification par termes-graines + détection de réclamations
critiques. Objets de valeur immuables ; le calcul vit dans
``domain.service.complaint_nlp`` (fonctions pures, déterministes, sans framework).
"""
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class ComplaintInsight:
    """Analyse d'une réclamation : sentiment, catégorie, criticité."""

    index: int
    sentiment: float          # polarité ∈ [-1, 1]
    sentiment_label: str      # "negative" | "neutral" | "positive"
    category: str             # catégorie détectée (ou "autre")
    critical: bool            # sentiment très négatif ou marqueur d'urgence/gravité


@dataclass(frozen=True, slots=True)
class ComplaintAnalysis:
    """Résultat d'une analyse de lot de réclamations."""

    n: int
    critical_count: int
    insights: list[ComplaintInsight] = field(default_factory=list)

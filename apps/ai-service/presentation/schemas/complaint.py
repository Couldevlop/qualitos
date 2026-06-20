"""Schémas Pydantic de l'analyse NLP des réclamations (§4.9)."""
from __future__ import annotations

from pydantic import BaseModel, Field

from domain.model.complaint import ComplaintAnalysis


class ComplaintAnalyzeRequest(BaseModel):
    texts: list[str] = Field(..., min_length=1, max_length=2000)
    # Taxonomie optionnelle {catégorie: [termes-graines]} ; défaut si absente.
    categories: dict[str, list[str]] | None = None
    # Moteur de sentiment : lexical (défaut, réel) | bert (lourd, opt-in extra ml ;
    # 422 si la lib n'est pas installée — ADR 0031).
    backend: str = Field(default="lexical", pattern="^(lexical|bert)$")


class ComplaintInsightResponse(BaseModel):
    index: int
    sentiment: float
    sentiment_label: str
    category: str
    critical: bool


class ComplaintAnalyzeResponse(BaseModel):
    n: int
    critical_count: int
    insights: list[ComplaintInsightResponse]

    @classmethod
    def from_domain(cls, a: ComplaintAnalysis) -> "ComplaintAnalyzeResponse":
        return cls(
            n=a.n, critical_count=a.critical_count,
            insights=[
                ComplaintInsightResponse(
                    index=i.index, sentiment=i.sentiment, sentiment_label=i.sentiment_label,
                    category=i.category, critical=i.critical)
                for i in a.insights
            ],
        )

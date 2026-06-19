"""NLP des réclamations clients — sentiment + classification (pur, sans dépendance lourde).

Couvre §4.9 « NLP de classification, sentiment analysis, détection de réclamations
critiques ». Approche **lexicale et par termes-graines**, FR/EN, déterministe et
explicable : pas de BERT/Whisper (qui exigent torch/GPU) — ceux-ci pourront se
brancher derrière le même contrat quand un budget GPU existera (cf. ADR 0014 §7.3).

- **Sentiment** : score de polarité ∈ [-1, 1] à partir d'un lexique pondéré, avec
  gestion de la **négation** (inverse le mot suivant) et des **intensifieurs**.
- **Classification** : score par catégorie via des termes-graines ; la catégorie au
  plus fort score gagne (sinon « autre »). Catégories par défaut surchargeables.
- **Criticité** : réclamation critique si sentiment très négatif OU présence de
  marqueurs d'urgence/gravité (sécurité, juridique, rappel…).
"""
from __future__ import annotations

import re
import unicodedata

from domain.model.complaint import ComplaintAnalysis, ComplaintInsight

_TOKEN = re.compile(r"[a-z0-9]+")

# Lexique de polarité (mot normalisé -> poids). Compact mais réel, FR + EN.
_SENTIMENT: dict[str, float] = {
    # négatifs
    "mauvais": -1.0, "mauvaise": -1.0, "nul": -1.0, "horrible": -1.5, "inacceptable": -1.5,
    "deçu": -1.0, "decu": -1.0, "decevant": -1.0, "lent": -0.6, "lente": -0.6, "casse": -1.0,
    "defectueux": -1.2, "panne": -1.0, "probleme": -0.8, "erreur": -0.7, "retard": -0.8,
    "sale": -0.9, "danger": -1.4, "dangereux": -1.4, "fuite": -0.9, "absent": -0.7,
    "manquant": -0.8, "endommage": -1.1, "rembourse": -0.3, "reclamation": -0.5,
    "bad": -1.0, "poor": -1.0, "terrible": -1.5, "broken": -1.0, "slow": -0.6,
    "defective": -1.2, "late": -0.8, "dirty": -0.9, "missing": -0.8, "damaged": -1.1,
    "unacceptable": -1.5, "disappointed": -1.0, "leak": -0.9, "fault": -0.8,
    # positifs
    "bon": 0.9, "bonne": 0.9, "excellent": 1.4, "parfait": 1.3, "rapide": 0.6,
    "satisfait": 1.0, "merci": 0.6, "super": 1.0, "impeccable": 1.2, "conforme": 0.7,
    "good": 0.9, "great": 1.1, "excellent_en": 1.4, "perfect": 1.3, "fast": 0.6,
    "satisfied": 1.0, "thanks": 0.6, "clean": 0.7, "compliant": 0.7,
}

_NEGATORS = frozenset("ne pas plus aucun jamais sans non not no never none".split())
_INTENSIFIERS: dict[str, float] = {
    "tres": 1.5, "vraiment": 1.4, "extremement": 1.8, "trop": 1.4, "totalement": 1.6,
    "very": 1.5, "really": 1.4, "extremely": 1.8, "too": 1.4, "completely": 1.6,
}

# Catégories par défaut (id -> termes-graines normalisés).
_DEFAULT_CATEGORIES: dict[str, tuple[str, ...]] = {
    "produit": ("produit", "qualite", "defectueux", "casse", "endommage", "fuite",
                "defective", "broken", "quality", "product", "damaged"),
    "livraison": ("livraison", "retard", "colis", "expedition", "transport", "delai",
                  "delivery", "late", "shipping", "parcel", "delay"),
    "service": ("service", "sav", "accueil", "conseiller", "support", "attente",
                "staff", "agent", "rude", "wait"),
    "facturation": ("facture", "facturation", "paiement", "rembourse", "prix", "tarif",
                    "invoice", "billing", "payment", "refund", "price", "charge"),
    "securite": ("danger", "dangereux", "securite", "blessure", "risque", "rappel",
                 "safety", "danger", "injury", "hazard", "recall"),
}

# Marqueurs de criticité (urgence / gravité) — déclenchent le drapeau critique.
_CRITICAL_MARKERS = frozenset(
    "danger dangereux securite blessure risque rappel juridique avocat urgent grave "
    "safety injury hazard recall legal lawyer urgent severe".split()
)
_CRITICAL_SENTIMENT = -0.6  # en deçà, la réclamation est jugée critique


# Backends de sentiment. ``lexical`` = défaut réel (ci-dessous) ; ``bert`` =
# backend lourd opt-in, import paresseux (ADR 0031).
_BACKENDS = ("lexical", "bert")


def analyze(texts: list[str], *, categories: dict[str, list[str]] | None = None,
            backend: str = "lexical") -> ComplaintAnalysis:
    """Analyse une liste de réclamations : sentiment, catégorie, criticité par item.

    :param categories: taxonomie {catégorie: [termes-graines]} ; défaut si None.
    :param backend: moteur de sentiment — ``lexical`` (défaut, réel, pur) | ``bert``
        (lourd, opt-in, extra ml ; sinon :class:`MlBackendUnavailableError`).
        La classification et la criticité restent identiques dans les deux cas.
    :raises ValueError: liste vide, trop volumineuse ou backend invalide.
    :raises MlBackendUnavailableError: ``bert`` sélectionné mais lib absente.
    """
    if backend not in _BACKENDS:
        raise ValueError(f"backend must be one of {_BACKENDS}")
    if backend == "bert":
        from domain.service.ml_backends import sentiment_bert
        return sentiment_bert.analyze(texts, categories=categories)

    # --- défaut : sentiment lexical (réel, sans dépendance lourde) -----------------
    if not texts:
        raise ValueError("texts must be a non-empty list")
    if len(texts) > 2000:
        raise ValueError("too many texts (max 2000)")

    cats = ({k: tuple(_norm(t) for t in v) for k, v in categories.items()}
            if categories else _DEFAULT_CATEGORIES)

    insights = [_analyze_one(i, t, cats) for i, t in enumerate(texts)]
    critical_count = sum(1 for x in insights if x.critical)
    return ComplaintAnalysis(n=len(texts), critical_count=critical_count, insights=insights)


def _analyze_one(index: int, text: str, cats: dict[str, tuple[str, ...]]) -> ComplaintInsight:
    tokens = _tokenize(text)
    polarity = _sentiment(tokens)
    category, _cat_score = _classify(tokens, cats)
    critical = polarity <= _CRITICAL_SENTIMENT or any(t in _CRITICAL_MARKERS for t in tokens)
    label = "negative" if polarity < -0.15 else "positive" if polarity > 0.15 else "neutral"
    return ComplaintInsight(
        index=index, sentiment=round(polarity, 3), sentiment_label=label,
        category=category, critical=critical,
    )


def _sentiment(tokens: list[str]) -> float:
    """Score de polarité borné [-1, 1] (lexique + négation + intensifieurs)."""
    score = 0.0
    hits = 0
    intensifier = 1.0
    negate = False
    for tok in tokens:
        if tok in _NEGATORS:
            negate = True
            continue
        if tok in _INTENSIFIERS:
            intensifier = _INTENSIFIERS[tok]
            continue
        if tok in _SENTIMENT:
            w = _SENTIMENT[tok] * intensifier * (-1.0 if negate else 1.0)
            score += w
            hits += 1
        intensifier = 1.0
        negate = False
    if hits == 0:
        return 0.0
    # Moyenne par mot porteur, bornée.
    return max(-1.0, min(1.0, score / hits))


def _classify(tokens: list[str], cats: dict[str, tuple[str, ...]]) -> tuple[str, float]:
    """Catégorie au plus fort recouvrement de termes-graines ('autre' si aucun)."""
    tset = set(tokens)
    best_cat = "autre"
    best_score = 0
    for cat, seeds in cats.items():
        s = sum(1 for seed in seeds if seed in tset)
        if s > best_score:
            best_score = s
            best_cat = cat
    return best_cat, float(best_score)


def _tokenize(text: str) -> list[str]:
    return _TOKEN.findall(_norm(text))


def _norm(text: str) -> str:
    norm = unicodedata.normalize("NFD", (text or "").lower())
    return "".join(c for c in norm if unicodedata.category(c) != "Mn")

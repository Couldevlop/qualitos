"""Backend de sentiment réclamations **BERT** (transformers, opt-in, paresseux — ADR 0031).

Vrai modèle Transformer (Hugging Face ``pipeline('sentiment-analysis')``,
multilingue) pour la **polarité**, derrière le contrat ``ComplaintAnalysis``
existant. La **classification par catégorie** et la **détection de criticité**
réutilisent telles quelles les heuristiques validées du backend lexical par défaut
(:mod:`domain.service.complaint_nlp`) : seule la note de sentiment passe au modèle
neuronal. La polarité BERT (label POSITIVE/NEGATIVE + score ∈ [0,1]) est projetée
sur l'échelle [-1, 1] attendue par le contrat.

``transformers`` (+ ``torch``) est **lourd** : il vit dans l'extra ``ml`` du
``pyproject.toml`` et n'est PAS installé en CI. L'import est fait **dans**
:func:`analyze` ; son absence lève :class:`MlBackendUnavailableError` (→ 422),
jamais un faux résultat.
"""
from __future__ import annotations

from domain.model.complaint import ComplaintAnalysis, ComplaintInsight
from domain.service import complaint_nlp as _base
from domain.service.ml_backends import MlBackendUnavailableError

# Modèle multilingue compact par défaut ; surchargeable par l'appelant si besoin.
_DEFAULT_MODEL = "nlptown/bert-base-multilingual-uncased-sentiment"


def analyze(texts: list[str], *, categories: dict[str, list[str]] | None = None,
            model_name: str | None = None) -> ComplaintAnalysis:
    """Analyse un lot de réclamations : sentiment **BERT** + catégorie + criticité.

    :param categories: taxonomie {catégorie: [termes-graines]} ; défaut si None.
    :param model_name: modèle HF à charger ; défaut multilingue compact.
    :raises ValueError: liste vide ou trop volumineuse.
    :raises MlBackendUnavailableError: ``transformers`` non installé (extra ml).
    """
    if not texts:
        raise ValueError("texts must be a non-empty list")
    if len(texts) > 2000:
        raise ValueError("too many texts (max 2000)")

    try:  # import paresseux : transformers/torch ne sont tirés que si ce backend est choisi.
        from transformers import pipeline
    except ImportError as exc:  # pragma: no cover - exercé sans la lib en CI via le wrapper
        raise MlBackendUnavailableError("bert", "transformers") from exc

    clf = pipeline("sentiment-analysis", model=model_name or _DEFAULT_MODEL)
    raw = clf(list(texts))

    cats = ({k: tuple(_base._norm(t) for t in v) for k, v in categories.items()}
            if categories else _base._DEFAULT_CATEGORIES)

    insights: list[ComplaintInsight] = []
    for index, (text, pred) in enumerate(zip(texts, raw)):
        polarity = _to_polarity(pred)
        tokens = _base._tokenize(text)
        category, _ = _base._classify(tokens, cats)
        critical = (
            polarity <= _base._CRITICAL_SENTIMENT
            or any(t in _base._CRITICAL_MARKERS for t in tokens)
        )
        label = "negative" if polarity < -0.15 else "positive" if polarity > 0.15 else "neutral"
        insights.append(ComplaintInsight(
            index=index, sentiment=round(polarity, 3), sentiment_label=label,
            category=category, critical=critical,
        ))

    critical_count = sum(1 for x in insights if x.critical)
    return ComplaintAnalysis(n=len(texts), critical_count=critical_count, insights=insights)


def _to_polarity(pred: dict) -> float:
    """Projette une sortie de pipeline HF vers une polarité ∈ [-1, 1].

    Gère les deux familles de labels : binaire (POSITIVE/NEGATIVE) et étoiles
    (« 1 star » … « 5 stars » du modèle nlptown multilingue).
    """
    label = str(pred.get("label", "")).strip().lower()
    score = float(pred.get("score", 0.0))
    if "star" in label:  # nlptown : 1..5 étoiles → [-1, 1].
        stars = int(label.split()[0])
        return (stars - 3) / 2.0
    if label.startswith("neg"):
        return -score
    if label.startswith("pos"):
        return score
    return 0.0

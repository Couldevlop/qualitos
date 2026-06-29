"""Golden test « preuve » du backend sentiment **BERT** (DistilCamemBERT FR, ADR 0045).

Contrairement à ``test_ml_backends.py`` (qui vérifie le *message d'erreur* quand la
lib est absente), ce test **charge et exécute le vrai modèle ouvert** lorsque
``torch`` + ``transformers`` sont installés (extra ``ml``). Il est **skippé
automatiquement en CI** (libs absentes) et s'exécute en local/on-prem pour figer le
comportement de référence.

Invariant doré : le modèle neuronal FR (a) classe correctement une réclamation
clairement négative et une clairement positive, et (b) **rattrape un cas que le
backend lexical par défaut rate** — la facture erronée, dont le vocabulaire n'est
pas dans le lexique mais dont le *sens* est négatif.
"""
from __future__ import annotations

import importlib.util

import pytest

from domain.service import complaint_nlp

_HAS_BERT = (
    importlib.util.find_spec("transformers") is not None
    and importlib.util.find_spec("torch") is not None
)

pytestmark = pytest.mark.skipif(
    not _HAS_BERT,
    reason="extra ml (torch+transformers) absent : chemin BERT non exécuté (skippé en CI)",
)

_NEG = "Le produit est arrivé cassé et le service après-vente est inacceptable."
_POS = "Service impeccable, merci beaucoup, je recommande vivement !"
_BILLING = "On m'a facturé deux fois, la facture est erronée."  # piège du lexical


def test_bert_classifies_negative_and_positive():
    res = complaint_nlp.analyze([_NEG, _POS], backend="bert")
    assert res.n == 2
    neg, pos = res.insights
    assert neg.sentiment < -0.3 and neg.sentiment_label == "negative"
    assert pos.sentiment > 0.3 and pos.sentiment_label == "positive"
    assert neg.sentiment < pos.sentiment


def test_bert_is_critical_on_dangerous_complaint():
    res = complaint_nlp.analyze([_NEG], backend="bert")
    assert res.insights[0].critical is True
    assert res.critical_count == 1


def test_bert_outperforms_lexical_on_semantic_case():
    """La facture erronée : lexical ≈ neutre (mots hors lexique), BERT = négatif."""
    bert = complaint_nlp.analyze([_BILLING], backend="bert").insights[0]
    lexical = complaint_nlp.analyze([_BILLING], backend="lexical").insights[0]
    assert lexical.sentiment >= -0.15  # le lexical ne « voit » pas la négativité
    assert bert.sentiment < lexical.sentiment  # le neuronal, si
    assert bert.sentiment_label == "negative"


def test_bert_preserves_category_and_contract_shape():
    """Catégorie/criticité réutilisent les heuristiques ; le contrat reste stable."""
    res = complaint_nlp.analyze([_NEG, _POS, _BILLING], backend="bert")
    assert [i.index for i in res.insights] == [0, 1, 2]
    assert res.insights[2].category == "facturation"
    assert all(-1.0 <= i.sentiment <= 1.0 for i in res.insights)

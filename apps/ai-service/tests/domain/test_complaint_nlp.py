"""Tests du NLP des réclamations (sentiment lexical + classification)."""
import pytest

from domain.service import complaint_nlp


class TestSentiment:
    def test_negative_complaint(self):
        r = complaint_nlp.analyze(["Produit défectueux, livraison en retard, inacceptable"])
        ins = r.insights[0]
        assert ins.sentiment < -0.15
        assert ins.sentiment_label == "negative"

    def test_positive_feedback(self):
        r = complaint_nlp.analyze(["Service excellent et livraison rapide, parfait merci"])
        assert r.insights[0].sentiment_label == "positive"

    def test_negation_flips_polarity(self):
        pos = complaint_nlp.analyze(["produit bon"]).insights[0].sentiment
        neg = complaint_nlp.analyze(["produit pas bon"]).insights[0].sentiment
        assert pos > 0 > neg

    def test_neutral_when_no_sentiment_words(self):
        r = complaint_nlp.analyze(["commande numero 12345 du 3 mars"])
        assert r.insights[0].sentiment_label == "neutral"

    def test_intensifier_amplifies(self):
        # « lent » = -0.6 (ne sature pas le plancher -1.0), l'intensifieur est visible.
        base = complaint_nlp.analyze(["livraison lente"]).insights[0].sentiment
        strong = complaint_nlp.analyze(["livraison tres lente"]).insights[0].sentiment
        assert strong < base  # plus négatif


class TestClassification:
    def test_categories_detected(self):
        texts = [
            "Le colis est arrivé avec 5 jours de retard",         # livraison
            "Produit cassé à la réception",                        # produit
            "Erreur sur ma facture, je veux un remboursement",     # facturation
            "Le conseiller du SAV était désagréable",              # service
        ]
        r = complaint_nlp.analyze(texts)
        cats = [i.category for i in r.insights]
        assert cats[0] == "livraison"
        assert cats[1] == "produit"
        assert cats[2] == "facturation"
        assert cats[3] == "service"

    def test_unmatched_is_autre(self):
        r = complaint_nlp.analyze(["bonjour"])
        assert r.insights[0].category == "autre"

    def test_custom_categories(self):
        cats = {"hygiene": ["sale", "propre", "hygiene"]}
        r = complaint_nlp.analyze(["la chambre était sale"], categories=cats)
        assert r.insights[0].category == "hygiene"


class TestCriticality:
    def test_safety_marker_is_critical(self):
        r = complaint_nlp.analyze(["Produit dangereux, risque de blessure, rappel urgent"])
        assert r.insights[0].critical is True
        assert r.critical_count == 1

    def test_very_negative_is_critical(self):
        r = complaint_nlp.analyze(["horrible inacceptable tres mauvais nul"])
        assert r.insights[0].critical is True

    def test_positive_is_not_critical(self):
        r = complaint_nlp.analyze(["service parfait merci"])
        assert r.insights[0].critical is False


class TestValidation:
    def test_empty_rejected(self):
        with pytest.raises(ValueError):
            complaint_nlp.analyze([])

    def test_too_many_rejected(self):
        with pytest.raises(ValueError, match="too many"):
            complaint_nlp.analyze(["x"] * 2001)

    def test_deterministic(self):
        texts = ["produit cassé", "service excellent"]
        assert complaint_nlp.analyze(texts) == complaint_nlp.analyze(texts)

"""Tests du clustering de non-conformités (TF-IDF + cosinus, NumPy pur)."""
import pytest

from domain.service import nc_clustering


class TestNcClustering:
    def test_similar_texts_cluster_together(self):
        texts = [
            "Fuite d'huile sur la presse hydraulique ligne 2",
            "Fuite d'huile détectée presse hydraulique ligne 3",
            "Étiquette produit manquante sur carton expédition",
            "Étiquette produit manquante sur palette expédition",
            "Capteur de température défaillant chambre froide",
        ]
        result = nc_clustering.cluster(texts)
        assert result.n == 5
        assert len(result.clusters) == 2
        sizes = sorted(c.size for c in result.clusters)
        assert sizes == [2, 2]
        # Le capteur isolé reste du bruit.
        assert 4 in result.noise_indices

    def test_top_terms_explain_the_cluster(self):
        texts = [
            "Fuite d'huile sur la presse hydraulique",
            "Fuite d'huile presse hydraulique atelier",
            "Rayure sur capot peinture",
        ]
        result = nc_clustering.cluster(texts)
        leak = next(c for c in result.clusters if 0 in c.indices)
        assert any(t in ("fuite", "huile", "presse", "hydraulique") for t in leak.top_terms)

    def test_accents_and_case_are_normalized(self):
        texts = ["Étiquette MANQUANTE produit", "etiquette manquante PRODUIT"]
        result = nc_clustering.cluster(texts)
        assert len(result.clusters) == 1
        assert result.clusters[0].size == 2

    def test_all_dissimilar_is_all_noise(self):
        texts = ["fuite huile presse", "soudure fissuree chassis", "logiciel plantage serveur"]
        result = nc_clustering.cluster(texts)
        assert result.clusters == []
        assert sorted(result.noise_indices) == [0, 1, 2]
        assert result.clustered_ratio == 0.0

    def test_empty_list_yields_empty_result(self):
        result = nc_clustering.cluster([])
        assert result.n == 0
        assert result.clusters == []

    def test_stopwords_only_texts_are_noise(self):
        result = nc_clustering.cluster(["le la les de", "et ou sur dans"])
        assert result.clusters == []
        assert sorted(result.noise_indices) == [0, 1]

    def test_threshold_validation(self):
        with pytest.raises(ValueError):
            nc_clustering.cluster(["a b", "a b"], threshold=0.0)
        with pytest.raises(ValueError):
            nc_clustering.cluster(["a b", "a b"], threshold=1.0)

    def test_too_many_texts_rejected(self):
        with pytest.raises(ValueError, match="too many"):
            nc_clustering.cluster(["texte exemple"] * 2001)

    def test_deterministic(self):
        texts = ["fuite huile presse", "fuite huile presse ligne", "rayure capot"]
        a = nc_clustering.cluster(texts)
        b = nc_clustering.cluster(texts)
        assert a == b


class TestDbscanDensity:
    def test_method_is_dbscan(self):
        result = nc_clustering.cluster(["fuite huile presse", "fuite huile presse ligne"])
        assert result.method == "dbscan"

    def test_min_samples_requires_denser_neighborhood(self):
        # Deux textes similaires : cluster avec min_samples=2, mais bruit avec min_samples=3
        # (chaque point n'a qu'un voisin → pas assez dense pour être un point-cœur).
        texts = ["fuite huile presse hydraulique", "fuite huile presse hydraulique ligne"]
        loose = nc_clustering.cluster(texts, min_samples=2)
        assert len(loose.clusters) == 1
        strict = nc_clustering.cluster(texts, min_samples=3)
        assert strict.clusters == []
        assert sorted(strict.noise_indices) == [0, 1]

    def test_dense_core_forms_cluster_with_border(self):
        # 3 textes quasi identiques (cœur dense) → un seul cluster même à min_samples=3.
        texts = [
            "fuite huile presse hydraulique ligne 2",
            "fuite huile presse hydraulique ligne 3",
            "fuite huile presse hydraulique ligne 4",
        ]
        result = nc_clustering.cluster(texts, min_samples=3)
        assert len(result.clusters) == 1
        assert result.clusters[0].size == 3

    def test_min_samples_validation(self):
        with pytest.raises(ValueError, match="min_samples"):
            nc_clustering.cluster(["a b", "a b"], min_samples=1)

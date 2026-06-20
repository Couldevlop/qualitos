"""Tests des backends ML enfichables opt-in (ADR 0031).

Trois axes par capacité :
1. **Défaut inchangé** : le chemin réel léger répond comme avant.
2. **Sélection de backend** : le paramètre route bien vers le backend lourd.
3. **Backend indisponible → erreur claire** : les libs lourdes (prophet/torch/
   hdbscan/transformers/whisper) ne sont PAS installées en CI ; ces tests
   vérifient le **message d'erreur** ``MlBackendUnavailableError`` (pas l'inférence
   réelle). Si une lib venait à être installée, le test est neutralisé via skip.
"""
from __future__ import annotations

import importlib.util

import pytest

from domain.service import complaint_nlp, forecasting, nc_clustering
from domain.service.ml_backends import (
    MlBackendUnavailableError,
    transcribe_whisper,
)


def _installed(module: str) -> bool:
    return importlib.util.find_spec(module) is not None


# --- défaut inchangé --------------------------------------------------------------

def test_forecast_default_is_holt_winters_unchanged():
    res_default = forecasting.forecast([10, 12, 14, 16, 18, 20], 30)
    res_explicit = forecasting.forecast([10, 12, 14, 16, 18, 20], 30, model="holt_winters")
    assert res_default.model in ("holt_linear", "holt_winters_additive")
    assert res_explicit.model == res_default.model
    assert res_explicit.probability == res_default.probability


def test_clustering_default_is_dbscan_unchanged():
    texts = ["fuite huile presse", "fuite huile presse ligne 2", "erreur ecran"]
    res = nc_clustering.cluster(texts)
    res2 = nc_clustering.cluster(texts, method="dbscan")
    assert res.method == "dbscan"
    assert res2.method == "dbscan"
    assert [c.size for c in res.clusters] == [c.size for c in res2.clusters]


def test_complaint_default_is_lexical_unchanged():
    res = complaint_nlp.analyze(["produit cassé et dangereux"])
    res2 = complaint_nlp.analyze(["produit cassé et dangereux"], backend="lexical")
    assert res.insights[0].critical is True
    assert res2.insights[0].sentiment == res.insights[0].sentiment


# --- sélection de backend : validation des noms -----------------------------------

def test_forecast_rejects_unknown_model():
    with pytest.raises(ValueError, match="model must be one of"):
        forecasting.forecast([1, 2, 3, 4], 5, model="xgboost")


def test_clustering_rejects_unknown_method():
    with pytest.raises(ValueError, match="method must be one of"):
        nc_clustering.cluster(["a b", "a b"], method="kmeans")


def test_complaint_rejects_unknown_backend():
    with pytest.raises(ValueError, match="backend must be one of"):
        complaint_nlp.analyze(["x"], backend="gpt")


# --- backend indisponible → erreur claire (libs absentes en CI) -------------------

@pytest.mark.skipif(_installed("prophet"), reason="prophet installé : chemin réel non testé ici")
def test_forecast_prophet_unavailable_raises_clear_error():
    with pytest.raises(MlBackendUnavailableError) as exc:
        forecasting.forecast([10, 12, 14, 16, 18, 20], 30, model="prophet")
    assert exc.value.backend == "prophet"
    assert "extra ml" in str(exc.value)


@pytest.mark.skipif(_installed("torch"), reason="torch installé : chemin réel non testé ici")
def test_forecast_lstm_unavailable_raises_clear_error():
    with pytest.raises(MlBackendUnavailableError) as exc:
        forecasting.forecast([10, 12, 14, 16, 18, 20], 30, model="lstm")
    assert exc.value.backend == "lstm"
    assert exc.value.package == "torch"


@pytest.mark.skipif(_installed("hdbscan"), reason="hdbscan installé : chemin réel non testé ici")
def test_clustering_hdbscan_unavailable_raises_clear_error():
    with pytest.raises(MlBackendUnavailableError) as exc:
        nc_clustering.cluster(["a b c", "a b c", "d e f"], method="hdbscan")
    assert exc.value.backend == "hdbscan"
    assert "extra ml" in str(exc.value)


@pytest.mark.skipif(_installed("transformers"),
                    reason="transformers installé : chemin réel non testé ici")
def test_complaint_bert_unavailable_raises_clear_error():
    with pytest.raises(MlBackendUnavailableError) as exc:
        complaint_nlp.analyze(["produit cassé"], backend="bert")
    assert exc.value.backend == "bert"
    assert exc.value.package == "transformers"


@pytest.mark.skipif(_installed("whisper"), reason="whisper installé : chemin réel non testé ici")
def test_transcribe_whisper_unavailable_raises_clear_error():
    with pytest.raises(MlBackendUnavailableError) as exc:
        transcribe_whisper.transcribe(b"\x00\x01\x02fake-audio")
    assert exc.value.backend == "whisper"
    assert exc.value.package == "openai-whisper"


# --- garde-fous de validation des backends lourds (avant import lib) --------------

def test_forecast_prophet_validates_inputs_before_import():
    # direction invalide → ValueError même backend lourd (validation en amont).
    with pytest.raises(ValueError, match="direction"):
        forecasting.forecast([1, 2, 3, 4], 5, model="prophet", direction="bogus")


def test_forecast_lstm_validates_short_series_before_import():
    with pytest.raises(ValueError, match="data points"):
        forecasting.forecast([1, 2], 5, model="lstm")


def test_clustering_hdbscan_validates_min_samples():
    with pytest.raises(ValueError, match="min_samples"):
        nc_clustering.cluster(["a b", "a b"], method="hdbscan", min_samples=1)


def test_transcribe_validates_empty_audio_before_import():
    with pytest.raises(ValueError, match="non-empty"):
        transcribe_whisper.transcribe(b"")

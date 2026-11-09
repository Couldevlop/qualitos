"""Quand un procédé se dirige vers la sortie de ses limites (§6.5).

Ce qui est tenu ici n'est pas « le modèle est bon » — c'est que la réponse soit
LISIBLE et honnête : les limites annoncées sont celles de la carte, le risque
monte avec l'horizon et jamais l'inverse, une dérive franche est datée, un
procédé stable ne déclenche rien, et une série courte le dit au lieu d'annoncer
une date.
"""
from __future__ import annotations

import pytest

from domain.service import spc_forecast, spc_rules


def _stable(n: int = 30) -> list[float]:
    """Procédé centré sur 10, bruit alternant faible : rien ne dérive."""
    return [10.0 + (0.2 if i % 2 else -0.2) for i in range(n)]


def _derive(n: int = 30, pas: float = 0.5) -> list[float]:
    """Dérive franche vers le haut : +0,5 par période autour d'un bruit faible."""
    return [10.0 + pas * i + (0.1 if i % 2 else -0.1) for i in range(n)]


# ---------- ce que la prévision annonce ----------

def test_derive_franche_est_datee_et_du_bon_cote():
    f = spc_forecast.forecast_breach(_derive(), horizon=12)

    assert f.will_breach is True
    assert f.first_breach_step is not None and 1 <= f.first_breach_step <= 12
    assert f.side == "upper"
    assert f.trend > 0


def test_derive_descendante_sort_par_le_bas():
    f = spc_forecast.forecast_breach(_derive(pas=-0.5), horizon=12)

    assert f.side == "lower"
    assert f.trend < 0


def test_un_procede_stable_ne_declenche_rien():
    f = spc_forecast.forecast_breach(_stable(), horizon=6)

    assert f.will_breach is False
    assert f.first_breach_step is None
    assert f.side is None


def test_les_limites_annoncees_sont_celles_de_la_carte():
    valeurs = _derive()
    f = spc_forecast.forecast_breach(valeurs, horizon=6)
    carte = spc_rules.estimate_limits(valeurs)

    # Juger la prévision contre d'autres limites que celles affichées ne
    # parlerait pas du même procédé.
    assert f.limits.center_line == pytest.approx(carte.center_line)
    assert f.limits.ucl == pytest.approx(carte.ucl)
    assert f.limits.lcl == pytest.approx(carte.lcl)
    assert f.limits.estimated is True


def test_une_reference_connue_remplace_les_limites_estimees():
    f = spc_forecast.forecast_breach(_stable(), horizon=6, center=10.0, sigma=0.5)

    assert f.limits.estimated is False
    assert f.limits.ucl == pytest.approx(11.5)
    assert f.limits.lcl == pytest.approx(8.5)


def test_le_risque_monte_avec_lhorizon_sur_une_derive_naissante():
    # Référence large et dérive lente : la première période est encore
    # confortablement dans la bande, la dernière beaucoup moins. Sur une dérive
    # franche, le risque est déjà à 1 dès la première période — ce qui est juste,
    # mais ne dit rien de la monotonie.
    f = spc_forecast.forecast_breach(_derive(pas=0.1), horizon=10,
                                     center=10.0, sigma=1.0)

    risques = [p.p_breach for p in f.points]
    assert risques == sorted(risques)
    assert f.points[-1].p_breach > f.points[0].p_breach


def test_chaque_periode_porte_son_intervalle_et_ses_deux_cotes():
    f = spc_forecast.forecast_breach(_derive(), horizon=4)

    assert [p.step for p in f.points] == [1, 2, 3, 4]
    for p in f.points:
        assert p.low <= p.value <= p.high
        assert 0.0 <= p.p_above_ucl <= 1.0
        assert 0.0 <= p.p_below_lcl <= 1.0
        assert p.p_breach == pytest.approx(min(1.0, p.p_above_ucl + p.p_below_lcl))


def test_un_seuil_plus_exigeant_repousse_la_date():
    tot = spc_forecast.forecast_breach(_derive(pas=0.25), horizon=20, threshold=0.3)
    tard = spc_forecast.forecast_breach(_derive(pas=0.25), horizon=20, threshold=0.9)

    assert tot.first_breach_step is not None
    assert tard.first_breach_step is None or tard.first_breach_step >= tot.first_breach_step


# ---------- ce que la prévision refuse d'affirmer ----------

def test_une_serie_courte_annonce_une_confiance_basse():
    f = spc_forecast.forecast_breach([10.0, 10.5, 11.0, 11.5, 12.0], horizon=6)

    # Cinq points : la tendance existe, mais la dater serait prêter au chiffre
    # une précision qu'il n'a pas.
    assert f.confidence in ("low", "medium")
    assert f.n == 5


def test_un_procede_plat_ne_franchit_rien_faute_de_dispersion():
    f = spc_forecast.forecast_breach([7.0] * 10, horizon=6)

    # σ nul : la bande a une épaisseur nulle, et annoncer un franchissement
    # contre elle n'informerait de rien.
    assert f.limits.sigma == pytest.approx(0.0)
    assert f.will_breach is False


def test_une_serie_trop_courte_est_refusee():
    with pytest.raises(ValueError, match="data points"):
        spc_forecast.forecast_breach([1.0, 2.0, 3.0], horizon=3)


@pytest.mark.parametrize("horizon", [0, 61, -1])
def test_un_horizon_hors_bornes_est_refuse(horizon: int):
    with pytest.raises(ValueError, match="horizon"):
        spc_forecast.forecast_breach(_stable(), horizon=horizon)


@pytest.mark.parametrize("seuil", [0.0, 1.5, -0.2])
def test_un_seuil_hors_bornes_est_refuse(seuil: float):
    with pytest.raises(ValueError, match="threshold"):
        spc_forecast.forecast_breach(_stable(), threshold=seuil)


def test_une_reference_incomplete_est_refusee():
    with pytest.raises(ValueError, match="together"):
        spc_forecast.forecast_breach(_stable(), center=10.0)


def test_un_sigma_de_reference_nul_est_refuse():
    with pytest.raises(ValueError, match="sigma"):
        spc_forecast.forecast_breach(_stable(), center=10.0, sigma=0.0)


# ---------- l'explicabilité (§12.3) ----------

def test_la_reponse_porte_de_quoi_juger_le_chiffre():
    f = spc_forecast.forecast_breach(_derive(), horizon=6)

    assert f.model in ("holt_linear", "holt_winters_additive")
    assert f.residual_sigma > 0
    assert 0.0 <= f.r2 <= 1.0
    assert f.confidence in ("low", "medium", "high")
    assert f.threshold == pytest.approx(0.5)


def test_la_saisonnalite_est_prise_en_compte_quand_elle_est_fournie():
    base = [10.0, 12.0, 14.0, 12.0] * 6  # cycle de 4 périodes
    f = spc_forecast.forecast_breach(base, horizon=4, seasonal_period=4)

    assert f.model == "holt_winters_additive"
    assert len(f.points) == 4

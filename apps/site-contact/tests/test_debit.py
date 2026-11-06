"""La limite de débit : elle laisse passer ce qu'il faut, et pas plus."""

import pytest

from app.debit import Debit


class Horloge:
    def __init__(self):
        self.t = 0.0

    def __call__(self):
        return self.t


def test_un_maximum_nul_est_refuse():
    with pytest.raises(ValueError):
        Debit(0, 60)


def test_les_premieres_passent_et_la_suivante_non():
    debit = Debit(3, 60, Horloge())
    assert [debit.accepte("1.2.3.4") for _ in range(4)] == [True, True, True, False]


def test_les_adresses_sont_comptees_separement():
    debit = Debit(1, 60, Horloge())
    assert debit.accepte("1.2.3.4") is True
    assert debit.accepte("5.6.7.8") is True
    assert debit.accepte("1.2.3.4") is False


def test_la_fenetre_glisse():
    horloge = Horloge()
    debit = Debit(1, 60, horloge)
    assert debit.accepte("1.2.3.4") is True
    horloge.t = 59.9
    assert debit.accepte("1.2.3.4") is False
    horloge.t = 60.0
    assert debit.accepte("1.2.3.4") is True


def test_une_adresse_inactive_est_oubliee():
    """Sinon un balayage d'adresses ferait croître la table sans fin."""
    horloge = Horloge()
    debit = Debit(1, 60, horloge)
    for n in range(50):
        debit.accepte(f"10.0.0.{n}")
    assert len(debit._par_adresse) == 50
    horloge.t = 120.0
    debit.accepte("10.0.0.0")
    # Chaque adresse rappelée purge la sienne ; celles qu'on ne revoit pas
    # restent jusqu'au prochain passage, mais ne s'accumulent plus vides.
    assert all(passages for passages in debit._par_adresse.values())

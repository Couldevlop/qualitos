"""Le jeton, et le temps qu'il mesure.

Le piège à robots attrape l'automate qui remplit tout ce qu'il trouve ; il ne
voit pas celui qui a été réglé sur ce formulaire-ci, champ par champ. Le temps,
lui, ne se simule pas sans perdre l'intérêt économique de l'opération : c'est ce
que ces bancs vérifient.
"""

import pytest

from app.jeton import DELAI_MINIMAL_S, PEREMPTION_S, JetonRefuse, Jetons


class Horloge:
    def __init__(self, depart=1_000_000.0):
        self.instant = depart

    def __call__(self):
        return self.instant

    def avance(self, secondes):
        self.instant += secondes


@pytest.fixture
def horloge():
    return Horloge()


@pytest.fixture
def jetons(horloge):
    return Jetons(horloge=horloge)


def test_un_jeton_emis_puis_rendu_apres_un_delai_humain_passe(jetons, horloge):
    jeton = jetons.emets()
    horloge.avance(DELAI_MINIMAL_S + 0.1)
    jetons.verifie(jeton)  # ne lève pas


def test_un_formulaire_rendu_instantanement_est_refuse(jetons):
    # Personne ne lit six champs et n'écrit sa demande en zéro seconde.
    jeton = jetons.emets()
    with pytest.raises(JetonRefuse) as refus:
        jetons.verifie(jeton)
    assert "trop vite" in refus.value.raison


def test_un_jeton_de_la_veille_est_refuse(jetons, horloge):
    jeton = jetons.emets()
    horloge.avance(PEREMPTION_S + 1)
    with pytest.raises(JetonRefuse) as refus:
        jetons.verifie(jeton)
    assert "périmé" in refus.value.raison


@pytest.mark.parametrize("valeur", [None, "", "sanspoint", "1000.", ".signature", "a.b.c"])
def test_un_jeton_malforme_est_refuse(jetons, valeur):
    with pytest.raises(JetonRefuse):
        jetons.verifie(valeur)


def test_un_instant_rejoue_avec_une_autre_valeur_ne_se_verifie_pas(jetons, horloge):
    """Le cœur de l'affaire : l'horodatage ne se réécrit pas.

    Sans signature, il suffirait d'annoncer une minute d'ancienneté pour
    traverser le délai minimal. On le tente ici, en gardant la signature d'un
    jeton authentique et en reculant seulement l'instant.
    """
    jeton = jetons.emets()
    _, signature = jeton.split(".")
    vieilli = f"{int((horloge.instant - 3600) * 1000)}.{signature}"
    with pytest.raises(JetonRefuse) as refus:
        jetons.verifie(vieilli)
    assert "non reconnu" in refus.value.raison


def test_deux_processus_ne_partagent_pas_leur_secret(horloge):
    """Le secret ne quitte pas la mémoire du processus qui l'a tiré.

    C'est ce qui dispense d'un secret à déposer, à faire tourner et à protéger.
    Le prix, assumé : un redémarrage périme les jetons déjà distribués, et c'est
    pourquoi le service suppose un seul exemplaire — comme le compteur de débit.
    """
    un, autre = Jetons(horloge=horloge), Jetons(horloge=horloge)
    jeton = un.emets()
    horloge.avance(DELAI_MINIMAL_S + 1)
    un.verifie(jeton)
    with pytest.raises(JetonRefuse):
        autre.verifie(jeton)


def test_le_jeton_ne_porte_qu_un_seul_point(jetons):
    """Le défaut qui a fait rejeter TOUS les jetons à la première exécution.

    L'instant était écrit en secondes décimales — « 1000000.000 » — et le point
    sépare les deux parties du jeton : le découpage rendait trois morceaux au
    lieu de deux, et rien ne passait. L'instant est depuis porté en
    millisecondes entières.
    """
    assert jetons.emets().count(".") == 1

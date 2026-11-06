"""Ce qu'une demande accepte, et ce qu'elle refuse."""

import pytest

from app.domaine import Demande, DemandeInvalide


def minimale(**surcharges):
    champs = {"nom": "Claire Vidal", "courriel": "claire@usine.fr"}
    champs.update(surcharges)
    return champs


def test_une_demande_minimale_passe():
    demande = Demande.depuis(minimale())
    assert demande.nom == "Claire Vidal"
    assert demande.courriel == "claire@usine.fr"
    assert demande.organisation == ""


def test_les_espaces_autour_sont_retires():
    demande = Demande.depuis(minimale(nom="  Claire Vidal  "))
    assert demande.nom == "Claire Vidal"


@pytest.mark.parametrize("manquant", ["nom", "courriel"])
def test_les_deux_champs_indispensables(manquant):
    champs = minimale()
    champs[manquant] = "   "
    with pytest.raises(DemandeInvalide) as refus:
        Demande.depuis(champs)
    assert refus.value.champ == manquant


@pytest.mark.parametrize(
    "adresse",
    ["claire", "claire@", "@usine.fr", "claire@usine", "claire usine@x.fr", "a@b@c.fr"],
)
def test_adresses_refusees(adresse):
    with pytest.raises(DemandeInvalide) as refus:
        Demande.depuis(minimale(courriel=adresse))
    assert refus.value.champ == "courriel"


@pytest.mark.parametrize(
    "adresse",
    ["claire@usine.fr", "c.vidal+devis@groupe.co.uk", "c_v@sous.domaine.example"],
)
def test_adresses_acceptees(adresse):
    assert Demande.depuis(minimale(courriel=adresse)).courriel == adresse


def test_un_retour_a_la_ligne_dans_un_champ_est_refuse():
    """L'injection d'en-tête : « \\nBcc: » ajouterait un destinataire."""
    with pytest.raises(DemandeInvalide) as refus:
        Demande.depuis(minimale(nom="Claire\nBcc: ailleurs@exemple.fr"))
    assert refus.value.champ == "nom"


def test_un_champ_trop_long_est_refuse():
    with pytest.raises(DemandeInvalide) as refus:
        Demande.depuis(minimale(besoin="x" * 4001))
    assert refus.value.champ == "besoin"


def test_un_besoin_a_la_borne_passe():
    assert len(Demande.depuis(minimale(besoin="x" * 4000)).besoin) == 4000


def test_un_champ_absent_vaut_vide():
    demande = Demande.depuis({"nom": "Claire", "courriel": "c@u.fr"})
    assert demande.telephone == "" and demande.secteur == ""


def test_une_valeur_nulle_vaut_vide():
    demande = Demande.depuis({"nom": "Claire", "courriel": "c@u.fr", "fonction": None})
    assert demande.fonction == ""


def test_le_sujet_porte_l_organisation_quand_elle_existe():
    demande = Demande.depuis(minimale(organisation="Fonderie du Nord"))
    assert demande.sujet() == "Demande de démonstration QualitOS — Fonderie du Nord"


def test_le_sujet_retombe_sur_le_nom():
    assert Demande.depuis(minimale()).sujet().endswith("— Claire Vidal")


def test_le_corps_omet_les_champs_vides_et_garde_les_autres():
    corps = Demande.depuis(minimale(fonction="Directrice qualité", besoin="Viser l'ISO 9001.")).corps()
    assert "Fonction : Directrice qualité" in corps
    assert "Téléphone" not in corps
    assert "Viser l'ISO 9001." in corps
    assert "qualitos.fr" in corps

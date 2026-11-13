"""Ce qui ne peut pas être la saisie d'un prospect, et ce dont on se méfie.

Les règles de `soupcon.py`, jugées sur le cas réel qui les a motivées : une
demande de démonstration reçue le 12 novembre 2026, portant un lien vers un
domaine sans rapport, écrit « hhtps:// » — avec une faute, pour traverser les
filtres tout en restant cliquable.
"""

import pytest

from app.soupcon import SaisieIrrecevable, compte_liens, examine


def saisie(**over):
    base = {
        "nom": "Claire Vidal",
        "fonction": "Directrice qualité",
        "organisation": "Fonderie du Nord",
        "courriel": "claire@fonderie.fr",
        "telephone": "0601020304",
        "secteur": "industrie",
        "besoin": "Nous visons l'ISO 9001 en mars.",
    }
    base.update(over)
    return base


# --- une demande ordinaire ne déclenche rien --------------------------------

def test_une_demande_ordinaire_ne_souleve_aucun_soupcon():
    assert examine(saisie()) == ()


def test_une_demande_sans_texte_libre_passe():
    assert examine(saisie(besoin="")) == ()


# --- les liens, écrits comme ils le sont vraiment ---------------------------

@pytest.mark.parametrize("texte", [
    "Voir https://exemple.fr/page",
    # Le cas réel : un schéma mal orthographié. Un filtre qui n'attrape que
    # « https:// » n'attrape que les liens honnêtes.
    "Voir hhtps://goparatge.online/fr/sw/qualitos",
    "Voir hxxp://exemple.fr",
    "Notre site www.exemple.fr vous intéressera",
    "Tout est sur exemple.fr/offre",
])
def test_les_formes_de_lien_sont_reconnues(texte):
    assert compte_liens(texte) >= 1


def test_un_domaine_cite_sans_barre_oblique_n_est_pas_un_lien():
    # « Nous sommes chez fonderie.fr » n'est pas un lien, et le dire en serait
    # un faux positif de plus — ici sans conséquence, mais le filtre doit rester
    # prévisible pour qu'on puisse le relire.
    assert compte_liens("Notre messagerie est chez fonderie.fr") == 0


# --- ce qui est MARQUÉ sans être refusé -------------------------------------

def test_un_lien_dans_le_texte_libre_est_marque_mais_pas_refuse():
    # Un prospect peut citer son propre site : le refuser écarterait un client,
    # et personne ne l'apprendrait jamais.
    soupcons = examine(saisie(besoin="Nous visons l'ISO 9001, voir https://fonderie.fr/qualite"))
    assert soupcons == ("le message contient un lien",)


def test_deux_liens_sont_marques_en_le_disant():
    soupcons = examine(saisie(
        besoin="Notre contexte est decrit sur https://a.fr/un et sur https://b.fr/deux, merci."))
    assert soupcons == ("le message contient 2 liens",)


# --- ce qui est REFUSÉ ------------------------------------------------------

@pytest.mark.parametrize("champ", ["nom", "fonction", "organisation", "telephone", "secteur"])
def test_un_lien_dans_un_champ_d_identite_est_refuse(champ):
    # Un nom, une fonction, un secteur ou un numéro de téléphone qui contient un
    # lien ne peut pas être la saisie d'une personne qui veut qu'on la rappelle.
    with pytest.raises(SaisieIrrecevable) as refus:
        examine(saisie(**{champ: "https://exemple.fr/promo"}))
    assert refus.value.champ == champ


def test_trois_liens_sont_refuses():
    with pytest.raises(SaisieIrrecevable) as refus:
        examine(saisie(besoin="https://a.fr/1 https://b.fr/2 https://c.fr/3 et encore du texte"))
    assert refus.value.champ == "besoin"


def test_un_texte_qui_n_est_qu_un_lien_est_refuse():
    # Celui-ci ne demande rien : c'est le cas du message reçu.
    with pytest.raises(SaisieIrrecevable):
        examine(saisie(besoin="hhtps://goparatge.online/fr/sw/qualitos"))


@pytest.mark.parametrize("texte", [
    "<a href='https://exemple.fr'>cliquez</a>",
    "[url=https://exemple.fr]cliquez[/url]",
    "<script>alert(1)</script>",
    "<b>offre</b> exceptionnelle sur nos services de conseil qualite",
])
def test_le_balisage_est_refuse(texte):
    # Un prospect écrit en français, pas en HTML ni en BBCode.
    with pytest.raises(SaisieIrrecevable):
        examine(saisie(besoin=texte))


def test_le_balisage_est_refuse_dans_n_importe_quel_champ():
    with pytest.raises(SaisieIrrecevable) as refus:
        examine(saisie(nom="<b>Claire</b>"))
    assert refus.value.champ == "nom"


@pytest.mark.parametrize("texte", [
    # Une comparaison écrite en toutes lettres : une seule lettre entre les
    # chevrons, aucun attribut, rien de fermé. L'ancien motif y voyait « <b> »
    # et refusait la demande.
    "Nos lots sont conformes si a<b>c sur toute la serie.",
    "Le delai tient si j<n>0 dans le plan de surveillance.",
])
def test_une_comparaison_n_est_pas_du_balisage(texte):
    """Ce contrôle REFUSE : un faux positif écarte un client en silence."""
    assert examine(saisie(besoin=texte)) == ()


def test_une_apostrophe_ou_un_chevron_de_citation_ne_sont_pas_du_balisage():
    # « < 30 jours » et « l'audit » doivent passer : un filtre qui refuse le
    # français ordinaire se fait désactiver au bout d'une semaine.
    assert examine(saisie(besoin="Nous voulons clore nos ecarts en < 30 jours, c'est l'enjeu.")) == ()

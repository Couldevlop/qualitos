"""Ce qui distingue une demande de démonstration d'un envoi de publicité.

Un formulaire public reçoit ce que n'importe qui veut y écrire : c'est sa
raison d'être, et aucune règle ne rendra ce fait faux. Ce fichier ne cherche
donc pas à deviner les intentions, mais à répondre à deux questions bien plus
étroites et vérifiables :

1. **Cette saisie pourrait-elle venir d'un prospect ?** Un lien dans le champ
   « Nom », une balise HTML dans « Organisation » : aucune demande légitime ne
   contient cela. On refuse, et l'on sait dire lequel des champs a motivé le
   refus.
2. **Y a-t-il de quoi se méfier avant de cliquer ?** Un lien dans le texte
   libre peut parfaitement être le site du prospect. On ne refuse pas — on
   **marque** le message, pour que celui qui l'ouvre sache avant d'agir.

L'arbitrage est volontairement asymétrique. Sur un formulaire de prospection,
un faux positif écarte un client et personne ne l'apprend jamais ; un faux
négatif arrive dans une boîte où un humain le lit. Le doute ne refuse pas : il
avertit.

Aucune dépendance au web, au courriel ni au réseau : ce sont des règles sur du
texte, et elles se testent comme telles.
"""

from __future__ import annotations

import re

# Ce qui ressemble à un lien, ÉCRIT LARGE ET À DESSEIN.
#
# Le message qui a motivé ce fichier portait « hhtps://… » — un schéma mal
# orthographié, qui traverse sans encombre tout motif exigeant `https?://`. La
# faute n'est pas une maladresse : elle sert à passer les filtres tout en
# restant cliquable pour un lecteur humain, ou copiable à la main. Un filtre qui
# n'attrape que les liens correctement écrits n'attrape donc que les liens
# honnêtes.
#
# Trois formes, dans cet ordre :
#   - n'importe quel schéma suivi de « :// » (couvre hhtps, hxxp, htp…) ;
#   - « www. » en début de mot ;
#   - un « domaine.extension/ » sans schéma du tout.
# La troisième reconnaît aussi « qualitos.fr/page » dans une phrase ordinaire :
# c'est assumé, parce que cette forme-là ne sert qu'à MARQUER, jamais à refuser
# un texte libre.
#
# Chaque alternative reconnait l'AMORCE d'un lien ; le `\S*` final avale ce qui
# suit, pour que le lien compte UNE fois. Sans lui, « https://fonderie.fr/page »
# declenchait deux alternatives a la fois (le schema, puis « fonderie.fr/ ») et
# un seul lien en valait deux : le seuil de refus tombait a un lien et demi, et
# un texte reduit a un lien laissait derriere lui assez de residu pour passer
# pour de la prose.
#
# Les gardes arriere interdisent de couper au milieu d'un mot ou d'une adresse
# de courriel : « claire@fonderie.fr » n'est pas un lien.
_LIEN = re.compile(
    r"(?:"
    r"[a-z][a-z0-9+.\-]{1,15}://"
    r"|(?<![\w.@\-])www\."
    r"|(?<![\w.@\-])[a-z0-9\-]{2,}\.[a-z]{2,24}/"
    r")\S*",
    re.IGNORECASE,
)

# Balisage. Un prospect écrit en français, pas en HTML ni en BBCode. Ces formes
# ne viennent que d'un automate qui publie le même texte sur mille formulaires
# sans savoir lequel rend du HTML.
#
# Le motif a été resserré : écrit « </?[a-z][a-z0-9]*[\s/>] », il reconnaissait
# « <b> » dans « si a<b>c », c'est-à-dire une comparaison écrite en toutes
# lettres. Or ce contrôle REFUSE, et le refus d'un formulaire de prospection
# écarte un client sans que personne ne l'apprenne jamais. Trois formes, aucune
# qu'une phrase française produise par accident :
#   - une balise FERMANTE, « </a> » : personne ne ferme une comparaison ;
#   - une balise ouvrante d'au moins DEUX lettres, « <br> », « <script> » ;
#   - une balise ouvrante portant un ATTRIBUT, « <a href=… », même à une lettre.
# « a<b>c » n'entre dans aucune : une seule lettre, aucun attribut, rien de
# fermé. « <b>offre</b> » entre par la première, et reste refusé.
_BALISE = re.compile(
    r"</[a-z][a-z0-9]{0,14}\s*>"
    r"|<[a-z][a-z0-9]{1,14}(?:\s[^<>]{0,200})?/?>"
    r"|<[a-z][a-z0-9]{0,14}\s+[a-z-]+\s*="
    r"|\[/?(?:url|link|a|img|b)[\]=]",
    re.IGNORECASE,
)

# Les champs où un lien n'a AUCUN usage légitime. On n'y refuse pas par excès de
# prudence : un nom, une fonction, un secteur ou un numéro de téléphone qui
# contient un lien ne peut pas être la saisie d'une personne qui veut qu'on la
# rappelle.
CHAMPS_SANS_LIEN = ("nom", "fonction", "organisation", "telephone", "secteur")

# Au-delà, le texte n'est plus une demande accompagnée d'une référence : c'est
# une liste de liens. Deux laissent la place à qui cite son site et une page.
LIENS_MAX = 3


class SaisieIrrecevable(ValueError):
    """La saisie ne peut pas être celle d'un prospect, et on sait dire où."""

    def __init__(self, champ: str, raison: str) -> None:
        super().__init__(f"{champ} : {raison}")
        self.champ = champ
        self.raison = raison


def compte_liens(texte: str) -> int:
    """Combien de liens, ou de choses qui en ont l'air, dans ce texte."""
    return len(_LIEN.findall(texte or ""))


def examine(valeurs: dict[str, str]) -> tuple[str, ...]:
    """Juge une saisie déjà nettoyée.

    Lève {@link SaisieIrrecevable} sur ce qui ne peut pas être une demande ;
    rend sinon la liste — souvent vide — de ce dont il faut se méfier. Les
    motifs sont rédigés pour être lus par la personne qui ouvrira le message,
    pas pour être analysés par une machine.
    """
    for champ, valeur in valeurs.items():
        if _BALISE.search(valeur or ""):
            raise SaisieIrrecevable(champ, "balisage non admis")

    for champ in CHAMPS_SANS_LIEN:
        if compte_liens(valeurs.get(champ, "")):
            raise SaisieIrrecevable(champ, "ce champ n'accepte pas de lien")

    besoin = valeurs.get("besoin", "") or ""
    liens = compte_liens(besoin)
    if liens >= LIENS_MAX:
        raise SaisieIrrecevable("besoin", "trop de liens pour une demande")

    # Un texte qui n'est QU'un lien ne demande rien. Le seuil est franc : hors
    # du lien, il reste moins de dix caractères de prose.
    if liens and len(_LIEN.sub(" ", besoin).strip()) < 10:
        raise SaisieIrrecevable("besoin", "aucune demande, seulement un lien")

    soupcons: list[str] = []
    if liens == 1:
        soupcons.append("le message contient un lien")
    elif liens:
        soupcons.append(f"le message contient {liens} liens")
    return tuple(soupcons)

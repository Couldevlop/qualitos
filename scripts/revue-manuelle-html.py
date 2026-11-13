# -*- coding: utf-8 -*-
"""Le guide de revue manuelle, en page consultable.

`docs/REVUE-MANUELLE.md` est la source : on l'ecrit, on le relit, on le
versionne en texte. Mais on ne s'en SERT pas en le lisant du haut : on s'en sert
pendant une revue, une fenetre a cote du code, pour retrouver une grille, cocher
ce qui est fait et chercher un mot. Un fichier de six cents lignes dans un
visualiseur Markdown ne donne ni la recherche, ni le repli, ni l'avancement.

Ce script produit donc `docs/revue-manuelle.html` DEPUIS le Markdown, et jamais
l'inverse : une page unique, sans dependance, sans reseau (aucune police
distante, aucun script externe), qui ajoute les trois gestes qui manquent --
chercher, replier, cocher. Le texte reste a un seul endroit ; la page se
regenere.

    python scripts/revue-manuelle-html.py

La page n'est PAS versionnee -- `.gitignore` ecarte `docs/**/*.html`, et la
regle est bonne : ce qui se versionne s'ecrit en Markdown, le reste se
reconstruit. On l'engendre quand on en a besoin, elle ne peut donc jamais
dire autre chose que sa source. La chaine d'integration la reconstruit a
chaque passage : si une tournure du Markdown casse la conversion, c'est la
qu'on l'apprend, pas devant la personne qui relit.

L'avancement des cases vit dans le navigateur de la personne qui relit
(localStorage) : une revue est son brouillon, elle n'a rien a faire dans le
depot ni sur un serveur.
"""

from __future__ import annotations

import argparse
import html as html_mod
import io
import os
import re
import sys
import unicodedata

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(RACINE, 'docs', 'REVUE-MANUELLE.md')
CIBLE = os.path.join(RACINE, 'docs', 'revue-manuelle.html')


# --------------------------------------------------------------------------
# Le niveau « ligne » : gras, code, liens.
# --------------------------------------------------------------------------

def _ligne(texte):
    """Convertit le balisage interne d'une ligne.

    Les fragments de code sont mis de cote AVANT le gras et l'italique : un
    `**` dans un extrait de commande est du code, pas de l'emphase.
    """
    texte = html_mod.escape(texte, quote=False)
    reserve = []

    def _garde(m):
        reserve.append(m.group(1))
        return '\x00%d\x00' % (len(reserve) - 1)

    texte = re.sub(r'`([^`]+)`', _garde, texte)
    texte = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', texte)
    texte = re.sub(r'(?<![*\w])\*([^*\n]+)\*(?!\*)', r'<em>\1</em>', texte)
    texte = re.sub(
        r'\[([^\]]+)\]\(([^)\s]+)\)',
        lambda m: '<a href="%s">%s</a>' % (html_mod.escape(m.group(2), quote=True), m.group(1)),
        texte)
    for i, brut in enumerate(reserve):
        texte = texte.replace('\x00%d\x00' % i, '<code>%s</code>' % brut)
    return texte


def _cellules(ligne):
    morceau = ligne.strip()
    if morceau.startswith('|'):
        morceau = morceau[1:]
    if morceau.endswith('|'):
        morceau = morceau[:-1]
    return [c.strip() for c in morceau.split('|')]


def ancre(titre):
    """Un identifiant stable, lisible, derive du titre."""
    plat = unicodedata.normalize('NFKD', titre)
    plat = ''.join(c for c in plat if not unicodedata.combining(c))
    plat = re.sub(r'[^A-Za-z0-9]+', '-', plat).strip('-').lower()
    if not plat:
        return 'section'
    # Les titres s'appellent « 5.5 Le catalogue » : l'identifiant commencerait
    # par un chiffre, et `querySelector('#5-5-...')` leve une erreur de selecteur.
    return plat if plat[0].isalpha() else 's' + plat


# --------------------------------------------------------------------------
# Le niveau « bloc ».
# --------------------------------------------------------------------------

def _ouvre_un_bloc(ligne):
    """Vrai si cette ligne commence autre chose qu'un paragraphe en cours."""
    return bool(
        ligne.startswith('>') or ligne.startswith('|') or ligne.startswith('```')
        or ligne.startswith('#') or re.match(r'^-{3,}$', ligne)
        or re.match(r'^[-*]\s+', ligne) or re.match(r'^\d+\.\s+', ligne))


def _blocs(lignes):
    """Convertit une suite de lignes Markdown en HTML."""
    sortie = []
    i = 0
    n = len(lignes)
    while i < n:
        nu = lignes[i].strip()

        if not nu:
            i += 1
            continue

        # Bloc de code.
        if nu.startswith('```'):
            langue = nu[3:].strip()
            i += 1
            corps = []
            while i < n and not lignes[i].strip().startswith('```'):
                corps.append(lignes[i])
                i += 1
            i += 1
            classe = ' class="langue-%s"' % html_mod.escape(langue, quote=True) if langue else ''
            sortie.append('<pre%s><code>%s</code></pre>'
                          % (classe, html_mod.escape('\n'.join(corps), quote=False)))
            continue

        # Trait de separation : les sections sont deja des cartes, il ne sert plus.
        if re.match(r'^-{3,}$', nu):
            i += 1
            continue

        # Titres internes (h3, h4) ; les h1/h2 sont traites par le decoupage.
        titre = re.match(r'^(#{3,4})\s+(.*)$', nu)
        if titre:
            niveau = len(titre.group(1))
            texte = titre.group(2).strip()
            cle = ancre(texte)
            # Un titre interne est un POINT de la revue : il se coche. C'est vrai
            # du h4 comme du h3 -- les familles de defaut du §5.5 sont des h4, et
            # ce sont justement les points qu'on veut pointer un par un.
            balise = 'h3' if niveau == 3 else 'h4'
            classe = 'sous-titre' if niveau == 3 else 'sous-sous-titre'
            sortie.append(
                '<%s id="%s" class="%s">'
                '<label class="coche"><input type="checkbox" data-point="%s">'
                '<span>%s</span></label>'
                '<a class="lien-ancre" href="#%s" aria-label="Lien vers ce point">#</a>'
                '</%s>' % (balise, cle, classe, cle, _ligne(texte), cle, balise))
            i += 1
            continue

        # Citation : dans ce guide, c'est toujours une mise en garde.
        if nu.startswith('>'):
            dedans = []
            while i < n and lignes[i].strip().startswith('>'):
                dedans.append(re.sub(r'^\s*>\s?', '', lignes[i]))
                i += 1
            sortie.append('<blockquote>%s</blockquote>' % _blocs(dedans))
            continue

        # Tableau.
        if (nu.startswith('|') and i + 1 < n
                and re.match(r'^\|[\s:|-]+\|$', lignes[i + 1].strip())):
            entetes = _cellules(lignes[i])
            i += 2
            rangs = []
            while i < n and lignes[i].strip().startswith('|'):
                rangs.append(_cellules(lignes[i]))
                i += 1
            morceaux = ['<div class="cadre-tableau"><table><thead><tr>']
            morceaux += ['<th>%s</th>' % _ligne(c) for c in entetes]
            morceaux.append('</tr></thead><tbody>')
            for rang in rangs:
                morceaux.append('<tr>')
                morceaux += ['<td>%s</td>' % _ligne(c) for c in rang]
                morceaux.append('</tr>')
            morceaux.append('</tbody></table></div>')
            sortie.append(''.join(morceaux))
            continue

        # Listes, a puces ou numerotees. Un item de ce guide tient souvent sur
        # plusieurs lignes : les lignes de suite rejoignent leur item, sinon un
        # gras a cheval sur un retour a la ligne se retrouve coupe en deux.
        marqueur = None
        if re.match(r'^[-*]\s+', nu):
            marqueur, balise = r'^[-*]\s+', 'ul'
        elif re.match(r'^\d+\.\s+', nu):
            marqueur, balise = r'^\d+\.\s+', 'ol'
        if marqueur:
            items = []
            while i < n:
                courant = lignes[i].strip()
                if re.match(marqueur, courant):
                    items.append([re.sub(marqueur, '', courant)])
                elif items and courant and not _ouvre_un_bloc(courant):
                    items[-1].append(courant)
                else:
                    break
                i += 1
            sortie.append('<%s>%s</%s>' % (balise, ''.join(
                '<li>%s</li>' % _ligne(' '.join(item)) for item in items), balise))
            continue

        # Paragraphe : jusqu'a la prochaine ligne vide ou au prochain bloc.
        para = []
        while i < n:
            courant = lignes[i].strip()
            if not courant or _ouvre_un_bloc(courant):
                break
            para.append(courant)
            i += 1
        if para:
            sortie.append('<p>%s</p>' % _ligne(' '.join(para)))
    return ''.join(sortie)


# --------------------------------------------------------------------------
# Le decoupage du document en sections de premier niveau.
# --------------------------------------------------------------------------

def decouper(texte):
    """Rend (titre, chapeau, sections) ; une section = (titre, html, points).

    Le chapeau est tout ce qui precede la premiere section : dans ce guide,
    l'avertissement qui dit ce que le document est et ce qu'il ne remplace pas.
    """
    lignes = texte.replace('\r\n', '\n').split('\n')
    titre = 'Revue manuelle'
    debut = 0
    for i, ligne in enumerate(lignes):
        if ligne.startswith('# '):
            titre = ligne[2:].strip()
            debut = i + 1
            break

    chapeau_lignes = []
    sections = []
    courante = None
    dans_code = False
    for ligne in lignes[debut:]:
        if ligne.strip().startswith('```'):
            dans_code = not dans_code
        if not dans_code and ligne.startswith('## '):
            if courante is not None:
                sections.append(courante)
            courante = [ligne[3:].strip(), []]
            continue
        if courante is None:
            chapeau_lignes.append(ligne)
        else:
            courante[1].append(ligne)
    if courante is not None:
        sections.append(courante)

    rendues = []
    for nom, corps in sections:
        corps_html = _blocs(corps)
        points = re.findall(r'data-point="([^"]+)"', corps_html)
        rendues.append((nom, corps_html, points))
    return titre, _blocs(chapeau_lignes), rendues


CSS = """
:root {
  --fond: #f6f7f9; --carte: #ffffff; --encre: #1b1f24; --encre-douce: #5b6472;
  --trait: #e3e6ec; --accent: #1f6feb; --accent-doux: #e8f0fe;
  --code-fond: #f2f4f7; --garde-fond: #fff9ec; --garde-trait: #e0b65a;
  --fait: #1a7f4b; --fait-fond: #e9f7ef;
  --rayon: 12px; --ombre: 0 1px 2px rgba(16,24,40,.05), 0 8px 24px rgba(16,24,40,.05);
}
@media (prefers-color-scheme: dark) {
  :root {
    --fond: #0f1216; --carte: #171b21; --encre: #e6e9ee; --encre-douce: #9aa4b2;
    --trait: #262b33; --accent: #6aa2ff; --accent-doux: #1b2740;
    --code-fond: #11151a; --garde-fond: #241f14; --garde-trait: #8a6a22;
    --fait: #5fd39b; --fait-fond: #13251c;
    --ombre: none;
  }
}
* { box-sizing: border-box; }
html { scroll-behavior: smooth; scroll-padding-top: 5.5rem; }
body {
  margin: 0; background: var(--fond); color: var(--encre);
  font: 16px/1.65 "Inter", "Segoe UI", system-ui, -apple-system, sans-serif;
  -webkit-font-smoothing: antialiased;
}
a { color: var(--accent); text-underline-offset: .2em; }
code, pre {
  font-family: "IBM Plex Mono", ui-monospace, "Cascadia Mono", Consolas, monospace;
}
code { background: var(--code-fond); padding: .1em .35em; border-radius: 5px; font-size: .88em; }
pre {
  background: var(--code-fond); border: 1px solid var(--trait); border-radius: var(--rayon);
  padding: 1rem 1.1rem; overflow-x: auto; font-size: .86rem; line-height: 1.6;
}
pre code { background: none; padding: 0; font-size: 1em; }

/* ---- Barre de tete ---- */
.tete {
  position: sticky; top: 0; z-index: 20; background: var(--carte);
  border-bottom: 1px solid var(--trait);
}
.tete__dedans {
  max-width: 1240px; margin: 0 auto; padding: .7rem 1.5rem;
  display: flex; gap: 1rem; align-items: center; flex-wrap: wrap;
}
.tete__titre { font-size: 1rem; font-weight: 650; letter-spacing: -.01em; margin: 0; }
.tete__titre small { display: block; font-weight: 400; color: var(--encre-douce); font-size: .76rem; }
.recherche { flex: 1 1 18rem; position: relative; min-width: 14rem; }
.recherche input {
  width: 100%; padding: .55rem .8rem .55rem 2.1rem; font: inherit; font-size: .92rem;
  color: var(--encre); background: var(--fond);
  border: 1px solid var(--trait); border-radius: 9px;
}
.recherche input:focus { outline: 2px solid var(--accent); outline-offset: 1px; border-color: var(--accent); }
.recherche::before {
  content: "⌕"; position: absolute; left: .7rem; top: 50%; transform: translateY(-52%);
  color: var(--encre-douce); font-size: 1.1rem;
}
.bouton {
  font: inherit; font-size: .84rem; padding: .45rem .8rem; cursor: pointer;
  color: var(--encre); background: var(--carte);
  border: 1px solid var(--trait); border-radius: 9px;
}
.bouton:hover { border-color: var(--accent); color: var(--accent); }
.bouton:focus-visible { outline: 2px solid var(--accent); outline-offset: 1px; }
.jauge { display: flex; align-items: center; gap: .55rem; font-size: .82rem; color: var(--encre-douce); }
.jauge__piste { width: 7rem; height: 6px; border-radius: 99px; background: var(--trait); overflow: hidden; }
.jauge__barre { height: 100%; width: 0; background: var(--fait); transition: width .25s ease; }

/* ---- Mise en page ---- */
.page { max-width: 1240px; margin: 0 auto; padding: 1.75rem 1.5rem 6rem; display: flex; gap: 2rem; }
.sommaire { flex: 0 0 16.5rem; align-self: flex-start; position: sticky; top: 5.2rem; }
.sommaire h2 {
  font-size: .72rem; text-transform: uppercase; letter-spacing: .09em;
  color: var(--encre-douce); margin: 0 0 .6rem .55rem;
}
.sommaire ol { list-style: none; margin: 0; padding: 0; max-height: calc(100vh - 9rem); overflow-y: auto; }
.sommaire a {
  display: block; padding: .32rem .6rem; border-radius: 7px; font-size: .87rem;
  color: var(--encre-douce); text-decoration: none; border-left: 2px solid transparent;
}
.sommaire a:hover { background: var(--accent-doux); color: var(--accent); }
.sommaire a[aria-current="true"] {
  color: var(--accent); background: var(--accent-doux); border-left-color: var(--accent); font-weight: 560;
}
.corps { flex: 1 1 auto; min-width: 0; }

.chapeau {
  background: var(--carte); border: 1px solid var(--trait); border-radius: var(--rayon);
  padding: .4rem 1.5rem; margin-bottom: 1.5rem; box-shadow: var(--ombre);
}
.chapeau blockquote { border-left-color: var(--accent); background: transparent; }

/* ---- Sections ---- */
.section {
  background: var(--carte); border: 1px solid var(--trait); border-radius: var(--rayon);
  margin-bottom: 1rem; box-shadow: var(--ombre); overflow: hidden;
}
.section__tete {
  display: flex; align-items: center; gap: .75rem; width: 100%;
  padding: 1rem 1.3rem; margin: 0; cursor: pointer; text-align: left;
  font: inherit; font-size: 1.12rem; font-weight: 640; letter-spacing: -.012em;
  color: var(--encre); background: none; border: 0;
}
.section__tete:hover { color: var(--accent); }
.section__tete:focus-visible { outline: 2px solid var(--accent); outline-offset: -3px; }
.section__fleche { flex: 0 0 auto; color: var(--encre-douce); transition: transform .18s ease; font-size: .8rem; }
.section[open] .section__fleche, .section.ouverte .section__fleche { transform: rotate(90deg); }
.section__compte {
  margin-left: auto; flex: 0 0 auto; font-size: .74rem; font-weight: 500;
  color: var(--encre-douce); background: var(--fond);
  border: 1px solid var(--trait); border-radius: 99px; padding: .12rem .5rem;
}
.section__compte--plein { color: var(--fait); background: var(--fait-fond); border-color: transparent; }
.section__corps { padding: 0 1.3rem 1.2rem; border-top: 1px solid var(--trait); }
.section.repliee .section__corps { display: none; }
.section.masquee { display: none; }

h3.sous-titre, h4.sous-sous-titre {
  display: flex; align-items: flex-start; gap: .5rem;
  font-weight: 620; scroll-margin-top: 5.5rem;
}
h3.sous-titre { font-size: 1rem; margin: 1.6rem 0 .6rem; }
h4.sous-sous-titre { font-size: .93rem; margin: 1.3rem 0 .45rem; color: var(--encre); }
.coche { display: flex; align-items: flex-start; gap: .55rem; cursor: pointer; }
.coche input { margin: .28rem 0 0; width: 1rem; height: 1rem; accent-color: var(--fait); flex: 0 0 auto; }
.coche input:checked + span { color: var(--encre-douce); text-decoration: line-through; text-decoration-thickness: 1px; }
.lien-ancre { opacity: 0; text-decoration: none; color: var(--encre-douce); font-weight: 400; }
h3.sous-titre:hover .lien-ancre { opacity: 1; }

p { margin: .7rem 0; }
ul, ol { margin: .7rem 0; padding-left: 1.3rem; }
li { margin: .3rem 0; }
blockquote {
  margin: 1rem 0; padding: .1rem 1.1rem; background: var(--garde-fond);
  border-left: 3px solid var(--garde-trait); border-radius: 0 var(--rayon) var(--rayon) 0;
}
.cadre-tableau { overflow-x: auto; margin: 1rem 0; border: 1px solid var(--trait); border-radius: var(--rayon); }
table { width: 100%; border-collapse: collapse; font-size: .89rem; }
th, td { padding: .55rem .75rem; text-align: left; vertical-align: top; border-bottom: 1px solid var(--trait); }
th { background: var(--fond); font-weight: 620; font-size: .82rem; white-space: nowrap; }
tbody tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: var(--accent-doux); }

mark { background: #ffe9a8; color: inherit; border-radius: 3px; padding: 0 .1em; }
@media (prefers-color-scheme: dark) { mark { background: #5c4a16; } }
.vide { display: none; padding: 2rem 0; color: var(--encre-douce); text-align: center; }
.vide.visible { display: block; }

@media (max-width: 950px) {
  .sommaire { display: none; }
  .page { padding: 1.25rem .9rem 4rem; }
  .tete__dedans { padding: .6rem .9rem; }
}
@media print {
  .tete, .sommaire, .lien-ancre { display: none; }
  .section, .chapeau { box-shadow: none; break-inside: avoid; }
  .section.repliee .section__corps { display: block; }
  body { background: #fff; }
}
"""


JS = """
(function () {
  'use strict';

  var CLE = 'qualitos.revue-manuelle.v1';
  var sections = Array.prototype.slice.call(document.querySelectorAll('.section'));
  var cases = Array.prototype.slice.call(document.querySelectorAll('input[data-point]'));
  var champ = document.getElementById('recherche');
  var vide = document.getElementById('aucun-resultat');
  var barre = document.getElementById('jauge-barre');
  var compteur = document.getElementById('jauge-texte');
  var liens = Array.prototype.slice.call(document.querySelectorAll('.sommaire a'));

  /* ---- L'avancement, dans le navigateur de la personne qui relit ---- */
  function lu() {
    try { return JSON.parse(localStorage.getItem(CLE)) || {}; } catch (e) { return {}; }
  }
  function ecrire(etat) {
    try { localStorage.setItem(CLE, JSON.stringify(etat)); } catch (e) { /* mode prive */ }
  }

  function rafraichirCompteurs() {
    var faits = cases.filter(function (c) { return c.checked; }).length;
    var total = cases.length;
    if (barre) { barre.style.width = total ? (100 * faits / total) + '%' : '0'; }
    if (compteur) { compteur.textContent = faits + ' / ' + total + ' points relus'; }
    sections.forEach(function (section) {
      var miennes = Array.prototype.slice.call(section.querySelectorAll('input[data-point]'));
      var pastille = section.querySelector('.section__compte');
      if (!pastille) { return; }
      if (!miennes.length) { pastille.hidden = true; return; }
      var ok = miennes.filter(function (c) { return c.checked; }).length;
      pastille.hidden = false;
      pastille.textContent = ok + '/' + miennes.length;
      pastille.classList.toggle('section__compte--plein', ok === miennes.length);
    });
  }

  var etat = lu();
  cases.forEach(function (c) {
    c.checked = !!etat[c.getAttribute('data-point')];
    c.addEventListener('change', function () {
      var courant = lu();
      if (c.checked) { courant[c.getAttribute('data-point')] = 1; }
      else { delete courant[c.getAttribute('data-point')]; }
      ecrire(courant);
      rafraichirCompteurs();
    });
  });
  rafraichirCompteurs();

  var remise = document.getElementById('remise-a-zero');
  if (remise) {
    remise.addEventListener('click', function () {
      cases.forEach(function (c) { c.checked = false; });
      ecrire({});
      rafraichirCompteurs();
    });
  }

  /* ---- Replier, deplier ---- */
  sections.forEach(function (section) {
    var tete = section.querySelector('.section__tete');
    tete.addEventListener('click', function () {
      var replie = section.classList.toggle('repliee');
      tete.setAttribute('aria-expanded', replie ? 'false' : 'true');
    });
  });

  function toutes(replier) {
    sections.forEach(function (section) {
      section.classList.toggle('repliee', replier);
      section.querySelector('.section__tete').setAttribute('aria-expanded', replier ? 'false' : 'true');
    });
  }
  var plier = document.getElementById('tout-replier');
  if (plier) {
    plier.addEventListener('click', function () {
      var ouvert = sections.some(function (s) { return !s.classList.contains('repliee'); });
      toutes(ouvert);
      plier.textContent = ouvert ? 'Tout déplier' : 'Tout replier';
    });
  }

  /* ---- Chercher ---- */
  function sansAccent(t) {
    return t.toLowerCase().normalize('NFD').replace(/[\\u0300-\\u036f]/g, '');
  }

  function demarquer(racine) {
    Array.prototype.slice.call(racine.querySelectorAll('mark')).forEach(function (m) {
      var pere = m.parentNode;
      pere.replaceChild(document.createTextNode(m.textContent), m);
      pere.normalize();
    });
  }

  function marquer(racine, terme) {
    var plat = sansAccent(terme);
    var marcheur = document.createTreeWalker(racine, NodeFilter.SHOW_TEXT, {
      acceptNode: function (noeud) {
        if (!noeud.nodeValue.trim()) { return NodeFilter.FILTER_REJECT; }
        var pere = noeud.parentNode.nodeName;
        if (pere === 'SCRIPT' || pere === 'STYLE' || pere === 'MARK') { return NodeFilter.FILTER_REJECT; }
        return sansAccent(noeud.nodeValue).indexOf(plat) >= 0
          ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
      }
    });
    var trouves = [];
    while (marcheur.nextNode()) { trouves.push(marcheur.currentNode); }
    trouves.forEach(function (noeud) {
      var texte = noeud.nodeValue;
      var ou = sansAccent(texte).indexOf(plat);
      if (ou < 0) { return; }
      var apres = noeud.splitText(ou);
      apres.splitText(terme.length);
      var balise = document.createElement('mark');
      balise.appendChild(document.createTextNode(apres.nodeValue));
      apres.parentNode.replaceChild(balise, apres);
    });
  }

  var attente = null;
  function chercher() {
    var terme = champ.value.trim();
    var trouvees = 0;
    sections.forEach(function (section) {
      demarquer(section);
      if (!terme) {
        section.classList.remove('masquee');
        trouvees++;
        return;
      }
      var porte = sansAccent(section.textContent).indexOf(sansAccent(terme)) >= 0;
      section.classList.toggle('masquee', !porte);
      if (porte) {
        section.classList.remove('repliee');
        section.querySelector('.section__tete').setAttribute('aria-expanded', 'true');
        marquer(section.querySelector('.section__corps'), terme);
        trouvees++;
      }
    });
    if (vide) { vide.classList.toggle('visible', terme && !trouvees); }
    liens.forEach(function (lien) {
      var cible = document.getElementById(lien.getAttribute('href').slice(1));
      lien.hidden = !!terme && cible && cible.classList.contains('masquee');
    });
  }

  if (champ) {
    champ.addEventListener('input', function () {
      clearTimeout(attente);
      attente = setTimeout(chercher, 120);
    });
    champ.addEventListener('keydown', function (ev) {
      if (ev.key === 'Escape') { champ.value = ''; chercher(); }
    });
  }

  document.addEventListener('keydown', function (ev) {
    var dansUnChamp = /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.nodeName);
    if (ev.key === '/' && !dansUnChamp && champ) { ev.preventDefault(); champ.focus(); }
  });

  /* ---- Le sommaire suit la lecture ---- */
  if ('IntersectionObserver' in window) {
    var vues = new IntersectionObserver(function (entrees) {
      entrees.forEach(function (entree) {
        if (!entree.isIntersecting) { return; }
        liens.forEach(function (lien) {
          lien.setAttribute('aria-current',
            lien.getAttribute('href') === '#' + entree.target.id ? 'true' : 'false');
        });
      });
    }, { rootMargin: '-88px 0px -70% 0px' });
    sections.forEach(function (section) { vues.observe(section); });
  }

  /* ---- Un lien profond deplie sa section ---- */
  function deplierLaCible() {
    if (!location.hash) { return; }
    var cible = document.getElementById(decodeURIComponent(location.hash.slice(1)));
    if (!cible) { return; }
    var section = cible.closest('.section');
    if (section) {
      section.classList.remove('repliee', 'masquee');
      section.querySelector('.section__tete').setAttribute('aria-expanded', 'true');
    }
    cible.scrollIntoView({ block: 'start' });
  }
  window.addEventListener('hashchange', deplierLaCible);
  deplierLaCible();
})();
"""


GABARIT = """<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{titre} — QualitOS</title>
<meta name="description" content="Grille de relecture du code de QualitOS : ce qu'on regarde, dans quel ordre, et ce qui a déjà cassé ici.">
<meta name="robots" content="noindex">
<style>{css}</style>
</head>
<body>
<header class="tete">
  <div class="tete__dedans">
    <h1 class="tete__titre">{titre_court}<small>{sous_titre}</small></h1>
    <div class="recherche">
      <input id="recherche" type="search" placeholder="Chercher un mot, une classe, un défaut…    /"
             autocomplete="off" aria-label="Chercher dans le guide">
    </div>
    <div class="jauge" title="Les points cochés restent dans ce navigateur, nulle part ailleurs.">
      <div class="jauge__piste"><div class="jauge__barre" id="jauge-barre"></div></div>
      <span id="jauge-texte">0 / 0</span>
    </div>
    <button class="bouton" id="tout-replier" type="button">Tout replier</button>
    <button class="bouton" id="remise-a-zero" type="button">Décocher</button>
  </div>
</header>

<div class="page">
  <nav class="sommaire" aria-label="Sommaire">
    <h2>Sommaire</h2>
    <ol>{sommaire}</ol>
  </nav>

  <main class="corps">
    <div class="chapeau">{chapeau}</div>
    {sections}
    <p class="vide" id="aucun-resultat">Aucune section ne contient ce mot.</p>
  </main>
</div>

<script>{js}</script>
</body>
</html>
"""


def construire(markdown):
    titre, chapeau, sections = decouper(markdown)
    titre_court = titre.split('—')[0].strip()
    sous_titre = titre.split('—', 1)[1].strip() if '—' in titre else ''

    sommaire = []
    corps = []
    for nom, html_section, points in sections:
        cle = ancre(nom)
        sommaire.append('<li><a href="#%s">%s</a></li>' % (cle, _ligne(nom)))
        pastille = ('<span class="section__compte">0/%d</span>' % len(points)) if points else \
                   '<span class="section__compte" hidden></span>'
        corps.append(
            '<section class="section" id="%s">'
            '<button class="section__tete" type="button" aria-expanded="true" aria-controls="%s-corps">'
            '<span class="section__fleche" aria-hidden="true">&#9656;</span>'
            '<span>%s</span>%s</button>'
            '<div class="section__corps" id="%s-corps">%s</div>'
            '</section>' % (cle, cle, _ligne(nom), pastille, cle, html_section))

    return GABARIT.format(
        titre=html_mod.escape(titre, quote=True),
        titre_court=html_mod.escape(titre_court, quote=False),
        sous_titre=html_mod.escape(sous_titre, quote=False),
        css=CSS, js=JS,
        sommaire=''.join(sommaire),
        chapeau=chapeau,
        sections='\n    '.join(corps))


def main(argv=None):
    argparse.ArgumentParser(description=__doc__.splitlines()[0]).parse_args(argv)

    with io.open(SOURCE, encoding='utf-8') as flux:
        page = construire(flux.read())

    # Un guide sans section ou sans point a cocher serait une page vide servie
    # avec le sourire : la conversion a casse sur une tournure du Markdown, et
    # personne ne le verrait avant d'ouvrir le fichier.
    sections = page.count('<section class="section"')
    points = page.count('data-point=')
    if sections < 2 or points < 1:
        print('Conversion suspecte : %d section(s), %d point(s). Page non ecrite.'
              % (sections, points))
        return 1

    with io.open(CIBLE, 'w', encoding='utf-8', newline='\n') as flux:
        flux.write(page)
    print('docs/revue-manuelle.html ecrite (%d sections, %d points a cocher).'
          % (sections, points))
    return 0


if __name__ == '__main__':
    sys.exit(main())

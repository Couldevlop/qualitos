/* ==========================================================================
   QualitOS — site de présentation
   Trois comportements, pas un de plus : ouvrir le menu sur petit écran,
   marquer dans le menu la section qu'on est en train de lire, et faire
   partir le formulaire de contact.
   ========================================================================== */
(function () {
  'use strict';

  // ---------- menu sur petit écran ----------
  var bascule = document.querySelector('.nav__bascule');
  var nav = document.getElementById('nav');

  if (bascule && nav) {
    bascule.addEventListener('click', function () {
      var ouvert = nav.classList.toggle('nav--ouverte');
      bascule.setAttribute('aria-expanded', String(ouvert));
      bascule.textContent = ouvert ? 'Fermer' : 'Menu';
    });

    // Suivre un lien referme le menu : sinon il recouvre la section visée.
    nav.addEventListener('click', function (e) {
      if (e.target.tagName === 'A' && nav.classList.contains('nav--ouverte')) {
        nav.classList.remove('nav--ouverte');
        bascule.setAttribute('aria-expanded', 'false');
        bascule.textContent = 'Menu';
      }
    });
  }

  // ---------- le menu suit le defilement ----------
  // La section active est celle dont le HAUT est passe sous la ligne de
  // l'en-tete collante : c'est celle qu'on lit, et c'est exactement ce que
  // montre l'ecran. Le calcul est direct, sur la position reelle du moment.
  //
  // POURQUOI PAS IntersectionObserver. Il a ete essaye d'abord. Il ne parle
  // que lorsqu'un element CHANGE d'etat : apres un saut d'ancre, les sections
  // restees hors cadre n'emettent rien, l'etat memorise reste celui d'avant le
  // saut, et le menu marque la section precedente. Mesure a l'ecran : un clic
  // sur « Votre metier » laissait « IA » souligne. Un calcul direct n'a pas de
  // memoire a desynchroniser.
  //
  // Le travail est reporte a la trame d'affichage suivante : le navigateur
  // emet des dizaines d'evenements de defilement par seconde, et mesurer a
  // chacun ne changerait rien a ce que l'oeil voit.
  var liens = nav ? nav.querySelectorAll('a[href^="#"]') : [];
  var reperes = [];

  Array.prototype.forEach.call(liens, function (lien) {
    var id = lien.getAttribute('href').slice(1);
    var section = id && document.getElementById(id);
    if (section) reperes.push({ lien: lien, section: section });
  });

  if (reperes.length) {
    var entete = document.querySelector('.entete');
    var demande = 0;

    var situer = function () {
      demande = 0;
      var ligne = (entete ? entete.getBoundingClientRect().height : 0) + 16;
      var courant = null;

      reperes.forEach(function (repere) {
        var cadre = repere.section.getBoundingClientRect();
        // Le haut est passe la ligne, et le bas ne l'a pas encore franchie :
        // la section occupe le regard. La derniere qui remplit la condition
        // gagne, ce qui donne la plus avancee quand deux se chevauchent.
        if (cadre.top <= ligne && cadre.bottom > ligne) courant = repere.lien;
      });

      // Le bas de page : la derniere section ne peut plus atteindre la ligne,
      // et sans ce rattrapage le menu s'eteindrait juste quand on arrive au
      // formulaire de contact.
      if (!courant && window.innerHeight + window.scrollY >= document.body.scrollHeight - 2) {
        courant = reperes[reperes.length - 1].lien;
      }

      Array.prototype.forEach.call(liens, function (lien) {
        var actif = lien === courant;
        lien.classList.toggle('nav__lien--actif', actif);
        // `aria-current` dit aux lecteurs d'ecran ce que la couleur dit a
        // l'oeil : sans lui, l'information n'existe que pour les voyants.
        if (actif) lien.setAttribute('aria-current', 'true');
        else lien.removeAttribute('aria-current');
      });
    };

    var planifier = function () {
      if (!demande) demande = window.requestAnimationFrame(situer);
    };

    window.addEventListener('scroll', planifier, { passive: true });
    window.addEventListener('resize', planifier);
    // Un saut d'ancre ne produit pas toujours d'evenement de defilement quand
    // la page est deja a la bonne hauteur : on situe aussi au clic.
    if (nav) nav.addEventListener('click', planifier);
    situer();
  }

  // ---------- formulaire de contact ----------
  var form = document.getElementById('formulaire-contact');
  var retour = document.getElementById('retour-formulaire');
  if (!form || !retour) return;

  var ADRESSE = 'contact@qualitos.fr';
  var ECHEC = 'L’envoi n’a pas abouti. Votre messagerie s’ouvre avec la demande déjà '
    + 'rédigée ; vous pouvez aussi écrire à ' + ADRESSE + '.';

  var INTITULES = {
    nom: 'Nom', fonction: 'Fonction', organisation: 'Organisation',
    courriel: 'Courriel', telephone: 'Téléphone', secteur: 'Secteur',
    besoin: 'Besoin'
  };

  function dire(texte) {
    retour.textContent = texte;
    retour.hidden = false;
  }

  function valeurs() {
    var data = new FormData(form);
    var out = {};
    data.forEach(function (v, k) { out[k] = String(v).trim(); });
    return out;
  }

  /**
   * Le jeton demande des l'affichage du formulaire.
   *
   * Il porte l'instant de son emission, signe par le service : la soumission
   * est ainsi datee, et un envoi arrive en moins de trois secondes apres le
   * chargement de la page est refuse. Le piege a robots attrape l'automate qui
   * remplit tout ce qu'il trouve ; il ne voit pas celui qui a ete regle sur ce
   * formulaire-ci, champ par champ. Le temps, lui, personne ne le simule sans
   * perdre l'interet economique de l'operation.
   *
   * Demande ICI et non au moment du clic : demande au clic, l'ecart mesure
   * serait toujours nul, et la protection ne protegerait de rien.
   */
  var jeton = '';
  (function demandeJeton() {
    var endpoint = form.getAttribute('data-endpoint');
    if (!endpoint || !window.fetch) return;
    fetch(endpoint + '/jeton', { headers: { Accept: 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (corps) { if (corps && corps.jeton) jeton = corps.jeton; })
      // Un echec ici ne doit RIEN casser : la soumission partira sans jeton, le
      // service la refusera en le disant, et le repli par messagerie reste la.
      .catch(function () { /* sans jeton */ });
  })();

  /**
   * Il n'y a pas encore de serveur pour recevoir ces demandes.
   *
   * Plutôt qu'un bouton qui ne fait rien — ou pire, qui affiche « merci » sans
   * que personne ne reçoive quoi que ce soit — le formulaire compose le
   * courriel et ouvre le client de messagerie. Le jour où un point de réception
   * existe, il suffit de renseigner `data-endpoint` sur le formulaire : la
   * branche au-dessus prend le relais, sans toucher au reste.
   */
  function parCourriel(v, texte) {
    var corps = Object.keys(INTITULES)
      .filter(function (k) { return v[k]; })
      .map(function (k) { return INTITULES[k] + ' : ' + v[k]; })
      .join('\n');

    var sujet = 'Demande de démonstration QualitOS'
      + (v.organisation ? ' — ' + v.organisation : '');

    window.location.href = 'mailto:' + ADRESSE
      + '?subject=' + encodeURIComponent(sujet)
      + '&body=' + encodeURIComponent(corps);

    // Le message vient de l'appelant quand il a quelque chose de plus précis à
    // dire — « l'envoi n'a pas abouti » plutôt que « votre messagerie s'ouvre ».
    // Sans cela, le repli écrasait la seule phrase qui expliquait pourquoi.
    dire(texte || 'Votre messagerie s’ouvre avec la demande déjà rédigée. '
      + 'Il ne reste qu’à l’envoyer. Si rien ne s’ouvre, écrivez directement à ' + ADRESSE + '.');
  }

  form.addEventListener('submit', function (e) {
    e.preventDefault();

    // La validation du navigateur fait le travail, et désigne le champ fautif.
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }

    var v = valeurs();
    var endpoint = form.getAttribute('data-endpoint');

    if (!endpoint) {
      parCourriel(v);
      return;
    }

    var bouton = form.querySelector('button[type="submit"]');
    bouton.disabled = true;
    dire('Envoi en cours…');

    fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(Object.assign({}, v, { jeton: jeton }))
    })
      .then(function (r) {
        return r.json().catch(function () { return {}; })
          .then(function (corps) { return { ok: r.ok, statut: r.status, corps: corps }; });
      })
      .then(function (reponse) {
        if (reponse.ok) {
          form.reset();
          dire('Demande reçue. Nous vous répondons sous un jour ouvré.');
          return;
        }

        // Une saisie refusée (4xx) n'est pas une panne : le serveur dit quel
        // champ reprendre, et basculer sur la messagerie ne ferait qu'envoyer
        // la même saisie fautive par un autre chemin.
        if (reponse.statut >= 400 && reponse.statut < 500) {
          dire(reponse.corps.message || 'La demande n’a pas été acceptée. Vérifiez vos coordonnées.');
          return;
        }

        // 5xx : c'est chez nous. Ce qui vient d'être écrit ne doit pas être
        // perdu — la messagerie s'ouvre avec les champs déjà remplis.
        parCourriel(v, ECHEC);
      })
      .catch(function () {
        // Réseau coupé : même issue, pour la même raison.
        parCourriel(v, ECHEC);
      })
      .then(function () { bouton.disabled = false; });
  });
})();

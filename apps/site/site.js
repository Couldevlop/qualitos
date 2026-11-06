/* ==========================================================================
   QualitOS — site de présentation
   Deux comportements, pas un de plus : ouvrir le menu sur petit écran, et
   faire partir le formulaire de contact.
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
      body: JSON.stringify(v)
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

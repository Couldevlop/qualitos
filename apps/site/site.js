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
  function parCourriel(v) {
    var corps = Object.keys(INTITULES)
      .filter(function (k) { return v[k]; })
      .map(function (k) { return INTITULES[k] + ' : ' + v[k]; })
      .join('\n');

    var sujet = 'Demande de démonstration QualitOS'
      + (v.organisation ? ' — ' + v.organisation : '');

    window.location.href = 'mailto:' + ADRESSE
      + '?subject=' + encodeURIComponent(sujet)
      + '&body=' + encodeURIComponent(corps);

    dire('Votre messagerie s’ouvre avec la demande déjà rédigée. '
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
        if (!r.ok) throw new Error('HTTP ' + r.status);
        form.reset();
        dire('Demande reçue. Nous vous répondons sous un jour ouvré.');
      })
      .catch(function () {
        // Un échec réseau ne doit pas faire perdre ce qui vient d'être écrit :
        // on bascule sur le courriel, avec les champs déjà remplis.
        dire('L’envoi n’a pas abouti. Votre messagerie s’ouvre avec la demande ; '
          + 'vous pouvez aussi écrire à ' + ADRESSE + '.');
        parCourriel(v);
      })
      .then(function () { bouton.disabled = false; });
  });
})();

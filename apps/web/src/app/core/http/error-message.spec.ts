import { HttpErrorResponse } from '@angular/common/http';

import { safeErrorMessage } from './error-message';

/**
 * La traduction d'une erreur HTTP en phrase montrée à l'utilisateur.
 *
 * <p>Ce module n'avait aucun banc — et c'est précisément pour cela que 401 et
 * 403 rendaient la même phrase sans que personne ne le voie. Un écran dont la
 * session avait expiré annonçait un refus de droits, ce qui envoie chercher du
 * côté de l'administrateur un problème qu'un simple retour à la page de
 * connexion résout.
 *
 * <p>Le second invariant tenu ici est de sécurité (OWASP A09) : aucune phrase ne
 * doit reprendre ce que dit le serveur. Un `detail` de ProblemDetail peut porter
 * un nom de classe, une contrainte JPA ou une trace — précieux pour qui cherche
 * une faille, inutile pour un responsable qualité.
 */
describe('safeErrorMessage', () => {

  const FALLBACK = 'Cycle introuvable.';

  function erreur(status: number, detail = 'org.hibernate.ConstraintViolation: uk_secret'): HttpErrorResponse {
    return new HttpErrorResponse({ status, error: { detail } });
  }

  // ---------- la distinction qui manquait ----------

  it('distingue une session expirée d\'un refus de droits', () => {
    const expiree = safeErrorMessage(erreur(401), FALLBACK);
    const refus = safeErrorMessage(erreur(403), FALLBACK);

    expect(expiree).not.toBe(refus);
    expect(expiree).toContain('Session expirée');
    expect(refus).toContain('droits');
  });

  it('invite à se reconnecter sur 401, et ne parle pas de droits', () => {
    // Le mot « droits » enverrait l'utilisateur voir son administrateur pour un
    // problème qu'il règle seul en se reconnectant.
    const message = safeErrorMessage(erreur(401), FALLBACK);

    expect(message).toContain('reconnect');
    expect(message).not.toContain('droits');
  });

  // ---------- le reste de la table ----------

  it('rend une phrase propre pour chaque statut connu', () => {
    const attendus: Array<[number, string]> = [
      [0, 'Service inaccessible'],
      [400, 'Champs invalides'],
      [409, 'État incompatible'],
      [422, 'refusées'],
      [429, 'Trop de requêtes']
    ];

    for (const [status, extrait] of attendus) {
      expect(safeErrorMessage(erreur(status), FALLBACK))
        .withContext('statut ' + status)
        .toContain(extrait);
    }
  });

  it('rend le repli sur 404, que l\'appelant sait formuler mieux que nous', () => {
    expect(safeErrorMessage(erreur(404), FALLBACK)).toBe(FALLBACK);
  });

  it('range les statuts inconnus de chaque famille', () => {
    expect(safeErrorMessage(erreur(418), FALLBACK)).toContain('refusée par le serveur');
    expect(safeErrorMessage(erreur(503), FALLBACK)).toContain('Erreur serveur');
  });

  it('rend le repli quand ce qu\'on lui donne n\'a pas de statut', () => {
    expect(safeErrorMessage(new Error('boum'), FALLBACK)).toBe(FALLBACK);
    expect(safeErrorMessage(null, FALLBACK)).toBe(FALLBACK);
    expect(safeErrorMessage(undefined, FALLBACK)).toBe(FALLBACK);
  });

  // ---------- l'invariant de sécurité ----------

  it('ne laisse JAMAIS filtrer ce que dit le serveur (OWASP A09)', () => {
    const statuts = [0, 400, 401, 403, 404, 409, 422, 429, 418, 500, 503];

    for (const status of statuts) {
      const message = safeErrorMessage(erreur(status), FALLBACK);
      expect(message)
        .withContext('statut ' + status)
        .not.toContain('Hibernate');
      expect(message).not.toContain('uk_secret');
    }
  });
});

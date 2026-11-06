/**
 * OWASP A09 — Security Logging and Monitoring Failures.
 *
 * Map an HttpErrorResponse (or anything that looks like one) to a French
 * user-safe message. NEVER returns raw backend `detail`/`message`/`title`
 * strings: those can disclose stack traces, internal class names, JPA
 * constraint names, or database hints — all of which are valuable to an
 * attacker and useless to a quality manager. The original error object
 * should be logged to the console (or to telemetry) by the caller, not
 * shown to the user.
 *
 * Use the second argument to override the 404 / network fallback when
 * the calling context has a more specific intent (e.g. "Cycle introuvable.").
 */
/** Ce qui trahit une trace technique plutôt qu'une phrase écrite pour un humain. */
const RETRACE = /[\r\n]|\bat [\w.$]+\(/;

export function safeErrorMessage(err: unknown, fallback: string): string {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const brut = err as any;
  const status: unknown = brut?.status;

  // Le canal DÉCLARÉ des messages destinés à l'utilisateur.
  //
  // La règle ci-dessus tient : `detail`, `message` et `title` ne sont jamais
  // affichés, parce qu'ils peuvent porter un nom de classe, une contrainte JPA
  // ou une trace. `userMessage` est différent : le serveur ne le pose que
  // lorsqu'il a rédigé une phrase POUR l'utilisateur, et il ne le pose nulle
  // part ailleurs. Sans ce canal, un refus de clôture DMAIC — « 3 points à
  // solder » — devenait « État incompatible, rechargez la page » : un conseil
  // faux, puisque recharger ne solde rien, et la perte de la seule information
  // utile.
  //
  // Les garde-fous restent : une seule ligne, longueur bornée, et rien qui
  // ressemble à une trace. Un serveur qui déborderait ce cadre retombe sur le
  // message générique plutôt que de projeter ses entrailles à l'écran.
  const declare: unknown = brut?.error?.userMessage;
  if (typeof declare === 'string') {
    const texte = declare.trim();
    if (texte.length > 0 && texte.length <= 300 && !RETRACE.test(texte)) {
      return texte;
    }
  }
  if (status === 0) return 'Service inaccessible — vérifiez votre connexion.';
  if (status === 400) return 'Champs invalides — vérifiez le formulaire.';
  // 401 et 403 ne disent PAS la même chose, et les confondre envoie l'utilisateur
  // chercher au mauvais endroit : une session expirée se rouvre en se
  // reconnectant, un refus de droits se règle avec son administrateur. Le message
  // unique faisait passer la première pour la seconde — un écran paraissait
  // interdit alors qu'il suffisait de se reconnecter.
  if (status === 401) return 'Session expirée — reconnectez-vous.';
  if (status === 403) return 'Vous n\'avez pas les droits pour cette action.';
  if (status === 404) return fallback;
  if (status === 409) return 'État incompatible — rechargez la page.';
  if (status === 422) return 'Données refusées par le serveur.';
  if (status === 429) return 'Trop de requêtes — réessayez dans un instant.';
  if (typeof status === 'number' && status >= 400 && status < 500) return 'Requête refusée par le serveur.';
  if (typeof status === 'number' && status >= 500) return 'Erreur serveur — réessayez dans un instant.';
  return fallback;
}

/**
 * Les raisons d'un refus, quand le serveur les a énumérées.
 *
 * Même canal déclaré que {@link safeErrorMessage} : seule la propriété
 * {@code reasons}, posée exprès par le serveur, est lue — jamais {@code detail}
 * ni {@code message}. Chaque ligne passe le même garde-fou : une phrase, bornée,
 * sans trace technique. Une liste vide signifie « le serveur n'a rien
 * énuméré », et l'écran retombe alors sur le message générique.
 */
export function gateReasons(err: unknown): string[] {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const brut: unknown = (err as any)?.error?.reasons;
  if (!Array.isArray(brut)) return [];
  return brut
    .filter((r): r is string => typeof r === 'string')
    .map(r => r.trim())
    .filter(r => r.length > 0 && r.length <= 300 && !RETRACE.test(r));
}

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
export function safeErrorMessage(err: unknown, fallback: string): string {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const status: unknown = (err as any)?.status;
  if (status === 0) return 'Service inaccessible — vérifiez votre connexion.';
  if (status === 400) return 'Champs invalides — vérifiez le formulaire.';
  if (status === 401 || status === 403) return 'Vous n\'avez pas les droits pour cette action.';
  if (status === 404) return fallback;
  if (status === 409) return 'État incompatible — rechargez la page.';
  if (status === 422) return 'Données refusées par le serveur.';
  if (status === 429) return 'Trop de requêtes — réessayez dans un instant.';
  if (typeof status === 'number' && status >= 400 && status < 500) return 'Requête refusée par le serveur.';
  if (typeof status === 'number' && status >= 500) return 'Erreur serveur — réessayez dans un instant.';
  return fallback;
}

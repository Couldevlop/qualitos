import { HttpResponse } from '@angular/common/http';

/**
 * Enregistrer un fichier reçu du serveur, sous le nom que le serveur lui donne.
 *
 * <p>Ces deux gestes se réécrivaient dans chaque écran qui propose une
 * extraction, et le nom du fichier finissait par être codé en dur côté
 * navigateur : le jour où le serveur le change, le fichier téléchargé porte
 * l'ancien. C'est l'en-tête `Content-Disposition` qui fait foi.
 */

/** Le nom de fichier annoncé par le serveur, ou `null` s'il n'en annonce pas. */
export function filenameFromResponse(response: HttpResponse<unknown>): string | null {
  const header = response.headers.get('Content-Disposition');
  const match = header ? /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(header) : null;
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Déclenche l'enregistrement d'un binaire.
 *
 * <p>L'URL temporaire est libérée aussitôt : la retenir ferait fuir le contenu
 * du fichier en mémoire pour toute la durée de la session.
 */
export function saveBlobAs(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

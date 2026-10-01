/**
 * Une cellule CSV sûre.
 *
 * <p>Un tableur exécute une cellule qui commence par `=`, `+`, `-`, `@`, une
 * tabulation ou un retour chariot (injection CSV, OWASP). Un intitulé de risque
 * est du texte saisi : on le préfixe d'une apostrophe, que le tableur affiche
 * comme du texte. Les guillemets sont doublés et la cellule est toujours
 * entourée de guillemets, pour que `;` et les sauts de ligne restent dans leur
 * cellule.
 */
export function csvCell(value: string | number | null | undefined): string {
  let v = value === null || value === undefined ? '' : String(value);
  if (/^[=+\-@\t\r]/.test(v)) {
    v = `'${v}`;
  }
  return `"${v.replace(/"/g, '""')}"`;
}

import { CapabilityStatus } from './spc-capability.types';

/**
 * Le statut d'une étude, en trois mots comme les trois couleurs du Cpk :
 * maîtrisé (vert), à surveiller (jaune), non maîtrisé (rouge). Le libellé et la
 * pastille disent toujours la même chose.
 */
export function statusLabel(s: CapabilityStatus): string {
  return s === 'CAPABLE' ? $localize`:@@spc.status.capable:Maîtrisé`
    : s === 'WATCH' ? $localize`:@@spc.status.watch:À surveiller`
      : $localize`:@@spc.status.not-capable:Non maîtrisé`;
}

/** La couleur d'une pastille de Cpk : vert, jaune, rouge. */
export function statusTone(s: CapabilityStatus): 'ok' | 'warn' | 'bad' {
  return s === 'CAPABLE' ? 'ok' : s === 'WATCH' ? 'warn' : 'bad';
}

/** Un nombre à décimales fixes, ou un tiret quand il n'a pas pu se calculer. */
export function fmt(v: number | null | undefined, digits = 3): string {
  return v === null || v === undefined || !Number.isFinite(v) ? '–' : v.toFixed(digits);
}

/**
 * Une cellule CSV sûre : un tableur exécute une cellule qui commence par `=`,
 * `+`, `-`, `@`, une tabulation ou un retour chariot (injection CSV, OWASP).
 * Une référence de pièce est du texte saisi : on la préfixe d'une apostrophe.
 * Les nombres ne sont pas préfixés — un Cpk négatif reste un nombre.
 */
export function csvCell(value: string | number | null | undefined): string {
  if (typeof value === 'number') return `"${value}"`;
  let v = value ?? '';
  if (/^[=+\-@\t\r]/.test(v)) v = `'${v}`;
  return `"${v.replace(/"/g, '""')}"`;
}

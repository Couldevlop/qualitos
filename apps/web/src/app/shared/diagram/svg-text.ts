/**
 * Mise en page de texte pour les diagrammes SVG dessinés à la main.
 *
 * <p>Pourquoi ce fichier existe : SVG ne sait pas replier un texte. Un
 * `<text>` déborde de sa boîte sans rien dire, et la hauteur d'un encart ne
 * peut donc pas se déduire du contenu — or c'est elle qui commande toute la
 * géométrie qui suit (position de l'encart suivant, hauteur totale du dessin).
 *
 * <p>Deux options se présentaient. `<foreignObject>` rendrait du vrai HTML,
 * donc un vrai repli avec la typographie du design system ; mais sa hauteur
 * n'est connue qu'après rendu — impossible d'en tirer une géométrie calculée
 * en amont — et il se comporte mal à l'export image. On découpe donc ici, en
 * amont, sur une estimation de largeur de caractère : le résultat est
 * déterministe, testable sans navigateur, et la hauteur est connue avant de
 * tracer le premier trait.
 *
 * <p>Conséquence assumée : le découpage est approximatif (un « W » est plus
 * large qu'un « i »). C'est pourquoi tout texte tronqué ici reste lisible
 * INTÉGRALEMENT dans la liste qui accompagne chaque diagramme — le dessin
 * donne la forme, la liste garde le texte.
 */

/** Caractère de troncature — un seul glyphe, contrairement à « ... ». */
const ELLIPSIS = '…';

/**
 * Replie un libellé en lignes d'au plus `maxChars` caractères, `maxLines` au
 * maximum. Le débordement est signalé par une ellipse sur la dernière ligne.
 *
 * <p>Un mot plus long que la ligne (référence, URL, mot composé) est coupé
 * net : le laisser déborder casserait l'encart, et le renvoyer seul sur une
 * ligne trop courte ne le rendrait pas plus lisible.
 */
export function wrapSvgText(text: string, maxChars: number, maxLines: number): string[] {
  const source = (text ?? '').replace(/\s+/g, ' ').trim();
  if (!source || maxChars < 1 || maxLines < 1) {
    return [];
  }

  const words = source.split(' ');
  const lines: string[] = [];
  let current = '';

  for (let i = 0; i < words.length; i++) {
    let word = words[i];

    // Mot imprenable : on le découpe en tranches de la largeur d'une ligne.
    while (word.length > maxChars) {
      if (current) {
        lines.push(current);
        current = '';
      }
      if (lines.length >= maxLines) {
        return withEllipsis(lines, maxLines, maxChars);
      }
      lines.push(word.slice(0, maxChars));
      word = word.slice(maxChars);
    }

    const candidate = current ? current + ' ' + word : word;
    if (candidate.length <= maxChars) {
      current = candidate;
      continue;
    }
    lines.push(current);
    current = word;
    if (lines.length >= maxLines) {
      // Il reste du texte (`current`) mais plus de ligne pour l'accueillir.
      return withEllipsis(lines, maxLines, maxChars);
    }
  }

  if (current) {
    lines.push(current);
  }
  return lines.length > maxLines ? withEllipsis(lines, maxLines, maxChars) : lines;
}

/**
 * Ramène un libellé sur une seule ligne, tronqué à `maxChars` ellipse comprise.
 * Utile pour les étiquettes de branche, qui n'ont droit qu'à un seul niveau.
 */
export function truncateSvgText(text: string, maxChars: number): string {
  const source = (text ?? '').replace(/\s+/g, ' ').trim();
  if (maxChars < 1) {
    return '';
  }
  if (source.length <= maxChars) {
    return source;
  }
  // `maxChars` borne la LARGEUR rendue : l'ellipse compte dans le budget.
  return source.slice(0, maxChars - 1).trimEnd() + ELLIPSIS;
}

/**
 * Coupe la liste à `maxLines` et marque la dernière ligne comme incomplète.
 *
 * L'ellipse compte DANS le budget de largeur, comme dans `truncateSvgText` : on
 * retire donc un caractère avant de l'ajouter. L'ajouter par-dessus une ligne
 * déjà pleine rendrait `maxChars + 1` caractères — un glyphe qui mord sur la
 * marge de l'encart, et une hauteur calculée sur une largeur fausse.
 */
function withEllipsis(lines: string[], maxLines: number, maxChars: number): string[] {
  const kept = lines.slice(0, maxLines);
  const last = kept[maxLines - 1];
  kept[maxLines - 1] = last.endsWith(ELLIPSIS)
    ? last
    : last.slice(0, Math.max(0, maxChars - 1)).trimEnd() + ELLIPSIS;
  return kept;
}

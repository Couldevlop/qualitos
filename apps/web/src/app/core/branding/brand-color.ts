/**
 * L'arithmétique de couleur de la marque d'un client : pure, sans Angular,
 * donc vérifiable au chiffre près.
 *
 * <p>Le serveur garantit déjà qu'une couleur d'accent enregistrée se lit sur le
 * fond de son thème (`ContrastGuard`, seuil 4,5 de WCAG 2.2 AA). Il ne dit rien,
 * en revanche, du <b>libellé posé SUR cette couleur</b> : un bouton plein a un
 * fond accentué et un texte dessus. C'est ce que calcule ce module, avec la même
 * formule que le serveur — luminance relative de WCAG 2.1, puis rapport
 * `(L_clair + 0,05) / (L_sombre + 0,05)`. Deux formules différentes des deux
 * côtés donneraient deux verdicts sur la même couleur.
 */

/** Une couleur décomposée en ses trois canaux, de 0 à 255. */
export interface Rgb {
  r: number;
  g: number;
  b: number;
}

/** Forme longue seulement, comme au serveur : `#RRGGBB`. */
const HEX = /^#[0-9a-fA-F]{6}$/;

/**
 * Lit une couleur, ou rend `null` si ce n'en est pas une.
 *
 * <p>Rendre `null` plutôt que lever : une marque mal formée venue du serveur ne
 * doit pas empêcher l'application de s'afficher. On garde alors les jetons du
 * socle, qui sont lisibles par construction.
 */
export function parseHex(value: string | null | undefined): Rgb | null {
  if (!value || !HEX.test(value)) {
    return null;
  }
  return {
    r: parseInt(value.slice(1, 3), 16),
    g: parseInt(value.slice(3, 5), 16),
    b: parseInt(value.slice(5, 7), 16)
  };
}

/** Écrit une couleur en `#RRGGBB`, canaux bornés et arrondis. */
export function toHex(color: Rgb): string {
  const canal = (v: number) => Math.max(0, Math.min(255, Math.round(v)))
    .toString(16)
    .padStart(2, '0');
  return `#${canal(color.r)}${canal(color.g)}${canal(color.b)}`;
}

/** Passage du canal 0-255 à sa valeur linéaire (seuil et exposant de WCAG 2.1). */
function linearize(channel: number): number {
  const c = channel / 255;
  return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
}

/** Luminance relative au sens de WCAG 2.1, de 0 (noir) à 1 (blanc). */
export function relativeLuminance(color: Rgb): number {
  return 0.2126 * linearize(color.r) + 0.7152 * linearize(color.g) + 0.0722 * linearize(color.b);
}

/** Rapport de contraste, de 1 (identiques) à 21 (noir et blanc). L'ordre est sans effet. */
export function contrastRatio(a: Rgb, b: Rgb): number {
  const la = relativeLuminance(a);
  const lb = relativeLuminance(b);
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
}

/** Le blanc et le quasi-noir entre lesquels se choisit un libellé. */
const WHITE: Rgb = { r: 255, g: 255, b: 255 };
const INK: Rgb = { r: 17, g: 24, b: 27 };

/**
 * Le libellé à poser sur un fond : celui des deux qui s'y lit le mieux.
 *
 * <p>On ne tranche pas sur un seuil de luminance (« clair → texte noir ») : ce
 * raccourci se trompe sur les couleurs saturées, un bleu vif et un jaune vif
 * ayant des luminances voisines pour des contrastes opposés. On mesure les deux
 * et on garde le meilleur, ce qui ne peut pas rendre un résultat pire que le
 * seuil.
 */
export function readableForeground(background: Rgb): Rgb {
  return contrastRatio(WHITE, background) >= contrastRatio(INK, background) ? WHITE : INK;
}

/**
 * Mélange deux couleurs. `ratio` vaut la part de `vers` : 0 rend `depuis`
 * inchangée, 1 rend `vers`.
 */
export function mix(depuis: Rgb, vers: Rgb, ratio: number): Rgb {
  const t = Math.max(0, Math.min(1, ratio));
  return {
    r: depuis.r + (vers.r - depuis.r) * t,
    g: depuis.g + (vers.g - depuis.g) * t,
    b: depuis.b + (vers.b - depuis.b) * t
  };
}

/** La même couleur, en rgba(), pour les fonds doux (pastilles, survols). */
export function withAlpha(color: Rgb, alpha: number): string {
  const a = Math.max(0, Math.min(1, alpha));
  const canal = (v: number) => Math.max(0, Math.min(255, Math.round(v)));
  return `rgba(${canal(color.r)}, ${canal(color.g)}, ${canal(color.b)}, ${a})`;
}

/**
 * La variante de survol d'une couleur d'action.
 *
 * <p>Sur fond clair on assombrit, sur fond sombre on éclaircit : dans les deux
 * cas le survol doit s'écarter du fond de la page, pas s'en rapprocher. Un
 * assombrissement systématique rendait le survol invisible en thème sombre.
 */
export function hoverVariant(color: Rgb, dark: boolean): Rgb {
  return dark ? mix(color, WHITE, 0.16) : mix(color, INK, 0.16);
}

/**
 * La teinte d'un texte accentué posé sur un fond doux (lien, pastille).
 *
 * <p>Elle n'est pas la couleur brute : celle-ci est réglée pour porter un
 * libellé clair, pas pour être lue en petit sur un fond presque blanc. On la
 * pousse vers l'encre (ou vers le blanc en sombre) jusqu'à ce qu'elle atteigne
 * le seuil AA sur ce fond doux, et on s'arrête au premier palier suffisant pour
 * rester le plus près possible de la marque choisie.
 */
export function softForeground(color: Rgb, dark: boolean): Rgb {
  const fondDoux = dark ? mix(INK, color, 0.18) : mix(WHITE, color, 0.12);
  const cible = dark ? WHITE : INK;
  let teinte = color;
  for (let pas = 0; pas <= 10; pas++) {
    teinte = mix(color, cible, pas / 10);
    if (contrastRatio(teinte, fondDoux) >= 4.5) {
      return teinte;
    }
  }
  return teinte;
}

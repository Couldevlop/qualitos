import { wrapSvgText } from '../../../../shared/diagram/svg-text';

/**
 * Géométrie de la cascade des 5 Pourquoi — calculée, jamais dessinée en dur.
 *
 * <p>La méthode dit une descente : chaque réponse est plus profonde que la
 * précédente, et la dernière touche la cause racine. Une liste à puces perd
 * exactement cela — elle empile des lignes équivalentes. L'escalier remet la
 * profondeur à l'écran : chaque encart est décalé d'un cran vers le bas et
 * vers l'extérieur, relié au précédent par une flèche coudée dont la teinte
 * passe du rouge (le symptôme visible) au vert (la cause racine).
 *
 * <p>Le nombre de pourquoi n'est PAS figé à cinq : le modèle en porte de trois
 * (minimum pour conclure) à sept (au-delà, on n'énumère plus que des
 * circonstances). Toute la géométrie se déduit donc du nombre réel d'étapes —
 * y compris la teinte, qui répartit la rampe sur la longueur effective de la
 * chaîne au lieu de supposer cinq crans.
 */

/** Une étape telle que le dessin la consomme — découplée du DTO serveur. */
export interface CascadeStepInput {
  id: string;
  position: number;
  answer: string;
}

export interface CascadeOptions {
  /** Sens de lecture. En RTL l'escalier descend vers la gauche (cf. mirroir). */
  rtl: boolean;
  /** Cause racine conclue : elle ferme la descente par un encart terminal. */
  rootCause?: string | null;
}

export interface CascadeTextLine {
  text: string;
  y: number;
}

export interface CascadeCard {
  key: string;
  /** Rang affiché dans la pastille. 0 = encart terminal « cause racine ». */
  position: number;
  /** Cran de la rampe rouge→vert, de 0 à 3. */
  tone: number;
  isRoot: boolean;
  x: number;
  y: number;
  width: number;
  height: number;
  badgeCx: number;
  badgeCy: number;
  textX: number;
  anchor: 'start' | 'end';
  captionY: number;
  lines: CascadeTextLine[];
}

export interface CascadeConnector {
  key: string;
  tone: number;
  /** Flèche coudée : trois points (descente puis avancée latérale). */
  points: string;
}

export interface CascadeLayout {
  width: number;
  height: number;
  cards: CascadeCard[];
  connectors: CascadeConnector[];
}

// --- Constantes de tracé -----------------------------------------------------
// Elles vivent ici et non en SCSS : la hauteur d'un encart dépend du nombre de
// lignes de texte, et la position de l'encart suivant dépend de cette hauteur.
// Une géométrie moitié CSS moitié TS serait impossible à tenir cohérente.

const PAD = 14;
const CARD_W = 320;
/** Décalage latéral d'un cran à l'autre : c'est lui qui fait l'« escalier ». */
const STEP_X = 46;
const GAP_Y = 26;
const BADGE_R = 15;
const TEXT_LEFT = 52;
const LINE_H = 19;
const HEAD_H = 44;
const FOOT_H = 16;
const MAX_LINES = 3;
/** Largeur de ligne en caractères, estimée pour ~14 px sur (320 − 52 − 16) px. */
const MAX_CHARS = 38;
/** Nombre de teintes de la rampe rouge → orange → ambre → vert. */
const TONES = 4;

const EMPTY: CascadeLayout = { width: 0, height: 0, cards: [], connectors: [] };

/**
 * Construit la cascade. Rend une mise en page vide (donc rien à dessiner)
 * quand la chaîne n'a pas encore de maillon : un escalier à zéro marche ne
 * dirait rien de plus que le message d'invite déjà présent à l'écran.
 */
export function buildCascadeLayout(
  steps: readonly CascadeStepInput[],
  options: CascadeOptions
): CascadeLayout {
  const chain = [...(steps ?? [])].sort((a, b) => a.position - b.position);
  if (chain.length === 0) {
    return EMPTY;
  }

  const rootCause = (options.rootCause ?? '').trim();
  /** Nombre total d'encarts, encart terminal compris. */
  const total = chain.length + (rootCause ? 1 : 0);

  // Deux passes : la largeur totale doit être connue AVANT de miroiter les
  // abscisses, et elle dépend du nombre de crans, pas du contenu.
  const width = PAD * 2 + CARD_W + (total - 1) * STEP_X;
  const mirrorX = (x: number) => (options.rtl ? width - x : x);
  const mirrorRect = (x: number, w: number) => (options.rtl ? width - x - w : x);
  const anchor: 'start' | 'end' = options.rtl ? 'end' : 'start';

  const cards: CascadeCard[] = [];
  const connectors: CascadeConnector[] = [];

  let cursorY = PAD;
  for (let i = 0; i < total; i++) {
    const isRoot = rootCause !== '' && i === chain.length;
    const step = isRoot ? null : chain[i];
    const label = isRoot ? rootCause : (step as CascadeStepInput).answer;
    const texts = wrapSvgText(label, MAX_CHARS, MAX_LINES);
    const height = HEAD_H + Math.max(texts.length, 1) * LINE_H + FOOT_H;
    const logicalX = PAD + i * STEP_X;

    cards.push({
      key: isRoot ? 'root' : (step as CascadeStepInput).id,
      position: isRoot ? 0 : (step as CascadeStepInput).position,
      // L'encart terminal est toujours au bout de la rampe : la cause racine
      // est la destination, pas une teinte intermédiaire.
      tone: isRoot ? TONES - 1 : toneOf(i, chain.length),
      isRoot,
      x: mirrorRect(logicalX, CARD_W),
      y: cursorY,
      width: CARD_W,
      height,
      badgeCx: mirrorX(logicalX + 28),
      badgeCy: cursorY + 28,
      textX: mirrorX(logicalX + TEXT_LEFT),
      anchor,
      captionY: cursorY + 24,
      lines: texts.map((text, k) => ({ text, y: cursorY + HEAD_H + k * LINE_H }))
    });

    cursorY += height + GAP_Y;
  }

  // La flèche part du bas de la pastille et rejoint la pastille suivante : elle
  // relie deux rangs, pas deux boîtes — c'est la chaîne des « pourquoi » qu'on
  // suit du regard, pas la bordure des encarts.
  for (let i = 0; i < cards.length - 1; i++) {
    const from = cards[i];
    const to = cards[i + 1];
    const startY = from.y + from.height;
    const endY = to.badgeCy;
    connectors.push({
      key: from.key + '>' + to.key,
      tone: to.tone,
      points: `${from.badgeCx},${startY} ${from.badgeCx},${endY} ${to.badgeCx},${endY}`
    });
  }

  return {
    width,
    height: cursorY - GAP_Y + PAD,
    cards,
    connectors
  };
}

/**
 * Répartit la rampe rouge→vert sur la longueur RÉELLE de la chaîne.
 *
 * <p>Une chaîne de trois et une chaîne de sept doivent toutes deux partir du
 * rouge et finir au vert : un pas fixe donnerait à la chaîne courte trois
 * nuances de rouge, et à la longue une fin de rampe indistincte.
 */
function toneOf(index: number, length: number): number {
  if (length <= 1) {
    return 0;
  }
  return Math.round((index / (length - 1)) * (TONES - 1));
}

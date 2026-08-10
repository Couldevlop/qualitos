import { truncateSvgText, wrapSvgText } from '../../../../shared/diagram/svg-text';

/**
 * Géométrie de l'arête de poisson (Ishikawa) — calculée, jamais dessinée en dur.
 *
 * <p>Une carte par catégorie avec sa liste de causes range l'information mais
 * perd ce que le diagramme dit : que TOUTES les familles de causes convergent
 * vers un seul effet. L'arête le remet à l'écran — une tête qui porte
 * l'énoncé du problème, une épine qui y mène, et des branches obliques qui
 * l'attaquent de part et d'autre.
 *
 * <p>Le nombre de catégories est configurable (6M, 7M, 8M). Rien n'est donc
 * dessiné pour six : le nombre de colonnes, la longueur de l'épine et la
 * hauteur totale se déduisent du nombre réel de branches et du nombre de
 * causes portées par chacune.
 *
 * <p>Sous-causes : elles ne sont PAS tracées. Une hiérarchie à trois niveaux
 * sur une oblique produit un buisson illisible dès la deuxième cause — les
 * traits se croisent, les libellés se recouvrent, et le diagramme perd le seul
 * avantage qu'il avait sur la liste. Le dessin porte donc le premier niveau,
 * et signale par un compteur (« +3 ») qu'une cause en cache d'autres ; la
 * hiérarchie complète, elle, reste dépliée dans les cartes de branche sous le
 * diagramme, où l'indentation la rend lisible et où elle est manipulable.
 */

export interface FishboneCauseInput {
  id: string;
  label: string;
  /** Nombre de sous-causes, tous niveaux confondus. */
  descendants: number;
}

export interface FishboneBranchInput {
  key: string;
  label: string;
  causes: readonly FishboneCauseInput[];
}

export interface FishboneOptions {
  /** Sens de lecture : en RTL la tête passe à gauche (cf. miroir des abscisses). */
  rtl: boolean;
}

export interface FishboneTextLine {
  text: string;
  y: number;
}

export interface FishboneCauseVm {
  key: string;
  label: string;
  /** 0 = cause feuille ; sinon, nombre de sous-causes masquées par le dessin. */
  descendants: number;
  tickX1: number;
  tickX2: number;
  tickY: number;
  textX: number;
  textY: number;
  anchor: 'start' | 'end';
}

export interface FishboneBoneVm {
  key: string;
  label: string;
  side: 'top' | 'bottom';
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  labelX: number;
  labelY: number;
  labelBoxX: number;
  labelBoxY: number;
  labelBoxWidth: number;
  labelBoxHeight: number;
  causes: FishboneCauseVm[];
  /** Vrai quand la branche n'a encore aucune cause : le dessin le dit. */
  empty: boolean;
}

export interface FishboneHeadVm {
  x: number;
  y: number;
  width: number;
  height: number;
  textX: number;
  anchor: 'start' | 'end';
  lines: FishboneTextLine[];
}

export interface FishboneLayout {
  width: number;
  height: number;
  spineX1: number;
  spineX2: number;
  spineY: number;
  head: FishboneHeadVm;
  bones: FishboneBoneVm[];
}

// --- Constantes de tracé -----------------------------------------------------

const PAD = 16;
/** Largeur d'une colonne de branche. Deux branches (haut/bas) la partagent. */
const SLOT_W = 230;
/**
 * Marge entre le début de l'épine et la première branche : elle doit loger la
 * moitié de l'étiquette de catégorie, qui coiffe la pointe de l'oblique et
 * déborderait sinon hors du dessin (BONE_DX + LABEL_W / 2).
 */
const SPINE_LEAD = 130;
/**
 * Marge entre la dernière branche et la tête : les libellés de cause s'écrivent
 * vers la tête, et sans cette réserve ceux de la dernière colonne passeraient
 * SOUS l'encart du problème.
 */
const SPINE_TAIL = 196;
/** Déport horizontal de l'oblique : c'est lui qui donne l'inclinaison. */
const BONE_DX = 58;
/** Longueur verticale minimale d'une branche, avant la première cause. */
const BONE_BASE = 54;
/** Pas vertical entre deux causes le long de l'oblique. */
const CAUSE_STEP = 26;
const TICK_LEN = 16;
const LABEL_H = 26;
const LABEL_W = 132;
const LABEL_GAP = 8;

const HEAD_W = 224;
const HEAD_PAD_Y = 34;
const HEAD_LINE_H = 19;
const HEAD_MAX_LINES = 3;
const HEAD_MAX_CHARS = 26;

const CAUSE_MAX_CHARS = 22;
const BRANCH_MAX_CHARS = 17;

const EMPTY_HEAD: FishboneHeadVm = {
  x: 0, y: 0, width: 0, height: 0, textX: 0, anchor: 'start', lines: []
};
const EMPTY: FishboneLayout = {
  width: 0, height: 0, spineX1: 0, spineX2: 0, spineY: 0, head: EMPTY_HEAD, bones: []
};

/**
 * Construit l'arête. Rend une mise en page vide quand aucune catégorie n'est
 * fournie : une épine sans branche n'illustre rien.
 */
export function buildFishboneLayout(
  problem: string,
  branches: readonly FishboneBranchInput[],
  options: FishboneOptions
): FishboneLayout {
  const list = branches ?? [];
  if (list.length === 0) {
    return EMPTY;
  }

  // Branches alternées : la 1re en haut, la 2e en bas, la 3e en haut… Une
  // colonne accueille donc au plus deux branches, d'où `ceil(n / 2)`.
  const columns = Math.ceil(list.length / 2);

  const spineX1 = PAD;
  const spineX2 = spineX1 + SPINE_LEAD + (columns - 1) * SLOT_W + SPINE_TAIL;
  const headX = spineX2;
  const width = headX + HEAD_W + PAD;

  // Tout est calculé dans un repère « sens de lecture naturel » puis miroité en
  // une seule passe : une géométrie qui se dédoublerait par des `if (rtl)`
  // dériverait à la première retouche. Le miroir porte sur les abscisses et sur
  // l'ancrage du texte — jamais sur le texte lui-même, qui garde son sens.
  const mirrorX = (x: number) => (options.rtl ? width - x : x);
  const mirrorRect = (x: number, w: number) => (options.rtl ? width - x - w : x);
  const anchor: 'start' | 'end' = options.rtl ? 'end' : 'start';

  // Passe 1 (repères logiques, avant miroir) : chaque branche connaît sa
  // hauteur, donc le dessin connaît son débordement en haut et en bas.
  const measured = list.map((branch, index) => {
    const side: 'top' | 'bottom' = index % 2 === 0 ? 'top' : 'bottom';
    const column = Math.floor(index / 2);
    const rootX = spineX1 + SPINE_LEAD + column * SLOT_W;
    const causeCount = branch.causes.length;
    const boneLength = BONE_BASE + Math.max(causeCount, 1) * CAUSE_STEP;
    return { branch, side, rootX, boneLength, extent: boneLength + LABEL_H + LABEL_GAP };
  });

  const extentOf = (side: 'top' | 'bottom') =>
    measured.filter(m => m.side === side).reduce((max, m) => Math.max(max, m.extent), 0);

  const headLines = wrapSvgText(problem, HEAD_MAX_CHARS, HEAD_MAX_LINES);
  const headHeight = HEAD_PAD_Y + Math.max(headLines.length, 1) * HEAD_LINE_H;
  /** La tête est centrée sur l'épine : elle déborde d'autant de part et d'autre. */
  const headHalf = headHeight / 2;

  // Le débordement retenu est le plus grand des deux — branches ou tête. Sans
  // ce max, un énoncé long sur des branches courtes ferait sortir la tête de la
  // boîte, donc du SVG : elle serait rognée sans un mot d'avertissement.
  const topExtent = Math.max(extentOf('top'), headHalf);
  const bottomExtent = Math.max(extentOf('bottom'), headHalf);
  const spineY = PAD + topExtent;
  const height = spineY + bottomExtent + PAD;

  const headY = spineY - headHalf;
  const headLogicalTextX = headX + 18;

  const head: FishboneHeadVm = {
    x: mirrorRect(headX, HEAD_W),
    y: headY,
    width: HEAD_W,
    height: headHeight,
    textX: mirrorX(headLogicalTextX),
    anchor,
    lines: headLines.map((text, i) => ({
      text,
      y: headY + HEAD_PAD_Y - 6 + i * HEAD_LINE_H
    }))
  };

  const bones: FishboneBoneVm[] = measured.map(m => {
    const dir = m.side === 'top' ? -1 : 1;
    const tipX = m.rootX - BONE_DX;
    const tipY = spineY + dir * m.boneLength;

    // L'étiquette de catégorie coiffe la pointe, au-delà de la branche : la
    // poser dessus masquerait la première cause.
    const labelBoxY = m.side === 'top'
      ? tipY - LABEL_GAP - LABEL_H
      : tipY + LABEL_GAP;

    const causes: FishboneCauseVm[] = m.branch.causes.map((cause, j) => {
      // Réparties entre l'épine (t → 0) et la pointe (t → 1), bornes exclues :
      // une cause collée à l'épine ou à l'étiquette deviendrait illisible.
      const t = (j + 1) / (m.branch.causes.length + 1);
      const px = m.rootX + (tipX - m.rootX) * t;
      const py = spineY + (tipY - spineY) * t;
      // Le libellé s'écrit du côté de la tête, dans l'espace laissé libre entre
      // cette oblique et la suivante : l'écrire de l'autre côté le ferait sortir
      // du dessin sur la première colonne.
      const tickEnd = px + TICK_LEN;
      return {
        key: cause.id,
        label: truncateSvgText(cause.label, CAUSE_MAX_CHARS),
        descendants: cause.descendants,
        tickX1: mirrorX(px),
        tickX2: mirrorX(tickEnd),
        tickY: py,
        textX: mirrorX(tickEnd + 5),
        textY: py - 4,
        anchor
      };
    });

    return {
      key: m.branch.key,
      label: truncateSvgText(m.branch.label, BRANCH_MAX_CHARS),
      side: m.side,
      x1: mirrorX(m.rootX),
      y1: spineY,
      x2: mirrorX(tipX),
      y2: tipY,
      labelX: mirrorX(tipX),
      labelY: labelBoxY + LABEL_H / 2 + 5,
      labelBoxX: mirrorRect(tipX - LABEL_W / 2, LABEL_W),
      labelBoxY,
      labelBoxWidth: LABEL_W,
      labelBoxHeight: LABEL_H,
      causes,
      empty: m.branch.causes.length === 0
    };
  });

  return {
    width,
    height,
    spineX1: mirrorX(spineX1),
    spineX2: mirrorX(spineX2),
    spineY,
    head,
    bones
  };
}

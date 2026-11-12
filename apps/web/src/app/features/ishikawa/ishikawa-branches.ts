import {
  FishboneBranchInput
} from './components/ishikawa-fishbone/ishikawa-fishbone.layout';
import { CauseCategory, IshikawaCauseResponse, IshikawaDiagramResponse } from './ishikawa.types';

/** Une cause et ses sous-causes, à tous les niveaux. */
export interface CauseNode {
  cause: IshikawaCauseResponse;
  children: CauseNode[];
}

/** Une branche de l'arête : sa catégorie, son libellé, ses causes de premier niveau. */
export interface BranchView {
  category: CauseCategory;
  label: string;
  /** Causes sans parent uniquement ; les sous-causes pendent de `children`. */
  roots: CauseNode[];
}

/**
 * Les familles de causes, dans l'ordre conventionnel des M.
 *
 * <p>L'ordre est celui de la méthode, pas l'ordre alphabétique ni celui de la
 * base : un qualiticien lit une arête de poisson de haut en bas en s'attendant
 * à retrouver Méthodes, Main-d'œuvre, Machines, Matières, Mesures, Milieu.
 */
const FAMILLES: { value: CauseCategory; label: string }[] = [
  { value: 'METHODS',      label: 'Méthodes' },
  { value: 'MANPOWER',     label: 'Main-d\'œuvre' },
  { value: 'MACHINES',     label: 'Machines' },
  { value: 'MATERIALS',    label: 'Matières' },
  { value: 'MEASUREMENTS', label: 'Mesures' },
  { value: 'ENVIRONMENT',  label: 'Milieu' }
];

/**
 * Range les causes d'un diagramme par famille, et reconstitue l'arbre
 * parent → enfants de chaque branche.
 *
 * <p>Fonction pure, hors de tout composant : le détail du diagramme la consomme
 * pour sa liste et son dessin, et la fiche de la non-conformité pour le même
 * dessin, en lecture. Deux implémentations divergeraient sur l'ordre des
 * familles — et l'ordre est la méthode.
 */
export function buildBranchViews(diagram: IshikawaDiagramResponse): BranchView[] {
  const familles = [...FAMILLES];
  if (diagram.mode === 'SEVEN_M' || diagram.mode === 'EIGHT_M') {
    familles.push({ value: 'MANAGEMENT', label: 'Management' });
  }
  if (diagram.mode === 'EIGHT_M') {
    familles.push({ value: 'MONEY', label: 'Moyens financiers' });
  }
  const parEnfantsDe = new Map<string, IshikawaCauseResponse[]>();
  for (const cause of diagram.causes) {
    const cle = cause.parentId ?? '__root__';
    const deja = parEnfantsDe.get(cle);
    if (deja) {
      deja.push(cause);
    } else {
      parEnfantsDe.set(cle, [cause]);
    }
  }
  const noeud = (cause: IshikawaCauseResponse): CauseNode => ({
    cause,
    children: (parEnfantsDe.get(cause.id) ?? []).map(noeud)
  });
  return familles.map(famille => ({
    category: famille.value,
    label: famille.label,
    roots: (parEnfantsDe.get('__root__') ?? [])
      .filter(cause => cause.category === famille.value)
      .map(noeud)
  }));
}

/**
 * Réduit l'arbre de causes à ce que le dessin sait représenter : le premier
 * niveau, et le nombre de sous-causes qu'il masque.
 */
export function toFishboneBranches(view: readonly BranchView[]): FishboneBranchInput[] {
  return view.map(branche => ({
    key: branche.category,
    label: branche.label,
    causes: branche.roots.map(n => ({
      id: n.cause.id,
      label: n.cause.label,
      // Le score suit la cause jusqu'au dessin : depuis le retrait des cartes de
      // branche, l'arête est le seul endroit où on le lit.
      score: n.cause.rootCauseScore,
      descendants: countDescendants(n)
    }))
  }));
}

/** Nombre de sous-causes, tous niveaux confondus — le dessin n'en montre que le compte. */
export function countDescendants(node: CauseNode): number {
  return node.children.reduce((somme, enfant) => somme + 1 + countDescendants(enfant), 0);
}

import { AnovaSource, GageRrCalculationView, GageRrDecision } from './gage-rr.types';

/** Le verdict AIAG sur le %GRR, en clair. */
export function decisionLabel(d: GageRrDecision): string {
  switch (d) {
    case 'ACCEPTABLE':
      return $localize`:@@spc.gage-rr.decision.acceptable:Acceptable`;
    case 'CONDITIONAL':
      return $localize`:@@spc.gage-rr.decision.conditional:Acceptable sous conditions`;
    default:
      return $localize`:@@spc.gage-rr.decision.not-acceptable:Non acceptable`;
  }
}

/** La couleur du verdict : vert, jaune, rouge. */
export function decisionTone(d: GageRrDecision): 'ok' | 'warn' | 'bad' {
  return d === 'ACCEPTABLE' ? 'ok' : d === 'CONDITIONAL' ? 'warn' : 'bad';
}

/** La décision sur le ndc (cellule C39). */
export function ndcLabel(sufficient: boolean): string {
  return sufficient
    ? $localize`:@@spc.gage-rr.ndc.sufficient:Discrimination suffisante`
    : $localize`:@@spc.gage-rr.ndc.insufficient:Discrimination insuffisante`;
}

/** « Opérateur A », « Opérateur B »… quand la personne n'est pas encore choisie. */
export function operatorLetter(index: number): string {
  return String.fromCharCode(65 + index);
}

export function operatorFallback(index: number): string {
  const letter = operatorLetter(index);
  return $localize`:@@spc.gage-rr.operator-n:Opérateur ${letter}:letter:`;
}

/** Le libellé d'une ligne du tableau d'ANOVA. */
export function anovaSourceLabel(s: AnovaSource, pooled = false): string {
  switch (s) {
    case 'PART':
      return $localize`:@@spc.gage-rr.anova.part:Pièces`;
    case 'OPERATOR':
      return $localize`:@@spc.gage-rr.anova.operator:Opérateurs`;
    case 'INTERACTION':
      return $localize`:@@spc.gage-rr.anova.interaction:Interaction pièce × opérateur`;
    case 'REPEATABILITY':
      return pooled
        ? $localize`:@@spc.gage-rr.anova.repeatability-pooled:Répétabilité (interaction regroupée)`
        : $localize`:@@spc.gage-rr.anova.repeatability:Répétabilité`;
    default:
      return $localize`:@@spc.gage-rr.anova.total:Total`;
  }
}

/** Un nombre à décimales fixes, ou un tiret quand il n'a pas pu se calculer. */
export function fmt(v: number | null | undefined, digits = 4): string {
  return v === null || v === undefined || !Number.isFinite(v) ? '–' : v.toFixed(digits);
}

/** Une fraction en pourcentage (0,21 → « 21.0 % »). */
export function pct(v: number | null | undefined, digits = 1): string {
  return v === null || v === undefined || !Number.isFinite(v) ? '–' : `${(v * 100).toFixed(digits)} %`;
}

/** Une p-value lisible : notation scientifique sous 0,0001. */
export function pValue(p: number | null): string {
  if (p === null || !Number.isFinite(p)) return '–';
  return p < 1e-4 ? p.toExponential(2) : p.toFixed(4);
}

/**
 * Le texte d'interprétation (cellules A42 à A46 de l'onglet « Résultats »),
 * recomposé à chaque calcul.
 */
export function interpretation(c: GageRrCalculationView): string[] {
  const ev = fmt(c.ev.value);
  const evPct = (c.ev.percentOfTotal * 100).toFixed(1);
  const av = fmt(c.av.value);
  const avPct = (c.av.percentOfTotal * 100).toFixed(1);
  const xdiff = fmt(c.xDiff);
  const grr = fmt(c.grr.value);
  const grrPct = (c.grr.percentOfTotal * 100).toFixed(1);
  const tolPct = c.grr.percentTolerance === null ? '–' : (c.grr.percentTolerance * 100).toFixed(1);
  const decision = decisionLabel(c.decision);
  const ndc = c.ndc;
  return [
    $localize`:@@spc.gage-rr.read.ev:Répétabilité (EV) = ${ev}:ev:, soit ${evPct}:pct: % de la variation totale : dispersion d'un même opérateur mesurant plusieurs fois la même pièce avec le même instrument.`,
    $localize`:@@spc.gage-rr.read.av:Reproductibilité (AV) = ${av}:av:, soit ${avPct}:pct: % : écart entre opérateurs (X̄diff = ${xdiff}:xdiff:).`,
    $localize`:@@spc.gage-rr.read.grr:GRR = ${grr}:grr: : ${grrPct}:pct: % de la variation totale et ${tolPct}:tol: % de la tolérance. Décision : ${decision}:decision:.`,
    c.ndcSufficient
      ? $localize`:@@spc.gage-rr.read.ndc-ok:Le système distingue ${ndc}:ndc: catégories de pièces (ndc). C'est suffisant pour piloter un procédé.`
      : $localize`:@@spc.gage-rr.read.ndc-ko:Le système distingue ${ndc}:ndc: catégories de pièces (ndc). C'est insuffisant : améliorer l'instrument ou la méthode.`,
    c.repeatabilityDominant
      ? $localize`:@@spc.gage-rr.read.dominant-ev:Le poste dominant est la répétabilité (instrument ou méthode) : vérifier la résolution, la fixation de la pièce, l'état de l'instrument.`
      : $localize`:@@spc.gage-rr.read.dominant-av:Le poste dominant est la reproductibilité (opérateurs) : harmoniser la méthode de mesure, former les opérateurs, vérifier la lisibilité de l'instrument.`
  ];
}

/** Ce que le serveur a refusé, dit à l'écran sans reprendre son texte (OWASP A09). */
export function fieldMessage(field: string): string {
  switch (field) {
    case 'partReference':
      return $localize`:@@spc.gage-rr.error.part:La référence de la pièce est obligatoire (120 caractères au plus).`;
    case 'studyDate':
      return $localize`:@@spc.gage-rr.error.date:La date de l'étude est obligatoire.`;
    case 'lsl':
    case 'usl':
      return $localize`:@@spc.gage-rr.error.limits:La limite supérieure (USL) doit dépasser la limite inférieure (LSL).`;
    case 'operators':
      return $localize`:@@spc.gage-rr.error.operators:Choisissez chaque opérateur dans l'annuaire, sans doublon.`;
    case 'responsibleName':
    case 'responsibleUserId':
      return $localize`:@@spc.gage-rr.error.responsible:Le responsable choisi n'a pas pu être retrouvé dans l'annuaire.`;
    case 'operatorCount':
    case 'partCount':
    case 'trialCount':
    case 'sigmaMultiplier':
      return $localize`:@@spc.gage-rr.error.plan:Le plan doit compter 2 ou 3 opérateurs, 2 à 10 pièces, 2 ou 3 essais, à 5,15 ou 6 σ.`;
    case 'measurements':
      return $localize`:@@spc.gage-rr.error.measurements:Toutes les mesures de la grille sont obligatoires, et elles ne doivent pas être toutes identiques.`;
    default:
      return $localize`:@@spc.gage-rr.error.generic:Un champ de l'étude a été refusé : vérifiez la saisie.`;
  }
}

import { CoqCategory } from './cost-of-quality.types';

/**
 * Les textes affichés du catalogue livré.
 *
 * <p>Le serveur range un libellé livré sous un `code` stable ; c'est ici qu'il
 * devient un texte dans la langue de l'écran. Un libellé saisi par le client
 * n'a pas de code : on affiche alors le texte tapé, tel quel.
 */
const BUILT_IN: Record<string, string> = {
  PREVENTION_TRAINING: $localize`:@@coq.label.prevention-training:Formation qualité du personnel`,
  PREVENTION_PLANNING: $localize`:@@coq.label.prevention-planning:Planification et système qualité`,
  PREVENTION_INTERNAL_AUDITS: $localize`:@@coq.label.prevention-audits:Audits qualité internes`,
  PREVENTION_DESIGN_REVIEW: $localize`:@@coq.label.prevention-design-review:Revue de conception / AMDEC`,
  APPRAISAL_INCOMING_INSPECTION: $localize`:@@coq.label.appraisal-incoming:Contrôle réception matières`,
  APPRAISAL_IN_PROCESS: $localize`:@@coq.label.appraisal-in-process:Inspections en cours de production`,
  APPRAISAL_TESTING_CALIBRATION: $localize`:@@coq.label.appraisal-testing:Essais et étalonnage`,
  APPRAISAL_PRODUCT_AUDIT: $localize`:@@coq.label.appraisal-product-audit:Audit produit / process`,
  INTERNAL_SCRAP: $localize`:@@coq.label.internal-scrap:Rebuts et mise au rebut`,
  INTERNAL_REWORK: $localize`:@@coq.label.internal-rework:Retouches et réparations`,
  INTERNAL_REINSPECTION: $localize`:@@coq.label.internal-reinspection:Re-contrôles après retouche`,
  INTERNAL_DOWNTIME: $localize`:@@coq.label.internal-downtime:Arrêts de production liés aux défauts`,
  EXTERNAL_COMPLAINTS: $localize`:@@coq.label.external-complaints:Réclamations et traitement litiges`,
  EXTERNAL_RETURNS: $localize`:@@coq.label.external-returns:Retours produits et remplacements`,
  EXTERNAL_WARRANTY: $localize`:@@coq.label.external-warranty:Garanties et interventions terrain`,
  EXTERNAL_PENALTIES: $localize`:@@coq.label.external-penalties:Pénalités contractuelles`
};

/** Le texte d'un libellé : sa traduction s'il est livré, son nom sinon. */
export function coqLabelText(code: string | null | undefined, name: string | null | undefined): string {
  return (code && BUILT_IN[code]) || name || '';
}

export interface CoqCategoryText {
  /** Pastille de la carte, comme sur la maquette : P, A, IF, EF. */
  badge: string;
  title: string;
  description: string;
}

export const CATEGORY_TEXT: Record<CoqCategory, CoqCategoryText> = {
  PREVENTION: {
    badge: $localize`:@@coq.cat.prevention-badge:P`,
    title: $localize`:@@coq.cat.prevention-title:Coûts de prévention`,
    description: $localize`:@@coq.cat.prevention-desc:Dépenses engagées en amont pour empêcher l'apparition de défauts : formation, planification qualité, conception, fiabilisation des processus.`
  },
  APPRAISAL: {
    badge: $localize`:@@coq.cat.appraisal-badge:A`,
    title: $localize`:@@coq.cat.appraisal-title:Coûts d'appréciation (détection)`,
    description: $localize`:@@coq.cat.appraisal-desc:Dépenses liées à la vérification de la conformité : contrôles, inspections et essais qui détectent les défauts avant livraison.`
  },
  INTERNAL_FAILURE: {
    badge: $localize`:@@coq.cat.internal-badge:IF`,
    title: $localize`:@@coq.cat.internal-title:Coûts des anomalies internes`,
    description: $localize`:@@coq.cat.internal-desc:Coûts des défauts détectés avant livraison au client : rebuts, retouches, arrêts de production liés à la non-qualité.`
  },
  EXTERNAL_FAILURE: {
    badge: $localize`:@@coq.cat.external-badge:EF`,
    title: $localize`:@@coq.cat.external-title:Coûts des anomalies externes`,
    description: $localize`:@@coq.cat.external-desc:Coûts des défauts découverts après livraison : réclamations, retours, garanties et impact sur la relation client.`
  }
};

/** Les devises proposées. Le serveur accepte tout code ISO 4217 ; l'écran propose les plus courantes. */
export const CURRENCIES = ['EUR', 'USD', 'GBP', 'CHF', 'CAD', 'MAD', 'XOF', 'XAF', 'TND', 'DZD', 'JPY', 'CNY'];

/**
 * Référentiel APQP — les cinq phases et leurs livrables.
 *
 * <p>APQP (Advanced Product Quality Planning) ordonne la planification qualité
 * d'un produit, du recueil du besoin client jusqu'au retour d'expérience. Les
 * cinq phases et la liste des livrables viennent du manuel AIAG ; elles ne sont
 * pas propres à QualitOS et ne se réinventent pas par tenant.
 *
 * <p><b>Ces libellés ne passent pas par i18n</b>, au même titre que les barèmes
 * de {@link ../fmea/fmea.reference}. Un livrable normatif traduit librement
 * n'est plus le même livrable : « Control Plan » désigne un document précis,
 * pas « un plan de contrôle » quelconque. Seule l'interface qui les entoure est
 * traduite. Les intitulés portent donc leur nom français d'usage dans
 * l'industrie, avec le terme d'origine quand il fait référence.
 *
 * <p>Base volontairement simple : la structure normative, et rien de plus. Ce
 * qui la personnalise — livrables ajoutés, responsables, échéances, preuves —
 * viendra par-dessus, et devra alors vivre en base et non ici.
 */

/** Une phase du cycle APQP. */
export interface ApqpPhase {
  /** Segment d'URL, stable : il est écrit dans les liens et les favoris. */
  readonly slug: string;
  /** Rang de 1 à 5, tel que le manuel les numérote. */
  readonly numero: number;
  /** Intitulé court, celui du schéma. */
  readonly titre: string;
  /** Ce que la phase établit — une phrase, pas un paragraphe. */
  readonly objet: string;
  /** La question à laquelle la phase répond, pour situer d'un coup d'œil. */
  readonly question: string;
  /** Les livrables attendus en sortie de phase. */
  readonly livrables: readonly string[];
}

export const APQP_PHASES: readonly ApqpPhase[] = [
  {
    slug: 'planification',
    numero: 1,
    titre: 'Planifier et définir',
    objet: "Traduire la voix du client en objectifs de conception mesurables.",
    question: "Que demande le client, et qu'est-ce que cela impose au produit ?",
    livrables: [
      'Voix du client (attentes, réclamations, retours de garantie)',
      "Plan d'affaires et stratégie marketing",
      'Étude comparative produit et processus (benchmark)',
      'Hypothèses produit et processus',
      'Études de fiabilité produit',
      'Objectifs de conception',
      'Objectifs de fiabilité et de qualité',
      'Nomenclature préliminaire',
      'Schéma de flux du processus préliminaire',
      'Liste préliminaire des caractéristiques spéciales',
      "Plan d'assurance produit",
      'Engagement de la direction'
    ]
  },
  {
    slug: 'conception-produit',
    numero: 2,
    titre: 'Conception du produit',
    objet: "Figer une conception fabricable, vérifiée et documentée.",
    question: "Le produit tel que dessiné tient-il ses objectifs, et sait-on le fabriquer ?",
    livrables: [
      'AMDEC produit (DFMEA)',
      "Conception pour la fabrication et l'assemblage",
      'Vérification de la conception',
      'Revues de conception',
      'Plan de surveillance prototype',
      "Dessins et spécifications d'ingénierie",
      'Spécifications matières',
      'Modifications de dessins et de spécifications',
      "Exigences en équipements, outillages et moyens de contrôle",
      'Caractéristiques spéciales produit et processus',
      "Engagement de faisabilité de l'équipe"
    ]
  },
  {
    slug: 'conception-processus',
    numero: 3,
    titre: 'Conception du processus',
    objet: "Définir le processus de fabrication et ce qui le surveillera.",
    question: "Comment fabrique-t-on, et comment saura-t-on que c'est conforme ?",
    livrables: [
      "Normes d'emballage",
      'Revue du système qualité produit et processus',
      'Schéma de flux du processus',
      "Plan d'implantation des postes",
      'Matrice des caractéristiques',
      'AMDEC processus (PFMEA)',
      'Plan de surveillance de pré-lancement',
      'Instructions de travail',
      "Plan d'analyse des systèmes de mesure",
      'Plan des études de capabilité préliminaires',
      'Soutien de la direction'
    ]
  },
  {
    slug: 'validation',
    numero: 4,
    titre: 'Validation',
    objet: "Prouver sur une production réelle que le processus tient ses capabilités.",
    question: "Le processus réel, aux cadences réelles, produit-il conforme ?",
    livrables: [
      'Essai de production significative',
      'Analyse des systèmes de mesure (MSA)',
      'Étude de capabilité préliminaire du processus',
      'Approbation des pièces de production (PPAP)',
      'Essais de validation de production',
      "Évaluation de l'emballage",
      'Plan de surveillance de production',
      'Clôture de la planification qualité'
    ]
  },
  {
    slug: 'production-serie',
    numero: 5,
    titre: 'Production série et retour d’expérience',
    objet: "Produire en série, mesurer ce que le client constate, et réduire la variation restante.",
    question: "Ce qui sort de la ligne satisfait-il le client, et que corrige-t-on ?",
    livrables: [
      'Réduction de la variation',
      'Satisfaction client',
      'Performance de livraison et de service',
      'Leçons apprises et bonnes pratiques'
    ]
  }
];

/** La phase portant ce segment d'URL, ou `undefined` si le segment est inconnu. */
export function phaseParSlug(slug: string | null): ApqpPhase | undefined {
  return APQP_PHASES.find(p => p.slug === slug);
}

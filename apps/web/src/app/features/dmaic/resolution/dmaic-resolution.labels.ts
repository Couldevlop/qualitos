import { ActionStatus, AnswerCode, AnswerType, DefineTopic, SheetSection, WhyBranch } from './dmaic-resolution.types';

/**
 * Les mots de la fiche de résolution. Le serveur rend des codes (`YES`,
 * `CONTAIN`, `IN_REVIEW`) ; ils deviennent ici des mots dans la langue de
 * l'écran. Un code inconnu s'affiche tel quel.
 */

export interface SectionText {
  /** Lettre de la phase, comme dans le classeur (le confinement et les causes partagent le « A »). */
  letter: string;
  title: string;
  /** La phrase qui ouvre la phase dans le classeur. */
  lead: string;
  /** L'explication longue de la feuille « Explication », repliée par défaut. */
  more: string;
  /** Le libellé de la synthèse de la phase, ou null quand la phase n'en a pas. */
  summary: string | null;
}

export const SECTION_TEXT: Readonly<Record<SheetSection, SectionText>> = {
  DEFINE: {
    letter: 'D',
    title: $localize`:@@dmaic.res.section.define:Définir`,
    lead: $localize`:@@dmaic.res.section.define-lead:Un problème bien défini est un problème à moitié résolu.`,
    more: $localize`:@@dmaic.res.section.define-more:Les problèmes viennent des indicateurs internes, des retours des clients et des employés, des vérifications par rapport aux normes. Nommez l'équipe, le rôle de chacun et les limites de son champ d'action. L'énoncé du problème fixe la portée du travail et dit ce qui est attendu de l'équipe. Allez voir sur place, parlez avec les gens, recueillez des faits, visualisez le problème.`,
    summary: null
  },
  MEASURE: {
    letter: 'M',
    title: $localize`:@@dmaic.res.section.measure:Mesurer`,
    lead: $localize`:@@dmaic.res.section.measure-lead:Mettre en place les moyens de mesure, pour collecter les données qui démontreront l'atteinte des objectifs.`,
    more: $localize`:@@dmaic.res.section.measure-more:Les systèmes de mesure et les données de cette phase servent deux fois : à trouver la cause racine du problème, puis à démontrer l'efficacité de la démarche.`,
    summary: $localize`:@@dmaic.res.section.measure-summary:Synthèse des mesures`
  },
  CONTAIN: {
    letter: 'A',
    title: $localize`:@@dmaic.res.section.contain:Analyser — contenir le problème`,
    lead: $localize`:@@dmaic.res.section.contain-lead:Mesures conservatoires pour contenir le problème jusqu'à ce que les actions correctives définitives soient en place.`,
    more: $localize`:@@dmaic.res.section.contain-more:Le confinement passe avant l'analyse : tant que la cause n'est pas traitée, il protège le client des produits suspects, y compris ceux encore en fabrication.`,
    summary: $localize`:@@dmaic.res.section.contain-summary:Mesures conservatoires`
  },
  ANALYZE: {
    letter: 'A',
    title: $localize`:@@dmaic.res.section.analyze:Analyser — les causes`,
    lead: $localize`:@@dmaic.res.section.analyze-lead:Trouver les causes racines est le cœur de la résolution. Testez chaque cause possible plutôt que de traiter les symptômes.`,
    more: $localize`:@@dmaic.res.section.analyze-more:Les causes apparentes sont souvent des symptômes ; les causes racines, celles qui les ont permis, sont ancrées dans le processus. Une fois connues, les solutions deviennent souvent évidentes. Tenez-vous loin de l'« erreur humaine » comme cause : c'est le signe qu'un processus n'était pas assez clair.`,
    summary: $localize`:@@dmaic.res.section.analyze-summary:Cause racine`
  },
  IMPROVE: {
    letter: 'I',
    title: $localize`:@@dmaic.res.section.improve:Améliorer`,
    lead: $localize`:@@dmaic.res.section.improve-lead:Choisir la meilleure solution, ou un ensemble de solutions robustes et rentables, et piloter leur mise en place par le plan d'actions.`,
    more: $localize`:@@dmaic.res.section.improve-more:Développez autant de solutions que possible, avec des personnes aux points de vue différents, sans les juger trop tôt. Si aucune solution n'apparaît, la cause racine n'est sans doute pas trouvée. Le plan d'actions dit ce qui doit être fait, par qui et pour quand.`,
    summary: $localize`:@@dmaic.res.section.improve-summary:Solutions retenues`
  },
  CONTROL: {
    letter: 'C',
    title: $localize`:@@dmaic.res.section.control:Contrôler`,
    lead: $localize`:@@dmaic.res.section.control-lead:Vérifier et valider que le problème est contenu dans la durée, avec un plan de surveillance qui soutient l'amélioration.`,
    more: $localize`:@@dmaic.res.section.control-more:Vérifier, c'est tester que la solution produit le résultat voulu ; valider, c'est s'assurer qu'elle résout vraiment le problème. Quand c'est acquis, l'équipe mérite d'être reconnue pour son travail.`,
    summary: $localize`:@@dmaic.res.section.control-summary:Plan de surveillance`
  }
};

export const TOPIC_LABEL: Readonly<Record<DefineTopic, string>> = {
  WHAT: $localize`:@@dmaic.res.topic.what:Quoi`,
  WHO: $localize`:@@dmaic.res.topic.who:Qui`,
  WHY: $localize`:@@dmaic.res.topic.why:Pourquoi`,
  WHERE: $localize`:@@dmaic.res.topic.where:Où`,
  WHEN: $localize`:@@dmaic.res.topic.when:Quand`,
  HOW_MANY: $localize`:@@dmaic.res.topic.how-many:Combien`
};

const ANSWER_LABEL: Readonly<Record<AnswerCode, string>> = {
  YES: $localize`:@@dmaic.res.answer.yes:Oui`,
  NO: $localize`:@@dmaic.res.answer.no:Non`,
  NA: $localize`:@@dmaic.res.answer.na:Sans objet`,
  UNKNOWN: $localize`:@@dmaic.res.answer.unknown:Inconnu`
};

export const ANSWER_TYPE_LABEL: Readonly<Record<AnswerType, string>> = {
  TEXT: $localize`:@@dmaic.res.answer-type.text:Texte libre`,
  YES_NO: $localize`:@@dmaic.res.answer-type.yes-no:Oui / Non`,
  YES_NO_NA: $localize`:@@dmaic.res.answer-type.yes-no-na:Oui / Non / Sans objet`,
  YES_NO_UNKNOWN: $localize`:@@dmaic.res.answer-type.yes-no-unknown:Oui / Non / Inconnu`
};

export const BRANCH_TEXT: Readonly<Record<WhyBranch, { title: string; question: string }>> = {
  OCCURRENCE: {
    title: $localize`:@@dmaic.res.branch.occurrence:Apparition`,
    question: $localize`:@@dmaic.res.branch.occurrence-q:Pourquoi le défaut est-il apparu ?`
  },
  DETECTION: {
    title: $localize`:@@dmaic.res.branch.detection:Non-détection`,
    question: $localize`:@@dmaic.res.branch.detection-q:Pourquoi a-t-il échappé jusqu'au client ?`
  },
  SYSTEMIC: {
    title: $localize`:@@dmaic.res.branch.systemic:Système`,
    question: $localize`:@@dmaic.res.branch.systemic-q:Pourquoi le système l'a-t-il permis ?`
  }
};

const ACTION_STATUS_LABEL: Readonly<Record<ActionStatus, string>> = {
  OPEN: $localize`:@@dmaic.res.action-status.open:Ouvert`,
  IN_PROGRESS: $localize`:@@dmaic.res.action-status.in-progress:En cours`,
  IN_REVIEW: $localize`:@@dmaic.res.action-status.in-review:En revue`,
  CLOSED: $localize`:@@dmaic.res.action-status.closed:Clos`
};

export const answerLabel = (code: string): string => ANSWER_LABEL[code as AnswerCode] ?? code;
export const actionStatusLabel = (code: string): string => ACTION_STATUS_LABEL[code as ActionStatus] ?? code;

/** Les cinq phases du projet, telles que le bandeau les nomme. */
export const PHASE_TEXT: Readonly<Record<string, { letter: string; label: string }>> = {
  DEFINE: { letter: 'D', label: $localize`:@@dmaic.res.phase.define:Définir` },
  MEASURE: { letter: 'M', label: $localize`:@@dmaic.res.phase.measure:Mesurer` },
  ANALYZE: { letter: 'A', label: $localize`:@@dmaic.res.phase.analyze:Analyser` },
  IMPROVE: { letter: 'I', label: $localize`:@@dmaic.res.phase.improve:Améliorer` },
  CONTROL: { letter: 'C', label: $localize`:@@dmaic.res.phase.control:Contrôler` }
};

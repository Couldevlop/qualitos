/**
 * Referentiel FMEA - extrait du classeur de reference qualite (baremes de
 * cotation, feuille 4, et exemple de PFMEA, feuille 2).
 *
 * <p>Ces tables ne sont pas des donnees de tenant : ce sont les echelles sur
 * lesquelles se cotent Severite, Occurrence et Detection. Sans elles a portee
 * d'ecran, chaque evaluateur invente son propre 1 a 10 et les RPN de deux
 * analyses cessent d'etre comparables.
 *
 * <p>Le contenu est reproduit dans la langue du referentiel d'origine
 * (anglais) : une echelle de cotation traduite librement n'est plus la meme
 * echelle. Seule l'interface qui l'entoure est traduite.
 *
 * <p>FICHIER GENERE par scripts/gen-fmea-reference.py depuis
 * « docs/QUALITOS BACKLOG.xlsx » - ne pas editer a la main : corriger le
 * classeur, puis regenerer.
 */

/** Une ligne du bareme de severite. */
export interface FmeaSeverityRow { effect: string; description: string; score: number; }

/** Une ligne du bareme de detection. */
export interface FmeaDetectionRow { chance: string; description: string; score: number; }

/**
 * Une ligne du bareme d'occurrence. Le classeur FUSIONNE l'intitule sur
 * plusieurs scores (« Moderate » couvre 6, 5 et 4) ; il est recopie sur chaque
 * ligne, parce qu'un score sans nom ne se cote pas.
 */
export interface FmeaOccurrenceRow {
  probability: string; timePeriod: string; failureRate: string; score: number;
}

/** Une ligne de l'exemple de PFMEA fourni comme modele. */
export interface FmeaExampleRow {
  step: string; failureMode: string; effects: string; severity: number;
  causes: string; occurrence: number; controls: string; detection: number;
  rpn: number; recommendedAction: string; responsible: string;
}

/** Severite : gravite de l'effet pour le client, de 10 (danger) a 1 (aucun effet). */
export const FMEA_SEVERITY_SCALE: ReadonlyArray<FmeaSeverityRow> = [
  { effect: "Hazardous - Without Warning", description: "May expose client to loss or harm without warning.", score: 10 },
  { effect: "Hazardous - With Warning", description: "May expose client to loss or harm with some warning.", score: 9 },
  { effect: "Very High", description: "Will cause major disruption of service directly affecting a client.", score: 8 },
  { effect: "High", description: "Minor disruption of service directly affecting a client.", score: 7 },
  { effect: "Moderate", description: "Major disruption of service not involving a client directly.", score: 6 },
  { effect: "Low", description: "Minor disruption of service not involving a client.", score: 5 },
  { effect: "Very Low", description: "Minor disruption of service involving client that doesn't require reworking or inconvenience to client.", score: 4 },
  { effect: "Minor", description: "Minor disruption of service not involving client that doesn't require reworking or inconvenience to client.", score: 3 },
  { effect: "Very Minor", description: "No disruption of service noticed by client, no rework necessary.", score: 2 },
  { effect: "None", description: "No Effect", score: 1 },
];

/** Detection : chance de reperer la defaillance avec les controles en place. */
export const FMEA_DETECTION_SCALE: ReadonlyArray<FmeaDetectionRow> = [
  { chance: "Nearly impossible", description: "No current way to detect failure", score: 10 },
  { chance: "Very Remote", description: "Very remote likelihood of detecting failure.", score: 9 },
  { chance: "Remote", description: "Remote likelihood of detecting failure.", score: 8 },
  { chance: "Very Low", description: "Very low likelihood of detecting failure.", score: 7 },
  { chance: "Low", description: "Low likelihood of detecting failure.", score: 6 },
  { chance: "Moderate", description: "Moderate likelihood of detecting failure.", score: 5 },
  { chance: "Moderately High", description: "Moderately high likelihood of detecting failure.", score: 4 },
  { chance: "High", description: "High likelihood of detecting failure.", score: 3 },
  { chance: "Very High", description: "Very high likelihood of detecting failure.", score: 2 },
  { chance: "Nearly Certain", description: "Near certain likelihood of detecting failure.", score: 1 },
];

/** Occurrence : frequence attendue de la defaillance. */
export const FMEA_OCCURRENCE_SCALE: ReadonlyArray<FmeaOccurrenceRow> = [
  { probability: "Very High", timePeriod: "More than once per day", failureRate: "> 1 in 2", score: 10 },
  { probability: "Very High", timePeriod: "Once every 3-4 days", failureRate: "1 in 3000", score: 9 },
  { probability: "High", timePeriod: "Once every week", failureRate: "1 in 8", score: 8 },
  { probability: "High", timePeriod: "Once every month", failureRate: "1 in 20", score: 7 },
  { probability: "Moderate", timePeriod: "Once every 3 months", failureRate: "1 in 800", score: 6 },
  { probability: "Moderate", timePeriod: "Once every 6 months", failureRate: "1 in 400", score: 5 },
  { probability: "Moderate", timePeriod: "Once a year", failureRate: "1 in 800", score: 4 },
  { probability: "Low", timePeriod: "Once every 1 - 3 years", failureRate: "1 in 1500", score: 3 },
  { probability: "Very Low", timePeriod: "Once every 3 - 6 years", failureRate: "1 in 3000", score: 2 },
  { probability: "Remote", timePeriod: "Once Every 7+ Years", failureRate: "1 in 6000", score: 1 },
];

/**
 * Exemple complet de PFMEA (faisceau electrique aeronautique), fourni comme
 * modele de redaction : ce qu'on attend dans « mode de defaillance », dans
 * « effets », et a quoi ressemble une action recommandee qui engage vraiment.
 */
export const FMEA_EXAMPLE_TITLE = "Electrical Wiring Harness - Aircraft Installation (PFMEA)";
export const FMEA_EXAMPLE_ROWS: ReadonlyArray<FmeaExampleRow> = [
  {
    step: "Wire cutting to length",
    failureMode: "Wire cut too short or too long",
    effects: "Improper fit in harness; wire scrapped or spliced, weakening connection",
    severity: 8,
    causes: "Incorrect cut-length program; uncalibrated cutting machine",
    occurrence: 4,
    controls: "Operator measures sample with calipers",
    detection: 5,
    rpn: 160,
    recommendedAction: "Automate length verification with in-line laser measurement",
    responsible: "Manufacturing Eng"
  },
  {
    step: "Wire stripping",
    failureMode: "Insulation nicked or conductor strands cut",
    effects: "Exposed/weakened conductor; short circuit or wire breakage in service",
    severity: 9,
    causes: "Worn stripper blades; incorrect strip-length setting",
    occurrence: 4,
    controls: "Periodic visual blade inspection",
    detection: 6,
    rpn: 216,
    recommendedAction: "Add blade wear monitoring and scheduled replacement interval",
    responsible: "Quality Eng"
  },
  {
    step: "Crimping terminal to wire",
    failureMode: "Under-crimp or over-crimp (out-of-spec crimp height)",
    effects: "Intermittent connection, arcing, connector failure in flight",
    severity: 10,
    causes: "Wrong crimp die selected; crimper out of calibration",
    occurrence: 3,
    controls: "Sample pull-test per lot",
    detection: 6,
    rpn: 180,
    recommendedAction: "100% crimp-height inspection with automated crimp-force monitor",
    responsible: "Process Eng"
  },
  {
    step: "Connector pin insertion",
    failureMode: "Pin not fully seated or backs out of connector",
    effects: "Intermittent or open circuit at connector",
    severity: 9,
    causes: "Operator error; missing/worn insertion tool",
    occurrence: 4,
    controls: "Visual inspection",
    detection: 5,
    rpn: 180,
    recommendedAction: "Use go/no-go pin insertion and retention gauge on every pin",
    responsible: "Production"
  },
  {
    step: "Wire routing and harness assembly",
    failureMode: "Wires routed incorrectly per drawing",
    effects: "Chafing against airframe structure; wear-through and short circuit",
    severity: 9,
    causes: "Outdated routing drawing; operator misreads routing diagram",
    occurrence: 3,
    controls: "Routing checklist sign-off",
    detection: 5,
    rpn: 135,
    recommendedAction: "Introduce laser-guided routing fixture referencing latest drawing",
    responsible: "Manufacturing Eng"
  },
  {
    step: "Cable clamping and bundling",
    failureMode: "Clamp installed too tight or too loose",
    effects: "Insulation damage, or wire movement leading to chafing",
    severity: 8,
    causes: "Incorrect torque applied; wrong clamp size used",
    occurrence: 4,
    controls: "Torque wrench with spec value",
    detection: 6,
    rpn: 192,
    recommendedAction: "Standardize clamp-size chart and add torque audit",
    responsible: "Quality Eng"
  },
  {
    step: "Shielding / braid termination",
    failureMode: "Incomplete 360-degree shield termination",
    effects: "EMI susceptibility, interference with avionics signals",
    severity: 8,
    causes: "Insufficient operator training on braid preparation",
    occurrence: 3,
    controls: "Visual inspection",
    detection: 6,
    rpn: 144,
    recommendedAction: "Add hands-on shield-termination training and destructive sample checks",
    responsible: "Training / Quality"
  },
  {
    step: "Heat-shrink sleeve application",
    failureMode: "Insufficient heat applied; sleeve not fully sealed",
    effects: "Moisture ingress leading to corrosion at splice/termination",
    severity: 7,
    causes: "Incorrect heat-gun temperature or dwell time",
    occurrence: 4,
    controls: "Operator visual judgment",
    detection: 6,
    rpn: 168,
    recommendedAction: "Use calibrated heat station with fixed process parameters",
    responsible: "Process Eng"
  },
  {
    step: "Continuity / electrical test",
    failureMode: "Test escape - open or short circuit not detected",
    effects: "Latent defect installed on aircraft; potential in-flight failure",
    severity: 10,
    causes: "Test program error; missing test point in fixture",
    occurrence: 2,
    controls: "Automated continuity tester",
    detection: 4,
    rpn: 80,
    recommendedAction: "Validate test program at first article and after every revision",
    responsible: "Test Eng"
  },
  {
    step: "Connector back-shell potting",
    failureMode: "Potting compound incompletely cured",
    effects: "Moisture/vibration-induced connection failure over time",
    severity: 8,
    causes: "Incorrect mix ratio or insufficient cure time",
    occurrence: 3,
    controls: "Batch mixing log",
    detection: 6,
    rpn: 144,
    recommendedAction: "Add automated dispense ratio control and cure-time monitoring",
    responsible: "Process Eng"
  },
  {
    step: "Labeling and identification",
    failureMode: "Wrong or missing wire/harness identification label",
    effects: "Misidentification during maintenance; wrong wire cut or reworked",
    severity: 6,
    causes: "Operator references outdated label list",
    occurrence: 4,
    controls: "Visual check against bill of materials",
    detection: 4,
    rpn: 96,
    recommendedAction: "Drive label printing from barcode-linked work order",
    responsible: "Production"
  },
  {
    step: "Final dimensional and visual inspection",
    failureMode: "Defect missed during final inspection",
    effects: "Defective harness shipped and installed on aircraft",
    severity: 9,
    causes: "Inspector fatigue; inadequate inspection criteria",
    occurrence: 3,
    controls: "Visual inspection per checklist",
    detection: 5,
    rpn: 135,
    recommendedAction: "Implement automated vision-assisted inspection system",
    responsible: "Quality Eng"
  },
  {
    step: "Packaging for shipment",
    failureMode: "Inadequate protection during packaging",
    effects: "Physical damage to connectors/wires in transit",
    severity: 6,
    causes: "Insufficient cushioning; incorrect packaging spec",
    occurrence: 3,
    controls: "Standard packaging instructions",
    detection: 5,
    rpn: 90,
    recommendedAction: "Revise packaging spec to include connector caps and rigid trays",
    responsible: "Logistics"
  },
  {
    step: "Documentation and traceability",
    failureMode: "Incomplete certificate of conformance or traveler",
    effects: "Non-conformance to airworthiness traceability requirements; delayed acceptance",
    severity: 7,
    causes: "Manual data entry error; incomplete traveler fields",
    occurrence: 3,
    controls: "Manual review by QA",
    detection: 4,
    rpn: 84,
    recommendedAction: "Implement digital traveler with mandatory fields and e-signature",
    responsible: "Quality Assurance"
  },
  {
    step: "Installation of harness onto aircraft structure",
    failureMode: "Harness improperly secured or bend radius below minimum",
    effects: "Wire fatigue failure; chafing leading to in-flight electrical fault",
    severity: 10,
    causes: "Installer does not follow minimum bend radius spec; missing standoffs",
    occurrence: 3,
    controls: "Installation checklist",
    detection: 5,
    rpn: 150,
    recommendedAction: "Engineering review of installation drawings with bend-radius verification",
    responsible: "Manufacturing Eng"
  },
];

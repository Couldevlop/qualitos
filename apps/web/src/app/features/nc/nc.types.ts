import { TeamMemberRef } from '../../shared/user-picker/user-team-picker.component';
import { OpenCapaRequest } from '../capa/capa.types';
import { SpringPage } from '../pdca/pdca.types';

/**
 * Borne haute d'une équipe (miroir de `NcDto.MAX_TEAM_MEMBERS`) : au-delà d'une
 * dizaine de personnes, ce n'est plus une équipe de résolution mais une liste de
 * diffusion, et le D1 du rapport 8D devient illisible.
 */
export const NC_MAX_TEAM_MEMBERS = 12;

export type { TeamMemberRef };

export type NcCategory =
  | 'PRODUCT'
  | 'PROCESS'
  | 'DOCUMENTATION'
  | 'SUPPLIER'
  | 'SAFETY'
  | 'ENVIRONMENT'
  | 'OTHER';

export type NcSeverity = 'MINOR' | 'MAJOR' | 'CRITICAL';

/**
 * Une NATURE d'écart, sous sa catégorie : « Cote hors spécification » sous
 * Produit, « Étalonnage hors validité » sous Processus.
 *
 * <p>C'est une donnée du client, pas une énumération du code : les natures
 * d'écart d'une usine ne sont pas celles d'un CHU, et la plateforme livre
 * seulement un catalogue de départ transverse (§18.2 #9).
 */
export interface NcSubcategory {
  code: string;
  category: NcCategory;
  label: string;
}

/**
 * Les natures d'une même famille, rangées sous elle.
 *
 * <p>Rendu en {@code mat-optgroup} par les deux écrans qui font choisir une
 * nature — le filtre du registre et la fenêtre de création. Soixante natures à
 * plat ne se parcourent pas : les titres de famille disent où l'on pioche.
 */
export interface NcSubcategoryGroup {
  category: NcCategory;
  subcategories: NcSubcategory[];
}

/** Le catalogue en vigueur et son origine. */
export interface NcSubcategoryCatalogue {
  entries: NcSubcategory[];
  /** Vrai quand ce sont les valeurs de la plateforme, faute de réglage du client. */
  defaults: boolean;
}

/**
 * D'où vient la non-conformité (§4.3). Une NC détectée par l'organisation
 * elle-même et une NC signalée du dehors — client, fournisseur, autorité,
 * certificateur — n'ont ni les mêmes délais ni les mêmes destinataires.
 */
export type NcOrigin = 'INTERNAL' | 'EXTERNAL';

export type NcStatus =
  | 'OPEN'
  | 'UNDER_ANALYSIS'
  | 'ACTION_DEFINED'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED'
  /**
   * Le constat a été instruit et écarté — réservé aux écarts signalés du dehors.
   *
   * <p>Distinct de `CANCELLED` : annuler dit « ce constat n'avait pas lieu
   * d'être », rejeter dit « il a été examiné, voici pourquoi il n'est pas
   * retenu ». C'est la seconde phrase qu'un client vient lire.
   */
  | 'REJECTED';

export interface NcResponse {
  id: string;
  tenantId?: string;
  reference: string;
  title: string;
  description?: string;
  category: NcCategory;
  /**
   * La nature fine de l'écart, par son code de catalogue. Absente sur les NC
   * saisies avant ce classement, ou que personne n'a encore classées.
   */
  subcategoryCode?: string;
  severity: NcSeverity;
  status: NcStatus;
  origin: NcOrigin;
  detectedAt: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne (chaîne unique côté backend). */
  photoUrls?: string;
  reporterId?: string;
  /**
   * Nom de la personne qui a signalé l'écart, figé au signalement par le
   * serveur. Absent sur les NC antérieures à cette donnée : la liste affiche
   * alors « — » plutôt qu'un identifiant qui n'apprend rien.
   */
  reporterName?: string;
  /** Produit concerné. Absent pour une NC de service ou administrative. */
  productId?: string;
  /**
   * Le client qui réclame, par le nom qu'il s'est donné. Absent sur un constat
   * interne — et sur les écarts saisis avant que la fiche sache le porter.
   */
  customerName?: string;
  /** Le lot incriminé, tel qu'il est marqué sur la pièce ou l'emballage. */
  batchNumber?: string;
  /**
   * Mode de défaillance du PFMEA que ce défaut illustre. Son absence est une
   * information en soi : un défaut que l'analyse n'avait pas prévu.
   */
  fmeaItemId?: string;
  capaCaseId?: string;
  /** Le cycle PDCA ouvert depuis cet ecart, s'il l'a ete. */
  pdcaCycleId?: string;
  /** Le projet DMAIC ouvert depuis cet ecart, s'il l'a ete. */
  dmaicProjectId?: string;
  rootCause?: string;
  /**
   * Le commentaire libre de l'analyse des causes (D4).
   *
   * <p>Ce qui ne tient ni dans un diagramme, ni dans une chaîne de pourquoi :
   * les observations, et les hypothèses écartées avec la raison de leur
   * abandon. Ne remplace pas `rootCause`, qui est la conclusion publiée.
   */
  causeAnalysisComment?: string;
  resolutionNote?: string;
  /** Pourquoi la réclamation a été écartée. */
  rejectionReason?: string;
  rejectedAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  /**
   * Quand l'écart a été REPRIS après avoir été refermé, s'il l'a été.
   *
   * <p>Ce qui l'avait refermé reste renseigné à côté : le bandeau de la fiche
   * montre le fait le plus récent, et l'historique ne perd rien.
   */
  reopenedAt?: string;
  /** Pourquoi il a été repris : récidive, relance du client, refus à tort. */
  reopenReason?: string;
  /**
   * Quand le SECOND REGARD a renvoyé le traitement au pilote, s'il l'a fait.
   *
   * <p>Ce n'est pas une reprise : l'écart n'avait pas été clos, son analyse
   * reste valable, et seul le traitement revient en chantier. Le fait reste
   * inscrit après la clôture qui a fini par venir — c'est la trace de ce que
   * le second regard a attrapé.
   */
  sentBackAt?: string;
  sentBackReason?: string;
  sentBackById?: string;
  sentBackByName?: string;
  /**
   * LE SECOND REGARD, en trois temps : qui a résolu, qui valide, qui a clos.
   *
   * <p>C'est ce qu'un auditeur vient lire — la preuve que la résolution et sa
   * validation ne portent pas la même signature (ISO 9001 §10.2). Les écarts
   * d'avant la règle n'ont pas ces noms, et la fiche n'en invente pas.
   */
  resolvedById?: string;
  resolvedByName?: string;
  closedById?: string;
  closedByName?: string;
  /**
   * La personne désignée pour porter ce second regard sur CET écart. Sans elle,
   * la clôture revient aux rôles de pilotage.
   */
  validatorId?: string;
  validatorName?: string;
  /**
   * CE QUI A ETE TRANCHE DE L'ANALYSE DE RISQUE, a la cloture.
   *
   * <p>`undefined` veut dire que la question n'a pas encore ete posee ;
   * `false` qu'elle l'a ete, et qu'on a juge que non. La premiere ne prouve
   * rien, la seconde est une decision -- et c'est elle que l'auditeur vient
   * lire quand le PFMEA n'a pas bouge.
   */
  pfmeaImpacted?: boolean;
  controlPlanImpacted?: boolean;
  createdAt: string;
  updatedAt: string;
  /**
   * L'équipe qui prend l'écart en charge, dans l'ordre où elle a été nommée —
   * le premier est le pilote. Vide sur les écarts saisis avant cette donnée, et
   * sur ceux qu'une machine a ouverts (constat d'audit, connecteur EHR) : c'est
   * à l'humain qui reprend la fiche de nommer l'équipe.
   */
  teamMembers?: TeamMemberRef[];
  /**
   * Ce qui s'oppose à l'avancement de l'écart, pour le dire AVANT le clic.
   * Absent sur les lignes d'une liste : le motif se lit sur la fiche.
   */
  blockers?: NcBlocker[];
  /**
   * TOUS les dossiers CAPA qui traitent cet écart, du plus ancien au plus
   * récent — `capaCaseId` n'en désigne que le premier.
   *
   * <p>Un second dossier ouvert depuis la liste en citant la référence de
   * l'écart ne remplace pas ce lien : ses actions retenaient donc la clôture
   * sans apparaître nulle part sur la fiche. Absent sur les lignes d'une liste.
   */
  capaCases?: NcCapaCaseRef[];
  /** true = saisie hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

/**
 * Un dossier CAPA de l'écart, réduit à ce que la fiche en montre.
 *
 * <p>Le titre et le statut viennent AVEC l'identifiant : sans eux, l'écran
 * devrait interroger chaque dossier pour écrire l'en-tête de son tableau
 * d'actions, et afficherait un identifiant en attendant.
 */
export interface NcCapaCaseRef {
  id: string;
  title?: string;
  status?: string;
  /** La note libre tenue à côté des preuves du dossier (D6) ; absente tant qu'on n'en a pas écrit. */
  effectivenessComment?: string;
  /**
   * Les actions de ce dossier qui RETIENNENT la clôture, chacune avec ce
   * qu'elle attend.
   *
   * <p>Le motif de blocage nommait le dossier ; un dossier porte autant
   * d'actions qu'on veut, et il fallait l'ouvrir pour trouver laquelle on
   * attendait. Vide dès que le dossier est réglé.
   */
  blockingActions?: NcBlockingAction[];
  /**
   * Le dossier ne porte QUE de l'endiguement.
   *
   * <p>Le D6 atteste les actions CORRECTIVES : une mesure d'endiguement
   * arrête l'effet et laisse la cause intacte, et sa preuve ne démontre rien
   * de ce que la discipline vient établir. Absent sur une réponse antérieure
   * à ce champ, ce qui se lit comme « on ne sait pas » : on montre alors le
   * dossier plutôt que de le cacher sur une supposition.
   */
  containmentOnly?: boolean;
}

/**
 * Une action qui retient la clôture, et la raison pour laquelle elle la
 * retient.
 *
 * <p>Deux raisons, les deux mêmes que la clôture du dossier oppose : le geste
 * n'a pas eu lieu, ou il a eu lieu sans que son effet soit vérifié (ADR 0132).
 */
export interface NcBlockingAction {
  id: string;
  title: string;
  reason: 'TO_DO' | 'TO_VERIFY' | string;
}

/**
 * CE QUE LA CLOTURE TRANCHE DE L'ANALYSE DE RISQUE.
 *
 * <p>Deux reponses obligatoires : l'ecart a-t-il impacte le PFMEA du produit,
 * et son plan de surveillance ? Un ecart traite enseigne quelque chose sur
 * l'analyse des risques, et l'IATF 16949 en fait une exigence -- toute
 * non-conformite se repercute sur la PFMEA et le plan de surveillance.
 *
 * <p>Les deux champs sont requis : un booleen absent vaudrait « non » en
 * silence, et l'ecart se refermerait sur une decision que personne n'a prise.
 */
export interface CloseNcRequest {
  pfmeaImpacted: boolean;
  controlPlanImpacted: boolean;
}

/** Les motifs que le serveur énumère ; l'écran les rédige. */
export type NcBlockerCode =
  | 'CAPA_ACTIONS_TO_CARRY_OUT'
  | 'CAPA_CASES_UNSETTLED'
  /** Aucun dossier CAPA ne traite l'écart : il n'y a rien à déclarer résolu. */
  | 'NO_CAPA_CASE'
  /**
   * Le SEUL motif qui dépende de qui lit la fiche : la personne qui a
   * résolu cet écart ne peut pas le clôturer. Le même écart n'en porte
   * aucun pour quelqu'un d'autre.
   */
  | 'SECOND_LOOK_REQUIRED'
  /**
   * L'ecart a ete declare impactant le PFMEA du produit, et aucune revision du
   * PFMEA n'a encore ete actee pour lui.
   *
   * <p>Ne nait pas d'une regle que l'ecran devine : nait de la REPONSE donnee a
   * la question posee a la cloture, et disparait des que la revision est
   * acceptee -- ou que la reponse change.
   */
  | 'PFMEA_REVISION_REQUIRED'
  /** Meme chose pour le plan de surveillance : les deux documents se revisent a part. */
  | 'CONTROL_PLAN_REVISION_REQUIRED';

/**
 * Un motif de blocage et son décompte.
 *
 * <p>Le code est ouvert à la lecture (`string`) : un serveur plus récent que
 * l'écran peut en renvoyer un que celui-ci ne connaît pas, et mieux vaut une
 * phrase générique qu'un bouton actif dont on ignore pourquoi il échouera.
 */
export interface NcBlocker {
  code: NcBlockerCode | string;
  count: number;
}

export type NcPage = SpringPage<NcResponse>;

/**
 * Photo binaire attachée à une NC (stockage objet, URL présignée 15 min).
 * Distincte des `photoUrls` texte (saisie libre / legacy / fallback offline).
 */
export interface NcPhoto {
  id: string;
  /** URL présignée d'accès (valide ~15 min) renvoyée par GET. */
  url?: string;
  /** Clé objet en stockage (présente à la création). */
  objectKey?: string;
  contentType: string;
  sizeBytes: number;
  originalFilename: string;
  createdAt: string;
}

export interface CreateNcRequest {
  title: string;
  description?: string;
  category: NcCategory;
  /** La nature fine, par son code de catalogue. Facultative : on classe finement après. */
  subcategoryCode?: string;
  severity: NcSeverity;
  detectedAt?: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne. */
  photoUrls?: string;
  reporterId?: string;
  /** Interne par défaut côté serveur si non précisée. */
  origin?: NcOrigin;
  /** Produit concerné, s'il y en a un. */
  productId?: string;
  /** Le client qui réclame. Facultatif : un constat interne n'en a pas. */
  customerName?: string;
  /** Le lot incriminé. Facultatif. */
  batchNumber?: string;
  /**
   * Le valideur désigné pour la clôture, choisi dans l'annuaire. Facultatif :
   * sans lui, clore revient aux rôles de pilotage.
   */
  validatorId?: string;
  validatorName?: string;
  /** Mode de défaillance du PFMEA que ce défaut illustre, confirmé par l'utilisateur. */
  fmeaItemId?: string;
  /**
   * L'équipe qui prend l'écart en charge (D1 du 8D), au moins une personne, dans
   * l'ordre de saisie : le premier nommé pilote. Le serveur refuse une liste
   * vide — nommer l'équipe à l'ouverture évite de la retaper dans le rapport.
   */
  teamMembers: TeamMemberRef[];
}

export interface UpdateNcRequest {
  title?: string;
  description?: string;
  /**
   * La date du constat, corrigée. Absente = inchangée ; jamais à venir.
   *
   * <p>Elle était figée à l'instant de la saisie : une réclamation reçue lundi
   * et enregistrée jeudi portait jeudi, alors que c'est elle qui fait courir
   * le délai de réponse dû au client.
   */
  detectedAt?: string;
  origin?: NcOrigin;
  category?: NcCategory;
  /** La nature fine. Chaîne vide = effacer le classement fin ; absente = inchangée. */
  subcategoryCode?: string;
  severity?: NcSeverity;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  photoUrls?: string;
  /** Chaîne vide = effacer le nom du client ; absent = inchangé. */
  customerName?: string;
  /** Chaîne vide = effacer le numéro de lot ; absent = inchangé. */
  batchNumber?: string;
  /** Le produit incriminé, choisi dans le référentiel. Absent = inchangé. */
  productId?: string;
  /** Chaîne vide = effacer le commentaire du D4 ; absent = inchangé. */
  causeAnalysisComment?: string;
  /**
   * Le valideur désigné. Fourni, il est réécrit en entier ; un nom VIDE le
   * retire — sans quoi une désignation posée par erreur serait définitive.
   */
  validatorId?: string;
  validatorName?: string;
  /**
   * L'équipe en charge, remplacée en bloc. Absente = inchangée ; jamais vide —
   * on ne retire pas la dernière personne d'un écart ouvert.
   */
  teamMembers?: TeamMemberRef[];
}

export interface ResolveNcRequest {
  /**
   * Ce qui a resolu l'ecart, en texte libre. FACULTATIF : l'ecran ne le
   * reclame plus -- les actions menees se lisent au D5 et au D7 du traitement
   * 8D, et les preuves de leur efficacite au D6. Le champ reste pour les
   * ecarts resolus avant ce jour, et pour l'API publique.
   */
  resolutionNote?: string;
}

/** Le rejet d'une réclamation externe : le motif est obligatoire. */
export interface RejectNcRequest {
  reason: string;
}

/**
 * La reprise d'un écart refermé, et pourquoi — si on veut le dire.
 *
 * <p>Le motif est FACULTATIF : rien n'est obligatoire dans l'écran de
 * traitement, et un écart qui revient doit pouvoir être repris tout de suite.
 */
export interface ReopenNcRequest {
  reason?: string;
}

/**
 * Le RENVOI EN MODIFICATION, par le second regard.
 *
 * <p>Message facultatif : exiger une justification découragerait le renvoi
 * lui-même, et un renvoi sans phrase vaut mieux qu'une clôture de complaisance.
 */
export interface SendBackNcRequest {
  message?: string;
}

// --- Analyse Vision 5S par IA (§3.2 / §1.4) ----------------------------------
// Surface la détection CV des non-conformités sur photo. Contrat backend figé :
// POST /api/v1/vision/5s/analyze (multipart, champ 'image'), réponse camelCase.

/** Pilier 5S porté par un finding de vision (enum backend, affiché brut/i18n). */
export type VisionPillar =
  | 'SEIRI'
  | 'SEITON'
  | 'SEISO'
  | 'SEIKETSU'
  | 'SHITSUKE';

/** Sévérité d'un finding vision (enum backend). */
export type VisionSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** Scores 5S calculés par le modèle (0-100 par pilier + global). */
export interface VisionScore {
  seiri: number;
  seiton: number;
  seiso: number;
  seiketsu: number;
  shitsuke: number;
  overall: number;
}

/** Non-conformité 5S détectée sur la photo. */
export interface VisionFinding {
  pillar: VisionPillar;
  description: string;
  /** Chaîne libre côté backend (LOW/MEDIUM/HIGH/CRITICAL le plus souvent). */
  severity: string;
  /** Confiance du modèle, 0.0 → 1.0. */
  confidence: number;
  /** Boîte englobante [x, y, w, h] en pixels, ou null si non localisée. */
  bbox: number[] | null;
}

/** Réponse complète de l'analyse vision 5S. */
export interface VisionAnalysis {
  imageSha256: string;
  width: number;
  height: number;
  score: VisionScore;
  findings: VisionFinding[];
}

export interface StartAnalysisNcRequest {
  rootCause?: string;
}

/**
 * Ce que la fenêtre de création CAPA commune envoie pour ouvrir la CAPA d'une
 * NC (ADR 0088) : le dossier et son action unique. La source est la NC — le
 * serveur la fixe, elle n'est pas lue dans le corps.
 */
export type EscalateCapaNcRequest = OpenCapaRequest;

/** Les deux methodes par lesquelles un ecart se traite (CLAUDE.md 3.6). */
export type NcMethod = 'PDCA' | 'DMAIC';

/**
 * Ouvrir, depuis un ecart, le cycle ou le projet qui le traitera.
 *
 * <p>`method` absente : la gravite decide, cote serveur. `pilotId` ne l'est
 * jamais -- un dossier dont personne ne repond ne se revoit pas. `dueDate` ne
 * concerne que le projet DMAIC ; un cycle PDCA n'en porte pas.
 */
export interface ConvertNcRequest {
  method?: NcMethod;
  pilotId: string;
  dueDate?: string | null;
}

/**
 * Dénombrements par statut, affichés en tuiles au-dessus d'une liste de NC.
 *
 * <p>`origin` rappelle SUR QUOI portent ces nombres : la même liste existe en
 * interne, en externe et toutes origines confondues. Un total sans son
 * périmètre se lit de travers.
 *
 * <p>`cancelled` n'a de sens que sur les écarts signalés du dehors — on
 * n'annule pas un constat qu'on a fait soi-même, on le résout ou on le clôt.
 */
export interface NcStatistics {
  tenantId: string;
  origin: NcOrigin | null;
  total: number;
  open: number;
  underAnalysis: number;
  actionDefined: number;
  resolved: number;
  closed: number;
  cancelled: number;
  /** Rejetées — toujours 0 sur l'écran interne, qui ne rejette pas. */
  rejected: number;
}

// ---- Rapport 8D (extrait à la clôture d'une non-conformité) -----------------

/** Brouillon tant qu'il se remanie ; émis, il est scellé et ne change plus. */
export type EightDStatus = 'DRAFT' | 'ISSUED';

/**
 * Une des huit disciplines.
 *
 * `sourced` à false veut dire « rien à montrer », et `sourceLabel` dit alors
 * POURQUOI : c'est ce qui distingue un 8D d'un formulaire aux cases vides.
 * `editable` n'est vrai que pour D8, la seule discipline qu'aucun module ne sait
 * renseigner : l'equipe vient de la fiche, l'endiguement de son dossier CAPA.
 */
export interface EightDDiscipline {
  code: string;
  title: string;
  sourced: boolean;
  /** Vide dès que la discipline porte quelque chose : ce qu'elle montre dit d'où il vient. */
  sourceLabel: string;
  lines: string[];
  /**
   * Ce que la discipline montre en colonnes, tel que le document émis le porte.
   *
   * L'équipe du D1, à ce jour. L'écran le REND, il ne le refabrique pas : une
   * liste de noms ici et un tableau dans le PDF finiraient par diverger.
   */
  tables: EightDTable[];
  editable: boolean;
}

/** Un tableau d'une discipline : ses colonnes, puis ses lignes de cellules. */
export interface EightDTable {
  caption: string;
  columns: string[];
  rows: string[][];
}

/** La preuve d'intégrité d'un rapport émis. */
export interface EightDSeal {
  sha256Hex: string;
  anchorTxRef: string;
  verificationCode: string;
  issuedAt: string;
  issuedByName: string | null;
}

export interface EightDReport {
  ncId: string;
  ncReference: string;
  ncTitle: string;
  status: EightDStatus;
  /** Vrai quand la NC est clôturée et que le rapport n'est pas encore émis. */
  issuable: boolean;
  /** Vrai dès qu'une discipline n'a rien à montrer. */
  partial: boolean;
  missingCodes: string[];
  team: string | null;
  containment: string | null;
  recognition: string | null;
  disciplines: EightDDiscipline[];
  seal: EightDSeal | null;
}

/** Les trois disciplines saisies. Toutes facultatives : un rapport partiel s'émet. */
export interface SaveEightDRequest {
  team?: string | null;
  containment?: string | null;
  recognition?: string | null;
}

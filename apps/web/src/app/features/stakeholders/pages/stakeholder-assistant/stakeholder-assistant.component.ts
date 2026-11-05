import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  categoryLabel,
  impactChip,
  impactLabel,
  natureLabel,
  requirementStatusLabel,
  sideLabel
} from '../../stakeholders.labels';
import { StakeholdersService } from '../../stakeholders.service';
import {
  DetectedChange,
  ExtractedRequirement,
  STAKEHOLDER_CATEGORIES,
  STAKEHOLDER_LIMITS,
  StakeholderCategory,
  StakeholderRequirement,
  StakeholderRow,
  StakeholderSide,
  SuggestedStakeholder
} from '../../stakeholders.types';

/** Une suggestion à l'écran, avec le choix et l'état que l'humain lui donne. */
export interface SuggestionRow {
  suggestion: SuggestedStakeholder;
  /** Interne ou externe : l'IA ne le sait pas, c'est à l'organisme de le dire. */
  side: StakeholderSide;
  /** Déjà au registre, au nom près : on ne la propose pas une seconde fois. */
  duplicate: boolean;
  busy: boolean;
  registered: boolean;
  /**
   * La proposition est ouverte a la correction : nom et famille.
   *
   * <p>Une suggestion est une PROPOSITION. L'inscrire telle quelle quand elle
   * est presque juste obligeait a la corriger ensuite dans la fiche ; la
   * rejeter pour la ressaisir a la main perdait le travail de l'assistant.
   * On la corrige donc sur place, avant de l'inscrire.
   */
  editing: boolean;
  /** Le nom retenu : celui propose, ou celui qu'on a corrige. */
  name: string;
  /** La famille retenue, modifiable comme le nom. */
  category: StakeholderCategory;
}

/** Une exigence extraite, avec l'état de son inscription. */
export interface ExtractedRow {
  extracted: ExtractedRequirement;
  busy: boolean;
  added: boolean;
}

/**
 * Les trois assistants du registre : proposer des parties prenantes, extraire
 * les exigences d'un document, repérer ce qui a changé entre deux versions.
 *
 * <p><strong>L'IA propose, elle n'écrit rien.</strong> Aucun des trois appels ne
 * modifie le registre : chacun rend une liste de propositions, et il faut un
 * geste humain explicite — une suggestion à la fois, une exigence à la fois, une
 * sélection d'exigences à réévaluer — pour que quoi que ce soit soit enregistré.
 * Un assistant qui inscrirait d'office remplirait le registre de lignes que
 * personne n'assume, et c'est exactement ce qu'un auditeur relève.
 *
 * <p>Chaque proposition est accompagnée de ce qui la rend vérifiable : la
 * justification pour une suggestion, la **citation** du document pour une
 * exigence extraite. Sans la citation, il n'y a pas de moyen de savoir si
 * l'exigence est dans le contrat ou dans l'imagination du modèle.
 */
@Component({
  selector: 'qos-stakeholder-assistant',
  templateUrl: './stakeholder-assistant.component.html',
  styleUrls: ['./stakeholder-assistant.component.scss'],
  standalone: false
})
export class StakeholderAssistantComponent {

  @Input() rows: StakeholderRow[] = [];

  /** Quelque chose a été inscrit : le registre doit se recharger. */
  @Output() readonly registered = new EventEmitter<void>();
  /** Ouvrir la fiche d'une partie prenante, dans le tiroir de l'hôte. */
  @Output() readonly opened = new EventEmitter<string>();

  readonly limits = STAKEHOLDER_LIMITS;

  /**
   * Les bornes des trois appels d'assistance, reprises du serveur.
   *
   * <p>Elles ne sont pas dans `STAKEHOLDER_LIMITS`, qui décrit la fiche et ses
   * exigences : le texte d'un contrat n'est pas un champ de fiche, et il a sa
   * propre mesure. Arrêtées ici, une saisie trop longue se voit au champ plutôt
   * qu'en retour d'un appel refusé après trente secondes d'attente.
   */
  readonly maxSector = 200;
  readonly maxName = STAKEHOLDER_LIMITS.name;
  readonly categories = STAKEHOLDER_CATEGORIES;
  readonly maxCharacteristics = 1000;
  readonly maxDocumentName = 255;
  readonly maxDocumentText = STAKEHOLDER_LIMITS.document;
  readonly maxComparedText = STAKEHOLDER_LIMITS.comparedDocument;

  readonly categoryLabel = categoryLabel;
  readonly sideLabel = sideLabel;
  readonly natureLabel = natureLabel;
  readonly impactLabel = impactLabel;
  readonly impactChip = impactChip;
  readonly requirementStatusLabel = requirementStatusLabel;

  // ---------- 1. suggestions ----------

  sector = '';
  characteristics = '';
  suggesting = false;
  suggestError: string | null = null;
  /** `null` tant qu'on n'a rien demandé ; `[]` est une réponse, pas une panne. */
  suggestions: SuggestionRow[] | null = null;

  // ---------- 2. extraction d'exigences ----------

  documentName = '';
  documentText = '';
  extracting = false;
  extractError: string | null = null;
  extracted: ExtractedRow[] | null = null;
  /** La partie prenante qui recevra les exigences retenues. */
  extractTargetId = '';

  // ---------- 3. détection de changements ----------

  changeDocumentName = '';
  previousText = '';
  currentText = '';
  detecting = false;
  changeError: string | null = null;
  detected: DetectedChange[] | null = null;

  reassessTargetId = '';
  reassessLoading = false;
  reassessError: string | null = null;
  reassessRequirements: StakeholderRequirement[] = [];
  readonly reassessSelection = new Set<string>();
  marking = false;

  constructor(
    private readonly svc: StakeholdersService,
    private readonly snack: MatSnackBar
  ) {}

  // ================= 1. suggestions =================

  /** Ce qui manque pour demander des suggestions, dit avant l'appel. */
  get suggestBlocked(): string | null {
    return this.sector.trim()
      ? null
      : $localize`:@@stakeholders.assistant.need-sector:Dites dans quel secteur vous êtes : sans cela, il n'y a rien à proposer qui vous concerne.`;
  }

  suggest(): void {
    if (this.suggestBlocked || this.suggesting) {
      return;
    }
    this.suggesting = true;
    this.suggestError = null;
    this.svc.suggest(this.sector.trim(), this.characteristics.trim())
      .pipe(finalize(() => (this.suggesting = false)))
      .subscribe({
        next: list => (this.suggestions = list.map(suggestion => ({
          suggestion,
          side: this.guessSide(suggestion),
          duplicate: this.alreadyRegistered(suggestion.name),
          editing: false,
          name: suggestion.name,
          category: suggestion.category,
          busy: false,
          registered: false
        }))),
        error: err => {
          this.suggestions = null;
          this.suggestError = safeErrorMessage(err,
            $localize`:@@stakeholders.assistant.suggest-failed:Les suggestions n'ont pas pu être obtenues.`);
        }
      });
  }

  /** Inscrire une suggestion : un geste, une ligne. C'est l'humain qui tranche. */
  register(row: SuggestionRow): void {
    if (row.duplicate || row.registered || row.busy) {
      return;
    }
    const name = row.name.trim();
    if (!name) {
      // Un nom vide n'est pas une correction, c'est un oubli : on garde la
      // ligne ouverte plutot que d'inscrire une fiche sans nom.
      row.editing = true;
      return;
    }
    row.busy = true;
    this.svc.create({
      name,
      category: row.category,
      side: row.side
    }).pipe(finalize(() => (row.busy = false))).subscribe({
      next: sheet => {
        row.registered = true;
        this.snack.open($localize`:@@stakeholders.assistant.registered:Partie prenante inscrite.`,
          undefined, { duration: 2500 });
        this.registered.emit();
        this.opened.emit(sheet.id);
      },
      error: err => this.fail(err,
        $localize`:@@stakeholders.create.failed:La partie prenante n'a pas pu être inscrite.`)
    });
  }

  // ================= 2. extraction =================

  get extractBlocked(): string | null {
    if (this.documentText.trim().length < STAKEHOLDER_LIMITS.documentMin) {
      return $localize`:@@stakeholders.assistant.need-document:Collez au moins ${STAKEHOLDER_LIMITS.documentMin}:minimum: caractères du document : en dessous, il n'y a rien à lire.`;
    }
    return null;
  }

  extract(): void {
    if (this.extractBlocked || this.extracting) {
      return;
    }
    this.extracting = true;
    this.extractError = null;
    this.svc.extract(this.documentName.trim(), this.documentText.trim())
      .pipe(finalize(() => (this.extracting = false)))
      .subscribe({
        next: list => (this.extracted = list.map(extracted => ({
          extracted, busy: false, added: false
        }))),
        error: err => {
          this.extracted = null;
          this.extractError = safeErrorMessage(err,
            $localize`:@@stakeholders.assistant.extract-failed:Les exigences n'ont pas pu être extraites.`);
        }
      });
  }

  /** Une exigence extraite n'existe pas sans destinataire : il faut le choisir. */
  get addBlocked(): string | null {
    return this.extractTargetId
      ? null
      : $localize`:@@stakeholders.assistant.need-target:Choisissez la partie prenante qui porte ces exigences.`;
  }

  add(row: ExtractedRow): void {
    if (row.added || row.busy || this.addBlocked) {
      return;
    }
    row.busy = true;
    // La citation devient la preuve de l'exigence, et la référence son origine :
    // la fiche garde ainsi le moyen de remonter au passage du document.
    // Les textes sont bornés à la longueur d'un champ d'exigence : une citation
    // longue partirait sinon se faire refuser par le serveur, après que
    // l'utilisateur a cliqué.
    this.svc.addRequirement(this.extractTargetId, {
      requirement: row.extracted.requirement.slice(0, STAKEHOLDER_LIMITS.requirement),
      origin: this.cut(row.extracted.reference ?? (this.documentName.trim() || null),
                       STAKEHOLDER_LIMITS.line),
      nature: row.extracted.nature,
      control: this.cut(row.extracted.control, STAKEHOLDER_LIMITS.line),
      evidence: this.cut(row.extracted.quote, STAKEHOLDER_LIMITS.line),
      status: 'TO_ANALYSE'
    }).pipe(finalize(() => (row.busy = false))).subscribe({
      next: () => {
        row.added = true;
        this.snack.open($localize`:@@stakeholders.requirement-added:Exigence ajoutée.`, undefined,
          { duration: 2500 });
        // Le registre compte les exigences par fiche : il doit se recharger.
        this.registered.emit();
      },
      error: err => this.fail(err,
        $localize`:@@stakeholders.requirement-failed:L'exigence n'a pas pu être ajoutée.`)
    });
  }

  openTarget(): void {
    if (this.extractTargetId) {
      this.opened.emit(this.extractTargetId);
    }
  }

  // ================= 3. changements =================

  get detectBlocked(): string | null {
    const min = STAKEHOLDER_LIMITS.documentMin;
    if (this.previousText.trim().length < min || this.currentText.trim().length < min) {
      return $localize`:@@stakeholders.assistant.need-two-versions:Collez les deux versions, au moins ${min}:minimum: caractères chacune : il n'y a pas de comparaison possible autrement.`;
    }
    return null;
  }

  detect(): void {
    if (this.detectBlocked || this.detecting) {
      return;
    }
    this.detecting = true;
    this.changeError = null;
    this.svc.changes(this.changeDocumentName.trim(), this.previousText.trim(),
                     this.currentText.trim())
      .pipe(finalize(() => (this.detecting = false)))
      .subscribe({
        next: list => (this.detected = list),
        error: err => {
          this.detected = null;
          this.changeError = safeErrorMessage(err,
            $localize`:@@stakeholders.assistant.detect-failed:Les changements n'ont pas pu être analysés.`);
        }
      });
  }

  /**
   * Charge les exigences de la partie prenante choisie : c'est parmi elles que
   * l'utilisateur désigne celles que le changement remet en cause.
   */
  onReassessTarget(id: string): void {
    this.reassessTargetId = id;
    this.reassessSelection.clear();
    this.reassessRequirements = [];
    this.reassessError = null;
    if (!id) {
      return;
    }
    this.reassessLoading = true;
    this.svc.get(id).pipe(finalize(() => (this.reassessLoading = false))).subscribe({
      next: sheet => (this.reassessRequirements = sheet.requirements),
      error: err => (this.reassessError = safeErrorMessage(err,
        $localize`:@@stakeholders.sheet-failed:La fiche n'a pas pu être ouverte.`))
    });
  }

  toggleRequirement(id: string): void {
    if (this.reassessSelection.has(id)) {
      this.reassessSelection.delete(id);
    } else {
      this.reassessSelection.add(id);
    }
  }

  isSelected(id: string): boolean {
    return this.reassessSelection.has(id);
  }

  get markBlocked(): string | null {
    if (!this.reassessTargetId) {
      return $localize`:@@stakeholders.assistant.need-target:Choisissez la partie prenante qui porte ces exigences.`;
    }
    if (!this.reassessSelection.size) {
      return $localize`:@@stakeholders.assistant.need-selection:Cochez les exigences que ce changement remet en cause.`;
    }
    return null;
  }

  markToReassess(): void {
    if (this.markBlocked || this.marking) {
      return;
    }
    this.marking = true;
    this.svc.markToReassess(this.reassessTargetId, [...this.reassessSelection])
      .pipe(finalize(() => (this.marking = false)))
      .subscribe({
        next: updated => {
          const byId = new Map(updated.map(r => [r.id, r]));
          this.reassessRequirements = this.reassessRequirements
            .map(r => byId.get(r.id) ?? r);
          this.reassessSelection.clear();
          this.snack.open($localize`:@@stakeholders.assistant.marked:Exigences marquées à réévaluer.`,
            undefined, { duration: 3000 });
          this.registered.emit();
        },
        error: err => this.fail(err,
          $localize`:@@stakeholders.assistant.mark-failed:Les exigences n'ont pas pu être marquées.`)
      });
  }

  // ================= internes =================

  /**
   * Ouvre ou referme la correction d'une proposition.
   *
   * <p>Refermer REMET les valeurs proposees : on annule une correction en
   * cours, on ne la valide pas par inadvertance en repliant le bloc.
   */
  toggleEdit(row: SuggestionRow): void {
    if (row.registered || row.duplicate || row.busy) {
      return;
    }
    if (row.editing) {
      row.name = row.suggestion.name;
      row.category = row.suggestion.category;
    }
    row.editing = !row.editing;
  }

  /**
   * Ecarte une proposition : elle quitte la liste, rien n'est ecrit.
   *
   * <p>C'est le troisieme geste que demande la demarche — confirmer, corriger,
   * rejeter. Sans lui, une proposition hors sujet restait a l'ecran et il
   * fallait relancer l'assistant pour s'en debarrasser.
   */
  reject(row: SuggestionRow): void {
    if (!this.suggestions || row.busy) {
      return;
    }
    this.suggestions = this.suggestions.filter(r => r !== row);
  }

  trackBySuggestion(_index: number, row: SuggestionRow): string {
    return row.suggestion.name;
  }

  trackByExtracted(index: number, row: ExtractedRow): string {
    return `${index}-${row.extracted.requirement}`;
  }

  trackByChange(index: number, change: DetectedChange): string {
    return `${index}-${change.title}`;
  }

  trackByRequirement(_index: number, requirement: StakeholderRequirement): string {
    return requirement.id;
  }

  /** Borne un texte à la longueur du champ qui le recevra. */
  private cut(value: string | null, max: number): string | null {
    if (!value) {
      return null;
    }
    return value.length > max ? value.slice(0, max) : value;
  }

  /** Le registre contient-il déjà ce nom ? Comparaison insensible à la casse. */
  private alreadyRegistered(name: string): boolean {
    const needle = name.trim().toLowerCase();
    return this.rows.some(row => row.name.trim().toLowerCase() === needle);
  }

  /**
   * Interne ou externe, proposé d'après la catégorie : les collaborateurs et la
   * gouvernance sont dedans, tout le reste est dehors. C'est une PROPOSITION,
   * modifiable avant inscription — une filiale et un sous-traitant ne se rangent
   * pas du même côté selon l'organisme.
   */
  private guessSide(suggestion: SuggestedStakeholder): StakeholderSide {
    return suggestion.category === 'EMPLOYEES' || suggestion.category === 'GOVERNANCE'
      ? 'INTERNAL'
      : 'EXTERNAL';
  }

  private fail(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 5000 });
  }
}

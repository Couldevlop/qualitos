import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  categoryLabel,
  impactChip,
  impactLabel,
  natureLabel,
  relevanceLabel,
  requirementStatusLabel,
  sideLabel,
  statusChip
} from '../../stakeholders.labels';
import { StakeholdersService } from '../../stakeholders.service';
import {
  IMPACT_LEVELS,
  ImpactLevel,
  RELEVANCES,
  REQUIREMENT_STATUSES,
  RequirementLink,
  RequirementStatus,
  STAKEHOLDER_CATEGORIES,
  STAKEHOLDER_LIMITS,
  StakeholderRequirement,
  StakeholderSheet,
  UpdateStakeholderRequest
} from '../../stakeholders.types';
import {
  StakeholderLinkDialogComponent,
  StakeholderLinkDialogData
} from '../stakeholder-link-dialog/stakeholder-link-dialog.component';
import {
  StakeholderRequirementDialogComponent,
  StakeholderRequirementDialogData
} from '../stakeholder-requirement-dialog/stakeholder-requirement-dialog.component';

/**
 * La fiche d'une partie intéressée, dans l'ordre où elle se remplit :
 * identification, analyse, pilotage, exigences.
 *
 * <p>Le brouillon se tient à l'écran et s'enregistre **d'un bloc** : la barre
 * d'enregistrement n'apparaît qu'avec des changements, et dit ce qui empêche
 * d'enregistrer plutôt que d'éteindre un bouton sans explication.
 *
 * <p>Les exigences, elles, partent **tout de suite** : ajouter une exigence,
 * changer son statut, la rattacher ou lui ouvrir une CAPA sont des actes qui
 * n'attendent pas un bouton — et qui doivent survivre à une fermeture du tiroir.
 */
@Component({
  selector: 'qos-stakeholder-sheet',
  templateUrl: './stakeholder-sheet.component.html',
  styleUrls: ['./stakeholder-sheet.component.scss'],
  standalone: false
})
export class StakeholderSheetComponent implements OnChanges {

  @Input({ required: true }) sheet!: StakeholderSheet;
  @Input() canManage = false;

  @Output() readonly saved = new EventEmitter<StakeholderSheet>();
  /** `true` quand la liste doit être rechargée (une exigence a bougé). */
  @Output() readonly closed = new EventEmitter<boolean>();

  readonly categories = STAKEHOLDER_CATEGORIES;
  readonly impacts = IMPACT_LEVELS;
  readonly relevances = RELEVANCES;
  readonly statuses = REQUIREMENT_STATUSES;
  readonly limits = STAKEHOLDER_LIMITS;

  readonly categoryLabel = categoryLabel;
  readonly sideLabel = sideLabel;
  readonly impactLabel = impactLabel;
  readonly relevanceLabel = relevanceLabel;
  readonly natureLabel = natureLabel;
  readonly requirementStatusLabel = requirementStatusLabel;
  readonly impactChip = impactChip;
  readonly statusChip = statusChip;

  /** Le brouillon à l'écran : la fiche ne part qu'à l'enregistrement. */
  draft!: UpdateStakeholderRequest;
  saving = false;
  /** Vrai dès qu'une exigence a bougé : la liste devra être rechargée. */
  private requirementsTouched = false;

  private savedJson = '';

  constructor(
    private readonly svc: StakeholdersService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnChanges(): void {
    this.take(this.sheet);
  }

  // ---------- brouillon ----------

  get dirty(): boolean {
    return JSON.stringify(this.draft) !== this.savedJson;
  }

  /**
   * Ce qui empêche d'enregistrer, dit en clair.
   *
   * <p>Les mêmes règles que le serveur, énoncées ici pour qu'une fiche ne
   * reparte pas en erreur après dix champs remplis.
   */
  get problems(): string[] {
    const p: string[] = [];
    if (!this.draft.name.trim()) {
      p.push($localize`:@@stakeholders.problem.name:Une partie prenante se désigne par un nom.`);
    }
    const decided = this.draft.relevance === 'RELEVANT' || this.draft.relevance === 'NOT_RELEVANT';
    if (decided && !(this.draft.relevanceJustification ?? '').trim()) {
      p.push($localize`:@@stakeholders.problem.justification:Dites pourquoi elle est pertinente, ou pourquoi elle ne l'est pas : un auditeur vient lire cette décision.`);
    }
    return p;
  }

  get criticality(): ImpactLevel {
    const weight = (level: ImpactLevel) => (level === 'HIGH' ? 3 : level === 'MEDIUM' ? 2 : 1);
    return weight(this.draft.qualityImpact) >= weight(this.draft.regulatoryImpact)
      ? this.draft.qualityImpact
      : this.draft.regulatoryImpact;
  }

  onOwner(user: DirectoryUser | null): void {
    this.draft.owner = user ? { accountId: user.accountId, name: user.displayName } : null;
  }

  discard(): void {
    this.draft = JSON.parse(this.savedJson) as UpdateStakeholderRequest;
  }

  save(): void {
    if (!this.canManage || !this.dirty || this.problems.length || this.saving) return;
    this.saving = true;
    this.svc.update(this.sheet.id, this.draft)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: sheet => {
          this.take(sheet);
          this.saved.emit(sheet);
          this.snack.open($localize`:@@stakeholders.saved:Fiche enregistrée.`, undefined,
            { duration: 2500 });
        },
        error: err => this.fail(err, $localize`:@@stakeholders.save-failed:La fiche n'a pas pu être enregistrée.`)
      });
  }

  markReviewed(): void {
    if (!this.canManage || this.saving) return;
    this.saving = true;
    this.svc.markReviewed(this.sheet.id)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: sheet => {
          this.take(sheet);
          this.saved.emit(sheet);
          this.snack.open($localize`:@@stakeholders.reviewed:Revue enregistrée à la date du jour.`,
            undefined, { duration: 2500 });
        },
        error: err => this.fail(err, $localize`:@@stakeholders.review-failed:La revue n'a pas pu être enregistrée.`)
      });
  }

  close(): void {
    this.closed.emit(this.requirementsTouched);
  }

  // ---------- exigences ----------

  get requirements(): StakeholderRequirement[] {
    return this.sheet.requirements;
  }

  addRequirement(): void {
    if (!this.canManage) return;
    const data: StakeholderRequirementDialogData = { stakeholderName: this.sheet.name };
    this.dialog.open(StakeholderRequirementDialogComponent, {
      data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(request => {
      if (!request) return;
      this.svc.addRequirement(this.sheet.id, request).subscribe({
        next: added => {
          this.sheet = { ...this.sheet, requirements: [...this.sheet.requirements, added] };
          this.requirementsTouched = true;
          this.snack.open($localize`:@@stakeholders.requirement-added:Exigence ajoutée.`, undefined,
            { duration: 2500 });
        },
        error: err => this.fail(err, $localize`:@@stakeholders.requirement-failed:L'exigence n'a pas pu être ajoutée.`)
      });
    });
  }

  setStatus(requirement: StakeholderRequirement, status: RequirementStatus): void {
    if (!this.canManage || status === requirement.status) return;
    this.svc.setRequirementStatus(this.sheet.id, requirement.id, status).subscribe({
      next: updated => this.replace(updated),
      error: err => this.fail(err, $localize`:@@stakeholders.status-failed:Le statut n'a pas pu être changé.`)
    });
  }

  /**
   * Rattache l'exigence à un objet d'un autre module, choisi dans une liste.
   *
   * <p>Un champ d'identifiant libre serait inutilisable : personne ne connaît
   * par cœur l'UUID d'un risque.
   */
  /**
   * Les rattachements ÉTABLIS d'une exigence, chacun avec la route qui ouvre
   * l'objet visé.
   *
   * <p>Jusqu'ici un rattachement ne se voyait que sur le bouton qui l'avait
   * créé, et seulement pour qui pouvait le modifier : un auditeur en lecture
   * ne pouvait pas savoir quel contrat fonde l'exigence qu'il lit, ni l'ouvrir.
   * Or c'est le sens du lien — « chaque exigence renvoie vers le document qui
   * la définit ou la justifie ». Les liens sortants sont donc rendus pour
   * tout le monde, séparément du geste qui les pose.
   */
  establishedLinks(requirement: StakeholderRequirement): { label: string; icon: string; route: unknown[] }[] {
    const liens: { label: string; icon: string; route: unknown[] }[] = [];
    if (requirement.riskId) {
      liens.push({
        label: $localize`:@@stakeholders.link.risk:Risque`,
        icon: 'warning',
        route: ['/risques', requirement.riskId]
      });
    }
    if (requirement.ncId) {
      liens.push({
        label: $localize`:@@stakeholders.link.nc:Écart`,
        icon: 'report_problem',
        route: ['/nc', requirement.ncId]
      });
    }
    if (requirement.auditId) {
      liens.push({
        label: $localize`:@@stakeholders.link.audit:Audit`,
        icon: 'fact_check',
        route: ['/audits', requirement.auditId]
      });
    }
    if (requirement.documentId) {
      liens.push({
        label: $localize`:@@stakeholders.link.document:Document`,
        icon: 'description',
        route: ['/documents', requirement.documentId]
      });
    }
    if (requirement.capaId) {
      liens.push({
        label: $localize`:@@stakeholders.link.capa:Action`,
        icon: 'add_task',
        route: ['/capa', requirement.capaId]
      });
    }
    return liens;
  }

  link(requirement: StakeholderRequirement, kind: RequirementLink): void {
    if (!this.canManage) return;
    const data: StakeholderLinkDialogData = { kind, requirement: requirement.requirement };
    this.dialog.open<StakeholderLinkDialogComponent, StakeholderLinkDialogData, string>(
      StakeholderLinkDialogComponent,
      { data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel' }
    ).afterClosed().subscribe(target => {
      if (!target) return;
      this.svc.linkRequirement(this.sheet.id, requirement.id, kind, target).subscribe({
        next: updated => {
          this.replace(updated);
          this.snack.open($localize`:@@stakeholders.linked:Exigence rattachée.`, undefined,
            { duration: 2500 });
        },
        error: err => this.fail(err, $localize`:@@stakeholders.link-failed:Le rattachement a échoué.`)
      });
    });
  }

  /**
   * Ouvre une CAPA pour une exigence qui n'est pas tenue.
   *
   * <p>Le dossier naît avec son action, confiée à l'utilisateur courant par
   * défaut ; l'exigence repasse « à réévaluer », parce qu'ouvrir une action
   * c'est constater un écart.
   */
  openCapa(requirement: StakeholderRequirement): void {
    if (!this.canManage || requirement.capaId) return;
    const me = this.auth.snapshot();
    if (!me?.userId) {
      this.snack.open($localize`:@@stakeholders.capa-no-account:Impossible d'identifier votre compte.`,
        undefined, { duration: 4000 });
      return;
    }
    const due = new Date();
    due.setDate(due.getDate() + 30);
    const dueDate = due.toISOString().slice(0, 10);
    this.svc.openCapa(this.sheet.id, requirement.id, requirement.requirement.slice(0, 200),
      me.userId, me.displayName, dueDate).subscribe({
      next: updated => {
        this.replace(updated);
        this.requirementsTouched = true;
        this.snack.open($localize`:@@stakeholders.capa-opened:CAPA ouverte ; l'exigence repasse à réévaluer.`,
          undefined, { duration: 4000 });
      },
      error: err => this.fail(err, $localize`:@@stakeholders.capa-failed:La CAPA n'a pas pu être ouverte.`)
    });
  }

  removeRequirement(requirement: StakeholderRequirement): void {
    if (!this.canManage) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@stakeholders.requirement-remove-title:Retirer cette exigence ?`,
        message: $localize`:@@stakeholders.requirement-remove-message:Elle disparaîtra de la fiche, ainsi que ses rattachements.`,
        confirmLabel: $localize`:@@stakeholders.requirement-remove:Retirer`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.removeRequirement(this.sheet.id, requirement.id).subscribe({
        next: () => {
          this.sheet = {
            ...this.sheet,
            requirements: this.sheet.requirements.filter(r => r.id !== requirement.id)
          };
          this.requirementsTouched = true;
        },
        error: err => this.fail(err, $localize`:@@stakeholders.requirement-remove-failed:L'exigence n'a pas pu être retirée.`)
      });
    });
  }

  trackByRequirement(_index: number, requirement: StakeholderRequirement): string {
    return requirement.id;
  }

  // ---------- internes ----------

  private replace(updated: StakeholderRequirement): void {
    this.sheet = {
      ...this.sheet,
      requirements: this.sheet.requirements.map(r => (r.id === updated.id ? updated : r))
    };
    this.requirementsTouched = true;
  }

  private take(sheet: StakeholderSheet): void {
    this.sheet = sheet;
    this.draft = {
      name: sheet.name,
      category: sheet.category,
      side: sheet.side,
      description: sheet.description,
      contact: sheet.contact,
      sites: sheet.sites,
      needs: sheet.needs,
      requirementsSummary: sheet.requirementsSummary,
      relevance: sheet.relevance,
      relevanceJustification: sheet.relevanceJustification,
      risksAndOpportunities: sheet.risksAndOpportunities,
      qualityImpact: sheet.qualityImpact,
      regulatoryImpact: sheet.regulatoryImpact,
      owner: sheet.owner,
      controls: sheet.controls,
      indicators: sheet.indicators,
      documents: sheet.documents,
      actions: sheet.actions,
      nextReviewOn: sheet.nextReviewOn
    };
    this.savedJson = JSON.stringify(this.draft);
  }

  private fail(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 5000 });
  }
}

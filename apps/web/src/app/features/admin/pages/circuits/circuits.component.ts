import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { roleLabel } from '../../../../core/authz/authz.labels';
import { AuthzService } from '../../../../core/authz/authz.service';
import { RoleView } from '../../../../core/authz/authz.types';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitStep, CircuitSubject, MAX_APPROVALS, MAX_STEPS } from '../../../../core/circuits/circuit.types';
import { safeErrorMessage } from '../../../../core/http/error-message';

/** Le droit qu'exige l'approbation d'une version de document : seuls ses porteurs tiennent une étape. */
const APPROVE = 'document.approve';
const NAME_MAX = 120;

/** Une étape en cours d'édition ; la clé ne sert qu'à suivre la carte quand elle bouge. */
export interface EditableStep extends CircuitStep {
  key: number;
}

/**
 * Circuits de validation (ADR 0080) : qui approuve, et dans quel ordre.
 *
 * <p>À gauche, les rôles qui ont le droit d'approuver ; à droite, le parcours
 * d'une version de document, de sa soumission à son approbation. Glisser un
 * rôle sur le parcours crée une étape à l'endroit du dépôt ; glisser une étape
 * la déplace. Le bouton « + » d'un rôle et les flèches d'une étape font la même
 * chose au clavier.
 *
 * <p>Rien ne part avant « Enregistrer ». Un circuit vide rend l'approbation
 * simple : une seule personne ayant le droit d'approuver suffit. Les versions
 * déjà soumises gardent le circuit de leur soumission.
 */
@Component({
  selector: 'qos-circuits',
  templateUrl: './circuits.component.html',
  styleUrls: ['./circuits.component.scss'],
  standalone: false
})
export class CircuitsComponent implements OnInit {

  readonly subject: CircuitSubject = 'document-version';
  readonly maxSteps = MAX_STEPS;
  readonly maxApprovals = MAX_APPROVALS;
  readonly nameMax = NAME_MAX;

  roles: RoleView[] = [];
  steps: EditableStep[] = [];
  private saved: CircuitStep[] = [];
  private nextKey = 0;

  loading = false;
  failed = false;
  saving = false;

  constructor(
    private readonly authz: AuthzService,
    private readonly circuits: CircuitService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    forkJoin({ roles: this.authz.roles(), circuit: this.circuits.circuit(this.subject) })
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: r => {
          this.roles = r.roles;
          this.reprendre(r.circuit.steps);
        },
        error: err => {
          this.failed = true;
          this.echouer(err);
        }
      });
  }

  // ---------- palette ----------

  /** Les rôles qui peuvent tenir une étape : ceux qui ont le droit d'approuver. */
  get approvers(): RoleView[] {
    return this.roles.filter(r => r.permissions.includes(APPROVE) || r.permissions.includes('*'));
  }

  get others(): RoleView[] {
    return this.roles.filter(r => !this.approvers.includes(r));
  }

  get full(): boolean {
    return this.steps.length >= MAX_STEPS;
  }

  // ---------- parcours ----------

  /** Ajoute une étape tenue par ce rôle, en fin de parcours ou à l'endroit indiqué. */
  add(role: RoleView, index = this.steps.length): void {
    if (this.full || this.saving) return;
    this.steps.splice(index, 0, { key: this.nextKey++, name: this.roleText(role), roleCode: role.code, minApprovals: 1 });
  }

  /** Un dépôt sur le parcours : une étape qu'on déplace, ou un rôle qui devient une étape. */
  drop(event: CdkDragDrop<EditableStep[], unknown, EditableStep | RoleView>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(this.steps, event.previousIndex, event.currentIndex);
      return;
    }
    const role = event.item.data as RoleView;
    if (role?.code) this.add(role, event.currentIndex);
  }

  move(index: number, delta: -1 | 1): void {
    const cible = index + delta;
    if (cible < 0 || cible >= this.steps.length) return;
    moveItemInArray(this.steps, index, cible);
  }

  remove(index: number): void {
    this.steps.splice(index, 1);
  }

  rename(step: EditableStep, value: string): void {
    step.name = value;
  }

  setRole(step: EditableStep, code: string): void {
    step.roleCode = code;
  }

  approvals(step: EditableStep, delta: -1 | 1): void {
    step.minApprovals = Math.min(MAX_APPROVALS, Math.max(1, step.minApprovals + delta));
  }

  nameInvalid(step: EditableStep): boolean {
    const n = step.name.trim();
    return !n || n.length > NAME_MAX;
  }

  // ---------- enregistrement ----------

  get dirty(): boolean {
    const actuel = this.payload();
    return actuel.length !== this.saved.length || actuel.some((s, i) =>
      s.name !== this.saved[i].name || s.roleCode !== this.saved[i].roleCode
      || s.minApprovals !== this.saved[i].minApprovals);
  }

  get invalid(): boolean {
    return this.steps.some(s => this.nameInvalid(s)) || this.steps.length > MAX_STEPS;
  }

  discard(): void {
    this.reprendre(this.saved);
  }

  save(): void {
    if (!this.dirty || this.invalid || this.saving) return;
    this.saving = true;
    this.circuits.save(this.subject, this.payload()).pipe(finalize(() => this.saving = false)).subscribe({
      next: c => {
        this.reprendre(c.steps);
        this.snack.open(c.steps.length
          ? $localize`:@@circuits.saved:Circuit enregistré. Les prochaines soumissions le suivront.`
          : $localize`:@@circuits.cleared:Circuit retiré : une seule approbation suffit de nouveau.`,
        undefined, { duration: 3000 });
      },
      error: err => this.echouer(err)
    });
  }

  private payload(): CircuitStep[] {
    return this.steps.map(s => ({ name: s.name.trim(), roleCode: s.roleCode, minApprovals: s.minApprovals }));
  }

  private reprendre(steps: CircuitStep[]): void {
    this.saved = steps.map(s => ({ ...s }));
    this.steps = steps.map(s => ({ ...s, key: this.nextKey++ }));
  }

  // ---------- gabarit ----------

  /** La phrase qui résume le parcours, telle qu'on l'expliquerait à un collègue. */
  get summary(): string {
    if (!this.steps.length) {
      return $localize`:@@circuits.summary.none:Aujourd'hui, une seule approbation suffit, par toute personne qui a le droit d'approuver.`;
    }
    const parts = this.steps.map(s => this.stepSentence(s))
      .join($localize`:@@circuits.summary.then:, puis `);
    return $localize`:@@circuits.summary.lead:Une version soumise est approuvée par ${parts}:steps:.`;
  }

  stepSentence(s: CircuitStep): string {
    const role = this.labelOf(s.roleCode);
    return s.minApprovals === 1
      ? $localize`:@@circuits.summary.one:une personne « ${role}:role: »`
      : $localize`:@@circuits.summary.many:${s.minApprovals}:count: personnes « ${role}:role: »`;
  }

  labelOf(code: string): string {
    const role = this.roles.find(r => r.code === code);
    return roleLabel(code, role?.name);
  }

  roleText(role: RoleView): string {
    return roleLabel(role.code, role.name);
  }

  /** La teinte d'un rôle, la même que dans « Rôles et droits ». */
  hue(code: string): number {
    const i = this.roles.findIndex(r => r.code === code);
    return i < 0 ? 0 : i % 6;
  }

  /** Une étape dont le rôle a perdu le droit d'approuver depuis : elle bloquerait le circuit. */
  orphan(step: EditableStep): boolean {
    return !this.approvers.some(r => r.code === step.roleCode);
  }

  addLabel(role: RoleView): string {
    return $localize`:@@circuits.add-aria:Ajouter une étape tenue par ${this.roleText(role)}:role:`;
  }

  /** La palette est une source : on n'y dépose rien. */
  readonly neverEnter = (): boolean => false;

  trackByKey(_i: number, s: EditableStep): number {
    return s.key;
  }

  trackByCode(_i: number, r: RoleView): string {
    return r.code;
  }

  private echouer(err: unknown): void {
    this.snack.open(safeErrorMessage(err, $localize`:@@circuits.failed:L'opération sur le circuit a échoué.`),
      undefined, { duration: 5000 });
  }
}

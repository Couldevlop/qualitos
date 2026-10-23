import { Component, ElementRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthzService } from '../../../../core/authz/authz.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  LEVEL_LABELS, LevelIndex, MIN_AUTONOMOUS, autonomousOn, categoriesOf, filterOperators, filterSkills,
  hasTrainerLevel, isAtRisk, levelLabel, nextLevel, operatorAverage, workshopFilterOptions
} from '../../competency-matrix/competency-matrix.model';
import { CompetencyMatrixService } from '../../competency-matrix/competency-matrix.service';
import { CompetencyMatrixView, MatrixOperator, MatrixSkill } from '../../competency-matrix/competency-matrix.types';
import {
  CompetencyOperatorDialogComponent, CompetencyOperatorDialogData
} from '../competency-operator-dialog/competency-operator-dialog.component';
import {
  CompetencySkillDialogComponent, CompetencySkillDialogData
} from '../competency-skill-dialog/competency-skill-dialog.component';

/** Une compétence à risque, telle que le panneau la nomme. */
export interface RiskEntry {
  skill: MatrixSkill;
  /** La seule personne autonome, ou `null` quand il n'y en a aucune. */
  soleAutonomous: string | null;
}

/**
 * La matrice de compétences éditable (ADR 0098) : opérateurs en lignes,
 * compétences en colonnes, un niveau de 0 à 4 à l'intersection.
 *
 * <p>Un clic (ou Entrée / Espace) sur une case fait tourner son niveau ; les
 * flèches déplacent le focus d'une case à l'autre. Chaque niveau posé est
 * l'évaluation de compétence de la personne, enregistrée par le serveur : la
 * case change tout de suite, et revient à la valeur du serveur si l'écriture
 * est refusée.
 *
 * <p>La figure se lit en colonne : moins de deux personnes autonomes, et la
 * compétence ne tient qu'à une personne — ou à personne. Le pied de colonne,
 * le panneau des risques et l'indicateur le disent ; la mise en évidence
 * estompe tout le reste.
 */
@Component({
  selector: 'qos-competency-matrix',
  templateUrl: './competency-matrix.component.html',
  styleUrls: ['./competency-matrix.component.scss'],
  standalone: false
})
export class CompetencyMatrixComponent implements OnInit {

  readonly levels = LEVEL_LABELS.map((label, level) => ({ level, label }));
  readonly minAutonomous = MIN_AUTONOMOUS;
  readonly searchPlaceholder = $localize`:@@training.matrix.search:Rechercher un opérateur…`;

  /** Écrire : le serveur tranche (action training.manage) ; l'écran n'affiche que ce qui servira. */
  canManage = false;
  /** Régler les ateliers : l'administrateur du client. */
  canAdmin = false;

  loading = false;
  failed = false;

  workshops: string[] = [];
  operators: MatrixOperator[] = [];
  skills: MatrixSkill[] = [];

  search = '';
  workshopFilter = '';
  categoryFilter = '';
  highlightRisks = false;

  /** La case qui porte le focus clavier (une seule à la fois dans l'ordre de tabulation). */
  activeRow = 0;
  activeCol = 0;

  /** Ce qui s'affiche — y compris les clics pas encore confirmés. */
  private shown = new LevelIndex();
  /** Ce que le serveur a confirmé : la valeur à rétablir sur un refus. */
  private confirmed = new LevelIndex();
  /** Les niveaux voulus pendant qu'une écriture de la même case est en vol. */
  private readonly wanted = new Map<string, number>();
  private readonly inFlight = new Set<string>();

  constructor(
    private readonly service: CompetencyMatrixService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly host: ElementRef<HTMLElement>,
    authz: AuthzService
  ) {
    authz.can('training.manage').pipe(takeUntilDestroyed()).subscribe(v => this.canManage = v);
    authz.can('authz.manage').pipe(takeUntilDestroyed()).subscribe(v => this.canAdmin = v);
  }

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.matrix().subscribe({
      next: view => {
        this.loading = false;
        this.apply(view);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@training.matrix.failed:Impossible de charger la matrice de compétences.`);
      }
    });
  }

  // ---------- lecture ----------

  get visibleOperators(): MatrixOperator[] {
    return filterOperators(this.operators, this.search, this.workshopFilter);
  }

  get visibleSkills(): MatrixSkill[] {
    return filterSkills(this.skills, this.categoryFilter);
  }

  get workshopOptions(): string[] {
    return workshopFilterOptions(this.workshops, this.operators);
  }

  get categoryOptions(): string[] {
    return categoriesOf(this.skills);
  }

  get atRiskCount(): number {
    return this.risks.length;
  }

  /** Compétences où au moins une personne est au niveau « formateur ». */
  get withTrainerCount(): number {
    return this.skills.filter(s => hasTrainerLevel(this.shown, this.operators, s.skillId)).length;
  }

  /** Les compétences affichées qui ont moins de deux personnes autonomes, et qui les tient. */
  get risks(): RiskEntry[] {
    return this.visibleSkills
      .filter(s => isAtRisk(this.shown, this.operators, s.skillId))
      .map(skill => ({ skill, soleAutonomous: autonomousOn(this.shown, this.operators, skill.skillId)[0]?.name ?? null }));
  }

  level(o: MatrixOperator, s: MatrixSkill): number {
    return this.shown.get(o.userId, s.skillId);
  }

  autonomousCount(s: MatrixSkill): number {
    return autonomousOn(this.shown, this.operators, s.skillId).length;
  }

  atRisk(s: MatrixSkill): boolean {
    return isAtRisk(this.shown, this.operators, s.skillId);
  }

  average(o: MatrixOperator): string {
    return operatorAverage(this.shown, o.userId, this.visibleSkills);
  }

  /** Le nom accessible d'une case : « Opérateur – Compétence : Niveau ». */
  cellLabel(o: MatrixOperator, s: MatrixSkill): string {
    const niveau = levelLabel(this.level(o, s));
    return $localize`:@@training.matrix.cell-label:${o.name}:operator: – ${s.name}:skill: : ${niveau}:level:`;
  }

  trainerLabel(s: MatrixSkill): string {
    return $localize`:@@training.matrix.trainer-of:Formateur : ${s.trainerName}:trainer:`;
  }

  soleLabel(name: string): string {
    return $localize`:@@training.matrix.sole-autonomous:Seule personne autonome : ${name}:name:`;
  }

  removeOperatorLabel(o: MatrixOperator): string {
    return $localize`:@@training.matrix.remove-operator:Retirer ${o.name}:name: de la matrice`;
  }

  removeSkillLabel(s: MatrixSkill): string {
    return $localize`:@@training.matrix.remove-skill:Retirer la compétence ${s.name}:name: de la matrice`;
  }

  trackRisk(_: number, r: RiskEntry): string {
    return r.skill.skillId;
  }

  trackOperator(_: number, o: MatrixOperator): string {
    return o.id;
  }

  trackSkill(_: number, s: MatrixSkill): string {
    return s.skillId;
  }

  resetFilters(): void {
    this.search = '';
    this.workshopFilter = '';
    this.categoryFilter = '';
  }

  // ---------- niveaux ----------

  /** Fait tourner le niveau de la case : 0 → 4 → 0. */
  cycle(o: MatrixOperator, s: MatrixSkill): void {
    if (!this.canManage) return;
    const niveau = nextLevel(this.level(o, s));
    this.shown.set(o.userId, s.skillId, niveau);
    this.wanted.set(this.key(o, s), niveau);
    this.send(o, s);
  }

  /** Flèches : d'une case à sa voisine, sans sortir de la grille affichée. */
  onCellKeydown(event: KeyboardEvent, row: number, col: number): void {
    const moves: Record<string, [number, number]> = {
      ArrowUp: [-1, 0], ArrowDown: [1, 0], ArrowLeft: [0, -1], ArrowRight: [0, 1]
    };
    let target: [number, number] | null = null;
    if (moves[event.key]) {
      target = [row + moves[event.key][0], col + moves[event.key][1]];
    } else if (event.key === 'Home') {
      target = [row, 0];
    } else if (event.key === 'End') {
      target = [row, this.visibleSkills.length - 1];
    }
    if (!target) return;
    event.preventDefault();
    const [r, c] = target;
    if (r < 0 || c < 0 || r >= this.visibleOperators.length || c >= this.visibleSkills.length) return;
    this.focusCell(r, c);
  }

  focusCell(row: number, col: number): void {
    this.activeRow = row;
    this.activeCol = col;
    const cell = this.host.nativeElement.querySelector<HTMLElement>(`[data-cell="${row}-${col}"]`);
    cell?.focus();
  }

  isActive(row: number, col: number): boolean {
    const r = Math.min(this.activeRow, Math.max(0, this.visibleOperators.length - 1));
    const c = Math.min(this.activeCol, Math.max(0, this.visibleSkills.length - 1));
    return row === r && col === c;
  }

  private send(o: MatrixOperator, s: MatrixSkill): void {
    const k = this.key(o, s);
    if (this.inFlight.has(k)) return;
    const niveau = this.wanted.get(k);
    if (niveau === undefined) return;
    this.inFlight.add(k);
    this.service.setLevel({ userId: o.userId, skillId: s.skillId, level: niveau }).subscribe({
      next: cell => {
        this.inFlight.delete(k);
        this.confirmed.set(cell.userId, cell.skillId, cell.level);
        if (this.wanted.get(k) === cell.level) {
          this.wanted.delete(k);
        } else {
          // Un autre clic est arrivé pendant l'écriture : on envoie le dernier voulu.
          this.send(o, s);
        }
      },
      error: err => {
        this.inFlight.delete(k);
        this.wanted.delete(k);
        this.shown.set(o.userId, s.skillId, this.confirmed.get(o.userId, s.skillId));
        this.echouer(err, $localize`:@@training.matrix.level-failed:Le niveau n'a pas pu être enregistré.`);
      }
    });
  }

  private key(o: MatrixOperator, s: MatrixSkill): string {
    return `${o.userId}|${s.skillId}`;
  }

  // ---------- opérateurs et compétences ----------

  addOperator(): void {
    this.dialog.open<CompetencyOperatorDialogComponent, CompetencyOperatorDialogData, CompetencyMatrixView>(
      CompetencyOperatorDialogComponent, {
        panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true,
        data: {
          workshops: this.workshops,
          workshop: this.workshopFilter || undefined,
          excludeIds: this.operators.map(o => o.userId)
        }
      }).afterClosed().subscribe(view => { if (view) this.apply(view); });
  }

  addSkill(): void {
    const categories = [...new Set([...this.categoryOptions, ...this.workshops])];
    this.dialog.open<CompetencySkillDialogComponent, CompetencySkillDialogData, CompetencyMatrixView>(
      CompetencySkillDialogComponent, {
        panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true,
        data: { categories, category: this.categoryFilter || undefined }
      }).afterClosed().subscribe(view => { if (view) this.apply(view); });
  }

  removeOperator(o: MatrixOperator): void {
    this.confirm(
      $localize`:@@training.matrix.remove-operator-title:Retirer ${o.name}:name: de la matrice ?`,
      $localize`:@@training.matrix.remove-operator-message:La ligne quitte la matrice. Les évaluations de la personne restent à son dossier de formation, et reviennent si elle est réinscrite.`,
      () => this.service.removeOperator(o.id).subscribe({
        next: view => this.apply(view),
        error: err => this.echouer(err, $localize`:@@training.matrix.remove-failed:Le retrait n'a pas pu être enregistré.`)
      }));
  }

  removeSkill(s: MatrixSkill): void {
    this.confirm(
      $localize`:@@training.matrix.remove-skill-title:Retirer « ${s.name}:name: » de la matrice ?`,
      $localize`:@@training.matrix.remove-skill-message:La colonne quitte la matrice. La compétence reste au catalogue, avec ses évaluations.`,
      () => this.service.removeSkill(s.skillId).subscribe({
        next: view => this.apply(view),
        error: err => this.echouer(err, $localize`:@@training.matrix.remove-failed:Le retrait n'a pas pu être enregistré.`)
      }));
  }

  private confirm(title: string, message: string, onConfirm: () => void): void {
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title, message,
        confirmLabel: $localize`:@@training.matrix.remove:Retirer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => { if (ok) onConfirm(); });
  }

  private apply(view: CompetencyMatrixView): void {
    this.workshops = view.workshops;
    this.operators = view.operators;
    this.skills = view.skills;
    this.shown = new LevelIndex(view.cells);
    this.confirmed = new LevelIndex(view.cells);
    this.wanted.clear();
    if (this.categoryFilter && !this.skills.some(s => s.category === this.categoryFilter)) {
      this.categoryFilter = '';
    }
  }

  private echouer(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}

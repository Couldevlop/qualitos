import { Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { AuthzService } from '../../../../../core/authz/authz.service';
import { safeErrorMessage } from '../../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { controlledLabel, csvCell, fmt, statusTone } from '../../spc-capability.labels';
import { SpcCapabilityService } from '../../spc-capability.service';
import { CapabilityStatus, StudySummary } from '../../spc-capability.types';

export type SortKey = 'partReference' | 'characteristic' | 'leaderName' | 'studyDate' | 'machine'
  | 'cp' | 'cpk' | 'status' | 'actionPlan';

/**
 * Le registre des études SPC (maquette `docs/spc/Études SPC – Registre et
 * capabilité.html`) : indicateurs, filtres, export, et une ligne par étude.
 *
 * <p>Le registre entier est chargé une fois — quelques centaines d'études au
 * plus — et filtré ici, sans aller-retour à chaque frappe. Les indicateurs se
 * lisent sur le verdict que le serveur a rendu avec les seuils figés de chaque
 * étude, pas sur des seuils codés ici.
 */
@Component({
  selector: 'qos-spc-capability-register',
  templateUrl: './capability-register.component.html',
  styleUrls: ['./capability-register.component.scss'],
  standalone: false
})
export class CapabilityRegisterComponent implements OnInit {

  /** Qui écrit : le serveur tranche (action spc.manage) ; l'écran n'affiche que ce qui servira. */
  canManage = false;

  studies: StudySummary[] = [];
  loading = false;
  failed = false;

  search = '';
  statusFilter: '' | 'controlled' | 'uncontrolled' = '';
  planFilter: '' | 'yes' | 'no' = '';
  sortKey: SortKey = 'studyDate';
  sortDir: 1 | -1 = -1;

  readonly searchPlaceholder = $localize`:@@spc.register.search:Rechercher : pièce, meneur, machine…`;

  constructor(
    private readonly service: SpcCapabilityService,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    authz: AuthzService
  ) {
    authz.can('spc.manage').pipe(takeUntilDestroyed()).subscribe(v => this.canManage = v);
  }

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.list().subscribe({
      next: studies => {
        this.studies = studies;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err, $localize`:@@spc.register.load-failed:Le registre des études n'a pas pu être chargé.`);
      }
    });
  }

  // ---------- indicateurs ----------

  get controlledPercent(): number | null {
    if (!this.studies.length) return null;
    return Math.round(100 * this.studies.filter(s => s.status === 'CAPABLE').length / this.studies.length);
  }

  get withPlan(): number {
    return this.studies.filter(s => s.actionPlan).length;
  }

  get toWatch(): number {
    return this.studies.filter(s => s.status === 'WATCH').length;
  }

  /** Non maîtrisées et sans plan d'actions : ce qui demande une décision. */
  get uncontrolledWithoutPlan(): number {
    return this.studies.filter(s => this.needsAction(s)).length;
  }

  /** Les seuils de surveillance des études, pour le libellé de l'indicateur. */
  get watchRange(): string {
    const s = this.studies[0];
    return s ? `${s.watchThreshold.toFixed(2)} – ${s.capableThreshold.toFixed(2)}` : '';
  }

  needsAction(s: StudySummary): boolean {
    return s.status !== 'CAPABLE' && !s.actionPlan;
  }

  // ---------- filtres et tri ----------

  get rows(): StudySummary[] {
    const q = this.search.trim().toLowerCase();
    return this.studies
      .filter(s => !q || [s.partReference, s.characteristic, s.leaderName, s.machine]
        .some(v => (v ?? '').toLowerCase().includes(q)))
      .filter(s => !this.statusFilter || (this.statusFilter === 'controlled') === (s.status === 'CAPABLE'))
      .filter(s => !this.planFilter || (this.planFilter === 'yes') === s.actionPlan)
      .sort((a, b) => this.compare(a, b) * this.sortDir);
  }

  sortBy(key: SortKey): void {
    this.sortDir = this.sortKey === key ? (this.sortDir === 1 ? -1 : 1) : 1;
    this.sortKey = key;
  }

  ariaSort(key: SortKey): 'ascending' | 'descending' | 'none' {
    if (this.sortKey !== key) return 'none';
    return this.sortDir === 1 ? 'ascending' : 'descending';
  }

  resetFilters(): void {
    this.search = '';
    this.statusFilter = '';
    this.planFilter = '';
  }

  private compare(a: StudySummary, b: StudySummary): number {
    const u = a[this.sortKey];
    const v = b[this.sortKey];
    if (typeof u === 'number' && typeof v === 'number') return u - v;
    if (typeof u === 'boolean' && typeof v === 'boolean') return Number(u) - Number(v);
    return String(u ?? '').localeCompare(String(v ?? ''), 'fr');
  }

  // ---------- actions ----------

  create(): void {
    this.router.navigate(['/spc', 'cartes', 'nouvelle']);
  }

  open(s: StudySummary): void {
    this.router.navigate(['/spc', 'cartes', s.id]);
  }

  /** Le plan d'actions se coche depuis la ligne ; en cas de refus, la ligne revient à l'état du serveur. */
  setPlan(s: StudySummary, value: boolean): void {
    const avant = s.actionPlan;
    s.actionPlan = value;
    this.service.setActionPlan(s.id, value).subscribe({
      next: maj => this.replace(maj),
      error: err => {
        s.actionPlan = avant;
        this.echouer(err, $localize`:@@spc.register.plan-failed:Le plan d'actions n'a pas pu être modifié.`);
      }
    });
  }

  remove(s: StudySummary): void {
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@spc.register.delete-title:Supprimer l'étude ${s.partReference}:part: ?`,
        message: $localize`:@@spc.register.delete-message:L'étude et ses mesures quittent le registre. La suppression est tracée dans le journal d'audit.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.service.delete(s.id).subscribe({
        next: () => this.studies = this.studies.filter(x => x.id !== s.id),
        error: err => this.echouer(err, $localize`:@@spc.register.delete-failed:L'étude n'a pas pu être supprimée.`)
      });
    });
  }

  /**
   * Les lignes AFFICHÉES, en CSV : ce que l'utilisateur voit est ce qu'il exporte.
   * Chaque cellule passe par `csvCell`, qui neutralise les formules (injection CSV).
   */
  exporter(): void {
    const lignes: (string | number)[][] = [[
      $localize`:@@spc.col.part:Référence pièce`, $localize`:@@spc.col.characteristic:Caractéristique`,
      $localize`:@@spc.col.specification:Spécification`, $localize`:@@spc.col.leader:Meneur`,
      $localize`:@@spc.col.date:Date`, $localize`:@@spc.col.machine:Machine`, 'Cp', 'Cpk',
      $localize`:@@spc.col.status:Statut`, $localize`:@@spc.col.plan:Plan d'actions`
    ], ...this.rows.map(s => [
      s.partReference, s.characteristic ?? '', s.specification ?? '', s.leaderName ?? '', s.studyDate,
      s.machine ?? '', s.cp, s.cpk, controlledLabel(s.status),
      s.actionPlan ? $localize`:@@common.yes:Oui` : $localize`:@@common.no:Non`
    ])];
    const csv = '\uFEFF' + lignes.map(l => l.map(csvCell).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'etudes-spc.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  // ---------- gabarit ----------

  controlled(s: CapabilityStatus): string { return controlledLabel(s); }
  tone(s: CapabilityStatus): string { return statusTone(s); }
  num(v: number, digits = 2): string { return fmt(v, digits); }

  trackById(_i: number, s: StudySummary): string {
    return s.id;
  }

  private replace(maj: StudySummary): void {
    this.studies = this.studies.map(x => x.id === maj.id ? maj : x);
  }

  private echouer(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}

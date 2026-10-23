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
import { csvCell } from '../../../capability/spc-capability.labels';
import { decisionLabel, decisionTone } from '../../gage-rr.labels';
import { GageRrService } from '../../gage-rr.service';
import { GageRrDecision, GageRrSummary } from '../../gage-rr.types';

export type GageRrSortKey = 'partReference' | 'characteristic' | 'instrument' | 'responsibleName' | 'studyDate'
  | 'grrPercent' | 'grrTolerancePercent' | 'ndc' | 'anovaGrrPercent' | 'decision';

/** L'ordre de gravité, pour trier les verdicts du meilleur au pire. */
const DECISION_RANK: Record<GageRrDecision, number> = { ACCEPTABLE: 0, CONDITIONAL: 1, NOT_ACCEPTABLE: 2 };

/**
 * Le registre des études Gage R&R : indicateurs, filtres, export, et une
 * ligne par étude avec le verdict rendu par le serveur.
 *
 * Le registre entier est chargé une fois — quelques centaines d'études au
 * plus — et filtré ici, sans aller-retour à chaque frappe.
 */
@Component({
  selector: 'qos-spc-gage-rr-register',
  templateUrl: './gage-rr-register.component.html',
  styleUrls: ['./gage-rr-register.component.scss'],
  standalone: false
})
export class GageRrRegisterComponent implements OnInit {

  canManage = false;
  studies: GageRrSummary[] = [];
  loading = false;
  failed = false;

  search = '';
  decisionFilter: '' | GageRrDecision = '';
  sortKey: GageRrSortKey = 'studyDate';
  sortDir: 1 | -1 = -1;

  readonly searchPlaceholder = $localize`:@@spc.gage-rr.register.search:Rechercher : pièce, instrument, opérateur…`;

  constructor(
    private readonly service: GageRrService,
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
        this.echouer(err, $localize`:@@spc.gage-rr.register.load-failed:Le registre des études R&R n'a pas pu être chargé.`);
      }
    });
  }

  // ---------- indicateurs ----------

  count(d: GageRrDecision): number {
    return this.studies.filter(s => s.decision === d).length;
  }

  /** Études dont le système ne distingue pas assez de catégories (ndc < 5). */
  get weakDiscrimination(): number {
    return this.studies.filter(s => s.ndc < 5).length;
  }

  // ---------- filtres et tri ----------

  get rows(): GageRrSummary[] {
    const q = this.search.trim().toLowerCase();
    return this.studies
      .filter(s => !q || [s.partReference, s.characteristic, s.instrument, s.responsibleName,
        ...s.operators.map(o => o.name)].some(v => (v ?? '').toLowerCase().includes(q)))
      .filter(s => !this.decisionFilter || s.decision === this.decisionFilter)
      .sort((a, b) => this.compare(a, b) * this.sortDir);
  }

  sortBy(key: GageRrSortKey): void {
    this.sortDir = this.sortKey === key ? (this.sortDir === 1 ? -1 : 1) : 1;
    this.sortKey = key;
  }

  ariaSort(key: GageRrSortKey): 'ascending' | 'descending' | 'none' {
    if (this.sortKey !== key) return 'none';
    return this.sortDir === 1 ? 'ascending' : 'descending';
  }

  resetFilters(): void {
    this.search = '';
    this.decisionFilter = '';
  }

  private compare(a: GageRrSummary, b: GageRrSummary): number {
    if (this.sortKey === 'decision') return DECISION_RANK[a.decision] - DECISION_RANK[b.decision];
    const u = a[this.sortKey];
    const v = b[this.sortKey];
    if (typeof u === 'number' && typeof v === 'number') return u - v;
    return String(u ?? '').localeCompare(String(v ?? ''), 'fr');
  }

  // ---------- actions ----------

  create(): void {
    this.router.navigate(['/spc', 'gage-rr', 'nouvelle']);
  }

  open(s: GageRrSummary): void {
    this.router.navigate(['/spc', 'gage-rr', s.id]);
  }

  remove(s: GageRrSummary): void {
    const part = s.partReference;
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@spc.gage-rr.register.delete-title:Supprimer l'étude R&R ${part}:part: ?`,
        message: $localize`:@@spc.gage-rr.register.delete-message:L'étude et ses mesures quittent le registre. La suppression est tracée dans le journal d'audit.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.service.delete(s.id).subscribe({
        next: () => this.studies = this.studies.filter(x => x.id !== s.id),
        error: err => this.echouer(err, $localize`:@@spc.gage-rr.register.delete-failed:L'étude n'a pas pu être supprimée.`)
      });
    });
  }

  /** Les lignes AFFICHÉES, en CSV ; chaque cellule passe par `csvCell` (injection CSV). */
  exporter(): void {
    const lignes: (string | number)[][] = [[
      $localize`:@@spc.col.part:Référence pièce`, $localize`:@@spc.col.characteristic:Caractéristique`,
      $localize`:@@spc.gage-rr.col.instrument:Instrument`, $localize`:@@spc.gage-rr.col.responsible:Responsable`,
      $localize`:@@spc.col.date:Date`, $localize`:@@spc.gage-rr.col.operators:Opérateurs`,
      $localize`:@@spc.gage-rr.col.plan:Plan (k × n × r)`, $localize`:@@spc.gage-rr.col.multiplier:Multiplicateur σ`,
      '%GRR', $localize`:@@spc.gage-rr.col.grr-tolerance:%GRR tolérance`, 'ndc', '%GRR ANOVA', 'ndc ANOVA',
      $localize`:@@spc.gage-rr.col.decision:Décision`
    ], ...this.rows.map(s => [
      s.partReference, s.characteristic ?? '', s.instrument ?? '', s.responsibleName ?? '', s.studyDate,
      s.operators.map(o => o.name).join(', '), this.plan(s), s.sigmaMultiplier,
      s.grrPercent, s.grrTolerancePercent, s.ndc, s.anovaGrrPercent, s.anovaNdc, decisionLabel(s.decision)
    ])];
    const csv = '﻿' + lignes.map(l => l.map(csvCell).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'etudes-gage-rr.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  // ---------- gabarit ----------

  decision(d: GageRrDecision): string { return decisionLabel(d); }
  tone(d: GageRrDecision): string { return decisionTone(d); }
  plan(s: GageRrSummary): string { return `${s.operatorCount} × ${s.partCount} × ${s.trialCount}`; }
  percent(v: number): string { return `${v.toFixed(1)} %`; }
  operatorNames(s: GageRrSummary): string { return s.operators.map(o => o.name).join(', '); }

  trackById(_i: number, s: GageRrSummary): string {
    return s.id;
  }

  private echouer(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), undefined, { duration: 5000 });
  }
}

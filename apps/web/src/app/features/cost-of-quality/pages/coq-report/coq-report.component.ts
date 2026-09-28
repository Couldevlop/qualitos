import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { CATEGORY_TEXT, CURRENCIES, CoqCategoryText, coqLabelText } from '../../cost-of-quality.labels';
import { CostOfQualityService } from '../../cost-of-quality.service';
import {
  CoqBlock, CoqCategory, CoqEntryRequest, CoqLabel, CoqLine, CoqReport
} from '../../cost-of-quality.types';
import {
  CoqEntryDialogComponent, CoqEntryDialogData, CoqEntryDialogResult
} from '../coq-entry-dialog/coq-entry-dialog.component';

/**
 * Qui saisit. Le serveur réserve l'écriture à ces rôles
 * (`CostOfQualityController.ROLES_SAISIE`) et aliase `QUALITY_DIRECTOR` vers
 * `DIRECTOR_QUALITY` ; le front n'a pas cet alias et liste donc les deux formes,
 * comme la boîte à idées.
 */
const ROLES_SAISIE =
  ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'QUALITY_DIRECTOR', 'ADMIN_TENANT', 'SUPER_ADMIN'];

export type CoqPeriodMode = 'month' | 'year';

/**
 * Le coût de la qualité, selon le modèle PAF.
 *
 * <p>Vue mois : chaque ligne est une imputation, qu'on ouvre pour la corriger
 * et dont on ajuste le montant directement dans la liste. Vue année : les
 * mêmes blocs, en lecture seule, chaque libellé cumulé sur douze mois, avec
 * l'histogramme mensuel. La saisie se fait toujours dans le mois concerné —
 * c'est la date d'imputation qui range une ligne, et une vue de douze mois ne
 * saurait pas laquelle proposer.
 */
@Component({
  selector: 'qos-coq-report',
  templateUrl: './coq-report.component.html',
  styleUrls: ['./coq-report.component.scss'],
  standalone: false
})
export class CoqReportComponent implements OnInit {

  readonly currencies = CURRENCIES;
  readonly editable: boolean;

  mode: CoqPeriodMode = 'month';
  year: number;
  month: number;

  report?: CoqReport;
  labels: CoqLabel[] = [];
  loading = false;
  failed = false;

  constructor(
    private readonly service: CostOfQualityService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_SAISIE);
    const aujourdhui = new Date();
    this.year = aujourdhui.getFullYear();
    this.month = aujourdhui.getMonth() + 1;
  }

  ngOnInit(): void {
    this.chargerLibelles();
    this.charger();
  }

  // ---------- période ----------

  setMode(mode: CoqPeriodMode): void {
    if (mode === this.mode) return;
    this.mode = mode;
    this.charger();
  }

  precedent(): void {
    if (this.mode === 'year') {
      this.year--;
    } else if (this.month === 1) {
      this.month = 12;
      this.year--;
    } else {
      this.month--;
    }
    this.charger();
  }

  suivant(): void {
    if (this.mode === 'year') {
      this.year++;
    } else if (this.month === 12) {
      this.month = 1;
      this.year++;
    } else {
      this.month++;
    }
    this.charger();
  }

  /** Premier jour de la période, pour que le gabarit la nomme dans la langue de l'écran. */
  get periodDate(): Date {
    return new Date(this.year, this.mode === 'year' ? 0 : this.month - 1, 1);
  }

  get isMonth(): boolean {
    return this.mode === 'month';
  }

  // ---------- lecture ----------

  get conformanceBlocks(): CoqBlock[] {
    return this.report?.blocks.filter(b => b.category === 'PREVENTION' || b.category === 'APPRAISAL') ?? [];
  }

  get nonConformanceBlocks(): CoqBlock[] {
    return this.report?.blocks.filter(b => b.category === 'INTERNAL_FAILURE' || b.category === 'EXTERNAL_FAILURE') ?? [];
  }

  /** Largeur de chaque barre de synthèse, rapportée au total : les deux se lisent l'une contre l'autre. */
  share(value: number): number {
    const total = this.report?.total ?? 0;
    return total > 0 ? Math.round((value / total) * 1000) / 10 : 0;
  }

  /** Hauteur d'un mois dans l'histogramme, rapportée au mois le plus chargé. */
  barHeight(value: number): number {
    const max = Math.max(0, ...(this.report?.months ?? []).map(m => m.conformance + m.nonConformance));
    return max > 0 ? Math.round((value / max) * 1000) / 10 : 0;
  }

  monthDate(month: number): Date {
    return new Date(this.year, month - 1, 1);
  }

  /** Le titre, la pastille et la description d'une famille (le contexte du gabarit de bloc n'est pas typé). */
  textOf(block: CoqBlock): CoqCategoryText {
    return CATEGORY_TEXT[block.category];
  }

  lineText(line: CoqLine): string {
    return coqLabelText(line.labelCode, line.labelName);
  }

  trackByCategory(_index: number, block: CoqBlock): CoqCategory {
    return block.category;
  }

  trackByLine(_index: number, line: CoqLine): string {
    return line.entryId ?? `vide-${line.labelId}`;
  }

  // ---------- écriture ----------

  /** Ouvre une ligne : une imputation pour la corriger, une ligne à zéro pour la saisir. */
  ouvrir(block: CoqBlock, line: CoqLine): void {
    if (!this.isMonth) return;
    if (!line.entryId && !this.editable) return;
    this.saisir(block.category, line.entryId ? line : undefined, line.entryId ? undefined : line.labelId);
  }

  ajouter(block: CoqBlock): void {
    this.saisir(block.category);
  }

  /**
   * Le montant corrigé directement dans la liste, comme sur la maquette. Le
   * reste de la ligne est renvoyé tel quel : l'API corrige une ligne entière.
   */
  changerMontant(line: CoqLine, saisie: string): void {
    if (!line.entryId || !this.editable) return;
    const montant = Number(saisie);
    if (saisie.trim() === '' || !Number.isFinite(montant) || montant < 0) {
      this.snack.open($localize`:@@coq.amount-invalid:Montant invalide : un nombre positif ou nul.`,
        undefined, { duration: 4000 });
      this.charger();
      return;
    }
    if (montant === line.amount) return;
    this.service.revise(line.entryId, { ...CoqReportComponent.requete(line), amount: montant }).subscribe({
      next: () => this.charger(),
      error: err => this.echouer(err)
    });
  }

  supprimer(line: CoqLine): void {
    if (!line.entryId || !this.editable) return;
    const entryId = line.entryId;
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@coq.delete-title:Supprimer cette ligne ?`,
        message: $localize`:@@coq.delete-message:Le montant sort des totaux de la période. La suppression est tracée dans le journal d'audit.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.service.delete(entryId).subscribe({
        next: () => this.charger(),
        error: err => this.echouer(err)
      });
    });
  }

  changerDevise(devise: string): void {
    if (!this.report || devise === this.report.currency) return;
    this.service.setCurrency(devise).subscribe({
      next: () => this.charger(),
      error: err => this.echouer(err)
    });
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.service.report(this.year, this.isMonth ? this.month : null).subscribe({
      next: report => {
        this.report = report;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.echouer(err);
      }
    });
  }

  // ---------- interne ----------

  private saisir(category: CoqCategory, line?: CoqLine, presetLabelId?: string): void {
    const data: CoqEntryDialogData = {
      category,
      labels: this.labels.filter(l => l.category === category),
      line,
      presetLabelId,
      defaultDate: this.dateParDefaut(),
      readOnly: !this.editable,
      currency: this.report?.currency ?? 'EUR'
    };
    this.dialog.open<CoqEntryDialogComponent, CoqEntryDialogData, CoqEntryDialogResult>(
      CoqEntryDialogComponent, {
        data,
        panelClass: 'qos-dialog-panel',
        autoFocus: CoqReportComponent.focusInitial(line, presetLabelId),
        restoreFocus: true
      }).afterClosed().subscribe(result => {
      if (!result) return;
      this.libelleDe(category, result).pipe(
        switchMap(labelId => {
          const requete: CoqEntryRequest = { ...result.entry, labelId };
          return line?.entryId
            ? this.service.revise(line.entryId, requete)
            : this.service.record(requete);
        })
      ).subscribe({
        next: () => {
          if (result.newLabel) this.chargerLibelles();
          this.charger();
        },
        error: err => this.echouer(err)
      });
    });
  }

  /**
   * Où poser le curseur à l'ouverture.
   *
   * <p>Ouverte depuis une ligne, la fenêtre connaît déjà son libellé : le focus
   * va au montant. Le poser sur le libellé déroulerait la liste de la famille
   * alors qu'on est déjà dedans. « Ajouter une ligne » garde le premier champ,
   * où la liste s'ouvre, puisque c'est là qu'on choisit.
   */
  static focusInitial(line?: CoqLine, presetLabelId?: string): string {
    return line || presetLabelId ? 'input[data-test="montant"]' : 'first-tabbable';
  }

  /** Le libellé retenu, créé d'abord s'il a été tapé en texte libre. */
  private libelleDe(category: CoqCategory, result: CoqEntryDialogResult): Observable<string> {
    if (result.labelId) return of(result.labelId);
    const nouveau = result.newLabel!;
    return this.service.createLabel({ category, name: nouveau.name, partControl: nouveau.partControl })
      .pipe(map(label => label.id));
  }

  /** Aujourd'hui si l'on regarde le mois en cours, sinon le premier du mois affiché. */
  private dateParDefaut(): string {
    const aujourdhui = new Date();
    const jour = aujourdhui.getFullYear() === this.year && aujourdhui.getMonth() + 1 === this.month
      ? aujourdhui.getDate()
      : 1;
    return `${this.year}-${String(this.month).padStart(2, '0')}-${String(jour).padStart(2, '0')}`;
  }

  private chargerLibelles(): void {
    this.service.labels().subscribe({
      next: labels => this.labels = labels,
      error: err => this.echouer(err)
    });
  }

  private echouer(err: unknown): void {
    this.snack.open(
      safeErrorMessage(err, $localize`:@@coq.error:Le coût de la qualité est indisponible.`),
      undefined, { duration: 5000 });
  }

  private static requete(line: CoqLine): CoqEntryRequest {
    return {
      labelId: line.labelId,
      amount: line.amount,
      responsible: line.responsible ?? '',
      imputationDate: line.imputationDate ?? '',
      comment: line.comment ?? undefined,
      partReference: line.partReference ?? undefined,
      partQuantity: line.partQuantity ?? undefined,
      lot: line.lot ?? undefined,
      receivedOrMadeOn: line.receivedOrMadeOn ?? undefined
    };
  }
}

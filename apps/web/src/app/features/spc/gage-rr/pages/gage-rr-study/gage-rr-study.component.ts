import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import type { EChartsCoreOption } from 'echarts/core';

import { AuthzService } from '../../../../../core/authz/authz.service';
import { personName } from '../../../../../core/directory/directory-names';
import { DirectoryUser } from '../../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  MAX_OPERATORS, MAX_PARTS, MAX_TRIALS, OPERATOR_COUNTS, PART_COUNTS, SIGMA_MULTIPLIERS, TRIAL_COUNTS, computeGageRr,
  isComplete, parseMeasurement, pasteBlock
} from '../../gage-rr.calc';
import { byPartOption, componentsOption, meanChartOption, rangeChartOption } from '../../gage-rr.charts';
import { WORKBOOK_EXAMPLE } from '../../gage-rr.examples';
import {
  anovaSourceLabel, decisionLabel, decisionTone, fieldMessage, fmt, interpretation, ndcLabel, operatorFallback,
  operatorLetter, pValue, pct
} from '../../gage-rr.labels';
import { GageRrService } from '../../gage-rr.service';
import {
  AnovaRow, GageRrCalculationView, GageRrDecision, GageRrOperator, GageRrRequest, GageRrSheet, VariationComponent
} from '../../gage-rr.types';

type OperatorControl = 'op0' | 'op1' | 'op2';
const OPERATOR_CONTROLS: readonly OperatorControl[] = ['op0', 'op1', 'op2'];

/** Une ligne de l'étape 3 : une source de variation, sa formule et son application numérique. */
export interface StepThreeRow {
  key: 'ev' | 'av' | 'grr' | 'pv' | 'tv';
  label: string;
  formula: string;
  application: string;
  component: VariationComponent;
}

/**
 * Une étude Gage R&R (classeur `docs/spc/Etude_RR_Gage_1.xlsx`, ADR 0097) :
 * identification et plan, grille de saisie comme l'onglet « Données »
 * (opérateurs × essais, moyenne et étendue de chaque opérateur, moyenne de la
 * pièce), résultats pas à pas comme l'onglet « Résultats », ANOVA à deux
 * facteurs, graphiques et guide.
 *
 * <p>Tout se recalcule à la frappe avec les mêmes constantes et les mêmes
 * formules que le serveur, qui refait le calcul à l'enregistrement : ce qui est
 * enregistré vient toujours de lui.
 *
 * <p>La grille garde le texte saisi au plus grand plan (3 × 10 × 3) : réduire
 * le plan ne perd pas les mesures masquées, et « 10, » en cours de frappe n'est
 * pas réécrit.
 */
@Component({
  selector: 'qos-spc-gage-rr-study',
  templateUrl: './gage-rr-study.component.html',
  styleUrls: ['./gage-rr-study.component.scss'],
  standalone: false
})
export class GageRrStudyComponent implements OnInit {

  readonly operatorCounts = OPERATOR_COUNTS;
  readonly partCounts = PART_COUNTS;
  readonly trialCounts = TRIAL_COUNTS;
  readonly multipliers = SIGMA_MULTIPLIERS;
  readonly operatorControls = OPERATOR_CONTROLS;

  canManage = false;
  id: string | null = null;
  loading = false;
  failed = false;
  saving = false;
  showGuide = false;

  /** Le texte des cellules : [opérateur][pièce][essai], au plus grand plan. */
  raw: string[][][] = emptyRaw();

  /** `null` tant que la grille n'est pas complète. */
  calc: GageRrCalculationView | null = null;
  stepThree: StepThreeRow[] = [];
  readings: string[] = [];
  componentsChart: EChartsCoreOption | null = null;
  rangeChart: EChartsCoreOption | null = null;
  meanChart: EChartsCoreOption | null = null;
  partChart: EChartsCoreOption | null = null;

  /** Le message d'erreur d'un champ refusé, par le serveur ou avant l'envoi. */
  fieldError: { field: string; message: string } | null = null;

  /** Les noms recopiés de l'annuaire : le responsable et chaque opérateur. */
  responsibleName: string | null = null;
  operatorNames: (string | null)[] = [null, null, null];

  readonly form = this.fb.group({
    partReference: ['', [Validators.required, Validators.maxLength(120)]],
    characteristic: ['', Validators.maxLength(150)],
    instrument: ['', Validators.maxLength(150)],
    lsl: [null as number | null, Validators.required],
    usl: [null as number | null, Validators.required],
    operatorCount: [3, Validators.required],
    partCount: [10, Validators.required],
    trialCount: [3, Validators.required],
    sigmaMultiplier: [5.15, Validators.required],
    studyDate: [new Date().toISOString().slice(0, 10), Validators.required],
    responsibleUserId: [null as string | null],
    op0: [null as string | null],
    op1: [null as string | null],
    op2: [null as string | null]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly service: GageRrService,
    private readonly snack: MatSnackBar,
    private readonly dialog: MatDialog,
    authz: AuthzService
  ) {
    authz.can('spc.manage').pipe(takeUntilDestroyed()).subscribe(v => {
      this.canManage = v;
      this.applyAccess();
    });
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.recompute());
    this.recompute();
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id');
    if (this.id) this.charger();
  }

  charger(): void {
    if (!this.id) return;
    this.loading = true;
    this.failed = false;
    this.service.get(this.id).subscribe({
      next: sheet => {
        this.loading = false;
        this.reprendre(sheet);
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.snack.open(safeErrorMessage(err, $localize`:@@spc.gage-rr.study.not-found:Étude R&R introuvable.`),
          undefined, { duration: 5000 });
      }
    });
  }

  // ---------- plan ----------

  get k(): number { return Number(this.form.controls.operatorCount.value) || 3; }
  get n(): number { return Number(this.form.controls.partCount.value) || 10; }
  get r(): number { return Number(this.form.controls.trialCount.value) || 3; }
  get multiplier(): number { return Number(this.form.controls.sigmaMultiplier.value) || 5.15; }

  get operators(): number[] { return range(this.k); }
  get parts(): number[] { return range(this.n); }
  get trials(): number[] { return range(this.r); }

  get lsl(): number | null { return toNumber(this.form.controls.lsl.value); }
  get usl(): number | null { return toNumber(this.form.controls.usl.value); }

  get tolerance(): number | null {
    return this.lsl !== null && this.usl !== null && this.usl > this.lsl ? this.usl - this.lsl : null;
  }

  get limitsInvalid(): boolean {
    return this.lsl !== null && this.usl !== null && !(this.usl > this.lsl);
  }

  /** La grille lue au plan courant ; une cellule vide ou illisible vaut `null`. */
  get grid(): (number | null)[][][] {
    return this.raw.slice(0, this.k).map(bloc =>
      bloc.slice(0, this.n).map(essais => essais.slice(0, this.r).map(parseMeasurement)));
  }

  /** Les cellules encore à saisir au plan courant. */
  get missing(): number {
    return this.grid.flat(2).filter(v => v === null).length;
  }

  get title(): string {
    return this.id
      ? $localize`:@@spc.gage-rr.study.title-edit:Étude R&R ${this.form.controls.partReference.value ?? ''}:part:`
      : $localize`:@@spc.gage-rr.study.title-new:Nouvelle étude R&R`;
  }

  // ---------- opérateurs ----------

  operatorName(o: number): string {
    return this.operatorNames[o] ?? operatorFallback(o);
  }

  letter(o: number): string {
    return operatorLetter(o);
  }

  /** Les personnes déjà choisies comme autres opérateurs : on ne les propose pas deux fois. */
  excludedFor(o: number): string[] {
    return OPERATOR_CONTROLS
      .filter((_, i) => i !== o && i < this.k)
      .map(c => this.form.controls[c].value)
      .filter((v): v is string => !!v);
  }

  onOperator(o: number, user: DirectoryUser | null): void {
    this.operatorNames[o] = user ? personName(user) : null;
    this.recompute();
  }

  onResponsible(user: DirectoryUser | null): void {
    this.responsibleName = user ? personName(user) : null;
  }

  // ---------- grille ----------

  cell(o: number, p: number, t: number): string {
    return this.raw[o][p][t];
  }

  cellLabel(o: number, p: number, t: number): string {
    const operator = this.operatorName(o);
    const part = p + 1;
    const trial = t + 1;
    return $localize`:@@spc.gage-rr.study.cell-aria:${operator}:operator:, pièce ${part}:part:, essai ${trial}:trial:`;
  }

  cellInvalid(o: number, p: number, t: number): boolean {
    const v = this.raw[o][p][t].trim();
    return v !== '' && parseMeasurement(v) === null;
  }

  onCell(o: number, p: number, t: number, event: Event): void {
    this.raw[o][p][t] = (event.target as HTMLInputElement).value;
    this.recompute();
  }

  /** Un bloc copié depuis un tableur remplit la grille d'un coup ; une valeur seule se colle normalement. */
  onPaste(o: number, p: number, t: number, event: ClipboardEvent): void {
    const text = event.clipboardData?.getData('text') ?? '';
    if (!/[\t\n]/.test(text)) return;
    event.preventDefault();
    this.raw = pasteBlock(this.raw, p, o * this.r + t, text, this.k, this.n, this.r);
    this.recompute();
  }

  /** La moyenne des essais d'un opérateur sur une pièce, dès qu'ils sont tous saisis. */
  rowMean(o: number, p: number): number | null {
    const v = this.complete(this.grid[o][p]);
    return v ? v.reduce((a, b) => a + b, 0) / v.length : null;
  }

  rowRange(o: number, p: number): number | null {
    const v = this.complete(this.grid[o][p]);
    return v ? Math.max(...v) - Math.min(...v) : null;
  }

  partMean(p: number): number | null {
    const v = this.complete(this.grid.flatMap(bloc => bloc[p]));
    return v ? v.reduce((a, b) => a + b, 0) / v.length : null;
  }

  /** Une étendue au-dessus de UCL<sub>R</sub> : une mesure à vérifier. */
  rangeOut(o: number, p: number): boolean {
    return this.calc !== null && this.calc.cellRanges[o][p] > this.calc.rangeUcl;
  }

  get hasMeasurements(): boolean {
    return this.raw.some(bloc => bloc.some(essais => essais.some(v => v.trim() !== '')));
  }

  /**
   * Charge le jeu de l'onglet « Données » du classeur : identification, limites,
   * plan et mesures. Les personnes ne sont pas chargées. Rien n'est enregistré ;
   * des mesures déjà saisies ne sont remplacées qu'après confirmation.
   */
  loadExample(): void {
    if (!this.canManage) return;
    if (!this.hasMeasurements) {
      this.applyExample();
      return;
    }
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@spc.gage-rr.example.confirm-title:Remplacer les mesures saisies ?`,
        message: $localize`:@@spc.gage-rr.example.confirm-message:L'exemple remplace l'identification, les limites, le plan et toutes les mesures de la grille. Rien n'est enregistré tant que vous n'enregistrez pas l'étude.`,
        confirmLabel: $localize`:@@spc.gage-rr.example.load:Charger l'exemple`
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (ok) this.applyExample();
    });
  }

  clearMeasurements(): void {
    this.raw = emptyRaw();
    this.recompute();
  }

  // ---------- enregistrement ----------

  save(): void {
    this.fieldError = null;
    if (!this.canManage || this.saving) return;
    // Les sélecteurs d'opérateurs portent `required` : un opérateur manquant rend
    // aussi le formulaire invalide. On le dit d'abord, sinon le message se perdrait.
    const operators = this.chosenOperators();
    if (!operators) {
      this.form.markAllAsTouched();
      this.fieldError = { field: 'operators', message: fieldMessage('operators') };
      return;
    }
    if (this.form.invalid || this.limitsInvalid) {
      this.form.markAllAsTouched();
      return;
    }
    const grid = this.grid;
    if (!isComplete(grid) || this.calc === null || !(this.calc.tv.value > 0)) {
      this.fieldError = { field: 'measurements', message: fieldMessage('measurements') };
      return;
    }
    const v = this.form.getRawValue();
    const req: GageRrRequest = {
      partReference: (v.partReference ?? '').trim(),
      characteristic: blankToNull(v.characteristic),
      instrument: blankToNull(v.instrument),
      lsl: this.lsl as number,
      usl: this.usl as number,
      operatorCount: this.k,
      partCount: this.n,
      trialCount: this.r,
      sigmaMultiplier: this.multiplier,
      studyDate: v.studyDate ?? '',
      responsibleUserId: v.responsibleUserId ?? null,
      responsibleName: v.responsibleUserId ? this.responsibleName : null,
      operators,
      measurements: grid
    };
    this.saving = true;
    const call = this.id ? this.service.update(this.id, req) : this.service.create(req);
    call.subscribe({
      next: sheet => {
        this.saving = false;
        this.snack.open($localize`:@@spc.gage-rr.study.saved:Étude R&R enregistrée.`, undefined, { duration: 3000 });
        if (!this.id) {
          this.router.navigate(['/spc', 'gage-rr', sheet.study.id], { replaceUrl: true });
          return;
        }
        this.reprendre(sheet);
      },
      error: err => {
        this.saving = false;
        const field = err instanceof HttpErrorResponse && err.status === 422 ? err.error?.field : undefined;
        if (typeof field === 'string') {
          this.fieldError = { field, message: fieldMessage(field) };
          return;
        }
        this.snack.open(safeErrorMessage(err, $localize`:@@spc.gage-rr.study.save-failed:L'étude R&R n'a pas pu être enregistrée.`),
          undefined, { duration: 5000 });
      }
    });
  }

  back(): void {
    this.router.navigate(['/spc', 'gage-rr']);
  }

  toggleGuide(): void {
    this.showGuide = !this.showGuide;
  }

  // ---------- gabarit ----------

  num(v: number | null | undefined, digits = 4): string { return fmt(v, digits); }
  percent(v: number | null | undefined, digits = 1): string { return pct(v, digits); }
  p(v: number | null): string { return pValue(v); }
  decision(d: GageRrDecision): string { return decisionLabel(d); }
  tone(d: GageRrDecision): string { return decisionTone(d); }
  ndcText(sufficient: boolean): string { return ndcLabel(sufficient); }
  source(row: AnovaRow, pooled: boolean): string { return anovaSourceLabel(row.source, pooled); }

  /** « max − min = a − b » sur les moyennes des opérateurs. */
  get xDiffApplication(): string {
    const c = this.calc as GageRrCalculationView;
    return `max − min = ${fmt(Math.max(...c.operatorMeans))} − ${fmt(Math.min(...c.operatorMeans))}`;
  }

  get meanRangeApplication(): string {
    const c = this.calc as GageRrCalculationView;
    return `(${c.operatorRanges.map(v => fmt(v)).join(' + ')}) / ${c.operatorRanges.length}`;
  }

  get partRangeApplication(): string {
    const c = this.calc as GageRrCalculationView;
    return `max − min = ${fmt(Math.max(...c.partMeans))} − ${fmt(Math.min(...c.partMeans))}`;
  }

  /** Comparer les deux méthodes : %GRR, %tolérance, ndc et verdict. */
  get methods(): { label: string; grr: VariationComponent; ndc: number; decision: GageRrDecision }[] {
    const c = this.calc as GageRrCalculationView;
    return [
      { label: $localize`:@@spc.gage-rr.method.average-range:Moyenne et étendue`, grr: c.grr, ndc: c.ndc, decision: c.decision },
      { label: $localize`:@@spc.gage-rr.method.anova:ANOVA`, grr: c.anova.grr, ndc: c.anova.ndc, decision: c.anova.decision }
    ];
  }

  /** Les composantes de l'ANOVA : variance, écart (σ × multiplicateur) et leurs parts. */
  get anovaComponents(): { label: string; variance: number; component: VariationComponent }[] {
    const a = (this.calc as GageRrCalculationView).anova;
    return [
      { label: $localize`:@@spc.gage-rr.anova.ev:Répétabilité (EV)`, variance: a.variance.repeatability, component: a.ev },
      { label: $localize`:@@spc.gage-rr.anova.av:Reproductibilité (AV)`, variance: a.variance.reproducibility, component: a.av },
      { label: $localize`:@@spc.gage-rr.anova.av-operator:dont opérateurs`, variance: a.variance.operator, component: a.operator },
      { label: $localize`:@@spc.gage-rr.anova.av-interaction:dont interaction`, variance: a.variance.interaction, component: a.interaction },
      { label: 'GRR', variance: a.variance.grr, component: a.grr },
      { label: $localize`:@@spc.gage-rr.anova.pv:Pièces (PV)`, variance: a.variance.part, component: a.pv },
      { label: $localize`:@@spc.gage-rr.anova.tv:Totale (TV)`, variance: a.variance.total, component: a.tv }
    ];
  }

  trackByIndex(i: number): number {
    return i;
  }

  private complete(values: (number | null)[]): number[] | null {
    return values.every((v): v is number => v !== null) ? values as number[] : null;
  }

  /** Les opérateurs du plan, chacun choisi dans l'annuaire, sans doublon ; `null` sinon. */
  private chosenOperators(): GageRrOperator[] | null {
    const chosen = this.operators.map(o => ({
      accountId: this.form.controls[OPERATOR_CONTROLS[o]].value,
      name: this.operatorNames[o]
    }));
    if (chosen.some(c => !c.accountId || !c.name)) return null;
    if (new Set(chosen.map(c => c.accountId)).size !== chosen.length) return null;
    return chosen as GageRrOperator[];
  }

  private recompute(): void {
    const grid = this.grid;
    const tolerance = this.tolerance;
    this.calc = isComplete(grid)
      ? computeGageRr(tolerance === null ? null : this.lsl, tolerance === null ? null : this.usl, this.multiplier, grid)
      : null;
    const c = this.calc;
    if (c === null || !(c.tv.value > 0)) {
      this.stepThree = [];
      this.readings = [];
      this.componentsChart = this.rangeChart = this.meanChart = this.partChart = null;
      return;
    }
    this.stepThree = stepThree(c, this.n, this.r);
    this.readings = interpretation(c);
    const names = this.operators.map(o => this.operatorName(o));
    this.componentsChart = componentsOption(c);
    this.rangeChart = rangeChartOption(c, names);
    this.meanChart = meanChartOption(c, names);
    this.partChart = byPartOption(grid as number[][][], c, names);
  }

  private reprendre(sheet: GageRrSheet): void {
    const s = sheet.study;
    this.responsibleName = s.responsibleName;
    this.operatorNames = range(MAX_OPERATORS).map(o => s.operators[o]?.name ?? null);
    this.raw = range(MAX_OPERATORS).map(o => range(MAX_PARTS).map(p => range(MAX_TRIALS).map(t => {
      const v = sheet.measurements[o]?.[p]?.[t];
      return v === null || v === undefined ? '' : String(v);
    })));
    this.form.setValue({
      partReference: s.partReference,
      characteristic: s.characteristic ?? '',
      instrument: s.instrument ?? '',
      lsl: sheet.lsl,
      usl: sheet.usl,
      operatorCount: s.operatorCount,
      partCount: s.partCount,
      trialCount: s.trialCount,
      sigmaMultiplier: s.sigmaMultiplier,
      studyDate: s.studyDate,
      responsibleUserId: s.responsibleUserId,
      op0: s.operators[0]?.accountId ?? null,
      op1: s.operators[1]?.accountId ?? null,
      op2: s.operators[2]?.accountId ?? null
    });
    this.form.markAsPristine();
    this.recompute();
  }

  private applyExample(): void {
    const ex = WORKBOOK_EXAMPLE;
    this.raw = range(MAX_OPERATORS).map(o => range(MAX_PARTS).map(p => range(MAX_TRIALS).map(t => {
      const v = ex.grid[o]?.[p]?.[t];
      return v === undefined ? '' : String(v);
    })));
    this.form.patchValue({
      partReference: ex.partReference,
      characteristic: ex.characteristic,
      instrument: ex.instrument,
      lsl: ex.lsl,
      usl: ex.usl,
      operatorCount: ex.operatorCount,
      partCount: ex.partCount,
      trialCount: ex.trialCount,
      sigmaMultiplier: ex.sigmaMultiplier
    });
    this.form.markAsDirty();
    this.recompute();
  }

  private applyAccess(): void {
    if (this.canManage) this.form.enable({ emitEvent: false });
    else this.form.disable({ emitEvent: false });
  }
}

/** L'étape 3 de l'onglet « Résultats » : chaque source, sa formule, son application numérique. */
export function stepThree(c: GageRrCalculationView, n: number, r: number): StepThreeRow[] {
  const f = (v: number) => fmt(v);
  const k = (v: number) => fmt(v, 3);
  return [
    { key: 'ev', label: $localize`:@@spc.gage-rr.step3.ev:EV – Répétabilité`, formula: 'R̄̄ × K1',
      application: `${f(c.meanRange)} × ${k(c.k1)}`, component: c.ev },
    { key: 'av', label: $localize`:@@spc.gage-rr.step3.av:AV – Reproductibilité`, formula: '√[(X̄diff × K2)² − EV² / (n × r)]',
      application: `√[(${f(c.xDiff)} × ${k(c.k2)})² − ${f(c.ev.value)}² / (${n} × ${r})]`, component: c.av },
    { key: 'grr', label: $localize`:@@spc.gage-rr.step3.grr:GRR – Répétabilité et reproductibilité`, formula: '√(EV² + AV²)',
      application: `√(${f(c.ev.value)}² + ${f(c.av.value)}²)`, component: c.grr },
    { key: 'pv', label: $localize`:@@spc.gage-rr.step3.pv:PV – Variation entre pièces`, formula: 'Rp × K3',
      application: `${f(c.partRange)} × ${k(c.k3)}`, component: c.pv },
    { key: 'tv', label: $localize`:@@spc.gage-rr.step3.tv:TV – Variation totale`, formula: '√(GRR² + PV²)',
      application: `√(${f(c.grr.value)}² + ${f(c.pv.value)}²)`, component: c.tv }
  ];
}

function range(length: number): number[] {
  return Array.from({ length }, (_, i) => i);
}

function emptyRaw(): string[][][] {
  return range(MAX_OPERATORS).map(() => range(MAX_PARTS).map(() => Array<string>(MAX_TRIALS).fill('')));
}

function toNumber(v: unknown): number | null {
  if (v === null || v === undefined || v === '') return null;
  const n = typeof v === 'number' ? v : Number(String(v).replace(',', '.'));
  return Number.isFinite(n) ? n : null;
}

function blankToNull(v: string | null | undefined): string | null {
  const t = (v ?? '').trim();
  return t ? t : null;
}

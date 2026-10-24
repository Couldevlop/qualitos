import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthzService } from '../../../../../core/authz/authz.service';
import { personName } from '../../../../../core/directory/directory-names';
import { DirectoryUser } from '../../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  CONSTANT_ROWS, MAX_SUBGROUP_SIZE, SUBGROUPS, SUBGROUP_SIZES, classify, computeCapability, displayDigits,
  measurementDecimals, parseMeasurement, pasteBlock
} from '../../spc-capability.calc';
import { completeValues } from '../../spc-capability.charts';
import { SPC_EXAMPLES, SpcExample } from '../../spc-capability.examples';
import { fmt, statusTone } from '../../spc-capability.labels';
import { CapabilityReadings, capabilityReadings } from '../../spc-capability.readings';
import { SpcCapabilityService } from '../../spc-capability.service';
import {
  CalculationView, CapabilityStatus, Measurement, StudyRequest, StudySheet
} from '../../spc-capability.types';

const DEFAULT_SUBGROUP_SIZE = 5;

/**
 * Une étude de capabilité (maquette `docs/spc/Étude de capabilité procédé –
 * Cp _ Cpk 4.html`) : identification, grille de mesures avec, à sa droite
 * comme dans le classeur, la table des constantes et toutes ses formules ;
 * résultats et statut du procédé ; cartes X̄ et R, puis distribution.
 *
 * <p>Tout se recalcule à la frappe avec la même table et les mêmes formules
 * que le serveur, qui refait le calcul à l'enregistrement : ce qui est
 * enregistré vient toujours de lui. Une étude nouvelle prend les seuils du
 * client ; une étude enregistrée garde ceux qu'elle avait à sa création.
 *
 * <p>La grille garde le texte saisi (10 lignes au plus) : réduire la taille du
 * sous-groupe ne perd pas les lignes masquées, et « 49, » en cours de frappe
 * n'est pas réécrit.
 */
@Component({
  selector: 'qos-spc-capability-study',
  templateUrl: './capability-study.component.html',
  styleUrls: ['./capability-study.component.scss'],
  standalone: false
})
export class CapabilityStudyComponent implements OnInit {

  readonly subgroupSizes = SUBGROUP_SIZES;
  readonly constantRows = CONSTANT_ROWS;
  readonly examples = SPC_EXAMPLES;
  readonly columns = Array.from({ length: SUBGROUPS }, (_, i) => i);
  readonly columnLabels = this.columns.map(i => `SG ${String(i + 1).padStart(2, '0')}`);
  readonly yes = $localize`:@@spc.study.yes:OUI`;
  readonly no = $localize`:@@spc.study.no:NON`;
  readonly xbarLabel = $localize`:@@spc.study.xbar-aria:Carte X̄ : moyenne de chaque sous-groupe et limites de contrôle`;
  readonly rangeLabel = $localize`:@@spc.study.range-aria:Carte R : étendue de chaque sous-groupe et limites de contrôle`;

  canManage = false;
  id: string | null = null;
  loading = false;
  failed = false;
  saving = false;
  showFormulas = true;

  capableThreshold = 1.67;
  watchThreshold = 1.33;

  /** Le texte des cellules, ligne par échantillon. */
  raw: string[][] = Array.from({ length: MAX_SUBGROUP_SIZE }, () => Array<string>(SUBGROUPS).fill(''));

  calc!: CalculationView;
  readings: CapabilityReadings | null = null;
  values: number[] = [];
  /** Le message d'erreur d'un champ refusé par le serveur. */
  fieldError: { field: string; message: string } | null = null;

  private leaderName: string | null = null;

  readonly form = this.fb.group({
    characteristic: ['', Validators.maxLength(150)],
    specification: ['', Validators.maxLength(150)],
    partReference: ['', [Validators.required, Validators.maxLength(120)]],
    machine: ['', Validators.maxLength(80)],
    leaderUserId: [null as string | null],
    actionPlan: [false],
    studyDate: [new Date().toISOString().slice(0, 10), Validators.required],
    lsl: [null as number | null, Validators.required],
    usl: [null as number | null, Validators.required],
    subgroupSize: [DEFAULT_SUBGROUP_SIZE, Validators.required]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly service: SpcCapabilityService,
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
    if (this.id) {
      this.charger();
    } else {
      this.service.settings().subscribe({
        next: s => {
          this.capableThreshold = s.capableThreshold;
          this.watchThreshold = s.watchThreshold;
          this.recompute();
        },
        // Sans les seuils du client, l'écran garde ceux de la plateforme ; le serveur appliquera les bons.
        error: () => undefined
      });
    }
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
        this.snack.open(safeErrorMessage(err, $localize`:@@spc.study.not-found:Étude introuvable.`),
          undefined, { duration: 5000 });
      }
    });
  }

  get n(): number {
    return Number(this.form.controls.subgroupSize.value) || DEFAULT_SUBGROUP_SIZE;
  }

  get rows(): number[] {
    return Array.from({ length: this.n }, (_, i) => i);
  }

  /** Les décimales des X̄ et des R, suivant la résolution des mesures saisies. */
  get digits(): { mean: number; range: number } {
    return displayDigits(measurementDecimals(this.raw.slice(0, this.n)));
  }

  get grid(): Measurement[][] {
    return this.raw.slice(0, this.n).map(row => row.map(parseMeasurement));
  }

  get lsl(): number | null {
    return toNumber(this.form.controls.lsl.value);
  }

  get usl(): number | null {
    return toNumber(this.form.controls.usl.value);
  }

  get limitsInvalid(): boolean {
    return this.lsl !== null && this.usl !== null && !(this.usl > this.lsl);
  }

  /** Le verdict en direct ; `null` tant que le Cpk ne se calcule pas. */
  get status(): CapabilityStatus | null {
    return this.calc.cpk === null ? null : classify(this.calc.cpk, this.capableThreshold, this.watchThreshold);
  }

  get controlled(): boolean {
    return this.status === 'CAPABLE';
  }

  get title(): string {
    return this.id
      ? $localize`:@@spc.study.title-edit:Étude de capabilité ${this.form.controls.partReference.value ?? ''}:part:`
      : $localize`:@@spc.study.title-new:Nouvelle étude de capabilité`;
  }

  // ---------- grille ----------

  cell(r: number, c: number): string {
    return this.raw[r][c];
  }

  cellLabel(r: number, c: number): string {
    return $localize`:@@spc.study.cell-aria:Échantillon ${r + 1}:sample:, sous-groupe ${c + 1}:subgroup:`;
  }

  /** Une cellule saisie mais illisible (« 4a9 ») : elle compte comme vide, on la signale. */
  cellInvalid(r: number, c: number): boolean {
    const t = this.raw[r][c].trim();
    return t !== '' && parseMeasurement(t) === null;
  }

  onCell(r: number, c: number, event: Event): void {
    this.raw[r][c] = (event.target as HTMLInputElement).value;
    this.recompute();
  }

  /** Un bloc copié depuis Excel remplit la grille d'un coup ; une valeur seule se colle normalement. */
  onPaste(r: number, c: number, event: ClipboardEvent): void {
    const text = event.clipboardData?.getData('text') ?? '';
    if (!/[\t\n]/.test(text)) return;
    event.preventDefault();
    this.raw = pasteBlock(this.raw, r, c, text);
    this.recompute();
  }

  /** Une mesure au moins est saisie : charger un exemple l'écraserait. */
  get hasMeasurements(): boolean {
    return this.raw.some(row => row.some(v => v.trim() !== ''));
  }

  exampleLabel(ex: SpcExample): string {
    return ex.id === 'workbook'
      ? $localize`:@@spc.example.workbook:Exemple 1 — classeur : poids 500 ± 25 g (maîtrisé)`
      : $localize`:@@spc.example.turning:Exemple 2 — diamètre Ø 12 ± 0,1 mm (non maîtrisé)`;
  }

  /**
   * Charge un jeu d'exemple : paramètre, tolérance, référence, limites, taille du
   * sous-groupe et mesures. Rien n'est enregistré ; des mesures déjà saisies ne
   * sont remplacées qu'après confirmation.
   */
  loadExample(ex: SpcExample): void {
    if (!this.canManage) return;
    if (!this.hasMeasurements) {
      this.applyExample(ex);
      return;
    }
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@spc.example.confirm-title:Remplacer les mesures saisies ?`,
        message: $localize`:@@spc.example.confirm-message:L'exemple remplace l'identification de la pièce, les limites et toutes les mesures de la grille. Rien n'est enregistré tant que vous n'enregistrez pas l'étude.`,
        confirmLabel: $localize`:@@spc.example.confirm:Charger l'exemple`
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (ok) this.applyExample(ex);
    });
  }

  clearMeasurements(): void {
    this.raw = this.raw.map(row => row.map(() => ''));
    this.recompute();
  }

  meanOut(c: number): boolean {
    const v = this.calc.subgroupMeans[c];
    return v !== null && this.calc.meanUcl !== null && this.calc.meanLcl !== null
      && (v > this.calc.meanUcl || v < this.calc.meanLcl);
  }

  rangeOut(c: number): boolean {
    const v = this.calc.subgroupRanges[c];
    return v !== null && this.calc.rangeUcl !== null && this.calc.rangeLcl !== null
      && (v > this.calc.rangeUcl || v < this.calc.rangeLcl);
  }

  // ---------- meneur ----------

  onLeader(user: DirectoryUser | null): void {
    this.leaderName = user ? personName(user) : null;
  }

  // ---------- enregistrement ----------

  save(): void {
    this.fieldError = null;
    if (!this.canManage || this.saving) return;
    if (this.form.invalid || this.limitsInvalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.calc.completeSubgroups === 0) {
      this.fieldError = { field: 'measurements', message: fieldMessage('measurements') };
      return;
    }
    const v = this.form.getRawValue();
    const req: StudyRequest = {
      partReference: (v.partReference ?? '').trim(),
      characteristic: blankToNull(v.characteristic),
      specification: blankToNull(v.specification),
      machine: blankToNull(v.machine),
      leaderUserId: v.leaderUserId ?? null,
      leaderName: v.leaderUserId ? this.leaderName : null,
      studyDate: v.studyDate ?? '',
      actionPlan: !!v.actionPlan,
      lsl: this.lsl as number,
      usl: this.usl as number,
      subgroupSize: this.n,
      measurements: this.grid
    };
    this.saving = true;
    const call = this.id ? this.service.update(this.id, req) : this.service.create(req);
    call.subscribe({
      next: sheet => {
        this.saving = false;
        this.snack.open($localize`:@@spc.study.saved:Étude enregistrée.`, undefined, { duration: 3000 });
        if (!this.id) {
          this.router.navigate(['/spc', 'cartes', sheet.study.id], { replaceUrl: true });
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
        this.snack.open(safeErrorMessage(err, $localize`:@@spc.study.save-failed:L'étude n'a pas pu être enregistrée.`),
          undefined, { duration: 5000 });
      }
    });
  }

  back(): void {
    this.router.navigate(['/spc', 'cartes']);
  }

  toggleFormulas(): void {
    this.showFormulas = !this.showFormulas;
  }

  // ---------- gabarit ----------

  num(v: number | null, digits = 3): string { return fmt(v, digits); }
  tone(s: CapabilityStatus): string { return statusTone(s); }
  fixed(v: number, digits = 2): string { return v.toFixed(digits); }

  private recompute(): void {
    const lsl = this.lsl;
    const usl = this.usl;
    const valid = lsl !== null && usl !== null && usl > lsl;
    const grid = this.grid;
    this.calc = computeCapability(valid ? lsl : null, valid ? usl : null, this.n, grid);
    this.values = completeValues(grid, this.n);
    this.readings = valid ? capabilityReadings(this.calc, lsl, usl, this.capableThreshold) : null;
  }

  private reprendre(sheet: StudySheet): void {
    const s = sheet.study;
    this.capableThreshold = s.capableThreshold;
    this.watchThreshold = s.watchThreshold;
    this.leaderName = s.leaderName;
    this.raw = Array.from({ length: MAX_SUBGROUP_SIZE }, (_, r) =>
      Array.from({ length: SUBGROUPS }, (_, c) => {
        const v = sheet.measurements[r]?.[c];
        return v === null || v === undefined ? '' : String(v);
      }));
    this.form.setValue({
      characteristic: s.characteristic ?? '',
      specification: s.specification ?? '',
      partReference: s.partReference,
      machine: s.machine ?? '',
      leaderUserId: s.leaderUserId,
      actionPlan: s.actionPlan,
      studyDate: s.studyDate,
      lsl: sheet.lsl,
      usl: sheet.usl,
      subgroupSize: sheet.subgroupSize
    });
    this.form.markAsPristine();
    this.recompute();
  }

  private applyExample(ex: SpcExample): void {
    this.raw = Array.from({ length: MAX_SUBGROUP_SIZE }, (_, r) =>
      Array.from({ length: SUBGROUPS }, (_, c) => {
        const v = ex.grid[r]?.[c];
        return v === null || v === undefined ? '' : String(v);
      }));
    this.form.patchValue({
      characteristic: ex.characteristic,
      specification: ex.specification,
      partReference: ex.partReference,
      lsl: ex.lsl,
      usl: ex.usl,
      subgroupSize: ex.subgroupSize
    });
    this.form.markAsDirty();
    this.recompute();
  }

  private applyAccess(): void {
    if (this.canManage) this.form.enable({ emitEvent: false });
    else this.form.disable({ emitEvent: false });
  }
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

/** Ce que le serveur a refusé, dit à l'écran sans reprendre son texte (OWASP A09). */
export function fieldMessage(field: string): string {
  switch (field) {
    case 'partReference':
      return $localize`:@@spc.study.error.part:La référence de la pièce est obligatoire (120 caractères au plus).`;
    case 'studyDate':
      return $localize`:@@spc.study.error.date:La date de l'étude est obligatoire.`;
    case 'lsl':
    case 'usl':
      return $localize`:@@spc.study.error.limits:La limite supérieure (USL) doit dépasser la limite inférieure (LSL).`;
    case 'leaderName':
      return $localize`:@@spc.study.error.leader:Le meneur choisi n'a pas pu être retrouvé dans l'annuaire.`;
    case 'measurements':
      return $localize`:@@spc.study.error.measurements:Saisissez au moins un sous-groupe complet, avec des mesures qui ne soient pas toutes identiques.`;
    default:
      return $localize`:@@spc.study.error.generic:Un champ de l'étude a été refusé : vérifiez la saisie.`;
  }
}

import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, map, switchMap } from 'rxjs/operators';

import { AuthzService } from '../../../../../core/authz/authz.service';
import { personName } from '../../../../../core/directory/directory-names';
import { DirectoryUser } from '../../../../../core/directory/directory.types';
import { filenameFromResponse, saveBlobAs } from '../../../../../core/http/file-download';
import { safeErrorMessage } from '../../../../../core/http/error-message';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  DEFAULT_SUBGROUPS, MAX_SUBGROUPS, MAX_SUBGROUP_SIZE, MIN_SUBGROUPS, RANGE_CHART_MAX,
  RANGE_CONSTANT_ROWS, RELIABLE_SUBGROUPS, SAMPLE_SIZES, STDEV_CONSTANT_ROWS, SUBGROUP_COUNTS,
  MIN_SUBGROUP_SIZE, classify, computeCapability, displayDigits,
  measurementDecimals, parseMeasurement, pasteBlock
} from '../../spc-capability.calc';
import { SpcService } from '../../../spc.service';
import { SpcAnalyzeResponse, SpcViolation } from '../../../spc.types';
import { completeValues } from '../../spc-capability.charts';
import { SPC_EXAMPLES, SpcExample } from '../../spc-capability.examples';
import { fmt, statusTone } from '../../spc-capability.labels';
import {
  NelsonSeries, nelsonSeries, rulesByColumn, seriesKey, severityTone, violationSubgroups
} from '../../spc-capability.nelson';
import { CapabilityReadings, capabilityReadings } from '../../spc-capability.readings';
import { SpcCapabilityService } from '../../spc-capability.service';
import {
  CalculationView, CapabilityStatus, ImportedGrid, Measurement, StudyRequest, StudySheet
} from '../../spc-capability.types';

const DEFAULT_SUBGROUP_SIZE = 5;

/**
 * Le délai avant qu'une série modifiée parte à l'analyse : assez court pour
 * que le signal suive la saisie, assez long pour qu'une colonne tapée chiffre
 * par chiffre ne compte que pour un appel (OWASP LLM04 : les quotas IA sont
 * comptés par client).
 */
const NELSON_DEBOUNCE_MS = 600;

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
 * <p>La grille garde le texte saisi (25 lignes au plus) : réduire la taille
 * d'échantillon ne perd pas les lignes masquées, et « 49, » en cours de frappe
 * n'est pas réécrit.
 *
 * <p>Deux réglages indépendants, et l'écran les sépare enfin (ADR 0107) : la
 * TAILLE D'ÉCHANTILLON — combien de pièces on mesure à chaque prélèvement,
 * c'est-à-dire le nombre de lignes de la grille — et le NOMBRE DE SOUS-GROUPES
 * — combien de prélèvements composent l'étude, c'est-à-dire le nombre de
 * colonnes. Les confondre sous un seul libellé « taille du sous-groupe » était
 * la source du malentendu.
 */
@Component({
  selector: 'qos-spc-capability-study',
  templateUrl: './capability-study.component.html',
  styleUrls: ['./capability-study.component.scss'],
  standalone: false
})
export class CapabilityStudyComponent implements OnInit {

  readonly sampleSizes = SAMPLE_SIZES;
  readonly rangeConstantRows = RANGE_CONSTANT_ROWS;
  readonly stdevConstantRows = STDEV_CONSTANT_ROWS;
  readonly rangeChartMax = RANGE_CHART_MAX;
  readonly examples = SPC_EXAMPLES;
  readonly minSubgroups = MIN_SUBGROUPS;
  readonly maxSubgroups = MAX_SUBGROUPS;
  readonly reliableSubgroups = RELIABLE_SUBGROUPS;
  readonly maxSampleSize = MAX_SUBGROUP_SIZE;
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

  /** La taille d'échantillon sort du domaine des deux cartes : rien n'est calculé. */
  unsupportedSize = false;

  capableThreshold = 1.67;
  watchThreshold = 1.33;

  /** Le texte des cellules, ligne par échantillon. */
  raw: string[][] = Array.from({ length: MAX_SUBGROUP_SIZE }, () => Array<string>(DEFAULT_SUBGROUPS).fill(''));

  /**
   * Le nombre de sous-groupes de l'étude : la largeur de la grille.
   *
   * <p>C'est un champ et non un calcul tiré de {@link raw} parce que la liste
   * déroulante y écrit : quand l'utilisateur réduit l'étude et refuse de perdre
   * ses mesures, on remet l'ancienne valeur ici, et la liste se recale.
   */
  subgroupCount = DEFAULT_SUBGROUPS;

  calc!: CalculationView;
  readings: CapabilityReadings | null = null;
  values: number[] = [];

  /** L'analyse IA des règles de Nelson sur la carte X̄ (ADR 0018). */
  nelson: SpcAnalyzeResponse | null = null;
  /** Les règles déclenchées par sous-groupe : ce que la carte X̄ montre. */
  nelsonRules: ReadonlyMap<number, string[]> = new Map();
  nelsonLoading = false;
  nelsonFailed = false;

  /** Les colonnes de la dernière série analysée, pour nommer les sous-groupes. */
  private nelsonColumns: number[] = [];
  /** La série déjà demandée : une frappe qui ne change pas le tracé n'appelle rien. */
  private nelsonKey: string | null = null;
  private readonly nelsonAsk$ = new Subject<NelsonSeries>();
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
    private readonly spc: SpcService,
    authz: AuthzService
  ) {
    authz.can('spc.manage').pipe(takeUntilDestroyed()).subscribe(v => {
      this.canManage = v;
      this.applyAccess();
    });
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.recompute());
    // Un seul appel en vol : switchMap abandonne l'analyse d'une série que
    // l'utilisateur vient de modifier, sinon une réponse en retard écraserait
    // les règles de la série affichée.
    this.nelsonAsk$.pipe(
      debounceTime(NELSON_DEBOUNCE_MS),
      switchMap(serie => this.spc
        .analyze({ values: serie.values, center: serie.center, sigma: serie.sigma })
        .pipe(
          map(res => ({ serie, res: res as SpcAnalyzeResponse | null })),
          // L'analyse est un PLUS : indisponible, la carte reste juste et le
          // dit. Une erreur IA ne doit pas emporter l'étude de capabilité.
          catchError(() => of({ serie, res: null }))
        )),
      takeUntilDestroyed()
    ).subscribe(({ serie, res }) => this.applyNelson(serie, res));
    this.recompute();
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id');
    if (this.id) {
      this.charger();
    } else {
      this.prefillFromProject();
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

  /** Une étude ouverte depuis un projet DMAIC reprend sa pièce et ses limites de spécification. */
  private prefillFromProject(): void {
    const q = this.route.snapshot?.queryParamMap;
    if (!q) return;
    const num = (key: string): number | null => {
      const raw = q.get(key);
      const v = raw === null || raw.trim() === '' ? NaN : Number(raw);
      return Number.isFinite(v) ? v : null;
    };
    const ref = (q.get('ref') ?? '').trim().slice(0, 120);
    if (ref) this.form.controls.partReference.setValue(ref);
    const lsl = num('lsl');
    const usl = num('usl');
    if (lsl !== null) this.form.controls.lsl.setValue(lsl);
    if (usl !== null) this.form.controls.usl.setValue(usl);
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

  /** La taille d'échantillon : combien de mesures composent un prélèvement. */
  get n(): number {
    return Number(this.form.controls.subgroupSize.value) || DEFAULT_SUBGROUP_SIZE;
  }

  /**
   * Les lignes de la grille : une par mesure du prélèvement.
   *
   * <p>Bornées par la hauteur réelle de {@link raw} : une taille d'échantillon
   * hors domaine ne doit pas faire lire une ligne qui n'existe pas.
   */
  get rows(): number[] {
    return Array.from({ length: Math.min(this.n, this.raw.length) }, (_, i) => i);
  }

  /** La carte que la taille d'échantillon impose : X̄ – R jusqu'à dix, X̄ – S au-delà. */
  get rangeChart(): boolean {
    return this.calc.dispersionChart === 'RANGE';
  }

  /**
   * Ce que la taille d'échantillon implique, dit sous le champ : la carte
   * retenue et l'estimateur de σ. L'utilisateur choisit un nombre de mesures,
   * pas une carte — mais il doit voir laquelle son choix entraîne.
   */
  chartHint(): string {
    if (this.unsupportedSize) {
      return $localize`:@@spc.study.chart-none:Hors domaine : aucune carte applicable.`;
    }
    const max = `${this.rangeChartMax}`;
    return this.rangeChart
      ? $localize`:@@spc.study.chart-range:Carte X̄ – R (σ = R̄ / d2), jusqu'à ${max}:max: mesures.`
      : $localize`:@@spc.study.chart-stdev:Carte X̄ – S (σ = s̄ / c4) : au-delà de ${max}:max: mesures, l'étendue estime mal σ.`;
  }

  /** « R » ou « S » : le symbole de la dispersion, dans la grille et les formules. */
  get dispersionSymbol(): string {
    return this.rangeChart ? 'R' : 'S';
  }

  /** « R̄ » ou « s̄ » : la dispersion moyenne. */
  get meanDispersionSymbol(): string {
    return this.rangeChart ? 'R̄' : 's̄';
  }

  /** Les noms des facteurs de la carte en vigueur, pour les libellés de formules. */
  get factorNames(): { sigma: string; mean: string; lower: string; upper: string } {
    return this.rangeChart
      ? { sigma: 'd2', mean: 'A2', lower: 'D3', upper: 'D4' }
      : { sigma: 'c4', mean: 'A3', lower: 'B3', upper: 'B4' };
  }

  /**
   * Les nombres de sous-groupes proposés : les paliers, plus la largeur courante
   * si elle n'en fait pas partie (une étude reprise, un bloc collé depuis un
   * tableur). Sans cela, une étude de vingt-deux sous-groupes s'afficherait vide.
   */
  get subgroupCountOptions(): number[] {
    const paliers = [...SUBGROUP_COUNTS];
    if (this.subgroupCount >= MIN_SUBGROUPS && !paliers.includes(this.subgroupCount)) {
      paliers.push(this.subgroupCount);
      paliers.sort((a, b) => a - b);
    }
    return paliers;
  }

  get columns(): number[] {
    return Array.from({ length: this.subgroupCount }, (_, i) => i);
  }

  get columnLabels(): string[] {
    return this.columns.map(i => `SG ${String(i + 1).padStart(2, '0')}`);
  }

  /**
   * L'étude porte moins de sous-groupes que la règle de l'art n'en demande.
   *
   * <p>On le DIT, on ne l'interdit pas : une étude en cours de collecte est
   * légitime, et ses limites de contrôle se resserreront. Les cacher
   * obligerait à attendre vingt relevés pour voir quoi que ce soit.
   */
  get fewSubgroups(): boolean {
    return this.subgroupCount > 0 && this.subgroupCount < RELIABLE_SUBGROUPS;
  }

  /**
   * Règle le nombre de sous-groupes depuis la liste déroulante.
   *
   * <p>Élargir n'enlève rien. Réduire sur des colonnes vides n'enlève rien non
   * plus. Réduire sur des colonnes SAISIES demande confirmation : on ne jette
   * pas des mesures d'un clic, et refuser remet la liste sur l'ancienne valeur.
   */
  onSubgroupCount(count: number): void {
    const precedent = this.subgroupCount;
    if (!this.canManage || count === precedent
      || count < MIN_SUBGROUPS || count > MAX_SUBGROUPS) {
      return;
    }
    this.subgroupCount = count;
    if (count > precedent || !this.wouldLoseMeasurements(count)) {
      this.resize(count);
      return;
    }
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@spc.study.shrink-title:Retirer des sous-groupes saisis ?`,
        message: $localize`:@@spc.study.shrink-message:Les sous-groupes retirés portent des mesures : elles seront perdues. Rien n'est enregistré tant que vous n'enregistrez pas l'étude.`,
        confirmLabel: $localize`:@@spc.study.shrink-confirm:Retirer les sous-groupes`
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (ok) {
        this.resize(count);
        return;
      }
      this.subgroupCount = precedent;
    });
  }

  /** Les colonnes au-delà de `count` portent au moins une mesure saisie. */
  private wouldLoseMeasurements(count: number): boolean {
    return this.raw.some(row => row.slice(count).some(v => v.trim() !== ''));
  }

  private resize(count: number): void {
    this.raw = this.raw.map(row => Array.from({ length: count }, (_, c) => row[c] ?? ''));
    this.subgroupCount = count;
    this.recompute();
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
    this.subgroupCount = this.raw[0]?.length ?? 0;
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

  // ---------- import Excel de la grille ----------

  /** Un classeur est en cours de lecture par le serveur. */
  importing = false;
  /** Le modèle est en cours de téléchargement. */
  downloadingTemplate = false;
  /** Ce que la lecture a écarté ou corrigé, dit en clair. Jamais un import muet. */
  importWarnings: string[] = [];
  /** Pourquoi l'import a échoué. */
  importError: string | null = null;

  /**
   * Télécharge le classeur modèle, aux dimensions de l'étude en cours.
   *
   * <p>Aux dimensions courantes, et non à des dimensions figées : un modèle de
   * 5 × 20 renvoyé à qui règle une étude de 3 × 12 se remplit de travers.
   */
  downloadTemplate(): void {
    if (this.downloadingTemplate) return;
    this.downloadingTemplate = true;
    this.importError = null;
    this.service.measurementsTemplate(this.n, this.subgroupCount).subscribe({
      next: reponse => {
        this.downloadingTemplate = false;
        if (!reponse.body) {
          this.importError = $localize`:@@spc.import.template-failed:Le modèle n'a pas pu être téléchargé.`;
          return;
        }
        saveBlobAs(reponse.body, filenameFromResponse(reponse)
          ?? `mesures-capabilite-${this.n}x${this.subgroupCount}.xlsx`);
      },
      error: () => {
        this.downloadingTemplate = false;
        this.importError = $localize`:@@spc.import.template-failed:Le modèle n'a pas pu être téléchargé.`;
      }
    });
  }

  /**
   * Un classeur a été choisi : on le lit, après confirmation s'il écrase une saisie.
   *
   * <p>Le champ de fichier est remis à zéro dans tous les cas, sinon choisir deux
   * fois le même fichier ne déclenche aucun événement — et l'utilisateur croit
   * que l'import ne répond plus.
   */
  onWorkbookChosen(event: Event): void {
    const champ = event.target as HTMLInputElement;
    const fichier = champ.files?.[0] ?? null;
    champ.value = '';
    if (!fichier || !this.canManage) return;
    this.importError = null;
    this.importWarnings = [];
    if (!/\.xlsx$/i.test(fichier.name)) {
      this.importError = $localize`:@@spc.import.wrong-extension:Attendu un classeur Excel .xlsx (ni .xls, ni .csv).`;
      return;
    }
    if (!this.hasMeasurements) {
      this.readWorkbook(fichier);
      return;
    }
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@spc.import.confirm-title:Remplacer les mesures saisies ?`,
        message: $localize`:@@spc.import.confirm-message:Le classeur remplace toutes les mesures de la grille. L'identification de la pièce et les limites ne changent pas, et rien n'est enregistré tant que vous n'enregistrez pas l'étude.`,
        confirmLabel: $localize`:@@spc.import.confirm:Importer le classeur`
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (ok) this.readWorkbook(fichier);
    });
  }

  private readWorkbook(fichier: File): void {
    this.importing = true;
    this.service.importMeasurements(fichier).subscribe({
      next: grille => {
        this.importing = false;
        this.applyImportedGrid(grille);
      },
      error: (err: HttpErrorResponse) => {
        this.importing = false;
        this.importError = safeErrorMessage(err,
          $localize`:@@spc.import.failed:Le classeur n'a pas pu être lu.`);
      }
    });
  }

  /**
   * Pose la grille lue dans la grille de saisie.
   *
   * <p>La taille d'échantillon et le nombre de sous-groupes suivent le classeur :
   * importer douze colonnes sans élargir la grille en cacherait huit. La grille
   * garde sa hauteur maximale en mémoire, comme à la saisie : réduire la taille
   * d'échantillon ne doit pas perdre les lignes masquées.
   */
  private applyImportedGrid(grille: ImportedGrid): void {
    const largeur = grille.subgroupCount;
    this.raw = Array.from({ length: MAX_SUBGROUP_SIZE }, (_, r) =>
      Array.from({ length: largeur }, (_, c) => grille.rows[r]?.[c] ?? ''));
    this.subgroupCount = largeur;
    // La taille d'échantillon n'existe qu'aux paliers tabulés : au-delà, la grille
    // montrerait des lignes que le calcul ne sait pas pondérer.
    const lignes = Math.max(MIN_SUBGROUP_SIZE, Math.min(grille.sampleSize, MAX_SUBGROUP_SIZE));
    this.form.patchValue({ subgroupSize: lignes });
    this.form.markAsDirty();
    this.importWarnings = grille.warnings ?? [];
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
    if (this.form.invalid || this.limitsInvalid || this.unsupportedSize) {
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
    // Hors du domaine des deux cartes, on ne calcule RIEN et on le dit : un Cpk
    // tiré d'une constante inexistante serait un chiffre faux présenté comme un
    // résultat. Le cas n'arrive pas depuis la liste déroulante ; il arriverait
    // d'une fiche venue d'ailleurs, et l'écran doit alors rester honnête.
    const n = this.n;
    this.unsupportedSize = !Number.isInteger(n) || n < MIN_SUBGROUP_SIZE || n > MAX_SUBGROUP_SIZE;
    if (this.unsupportedSize) {
      this.calc = computeCapability(null, null, DEFAULT_SUBGROUP_SIZE, []);
      this.readings = null;
      this.values = [];
      return;
    }
    const lsl = this.lsl;
    const usl = this.usl;
    const valid = lsl !== null && usl !== null && usl > lsl;
    const grid = this.grid;
    this.calc = computeCapability(valid ? lsl : null, valid ? usl : null, this.n, grid);
    this.values = completeValues(grid, this.n);
    this.readings = valid ? capabilityReadings(this.calc, lsl, usl, this.capableThreshold) : null;
    this.askNelson();
  }

  // ---------- règles de Nelson (détection IA) ----------

  /**
   * Demande l'analyse des règles de Nelson sur la carte X̄ courante.
   *
   * <p>Automatique : la détection n'a pas à être déclenchée à la main, sinon
   * elle ne tourne jamais. Mais elle ne repart que si le TRACÉ a changé —
   * corriger la référence de la pièce ne consomme aucun appel IA.
   */
  private askNelson(): void {
    const serie = nelsonSeries(this.calc);
    if (!serie) {
      this.resetNelson();
      return;
    }
    const key = seriesKey(serie);
    if (key === this.nelsonKey) return;
    this.nelsonKey = key;
    this.nelsonLoading = true;
    this.nelsonFailed = false;
    this.nelsonAsk$.next(serie);
  }

  /** Relance la détection après un échec (le serveur IA peut être revenu). */
  retryNelson(): void {
    this.nelsonKey = null;
    this.askNelson();
  }

  private applyNelson(serie: NelsonSeries, res: SpcAnalyzeResponse | null): void {
    this.nelsonLoading = false;
    if (!res) {
      this.nelsonFailed = true;
      this.nelson = null;
      this.nelsonRules = new Map();
      this.nelsonColumns = [];
      return;
    }
    this.nelsonFailed = false;
    this.nelson = res;
    this.nelsonColumns = serie.columns;
    this.nelsonRules = rulesByColumn(res.violations, serie.columns);
  }

  private resetNelson(): void {
    this.nelsonKey = null;
    this.nelsonLoading = false;
    this.nelsonFailed = false;
    this.nelson = null;
    this.nelsonRules = new Map();
    this.nelsonColumns = [];
  }

  get nelsonViolations(): SpcViolation[] {
    return this.nelson?.violations ?? [];
  }

  /** La carte a été analysée et aucune règle ne s'est déclenchée. */
  get nelsonClean(): boolean {
    return this.nelson !== null && this.nelsonViolations.length === 0;
  }

  /** Les sous-groupes visés par une règle, écrits « 7, 8, 9 ». */
  violationPoints(v: SpcViolation): string {
    return violationSubgroups(v, this.nelsonColumns).join(', ');
  }

  violationTone(v: SpcViolation): string {
    return severityTone(v.severity);
  }

  /** Le titre de la règle, tel que le service le renvoie, sans rang technique. */
  violationLabel(v: SpcViolation): string {
    return (v.title ?? '').trim() || v.rule;
  }

  private reprendre(sheet: StudySheet): void {
    const s = sheet.study;
    this.capableThreshold = s.capableThreshold;
    this.watchThreshold = s.watchThreshold;
    this.leaderName = s.leaderName;
    // La largeur vient de l'etude enregistree : une etude de douze sous-groupes
    // se rouvre avec douze colonnes, pas vingt dont huit vides.
    const largeur = sheet.measurements[0]?.length || DEFAULT_SUBGROUPS;
    this.raw = Array.from({ length: MAX_SUBGROUP_SIZE }, (_, r) =>
      Array.from({ length: largeur }, (_, c) => {
        const v = sheet.measurements[r]?.[c];
        return v === null || v === undefined ? '' : String(v);
      }));
    this.subgroupCount = largeur;
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
    const largeur = ex.grid[0]?.length || DEFAULT_SUBGROUPS;
    this.raw = Array.from({ length: MAX_SUBGROUP_SIZE }, (_, r) =>
      Array.from({ length: largeur }, (_, c) => {
        const v = ex.grid[r]?.[c];
        return v === null || v === undefined ? '' : String(v);
      }));
    this.subgroupCount = largeur;
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

import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, of, switchMap } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

import { AuthzService } from '../../../../core/authz/authz.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { DocumentProcess } from '../../../documents/documents.types';
import { DocumentsService } from '../../../documents/documents.service';
import { KpisService } from '../../../kpis/kpis.service';
import { KpiResponse } from '../../../kpis/kpis.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PERIODES } from '../../objectives.labels';
import { ObjectivesService } from '../../objectives.service';
import { CreateObjectiveRequest, KpiDirection, QualityObjective } from '../../objectives.types';

/** Ce que la fenêtre reçoit : rien pour créer, l'objectif pour corriger. */
export interface ObjectiveDialogData {
  objective?: QualityObjective;
}

/**
 * « Nouvel objectif qualité » — les trois sections de la maquette, dans l'ordre :
 * l'objectif, le KPI réel à comparer, la règle de déclenchement CAPA.
 *
 * <p><b>Aucune unité n'est écrite sur les champs de cible et de seuils.</b> Elle
 * change d'un indicateur à l'autre, et l'écrire en dur ferait mentir l'écran dès
 * le deuxième objectif. Elle est affichée en suffixe, reprise du KPI choisi —
 * c'est exactement ce que demande l'annotation de la maquette.
 *
 * <p><b>Les seuils sont repris du KPI choisi</b>, et modifiables. C'est la
 * demande de la recette : ne pas saisir deux fois la même cible dans deux
 * fenêtres, et ne pas risquer de les voir diverger. Modifiés ici, la fenêtre
 * PROPOSE de les reporter sur l'indicateur — elle ne le fait pas d'office : un
 * même KPI peut porter deux objectifs de cibles différentes (ADR 0133 §1), et
 * écrire sans demander écraserait silencieusement le premier.
 *
 * <p><b>Les seuils sont ordonnés dans le sens du KPI</b>, vérifié ici comme côté
 * serveur : un seuil d'alerte du bon côté de la cible ne déclencherait jamais, et
 * l'objectif aurait l'air réglé sans rien dire. L'écran le signale avant l'envoi
 * plutôt que de laisser découvrir le refus au clic.
 */
@Component({
  selector: 'qos-objective-dialog',
  templateUrl: './objective-dialog.component.html',
  styleUrls: ['./objective-dialog.component.scss'],
  standalone: false
})
export class ObjectiveDialogComponent implements OnInit {

  readonly periodes = PERIODES;

  /** Le catalogue des KPI : un objectif se mesure contre l'un d'eux. */
  kpis: KpiResponse[] = [];
  /** Les processus du référentiel documentaire, repris et non ressaisis. */
  processes: DocumentProcess[] = [];

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(250)]],
    processId: [''],
    isoClause: ['6.2', [Validators.maxLength(40)]],
    kpiId: ['', [Validators.required]],
    periodMonths: [1, [Validators.required]],
    targetValue: [null as number | null, [Validators.required]],
    thresholdWarning: [null as number | null, [Validators.required]],
    thresholdCritical: [null as number | null, [Validators.required]],
    warningPeriods: [2, [Validators.required, Validators.min(1)]],
    criticalPeriods: [1, [Validators.required, Validators.min(1)]],
    recoveryPeriods: [1, [Validators.required, Validators.min(1)]]
  });

  /** Vrai quand l'utilisateur peut écrire au catalogue des KPI : sinon, rien à proposer. */
  canManageKpis = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ObjectivesService,
    private readonly kpiSvc: KpisService,
    private readonly docs: DocumentsService,
    private readonly authz: AuthzService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ObjectiveDialogComponent, QualityObjective>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ObjectiveDialogData
  ) {}

  ngOnInit(): void {
    this.authz.can('kpi.manage').subscribe(ok => (this.canManageKpis = ok));
    this.kpiSvc.list(0, 200).subscribe({
      next: page => (this.kpis = page.content ?? []),
      error: () => (this.kpis = [])
    });
    this.docs.processes().subscribe({
      next: list => (this.processes = list ?? []),
      error: () => (this.processes = [])
    });
    if (this.data?.objective) {
      this.prefill(this.data.objective);
    }
  }

  /**
   * Le KPI vient d'être choisi : ses seuils descendent dans les trois champs.
   *
   * <p>À LA CRÉATION SEULEMENT, et seulement sur les valeurs que le KPI porte :
   * un indicateur sans cible ne doit pas effacer ce qui est déjà saisi. En
   * modification, le KPI est verrouillé et les seuils sont ceux de l'objectif —
   * les remplacer par ceux du catalogue réécrirait un engagement pris.
   */
  onKpiChosen(): void {
    if (this.editing) {
      return;
    }
    const k = this.kpi;
    if (!k) {
      return;
    }
    const valeurs: Record<string, number> = {};
    if (k.targetValue != null) valeurs['targetValue'] = k.targetValue;
    if (k.thresholdWarning != null) valeurs['thresholdWarning'] = k.thresholdWarning;
    if (k.thresholdCritical != null) valeurs['thresholdCritical'] = k.thresholdCritical;
    this.form.patchValue(valeurs);
  }

  get editing(): boolean {
    return !!this.data?.objective;
  }

  get dialogTitle(): string {
    return this.editing
      ? $localize`:@@objectives.dialog.edit-title:Modifier l'objectif qualité`
      : $localize`:@@objectives.dialog.title:Nouvel objectif qualité`;
  }

  /** Le KPI choisi, d'où viennent l'unité et le sens d'amélioration. */
  get kpi(): KpiResponse | undefined {
    return this.kpis.find(k => k.id === this.form.controls.kpiId.value);
  }

  /** L'unité, affichée en suffixe et jamais écrite en dur (annotation de la maquette). */
  get unit(): string {
    return this.kpi?.unit ?? '';
  }

  get direction(): KpiDirection | undefined {
    return this.kpi?.direction as KpiDirection | undefined;
  }

  /** « Diminuer » ou « Augmenter », repris du KPI : ce n'est pas une saisie. */
  get directionLabel(): string {
    switch (this.direction) {
      case 'LOWER_IS_BETTER':
        return $localize`:@@objectives.direction.lower:Diminuer`;
      case 'HIGHER_IS_BETTER':
        return $localize`:@@objectives.direction.higher:Augmenter`;
      default:
        return $localize`:@@objectives.direction.unknown:Choisissez un KPI`;
    }
  }

  /**
   * Ce qui empêche de valider, dit en clair plutôt que par un bouton grisé.
   *
   * <p>Le désordre des seuils vient en premier : c'est la seule erreur que
   * l'utilisateur ne peut pas deviner d'un champ vide.
   */
  get blockedReason(): string | undefined {
    if (this.thresholdsOutOfOrder) {
      return this.direction === 'HIGHER_IS_BETTER'
        ? $localize`:@@objectives.thresholds.order-higher:Sur un KPI qu'on cherche à augmenter, la cible doit être supérieure ou égale au seuil d'alerte, lui-même supérieur ou égal au seuil critique.`
        : $localize`:@@objectives.thresholds.order-lower:Sur un KPI qu'on cherche à diminuer, la cible doit être inférieure ou égale au seuil d'alerte, lui-même inférieur ou égal au seuil critique.`;
    }
    return undefined;
  }

  /**
   * Les seuils sont-ils à l'envers du sens du KPI ?
   *
   * <p>Sans KPI choisi ou sans les trois valeurs, la question ne se pose pas
   * encore : signaler un désordre sur un formulaire à moitié rempli ferait
   * reproche d'une saisie en cours.
   */
  get thresholdsOutOfOrder(): boolean {
    const cible = this.form.controls.targetValue.value;
    const alerte = this.form.controls.thresholdWarning.value;
    const critique = this.form.controls.thresholdCritical.value;
    if (cible == null || alerte == null || critique == null || !this.direction) {
      return false;
    }
    return this.direction === 'LOWER_IS_BETTER'
      ? !(cible <= alerte && alerte <= critique)
      : !(cible >= alerte && alerte >= critique);
  }

  /**
   * Les seuils saisis s'écartent-ils de ceux du KPI ?
   *
   * <p>Ni l'un ni l'autre n'a tort : la cible du catalogue dit la santé de
   * l'indicateur, celle de l'objectif l'engagement pris. C'est pourquoi on
   * DEMANDE au lieu d'écrire.
   */
  get thresholdsDifferFromKpi(): boolean {
    const k = this.kpi;
    if (!k || !this.canManageKpis) {
      return false;
    }
    const v = this.form.getRawValue();
    return v.targetValue !== (k.targetValue ?? null)
      || v.thresholdWarning !== (k.thresholdWarning ?? null)
      || v.thresholdCritical !== (k.thresholdCritical ?? null);
  }

  submit(): void {
    if (this.form.invalid || this.thresholdsOutOfOrder || this.submitting) {
      return;
    }
    const v = this.form.getRawValue();
    const processName = this.processes.find(p => p.id === v.processId)?.name;
    const body: CreateObjectiveRequest = {
      title: v.title.trim(),
      kpiId: v.kpiId,
      processId: v.processId || null,
      processName: processName ?? null,
      isoClause: v.isoClause?.trim() || null,
      periodMonths: v.periodMonths,
      targetValue: v.targetValue as number,
      thresholdWarning: v.thresholdWarning as number,
      thresholdCritical: v.thresholdCritical as number,
      warningPeriods: v.warningPeriods,
      criticalPeriods: v.criticalPeriods,
      recoveryPeriods: v.recoveryPeriods
    };
    this.submitting = true;
    const call = this.editing
      // Le KPI visé ne change pas : un objectif qui changerait d'indicateur
      // rendrait incomparable son propre historique.
      ? this.svc.update(this.data!.objective!.id, { ...body, kpiId: undefined } as never)
      : this.svc.create(body);
    call.pipe(
      // Le KPI n'est repris qu'APRÈS l'enregistrement de l'objectif : l'inverse
      // aurait réécrit le catalogue pour un objectif qui n'a pas abouti.
      switchMap(saved => this.reporterSurLeKpi(body).pipe(map(() => saved))),
      finalize(() => (this.submitting = false))
    ).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@objectives.error.save:L'objectif n'a pas pu être enregistré.`),
        undefined, { duration: 6000 })
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  /**
   * Propose de reporter les seuils saisis sur le KPI, et le fait si on accepte.
   *
   * <p>Un échec n'annule RIEN : l'objectif est enregistré, et c'est lui qui
   * compte. Le catalogue se corrigera depuis l'écran des KPI.
   */
  private reporterSurLeKpi(body: CreateObjectiveRequest): Observable<unknown> {
    const k = this.kpi;
    if (!k || !this.thresholdsDifferFromKpi) {
      return of(null);
    }
    return this.dialog.open(ConfirmDialogComponent, {
      panelClass: 'qos-dialog-panel',
      data: {
        title: $localize`:@@objectives.kpi-sync.title:Reporter ces seuils sur le KPI ?`,
        message: $localize`:@@objectives.kpi-sync.message:Le KPI « ${k.name}:kpi: » (${k.code}:code:) porte d'autres seuils. Les remplacer par ceux de cet objectif évite de les saisir deux fois — mais un autre objectif adossé au même indicateur garde les siens.`,
        confirmLabel: $localize`:@@objectives.kpi-sync.confirm:Reporter sur le KPI`,
        cancelLabel: $localize`:@@objectives.kpi-sync.cancel:Laisser le KPI tel quel`
      }
    }).afterClosed().pipe(
      switchMap(ok => ok
        ? this.kpiSvc.update(k.id, {
            targetValue: body.targetValue,
            thresholdWarning: body.thresholdWarning,
            thresholdCritical: body.thresholdCritical
          } as never).pipe(catchError(() => of(null)))
        : of(null))
    );
  }

  private prefill(o: QualityObjective): void {
    this.form.patchValue({
      title: o.title,
      processId: o.processId ?? '',
      isoClause: o.isoClause ?? '',
      kpiId: o.kpiId,
      periodMonths: o.periodMonths,
      targetValue: o.targetValue,
      thresholdWarning: o.thresholdWarning,
      thresholdCritical: o.thresholdCritical,
      warningPeriods: o.warningPeriods,
      criticalPeriods: o.criticalPeriods,
      recoveryPeriods: o.recoveryPeriods
    });
    // Le KPI ne se change pas après coup : la liste reste affichée, verrouillée.
    this.form.controls.kpiId.disable();
  }
}

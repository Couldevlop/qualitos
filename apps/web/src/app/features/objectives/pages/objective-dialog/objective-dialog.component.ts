import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { DirectoryUser } from '../../../../core/directory/directory.types';
import { personName } from '../../../../core/directory/directory-names';
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
    recoveryPeriods: [1, [Validators.required, Validators.min(1)]],
    capaOwnerId: ['', [Validators.required]],
    rootCauseDueDays: [5, [Validators.required, Validators.min(1)]]
  });

  private capaOwnerName?: string;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ObjectivesService,
    private readonly kpiSvc: KpisService,
    private readonly docs: DocumentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ObjectiveDialogComponent, QualityObjective>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ObjectiveDialogData
  ) {}

  ngOnInit(): void {
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

  /** Le nom est RECOPIÉ au choix (ADR 0052) : une revue n'affiche pas un UUID. */
  onCapaOwner(user: DirectoryUser | null): void {
    this.capaOwnerName = user ? personName(user) : undefined;
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
      recoveryPeriods: v.recoveryPeriods,
      capaOwnerId: v.capaOwnerId,
      capaOwnerName: this.capaOwnerName ?? this.data?.objective?.capaOwnerName ?? null,
      rootCauseDueDays: v.rootCauseDueDays
    };
    this.submitting = true;
    const call = this.editing
      // Le KPI visé ne change pas : un objectif qui changerait d'indicateur
      // rendrait incomparable son propre historique.
      ? this.svc.update(this.data!.objective!.id, { ...body, kpiId: undefined } as never)
      : this.svc.create(body);
    call.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@objectives.error.save:L'objectif n'a pas pu être enregistré.`),
        undefined, { duration: 6000 })
    });
  }

  cancel(): void {
    this.dialogRef.close();
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
      recoveryPeriods: o.recoveryPeriods,
      capaOwnerId: o.capaOwnerId,
      rootCauseDueDays: o.rootCauseDueDays
    });
    // Le KPI ne se change pas après coup : la liste reste affichée, verrouillée.
    this.form.controls.kpiId.disable();
    this.capaOwnerName = o.capaOwnerName ?? undefined;
  }
}

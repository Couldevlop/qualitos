import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { FmeaService } from '../../fmea.service';
import { FmeaItemResponse } from '../../fmea.types';

export interface FmeaItemDialogData {
  projectId: string;
  item?: FmeaItemResponse;       // present → edit; absent → create
  criticalRpnThreshold: number;  // for inline RPN colouring
}

@Component({
  selector: 'qos-fmea-item-dialog',
  templateUrl: './fmea-item-dialog.component.html',
  styleUrls: ['./fmea-item-dialog.component.scss'],
  standalone: false
})
export class FmeaItemDialogComponent {

  submitting = false;

  readonly scale = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  readonly form = this.fb.nonNullable.group({
    function:        [this.data.item?.function        ?? '', [Validators.maxLength(500)]],
    failureMode:     [this.data.item?.failureMode     ?? '', [Validators.maxLength(500)]],
    failureEffect:   [this.data.item?.failureEffect   ?? '', [Validators.maxLength(500)]],
    failureCause:    [this.data.item?.failureCause    ?? '', [Validators.maxLength(1000)]],
    currentControls: [this.data.item?.currentControls ?? '', [Validators.maxLength(1000)]],

    severity:   [this.data.item?.severity   ?? 5, [Validators.required, Validators.min(1), Validators.max(10)]],
    occurrence: [this.data.item?.occurrence ?? 5, [Validators.required, Validators.min(1), Validators.max(10)]],
    detection:  [this.data.item?.detection  ?? 5, [Validators.required, Validators.min(1), Validators.max(10)]],

    recommendedAction: [this.data.item?.recommendedAction ?? '', [Validators.maxLength(1000)]],
    actionDueDate:     [this.data.item?.actionDueDate     ?? ''],

    resultingSeverity:   [this.data.item?.resultingSeverity   ?? null as number | null],
    resultingOccurrence: [this.data.item?.resultingOccurrence ?? null as number | null],
    resultingDetection:  [this.data.item?.resultingDetection  ?? null as number | null]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: FmeaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FmeaItemDialogComponent, FmeaItemResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FmeaItemDialogData
  ) {}

  get dialogTitle(): string {
    return this.data.item
      ? $localize`:@@fmea.item.title-edit:Modifier l'item FMEA`
      : $localize`:@@fmea.item.title-create:Nouvel item FMEA`;
  }

  get submitLabel(): string {
    return this.data.item
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.add:Ajouter`;
  }

  get rpnLive(): number {
    const v = this.form.getRawValue();
    return (v.severity ?? 0) * (v.occurrence ?? 0) * (v.detection ?? 0);
  }

  get rpnAfterLive(): number | null {
    const v = this.form.getRawValue();
    const s = v.resultingSeverity, o = v.resultingOccurrence, d = v.resultingDetection;
    if (!s || !o || !d) return null;
    return s * o * d;
  }

  rpnClass(value: number): string {
    if (!value) return 'rpn rpn-na';
    if (value >= this.data.criticalRpnThreshold) return 'rpn rpn-critical';
    if (value >= this.data.criticalRpnThreshold * 0.6) return 'rpn rpn-high';
    return 'rpn rpn-ok';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    const payload = {
      function: v.function?.trim() || undefined,
      failureMode: v.failureMode?.trim() || undefined,
      failureEffect: v.failureEffect?.trim() || undefined,
      failureCause: v.failureCause?.trim() || undefined,
      currentControls: v.currentControls?.trim() || undefined,
      severity: v.severity, occurrence: v.occurrence, detection: v.detection,
      recommendedAction: v.recommendedAction?.trim() || undefined,
      actionDueDate: v.actionDueDate || undefined,
      resultingSeverity:   v.resultingSeverity   ?? undefined,
      resultingOccurrence: v.resultingOccurrence ?? undefined,
      resultingDetection:  v.resultingDetection  ?? undefined
    };

    const op$ = this.data.item
      ? this.svc.updateItem(this.data.projectId, this.data.item.id, payload)
      : this.svc.addItem(this.data.projectId, payload);

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: item => {
          this.snack.open(this.data.item ? $localize`:@@fmea.item.updated:Item mis à jour.` : $localize`:@@fmea.item.added:Item ajouté.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.dialogRef.close(item);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fmea-item] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@fmea.item.save-error:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

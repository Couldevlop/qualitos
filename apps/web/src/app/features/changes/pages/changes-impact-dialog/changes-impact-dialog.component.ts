import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ChangesService } from '../../changes.service';
import { ChangeImpactTargetType, ImpactResponse } from '../../changes.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface ChangesImpactDialogData { changeId: string; }

@Component({
  selector: 'qos-changes-impact-dialog',
  templateUrl: './changes-impact-dialog.component.html',
  styleUrls: ['./changes-impact-dialog.component.scss'],
  standalone: false
})
export class ChangesImpactDialogComponent {

  submitting = false;

  readonly targetTypes: { value: ChangeImpactTargetType; label: string }[] = [
    { value: 'DOCUMENT',      label: $localize`:@@changes.impact-target.document:Document` },
    { value: 'TRAINING_PATH', label: $localize`:@@changes.impact-target.training-path:Parcours de formation` },
    { value: 'SUPPLIER',      label: $localize`:@@changes.impact-target.supplier:Fournisseur` },
    { value: 'IOT_DEVICE',    label: $localize`:@@changes.impact-target.iot-device:Équipement IoT` },
    { value: 'FMEA_PROJECT',  label: $localize`:@@changes.impact-target.fmea-project:Projet FMEA` },
    { value: 'PDCA_CYCLE',    label: $localize`:@@changes.impact-target.pdca-cycle:Cycle PDCA` },
    { value: 'STANDARD',      label: $localize`:@@changes.impact-target.standard:Norme` },
    { value: 'OTHER',         label: $localize`:@@changes.impact-target.other:Autre` }
  ];

  readonly form = this.fb.nonNullable.group({
    targetType: ['DOCUMENT' as ChangeImpactTargetType, [Validators.required]],
    targetId:   ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    notes:      ['', [Validators.maxLength(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ChangesService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ChangesImpactDialogComponent, ImpactResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ChangesImpactDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.addImpact(this.data.changeId, {
      targetType: v.targetType,
      targetId: v.targetId.trim(),
      notes: v.notes?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: im => { this.snack.open($localize`:@@changes.impact.added:Impact ajouté.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(im); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[changes-impact] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-add:Erreur lors de l'ajout.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

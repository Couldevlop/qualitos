import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EhsService } from '../../ehs.service';
import { IncidentSeverity, IncidentView } from '../../ehs.types';

export interface EhsEditDialogData { incident: IncidentView; }

@Component({
  selector: 'qos-ehs-edit-dialog',
  templateUrl: './ehs-edit-dialog.component.html',
  styleUrls: ['./ehs-edit-dialog.component.scss'],
  standalone: false
})
export class EhsEditDialogComponent {

  submitting = false;

  readonly severities: IncidentSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    title:           [this.data.incident.title,            [Validators.required, Validators.maxLength(250)]],
    description:     [this.data.incident.description ?? '', [Validators.maxLength(4000)]],
    location:        [this.data.incident.location ?? '',    [Validators.maxLength(500)]],
    personsInvolved: [this.data.incident.personsInvolved ?? '', [Validators.maxLength(1000)]],
    severity:        [this.data.incident.severity, [Validators.required]],
    standardsCsv:    [this.data.incident.standardsCsv ?? '', [Validators.maxLength(500)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EhsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EhsEditDialogComponent, IncidentView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EhsEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.edit(this.data.incident.id, {
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      location: v.location?.trim() || undefined,
      personsInvolved: v.personsInvolved?.trim() || undefined,
      severity: v.severity,
      standardsCsv: v.standardsCsv?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open($localize`:@@ehs.edit.updated:Incident mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ehs-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

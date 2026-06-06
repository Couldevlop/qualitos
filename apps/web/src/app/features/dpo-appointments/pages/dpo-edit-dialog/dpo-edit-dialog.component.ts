import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpoAppointmentsService } from '../../dpo-appointments.service';
import { DpoAppointmentView, DpoType } from '../../dpo-appointments.types';

export interface DpoEditDialogData { appointment: DpoAppointmentView; }

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function linesToList(input?: string | null): string[] {
  if (!input) return [];
  const set = new Set<string>();
  for (const line of input.split('\n')) {
    const v = line.trim();
    if (v) set.add(v);
  }
  return Array.from(set);
}
function listToLines(arr?: string[] | null): string { return (arr ?? []).join('\n'); }
function uuidLinesValidator(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const ids = linesToList(ctrl.value);
    for (const id of ids) if (!UUID_REGEX.test(id)) return { uuidLine: id };
    return null;
  };
}

@Component({
  selector: 'qos-dpo-edit-dialog',
  templateUrl: './dpo-edit-dialog.component.html',
  styleUrls: ['./dpo-edit-dialog.component.scss'],
  standalone: false
})
export class DpoEditDialogComponent {

  submitting = false;
  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DpoAppointmentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DpoEditDialogComponent, DpoAppointmentView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DpoEditDialogData
  ) {
    const a = data.appointment;
    this.form = this.fb.nonNullable.group({
      dpoFullName: [a.dpoFullName, [Validators.required, Validators.maxLength(250)]],
      dpoEmail:    [a.dpoEmail,    [Validators.required, Validators.email, Validators.maxLength(320)]],
      dpoPhone:    [a.dpoPhone ?? '', [Validators.maxLength(64)]],
      dpoType:     [a.dpoType, [Validators.required]],
      externalCompanyName: [a.externalCompanyName ?? '', [Validators.maxLength(250)]],
      qualifications:      [a.qualifications ?? '',      [Validators.maxLength(4000)]],
      linkedProcessingActivityIds: [listToLines(a.linkedProcessingActivityIds), [uuidLinesValidator()]]
    });
  }

  isExternal(): boolean { return this.form.controls.dpoType.value === 'EXTERNAL'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (v.dpoType === 'EXTERNAL' && !v.externalCompanyName?.trim()) {
      this.snack.open($localize`:@@dpo-appointments.external-needs-company:Un DPO externe doit indiquer la société hébergeante.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.edit(this.data.appointment.id, {
      dpoFullName: v.dpoFullName.trim(),
      dpoEmail: v.dpoEmail.trim(),
      dpoPhone: v.dpoPhone?.trim() || undefined,
      dpoType: v.dpoType as DpoType,
      externalCompanyName: v.externalCompanyName?.trim() || undefined,
      qualifications: v.qualifications?.trim() || undefined,
      linkedProcessingActivityIds: linesToList(v.linkedProcessingActivityIds)
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => { this.snack.open($localize`:@@dpo-appointments.edit.updated-toast:Désignation mise à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(a); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpo-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

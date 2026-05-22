import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpoAppointmentsService } from '../../dpo-appointments.service';
import { DpoAppointmentView, DpoType } from '../../dpo-appointments.types';

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

function uuidLinesValidator(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const ids = linesToList(ctrl.value);
    for (const id of ids) if (!UUID_REGEX.test(id)) return { uuidLine: id };
    return null;
  };
}

@Component({
  selector: 'qos-dpo-propose-dialog',
  templateUrl: './dpo-propose-dialog.component.html',
  styleUrls: ['./dpo-propose-dialog.component.scss'],
  standalone: false
})
export class DpoProposeDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reference: ['', [
      Validators.required, Validators.maxLength(64),
      Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)
    ]],
    scope: ['', [
      Validators.required, Validators.maxLength(64),
      Validators.pattern(/^[A-Z][A-Z0-9_-]{0,63}$/)
    ]],
    dpoFullName: ['', [Validators.required, Validators.maxLength(250)]],
    dpoEmail:    ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    dpoPhone:    ['', [Validators.maxLength(64)]],
    dpoType:     ['INTERNAL' as DpoType, [Validators.required]],
    externalCompanyName: ['', [Validators.maxLength(250)]],
    qualifications:      ['', [Validators.maxLength(4000)]],
    linkedProcessingActivityIds: ['', [uuidLinesValidator()]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DpoAppointmentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DpoProposeDialogComponent, DpoAppointmentView>
  ) {}

  isExternal(): boolean { return this.form.controls.dpoType.value === 'EXTERNAL'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    // OWASP A04 — cohérence : DPO externe doit indiquer la société hébergeante
    if (v.dpoType === 'EXTERNAL' && !v.externalCompanyName?.trim()) {
      this.snack.open('Un DPO externe doit indiquer la société hébergeante.', 'OK', { duration: 4000 });
      return;
    }
    const createdByUserId = this.auth.snapshot()?.userId;
    if (!createdByUserId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.propose({
      reference: v.reference.trim(),
      scope: v.scope.trim(),
      dpoFullName: v.dpoFullName.trim(),
      dpoEmail: v.dpoEmail.trim(),
      dpoPhone: v.dpoPhone?.trim() || undefined,
      dpoType: v.dpoType,
      externalCompanyName: v.externalCompanyName?.trim() || undefined,
      qualifications: v.qualifications?.trim() || undefined,
      linkedProcessingActivityIds: linesToList(v.linkedProcessingActivityIds),
      createdByUserId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => { this.snack.open('Désignation proposée (PROPOSED).', 'OK', { duration: 2500 }); this.dialogRef.close(a); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpo-propose] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de la création.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

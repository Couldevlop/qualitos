import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpiaService } from '../../dpia.service';
import { DpiaView, RiskLevel } from '../../dpia.types';

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
  selector: 'qos-dpia-create-dialog',
  templateUrl: './dpia-create-dialog.component.html',
  styleUrls: ['./dpia-create-dialog.component.scss'],
  standalone: false
})
export class DpiaCreateDialogComponent {

  submitting = false;

  readonly risks: { value: RiskLevel; label: string; warn?: boolean }[] = [
    { value: 'LOW',    label: $localize`:@@dpia.create.risk-low:LOW — Risque faible` },
    { value: 'MEDIUM', label: $localize`:@@dpia.create.risk-medium:MEDIUM — Risque modéré` },
    { value: 'HIGH',   label: $localize`:@@dpia.create.risk-high:HIGH — Risque élevé (consultation Art. 36 requise)`, warn: true },
    { value: 'SEVERE', label: $localize`:@@dpia.create.risk-severe:SEVERE — Risque sévère (consultation Art. 36 obligatoire)`, warn: true }
  ];

  readonly form = this.fb.nonNullable.group({
    reference: ['', [
      Validators.required, Validators.maxLength(64),
      Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)
    ]],
    title:       ['', [Validators.required, Validators.maxLength(250)]],
    description: ['', [Validators.maxLength(4000)]],
    initialRiskLevel: ['MEDIUM' as RiskLevel, [Validators.required]],
    linkedProcessingActivityIds: ['', [uuidLinesValidator()]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DpiaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DpiaCreateDialogComponent, DpiaView>
  ) {}

  needsConsultation(): boolean {
    return DpiaService.requiresPriorConsultation(this.form.controls.initialRiskLevel.value);
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const createdByUserId = this.auth.snapshot()?.userId;
    if (!createdByUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.create({
      reference: v.reference.trim(),
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      initialRiskLevel: v.initialRiskLevel,
      linkedProcessingActivityIds: linesToList(v.linkedProcessingActivityIds),
      createdByUserId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: d => { this.snack.open($localize`:@@dpia.create.success:DPIA créée (DRAFT).`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(d); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpia-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

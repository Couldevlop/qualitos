import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachSeverity, BreachView } from '../../breach.types';

@Component({
  selector: 'qos-breach-detect-dialog',
  templateUrl: './breach-detect-dialog.component.html',
  styleUrls: ['./breach-detect-dialog.component.scss'],
  standalone: false
})
export class BreachDetectDialogComponent {

  submitting = false;
  readonly severities: BreachSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    internalReference: ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    title:        ['', [Validators.required, Validators.maxLength(250)]],
    description:  ['', [Validators.maxLength(4000)]],
    detectedAt:   [new Date().toISOString().slice(0, 16), [Validators.required]],
    occurredAt:   ['',  []],
    severity:     ['MEDIUM' as BreachSeverity, [Validators.required]],
    affectedSubjectsCount: [0, [Validators.required, Validators.min(0)]],
    affectedDataCategoriesRaw: ['', [Validators.maxLength(4000)]],
    riskOfHarmDescription:  ['', [Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: BreachService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BreachDetectDialogComponent, BreachView>
  ) {}

  isHighRisk(): boolean {
    const s = this.form.controls.severity.value;
    return s === 'HIGH' || s === 'CRITICAL';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const now = Date.now();
    const detMs = new Date(v.detectedAt).getTime();
    if (detMs > now) {
      this.snack.open('La date de détection ne peut pas être dans le futur.', 'OK', { duration: 4000 });
      return;
    }
    if (v.occurredAt) {
      const occMs = new Date(v.occurredAt).getTime();
      if (occMs > detMs) {
        this.snack.open('La date de survenue doit être ≤ à la date de détection.', 'OK', { duration: 4000 });
        return;
      }
    }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.detect({
      internalReference: v.internalReference.trim(),
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      detectedAt: new Date(v.detectedAt).toISOString(),
      occurredAt: v.occurredAt ? new Date(v.occurredAt).toISOString() : undefined,
      severity: v.severity,
      affectedSubjectsCount: v.affectedSubjectsCount,
      affectedDataCategories: (v.affectedDataCategoriesRaw ?? '').split(/\r?\n/).map(s => s.trim()).filter(s => s),
      riskOfHarmDescription: v.riskOfHarmDescription?.trim() || undefined,
      reportedByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: b => this.dialogRef.close(b),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[breach-detect] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Enregistrement impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

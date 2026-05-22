import { Component, Inject, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { daysToDuration, durationToDays, RetentionService } from '../../retention.service';
import { RetentionRuleView, RetentionUnit } from '../../retention.types';

export interface RetRuleDialogData { rule?: RetentionRuleView; }

@Component({
  selector: 'qos-ret-rule-dialog',
  templateUrl: './ret-rule-dialog.component.html',
  styleUrls: ['./ret-rule-dialog.component.scss'],
  standalone: false
})
export class RetRuleDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  readonly units: { value: RetentionUnit; label: string }[] = [
    { value: 'DAY',   label: 'Jour(s)' },
    { value: 'MONTH', label: 'Mois (~30 j)' },
    { value: 'YEAR',  label: 'An(s) (~365 j)' }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: RetentionService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<RetRuleDialogComponent, RetentionRuleView>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: RetRuleDialogData | null
  ) {
    this.isEdit = !!data?.rule;
    const r = data?.rule;

    // Décompose la durée existante en (amount, unit) "lisible" pour l'édition.
    let initialAmount = 30;
    let initialUnit: RetentionUnit = 'DAY';
    if (r?.retentionPeriod) {
      const days = durationToDays(r.retentionPeriod);
      if (days > 0 && days % 365 === 0) { initialAmount = days / 365; initialUnit = 'YEAR'; }
      else if (days > 0 && days % 30 === 0) { initialAmount = days / 30; initialUnit = 'MONTH'; }
      else { initialAmount = days; initialUnit = 'DAY'; }
    }

    this.form = this.fb.nonNullable.group({
      // OWASP A03 — code regex + length miroirs backend @Pattern + @Size.
      // Immutable en édition (clé fonctionnelle).
      dataCategoryCode: [
        { value: r?.dataCategoryCode ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(64),
         Validators.pattern(/^[a-z][a-z0-9._-]{1,63}$/)]
      ],
      dataCategoryLabel: [r?.dataCategoryLabel ?? '', [Validators.maxLength(250)]],

      periodAmount: [initialAmount, [Validators.required, Validators.min(1), Validators.max(36500)]],
      periodUnit:   [initialUnit, [Validators.required]],

      legalBasis:           [r?.legalBasis ?? '',           [Validators.required, Validators.maxLength(2000)]],
      lawfulBasisReference: [r?.lawfulBasisReference ?? '', [Validators.maxLength(1024)]]
    });

    // OWASP A05 — restreindre lawfulBasisReference à des URLs https://
    // quand renseignée (blocque http/javascript/data/file).
    this.form.controls.lawfulBasisReference.valueChanges.subscribe(v => {
      if (!v) {
        this.form.controls.lawfulBasisReference.setErrors(null);
        return;
      }
      if (!/^https:\/\/.+/.test(v)) {
        this.form.controls.lawfulBasisReference.setErrors({ httpsOnly: true });
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    const iso = daysToDuration(v.periodAmount, v.periodUnit);

    const payload = {
      dataCategoryLabel: v.dataCategoryLabel?.trim() || undefined,
      retentionPeriod: iso,
      legalBasis: v.legalBasis.trim(),
      lawfulBasisReference: v.lawfulBasisReference?.trim() || undefined
    };

    const op$ = this.isEdit
      ? this.svc.edit(this.data!.rule!.id, payload)
      : (() => {
          const createdByUserId = this.auth.snapshot()?.userId;
          if (!createdByUserId) {
            this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({
            dataCategoryCode: v.dataCategoryCode.trim(),
            ...payload,
            createdByUserId
          });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => {
          this.snack.open(this.isEdit ? 'Règle mise à jour.' : 'Règle créée (DRAFT).', 'OK', { duration: 2500 });
          this.dialogRef.close(r);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ret-rule-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

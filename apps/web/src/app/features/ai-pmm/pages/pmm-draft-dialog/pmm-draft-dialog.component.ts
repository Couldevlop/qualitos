import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PmmService } from '../../pmm.service';
import { FREQUENCY_LABEL, PmmPlanView, PmmReviewFrequency } from '../../pmm.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-pmm-draft-dialog',
  templateUrl: './pmm-draft-dialog.component.html',
  styleUrls: ['./pmm-draft-dialog.component.scss'],
  standalone: false
})
export class PmmDraftDialogComponent {

  submitting = false;
  readonly freqLabel = FREQUENCY_LABEL;
  readonly frequencies: PmmReviewFrequency[] = ['WEEKLY', 'MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL'];

  readonly form = this.fb.nonNullable.group({
    reference:                   ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    aiSystemId:                  ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    name:                        ['', [Validators.required, Validators.maxLength(250)]],
    description:                 ['', [Validators.maxLength(4000)]],
    metricsMonitored:            ['', [Validators.maxLength(4000)]],
    collectionMethod:            ['', [Validators.maxLength(4000)]],
    reviewFrequency:             ['QUARTERLY' as PmmReviewFrequency, [Validators.required]],
    responsiblePartyDescription: ['', [Validators.maxLength(4000)]],
    triggerCriteria:             ['', [Validators.maxLength(4000)]],
    qmsLinkReference:            ['', [Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: PmmService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PmmDraftDialogComponent, PmmPlanView>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.draft({
      reference: v.reference.trim(),
      aiSystemId: v.aiSystemId.trim(),
      name: v.name.trim(),
      description: v.description?.trim() || undefined,
      metricsMonitored: v.metricsMonitored?.trim() || undefined,
      collectionMethod: v.collectionMethod?.trim() || undefined,
      reviewFrequency: v.reviewFrequency,
      responsiblePartyDescription: v.responsiblePartyDescription?.trim() || undefined,
      triggerCriteria: v.triggerCriteria?.trim() || undefined,
      qmsLinkReference: v.qmsLinkReference?.trim() || undefined,
      createdByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => this.dialogRef.close(p),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pmm-draft] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Création impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

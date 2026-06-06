import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SubjectRequestsService } from '../../subject-requests.service';
import { SubjectRequestView } from '../../subject-requests.types';

export interface SrExtendDialogData {
  request: SubjectRequestView;
  maxDeadlineIso: string;  // received + 90 days (Art. 12§3)
}

@Component({
  selector: 'qos-sr-extend-dialog',
  templateUrl: './sr-extend-dialog.component.html',
  styleUrls: ['./sr-extend-dialog.component.scss'],
  standalone: false
})
export class SrExtendDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    newDeadline: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SubjectRequestsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SrExtendDialogComponent, SubjectRequestView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SrExtendDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue().newDeadline;
    const proposed = new Date(raw);
    const currentDeadline = new Date(this.data.request.deadlineAt);
    const max = new Date(this.data.maxDeadlineIso);

    // OWASP A04 — refuser une extension qui dégrade ou dépasse le maximum légal.
    if (proposed <= currentDeadline) {
      this.snack.open($localize`:@@subject-requests.extend.must-be-later:La nouvelle échéance doit être postérieure à l'actuelle.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    if (proposed > max) {
      this.snack.open($localize`:@@subject-requests.extend.max-90-days:Le RGPD limite l'extension à 90 jours après la réception (Art. 12§3).`, $localize`:@@common.ok:OK`, { duration: 4500 });
      return;
    }

    this.submitting = true;
    this.svc.extend(this.data.request.id, { newDeadline: proposed.toISOString() })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => { this.snack.open($localize`:@@subject-requests.extend.success:Échéance prolongée.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(r); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[sr-extend] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@subject-requests.extend.error:Prolongation impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

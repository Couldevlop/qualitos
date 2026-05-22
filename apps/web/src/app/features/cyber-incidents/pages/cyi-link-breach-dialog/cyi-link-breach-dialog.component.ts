import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiView } from '../../cyi.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface CyiLinkBreachDialogData { id: string; }

@Component({
  selector: 'qos-cyi-link-breach-dialog',
  templateUrl: './cyi-link-breach-dialog.component.html',
  styleUrls: ['./cyi-link-breach-dialog.component.scss'],
  standalone: false
})
export class CyiLinkBreachDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    breachId: ['', [Validators.required, Validators.pattern(UUID_REGEX)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CyberIncidentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CyiLinkBreachDialogComponent, CyiView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CyiLinkBreachDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    this.svc.linkBreach(this.data.id, { breachId: this.form.getRawValue().breachId.trim() })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Violation RGPD liée.', 'OK', { duration: 2200 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cyi-link] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Liaison impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

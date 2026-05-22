import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FriaService } from '../../fria.service';
import { FriaView } from '../../fria.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-fria-draft-dialog',
  templateUrl: './fria-draft-dialog.component.html',
  styleUrls: ['./fria-draft-dialog.component.scss'],
  standalone: false
})
export class FriaDraftDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reference:                     ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    aiSystemId:                    ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    processDescription:            ['', [Validators.required, Validators.maxLength(4000)]],
    deploymentDurationDescription: ['', [Validators.maxLength(4000)]],
    affectedPersonsCategories:     ['', [Validators.required, Validators.maxLength(4000)]],
    specificRisks:                 ['', [Validators.required, Validators.maxLength(4000)]],
    mitigationMeasures:            ['', [Validators.maxLength(4000)]],
    humanOversightMeasures:        ['', [Validators.maxLength(4000)]],
    complaintMechanismDescription: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: FriaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FriaDraftDialogComponent, FriaView>
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
      processDescription: v.processDescription.trim(),
      deploymentDurationDescription: v.deploymentDurationDescription?.trim() || undefined,
      affectedPersonsCategories: v.affectedPersonsCategories.trim(),
      specificRisks: v.specificRisks.trim(),
      mitigationMeasures: v.mitigationMeasures?.trim() || undefined,
      humanOversightMeasures: v.humanOversightMeasures?.trim() || undefined,
      complaintMechanismDescription: v.complaintMechanismDescription?.trim() || undefined,
      createdByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: f => this.dialogRef.close(f),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fria-draft] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Création impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiConformityService } from '../../ai-conformity.service';
import { ConformityView } from '../../ai-conformity.types';

export interface CnfCertifyDialogData { conformityId: string; }

@Component({
  selector: 'qos-cnf-certify-dialog',
  templateUrl: './cnf-certify-dialog.component.html',
  styleUrls: ['./cnf-certify-dialog.component.scss'],
  standalone: false
})
export class CnfCertifyDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    certificateNumber:      ['', [Validators.required, Validators.maxLength(250)]],
    euDeclarationReference: ['', [Validators.required, Validators.maxLength(250)]],
    validUntil:             ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiConformityService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CnfCertifyDialogComponent, ConformityView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CnfCertifyDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    // OWASP A04 — validUntil obligatoirement futur (mirror @Future backend)
    if (new Date(v.validUntil) <= new Date()) {
      this.snack.open($localize`:@@ai-conformity.certify.future-date:La date de validité doit être dans le futur.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.certify(this.data.conformityId, {
      certificateNumber: v.certificateNumber.trim(),
      euDeclarationReference: v.euDeclarationReference.trim(),
      validUntil: new Date(v.validUntil).toISOString()
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => { this.snack.open($localize`:@@ai-conformity.certify.recorded:Certification enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(c); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cnf-certify] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@ai-conformity.certify.failed:Certification impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

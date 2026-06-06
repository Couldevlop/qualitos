import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SubjectRequestsService } from '../../subject-requests.service';
import { SubjectRequestType, SubjectRequestView } from '../../subject-requests.types';

@Component({
  selector: 'qos-sr-receive-dialog',
  templateUrl: './sr-receive-dialog.component.html',
  styleUrls: ['./sr-receive-dialog.component.scss'],
  standalone: false
})
export class SrReceiveDialogComponent {

  submitting = false;

  readonly types: { value: SubjectRequestType; label: string }[] = [
    { value: 'ACCESS',        label: $localize`:@@subject-requests.type.access:Accès aux données (Art. 15)` },
    { value: 'RECTIFICATION', label: $localize`:@@subject-requests.type.rectification:Rectification (Art. 16)` },
    { value: 'ERASURE',       label: $localize`:@@subject-requests.type.erasure:Effacement / droit à l'oubli (Art. 17)` },
    { value: 'RESTRICTION',   label: $localize`:@@subject-requests.type.restriction:Limitation du traitement (Art. 18)` },
    { value: 'PORTABILITY',   label: $localize`:@@subject-requests.receive.type-portability:Portabilité des données (Art. 20)` },
    { value: 'OBJECTION',     label: $localize`:@@subject-requests.receive.type-objection:Opposition au traitement (Art. 21)` }
  ];

  // OWASP A03 — longueurs miroirs backend @Size.
  readonly form = this.fb.nonNullable.group({
    type: ['ACCESS' as SubjectRequestType, [Validators.required]],
    subjectIdentifier:      ['', [Validators.required, Validators.maxLength(320)]],
    subjectIdentifierLabel: ['', [Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SubjectRequestsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SrReceiveDialogComponent, SubjectRequestView>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const requestedByUserId = this.auth.snapshot()?.userId;
    if (!requestedByUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.receive({
      type: v.type,
      subjectIdentifier: v.subjectIdentifier.trim(),
      subjectIdentifierLabel: v.subjectIdentifierLabel?.trim() || undefined,
      requestedByUserId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => {
          // OWASP A02 — clear plaintext after successful submit
          this.form.controls.subjectIdentifier.reset('');
          this.snack.open($localize`:@@subject-requests.receive.success:Demande enregistrée — délai RGPD 1 mois.`, $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(r);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[sr-receive] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@subject-requests.receive.error:Enregistrement impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

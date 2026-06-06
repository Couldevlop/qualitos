import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConsentsService } from '../../consents.service';
import { ConsentSource, ConsentView } from '../../consents.types';

@Component({
  selector: 'qos-consents-grant-dialog',
  templateUrl: './consents-grant-dialog.component.html',
  styleUrls: ['./consents-grant-dialog.component.scss'],
  standalone: false
})
export class ConsentsGrantDialogComponent {

  submitting = false;

  readonly sources: { value: ConsentSource; label: string }[] = [
    { value: 'WEB_FORM',   label: $localize`:@@consents.source.web-form:Formulaire web` },
    { value: 'MOBILE_APP', label: $localize`:@@consents.source.mobile-app:Application mobile` },
    { value: 'EMAIL',      label: $localize`:@@consents.source.email:E-mail` },
    { value: 'PAPER',      label: $localize`:@@consents.grant.source-paper:Papier (signé)` },
    { value: 'PHONE',      label: $localize`:@@consents.grant.source-phone:Téléphone (enregistré)` },
    { value: 'API',        label: $localize`:@@consents.source.api:API` },
    { value: 'IMPORT',     label: $localize`:@@consents.grant.source-import:Import en masse` },
    { value: 'OTHER',      label: $localize`:@@consents.source.other:Autre` }
  ];

  // OWASP A03 — regex + length mirror backend @Pattern + @Size.
  readonly form = this.fb.nonNullable.group({
    subjectIdentifier: ['', [Validators.required, Validators.maxLength(320)]],
    subjectIdentifierLabel: ['', [Validators.maxLength(250)]],
    purposeCode:    ['', [Validators.required, Validators.maxLength(64),
                         Validators.pattern(/^[a-z][a-z0-9._-]{1,63}$/)]],
    purposeVersion: ['', [Validators.required, Validators.maxLength(32),
                         Validators.pattern(/^[A-Za-z0-9._:-]{1,32}$/)]],
    source: ['WEB_FORM' as ConsentSource, [Validators.required]],
    evidenceUrl: ['', [
      Validators.maxLength(1024),
      // OWASP A05 — only https URLs accepted as evidence; blocks
      // http://, javascript:, data:, file: etc.
      Validators.pattern(/^https:\/\/.+/)
    ]],
    ipAddress: ['', [Validators.maxLength(64)]],
    userAgent: ['', [Validators.maxLength(500)]],
    expiresAt: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ConsentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ConsentsGrantDialogComponent, ConsentView>
  ) {
    // evidenceUrl pattern only applies when value is non-empty;
    // strip pattern error when field is blank.
    this.form.controls.evidenceUrl.valueChanges.subscribe(v => {
      if (!v) this.form.controls.evidenceUrl.setErrors(null);
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const grantedByUserId = this.auth.snapshot()?.userId;
    if (!grantedByUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.grant({
      subjectIdentifier: v.subjectIdentifier.trim(),
      subjectIdentifierLabel: v.subjectIdentifierLabel?.trim() || undefined,
      purposeCode: v.purposeCode.trim(),
      purposeVersion: v.purposeVersion.trim(),
      source: v.source,
      evidenceUrl: v.evidenceUrl?.trim() || undefined,
      ipAddress: v.ipAddress?.trim() || undefined,
      userAgent: v.userAgent?.trim() || undefined,
      expiresAt: v.expiresAt
        ? new Date(v.expiresAt).toISOString()
        : undefined,
      grantedByUserId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          // Effacer le plaintext du formulaire après envoi — l'identifiant
          // est désormais hashé côté backend, on ne le garde plus côté client.
          this.form.controls.subjectIdentifier.reset('');
          this.snack.open($localize`:@@consents.grant.success:Consentement enregistré.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[consents-grant] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@consents.grant.error:Enregistrement impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

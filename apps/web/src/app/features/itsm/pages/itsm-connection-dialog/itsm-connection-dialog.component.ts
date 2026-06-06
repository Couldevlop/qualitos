import { Component, Inject, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ItsmService } from '../../itsm.service';
import {
  ConnectionResponse,
  ConnectionStatus,
  ItsmProvider
} from '../../itsm.types';

export interface ItsmConnectionDialogData {
  connection?: ConnectionResponse;
}

/**
 * OWASP A02 (Cryptographic / secret handling):
 *  - the secret field is a password input (no clipboard preview by default)
 *  - we never display the existing secret in edit mode (the API does not
 *    return it; the field is left empty and only sent if the admin types
 *    a new value — supports secret rotation without leaking)
 *  - any non-empty value is sent verbatim to the backend which encrypts it
 *  - autocomplete="new-password" prevents browser autofill of unrelated creds
 */
@Component({
  selector: 'qos-itsm-connection-dialog',
  templateUrl: './itsm-connection-dialog.component.html',
  styleUrls: ['./itsm-connection-dialog.component.scss'],
  standalone: false
})
export class ItsmConnectionDialogComponent {

  submitting = false;
  showSecret = false;
  readonly isEdit: boolean;
  readonly form;

  readonly providers: { value: ItsmProvider; label: string }[] = [
    { value: 'SERVICENOW', label: 'ServiceNow' },
    { value: 'JIRA_SM',    label: 'Jira Service Management' }
  ];

  readonly statuses: ConnectionStatus[] = ['ACTIVE', 'DISABLED'];

  get dialogTitle(): string {
    return this.isEdit
      ? $localize`:@@itsm.dialog.edit-title:Modifier la connexion`
      : $localize`:@@itsm.dialog.create-title:Nouvelle connexion ITSM`;
  }
  get submitLabel(): string {
    return this.isEdit
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@common.create:Créer`;
  }
  get secretLabel(): string {
    return this.isEdit
      ? $localize`:@@itsm.dialog.secret-edit-label:Nouveau secret (laisser vide pour conserver)`
      : $localize`:@@itsm.dialog.secret-create-label:Secret / API token`;
  }
  get secretToggleAria(): string {
    return this.showSecret
      ? $localize`:@@itsm.dialog.secret-hide-aria:Masquer le secret`
      : $localize`:@@itsm.dialog.secret-show-aria:Afficher le secret`;
  }
  get secretToggleTooltip(): string {
    return this.showSecret
      ? $localize`:@@itsm.dialog.secret-hide:Masquer`
      : $localize`:@@itsm.dialog.secret-show:Afficher`;
  }

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ItsmService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ItsmConnectionDialogComponent, ConnectionResponse>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: ItsmConnectionDialogData | null
  ) {
    this.isEdit = !!data?.connection;
    const c = data?.connection;
    this.form = this.fb.nonNullable.group({
      name: [c?.name ?? '', [Validators.required, Validators.maxLength(120)]],
      // provider is immutable after creation (matches backend UpdateConnectionRequest)
      provider: [
        { value: (c?.provider ?? 'SERVICENOW') as ItsmProvider, disabled: this.isEdit },
        [Validators.required]
      ],
      // OWASP A03 — https://… only, mirrors backend @Pattern
      baseUrl: [c?.baseUrl ?? '', [
        Validators.required, Validators.maxLength(512),
        Validators.pattern(/^https:\/\/.+$/)
      ]],
      username: [c?.username ?? '', [Validators.maxLength(200)]],
      // secret is required on create, optional on edit (rotation only)
      secret: ['', this.isEdit
        ? [Validators.minLength(4), Validators.maxLength(1024)]
        : [Validators.required, Validators.minLength(4), Validators.maxLength(1024)]
      ],
      externalScope: [c?.externalScope ?? '', [Validators.maxLength(200)]],
      // status is only editable in edit mode (DISABLED_ON_ERRORS is set by the system)
      status: [c?.status ?? 'ACTIVE' as ConnectionStatus]
    });
  }

  toggleSecret(): void { this.showSecret = !this.showSecret; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();

    const op$ = this.isEdit
      ? this.svc.update(this.data!.connection!.id, {
          name: v.name.trim(),
          baseUrl: v.baseUrl.trim(),
          username: v.username?.trim() || undefined,
          // OWASP A02 — only include secret in payload when rotating
          secret: v.secret?.trim() ? v.secret.trim() : undefined,
          externalScope: v.externalScope?.trim() || undefined,
          status: v.status === 'DISABLED_ON_ERRORS' ? undefined : v.status
        })
      : (() => {
          const createdBy = this.auth.snapshot()?.userId;
          if (!createdBy) {
            this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({
            name: v.name.trim(),
            provider: v.provider,
            baseUrl: v.baseUrl.trim(),
            username: v.username?.trim() || undefined,
            secret: v.secret.trim(),
            externalScope: v.externalScope?.trim() || undefined,
            createdBy
          });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open(this.isEdit ? $localize`:@@itsm.dialog.updated:Connexion mise à jour.` : $localize`:@@itsm.dialog.created:Connexion créée.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[itsm-conn] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@itsm.dialog.save-error:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

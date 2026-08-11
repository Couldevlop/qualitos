import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditType } from '../../audits.types';

/**
 * Adresse de courriel valide, espaces de bordure tolérées.
 *
 * <p>`Validators.email` s'applique à la valeur BRUTE, et le nettoyage n'a lieu
 * qu'à l'envoi : une adresse collée depuis un annuaire ou un courriel — qui
 * emporte presque toujours une espace — serait donc refusée alors qu'elle est
 * juste, avec un message qui accuse l'adresse et non l'espace invisible qui la
 * suit. On valide ce qui sera réellement envoyé.
 *
 * <p>La clé d'erreur reste `email`, celle qu'attend le gabarit.
 */
function trimmedEmail(control: AbstractControl): ValidationErrors | null {
  const raw = control.value;
  if (typeof raw !== 'string' || raw.trim() === '') {
    return null;
  }
  return Validators.email({ value: raw.trim() } as AbstractControl);
}

@Component({
  selector: 'qos-audits-create-dialog',
  templateUrl: './audits-create-dialog.component.html',
  styleUrls: ['./audits-create-dialog.component.scss'],
  standalone: false
})
export class AuditsCreateDialogComponent {

  submitting = false;

  readonly types: { value: AuditType; label: string }[] = [
    { value: 'INTERNAL',      label: $localize`:@@audits.type.internal:Interne` },
    { value: 'EXTERNAL',      label: $localize`:@@audits.type.external:Externe` },
    { value: 'SUPPLIER',      label: $localize`:@@audits.type.supplier:Fournisseur` },
    { value: 'LPA',           label: $localize`:@@audits.type.lpa:LPA (Layered Process Audit)` },
    { value: 'CERTIFICATION', label: $localize`:@@audits.type.certification:Certification` },
    { value: 'SURVEILLANCE',  label: $localize`:@@audits.type.surveillance:Surveillance` }
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    scope: [''],
    type: ['INTERNAL' as AuditType, [Validators.required]],
    standard: ['', [Validators.maxLength(100)]],
    scheduledDate: [''],
    // Destinataire du rappel à 30 jours. Validation du formulaire en plus de
    // type="email" : celle du navigateur ne s'applique qu'à la saisie manuelle et
    // se contourne, celle-ci vaut aussi pour un collage ou un pré-remplissage.
    reminderEmail: ['', [trimmedEmail, Validators.maxLength(320)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsCreateDialogComponent, AuditPlanResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const leadAuditorId = this.auth.snapshot()?.userId;
    if (!leadAuditorId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.audits
      .createPlan({
        title: v.title.trim(),
        scope: v.scope?.trim() || undefined,
        type: v.type,
        standard: v.standard?.trim() || undefined,
        scheduledDate: v.scheduledDate || undefined,
        reminderEmail: v.reminderEmail?.trim() || undefined,
        leadAuditorId
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: plan => {
          this.snack.open($localize`:@@audits.create.success:Plan d'audit créé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(plan);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
            $localize`:@@common.ok:OK`,
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}

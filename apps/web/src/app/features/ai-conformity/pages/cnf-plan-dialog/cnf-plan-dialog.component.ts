import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiConformityService } from '../../ai-conformity.service';
import { ConformityProcedure, ConformityView } from '../../ai-conformity.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-cnf-plan-dialog',
  templateUrl: './cnf-plan-dialog.component.html',
  styleUrls: ['./cnf-plan-dialog.component.scss'],
  standalone: false
})
export class CnfPlanDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reference: ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    aiSystemId: ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    qmsId:      ['', [Validators.pattern(UUID_REGEX)]],
    procedure:  ['INTERNAL_CONTROL' as ConformityProcedure, [Validators.required]],
    notifiedBodyId:   ['', [Validators.maxLength(8), Validators.pattern(/^$|^[0-9]{4}$/)]],
    notifiedBodyName: ['', [Validators.maxLength(250)]],
    scope: ['', [Validators.required, Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiConformityService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CnfPlanDialogComponent, ConformityView>
  ) {
    // qmsId optional pattern only when non-empty
    this.form.controls.qmsId.valueChanges.subscribe(v => {
      if (!v) this.form.controls.qmsId.setErrors(null);
    });
  }

  isNotifiedBody(): boolean { return this.form.controls.procedure.value === 'NOTIFIED_BODY'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    // OWASP A04 — NOTIFIED_BODY exige id + name
    if (v.procedure === 'NOTIFIED_BODY' && (!v.notifiedBodyId?.trim() || !v.notifiedBodyName?.trim())) {
      this.snack.open($localize`:@@ai-conformity.plan.notified-body-required:La procédure NOTIFIED_BODY exige l'ID 4 chiffres + nom de l'organisme.`,
        $localize`:@@common.ok:OK`, { duration: 4500 });
      return;
    }
    const createdByUserId = this.auth.snapshot()?.userId;
    if (!createdByUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.plan({
      reference: v.reference.trim(),
      aiSystemId: v.aiSystemId.trim(),
      qmsId: v.qmsId?.trim() || undefined,
      procedure: v.procedure,
      notifiedBodyId:   v.notifiedBodyId?.trim() || undefined,
      notifiedBodyName: v.notifiedBodyName?.trim() || undefined,
      scope: v.scope.trim(),
      createdByUserId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => { this.snack.open($localize`:@@ai-conformity.plan.planned:Évaluation planifiée.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(c); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cnf-plan] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

import { Component, Inject, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditType } from '../../audits.types';
import { LeadAuditorDirectory, LeadAuditorOption, leadAuditorDisplay } from '../../lead-auditor';

export interface AuditsEditDialogData {
  plan: AuditPlanResponse;
}

/**
 * Adresse valide, espaces de bordure tolérées — même validateur que le dialogue
 * de création : `Validators.email` s'applique à la valeur brute, et une adresse
 * collée depuis un annuaire emporte presque toujours une espace.
 */
function trimmedEmail(control: AbstractControl): ValidationErrors | null {
  const raw = control.value;
  if (typeof raw !== 'string' || raw.trim() === '') {
    return null;
  }
  return Validators.email({ value: raw.trim() } as AbstractControl);
}

@Component({
  selector: 'qos-audits-edit-dialog',
  templateUrl: './audits-edit-dialog.component.html',
  styleUrls: ['./audits-edit-dialog.component.scss'],
  standalone: false
})
export class AuditsEditDialogComponent implements OnInit {
  submitting = false;

  leadAuditors: LeadAuditorOption[] = [];
  directoryUnavailable = false;

  readonly types: { value: AuditType; label: string }[] = [
    { value: 'INTERNAL',      label: $localize`:@@audits.type.internal:Interne` },
    { value: 'EXTERNAL',      label: $localize`:@@audits.type.external:Externe` },
    { value: 'SUPPLIER',      label: $localize`:@@audits.type.supplier:Fournisseur` },
    { value: 'LPA',           label: $localize`:@@audits.type.lpa-short:LPA` },
    { value: 'CERTIFICATION', label: $localize`:@@audits.type.certification:Certification` },
    { value: 'SURVEILLANCE',  label: $localize`:@@audits.type.surveillance:Surveillance` }
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    scope: [''],
    type: ['INTERNAL' as AuditType, [Validators.required]],
    standard: ['', [Validators.maxLength(100)]],
    leadAuditorId: ['', [Validators.required]],
    scheduledDate: [''],
    // Le destinataire du rappel doit pouvoir se corriger, et surtout se RETIRER :
    // le service traite la chaîne vide comme un effacement explicite, et sans ce
    // champ une adresse saisie de travers à la création restait indéboulonnable.
    reminderEmail: ['', [trimmedEmail, Validators.maxLength(320)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly directory: LeadAuditorDirectory,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsEditDialogComponent, AuditPlanResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AuditsEditDialogData
  ) {
    this.form.patchValue({
      title: data.plan.title,
      leadAuditorId: data.plan.leadAuditorId,
      scope: data.plan.scope ?? '',
      type: data.plan.type,
      standard: data.plan.standard ?? '',
      scheduledDate: data.plan.scheduledDate ?? '',
      reminderEmail: data.plan.reminderEmail ?? ''
    });
  }

  ngOnInit(): void {
    this.directory.choices().subscribe(c => {
      // L'auditeur en place reste proposé même s'il n'est pas (ou plus) dans
      // l'annuaire lisible : sans cela, le formulaire perdrait sa valeur et
      // obligerait à réattribuer l'audit pour simplement corriger un titre.
      const current = this.data.plan.leadAuditorId;
      const options = c.options.some(o => o.id === current)
        ? c.options
        : [...c.options, { id: current, label: leadAuditorDisplay(this.data.plan, this.auth.snapshot()) }];
      this.leadAuditors = options;
      this.directoryUnavailable = !c.directoryAvailable;
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    // L'auditeur ne part que s'il change : le renvoyer à l'identique recopierait
    // un nom d'annuaire par-dessus celui enregistré à la désignation.
    const lead = v.leadAuditorId !== this.data.plan.leadAuditorId
      ? this.leadAuditors.find(o => o.id === v.leadAuditorId)
      : undefined;
    this.audits
      .updatePlan(this.data.plan.id, {
        ...(lead ? { leadAuditorId: lead.id, leadAuditorName: this.directory.nameFor(lead) } : {}),
        title: v.title.trim(),
        scope: v.scope?.trim() || undefined,
        type: v.type,
        standard: v.standard?.trim() || undefined,
        scheduledDate: v.scheduledDate || undefined,
        // Chaîne vide TRANSMISE, jamais `undefined` : c'est elle qui retire le
        // destinataire côté serveur. La remplacer par `undefined` signifierait
        // « ne touche pas » et rendrait l'effacement impossible.
        reminderEmail: v.reminderEmail?.trim() ?? ''
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => {
          this.snack.open($localize`:@@audits.edit.success:Plan mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

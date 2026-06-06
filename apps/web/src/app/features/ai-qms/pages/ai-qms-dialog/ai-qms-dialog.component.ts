import { Component, Inject, Optional } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiQmsService } from '../../ai-qms.service';
import { AiQmsView } from '../../ai-qms.types';

export interface AiQmsDialogData { qms?: AiQmsView; }

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function lines(input?: string | null): string[] {
  if (!input) return [];
  const set = new Set<string>();
  for (const line of input.split('\n')) {
    const v = line.trim();
    if (v) set.add(v);
  }
  return Array.from(set);
}
function listToLines(arr?: string[] | null): string { return (arr ?? []).join('\n'); }
function uuidLinesValidator(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const ids = lines(ctrl.value);
    for (const id of ids) if (!UUID_REGEX.test(id)) return { uuidLine: id };
    return null;
  };
}

@Component({
  selector: 'qos-ai-qms-dialog',
  templateUrl: './ai-qms-dialog.component.html',
  styleUrls: ['./ai-qms-dialog.component.scss'],
  standalone: false
})
export class AiQmsDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  readonly editTitle   = $localize`:@@ai-qms.dialog.edit-title:Modifier le QMS`;
  readonly createTitle = $localize`:@@ai-qms.dialog.create-title:Nouveau QMS IA (Art. 17)`;
  readonly editLabel   = $localize`:@@common.save:Enregistrer`;
  readonly createLabel = $localize`:@@ai-qms.dialog.create-label:Créer (DRAFT)`;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiQmsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AiQmsDialogComponent, AiQmsView>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: AiQmsDialogData | null
  ) {
    this.isEdit = !!data?.qms;
    const q = data?.qms;
    this.form = this.fb.nonNullable.group({
      // OWASP A03 — reference + version regex miroirs backend.
      reference: [
        { value: q?.reference ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(64),
         Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]
      ],
      version: [
        { value: q?.version ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(32),
         Validators.pattern(/^\d+\.\d+(\.\d+)?$/)]
      ],
      name:        [q?.name ?? '',        [Validators.required, Validators.maxLength(250)]],
      description: [q?.description ?? '', [Validators.maxLength(4000)]],

      regulatoryComplianceStrategy:      [q?.regulatoryComplianceStrategy ?? '',      [Validators.maxLength(8000)]],
      designControlDescription:          [q?.designControlDescription ?? '',          [Validators.maxLength(8000)]],
      qualityControlDescription:         [q?.qualityControlDescription ?? '',         [Validators.maxLength(8000)]],
      dataManagementDescription:         [q?.dataManagementDescription ?? '',         [Validators.maxLength(8000)]],
      riskManagementDescription:         [q?.riskManagementDescription ?? '',         [Validators.maxLength(8000)]],
      pmmDescription:                    [q?.pmmDescription ?? '',                    [Validators.maxLength(8000)]],
      regulatorCommunicationDescription: [q?.regulatorCommunicationDescription ?? '', [Validators.maxLength(8000)]],
      resourceManagementDescription:     [q?.resourceManagementDescription ?? '',     [Validators.maxLength(8000)]],
      supplierMonitoringDescription:     [q?.supplierMonitoringDescription ?? '',     [Validators.maxLength(8000)]],

      coveredAiSystemIds: [listToLines(q?.coveredAiSystemIds), [uuidLinesValidator()]]
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    const payload = {
      name: v.name.trim(),
      description: v.description?.trim() || undefined,
      regulatoryComplianceStrategy:      v.regulatoryComplianceStrategy?.trim() || undefined,
      designControlDescription:          v.designControlDescription?.trim() || undefined,
      qualityControlDescription:         v.qualityControlDescription?.trim() || undefined,
      dataManagementDescription:         v.dataManagementDescription?.trim() || undefined,
      riskManagementDescription:         v.riskManagementDescription?.trim() || undefined,
      pmmDescription:                    v.pmmDescription?.trim() || undefined,
      regulatorCommunicationDescription: v.regulatorCommunicationDescription?.trim() || undefined,
      resourceManagementDescription:     v.resourceManagementDescription?.trim() || undefined,
      supplierMonitoringDescription:     v.supplierMonitoringDescription?.trim() || undefined,
      coveredAiSystemIds: lines(v.coveredAiSystemIds)
    };
    const op$ = this.isEdit
      ? this.svc.edit(this.data!.qms!.id, payload)
      : (() => {
          const createdByUserId = this.auth.snapshot()?.userId;
          if (!createdByUserId) {
            this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.draft({ reference: v.reference.trim(), version: v.version.trim(), ...payload, createdByUserId });
        })();
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: q => { this.snack.open(this.isEdit ? $localize`:@@ai-qms.dialog.updated:QMS mis à jour.` : $localize`:@@ai-qms.dialog.created:QMS créé (DRAFT).`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(q); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ai-qms-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@ai-qms.dialog.error-save:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

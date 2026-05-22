import { Component, Inject, Optional } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PaService } from '../../pa.service';
import { PaView } from '../../pa.types';

export interface PaDialogData { agreement?: PaView; }

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
  selector: 'qos-pa-dialog',
  templateUrl: './pa-dialog.component.html',
  styleUrls: ['./pa-dialog.component.scss'],
  standalone: false
})
export class PaDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: PaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PaDialogComponent, PaView>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: PaDialogData | null
  ) {
    this.isEdit = !!data?.agreement;
    const a = data?.agreement;
    const dateOnly = (iso?: string): string => iso ? iso.slice(0, 10) : '';
    this.form = this.fb.nonNullable.group({
      // OWASP A03 — reference regex + length miroirs backend
      reference: [
        { value: a?.reference ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(64),
         Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]
      ],
      processorName:        [a?.processorName ?? '',        [Validators.required, Validators.maxLength(250)]],
      processorLegalEntity: [a?.processorLegalEntity ?? '', [Validators.maxLength(250)]],
      processorContact:     [a?.processorContact ?? '',     [Validators.maxLength(250)]],
      processorDpoContact:  [a?.processorDpoContact ?? '',  [Validators.maxLength(250)]],
      processorCountry:     [a?.processorCountry ?? '',     [Validators.maxLength(2), Validators.pattern(/^[A-Za-z]{2}$/)]],
      servicesDescription:  [a?.servicesDescription ?? '',  [Validators.required, Validators.maxLength(4000)]],

      subProcessorCategories: [listToLines(a?.subProcessorCategories)],
      linkedProcessingActivityIds: [listToLines(a?.linkedProcessingActivityIds), [uuidLinesValidator()]],
      thirdCountryTransfers: [listToLines(a?.thirdCountryTransfers)],
      transferSafeguards:    [a?.transferSafeguards ?? '', [Validators.maxLength(4000)]],

      contractDocumentUrl: ['', [Validators.maxLength(1024)]],
      signedAt:       [dateOnly(a?.signedAt)],
      effectiveFrom:  [dateOnly(a?.effectiveFrom)],
      expirationDate: [dateOnly(a?.expirationDate)],

      securityMeasures: [a?.securityMeasures ?? '', [Validators.maxLength(4000)]],
      breachNotificationCommitmentHours: [
        a?.breachNotificationCommitmentHours ?? 72,
        [Validators.required, Validators.min(1), Validators.max(720)]
      ],
      auditRights: [a?.auditRights ?? true],
      auditRightsNotes: [a?.auditRightsNotes ?? '', [Validators.maxLength(4000)]],
      dataReturnOrDeletionTerms: [a?.dataReturnOrDeletionTerms ?? '', [Validators.maxLength(4000)]]
    });

    // OWASP A05 — contractDocumentUrl https-only when set.
    this.form.controls.contractDocumentUrl.valueChanges.subscribe(v => {
      if (!v) { this.form.controls.contractDocumentUrl.setErrors(null); return; }
      if (!/^https:\/\/.+/.test(v)) this.form.controls.contractDocumentUrl.setErrors({ httpsOnly: true });
    });
    this.form.controls.contractDocumentUrl.setValue(a?.contractDocumentUrl ?? '');
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    // OWASP A04 — cohérence dates : effectiveFrom ≥ signedAt, expirationDate > effectiveFrom
    if (v.signedAt && v.effectiveFrom && new Date(v.effectiveFrom) < new Date(v.signedAt)) {
      this.snack.open('La date d\'entrée en vigueur doit être ≥ à la date de signature.', 'OK', { duration: 4000 });
      return;
    }
    if (v.effectiveFrom && v.expirationDate && new Date(v.expirationDate) <= new Date(v.effectiveFrom)) {
      this.snack.open('La date d\'expiration doit être > à l\'entrée en vigueur.', 'OK', { duration: 4000 });
      return;
    }
    // OWASP A04 — transferts hors UE exigent garanties (sauf si Adequacy implicite côté UE)
    const transfers = lines(v.thirdCountryTransfers);
    if (transfers.length > 0 && !v.transferSafeguards?.trim()) {
      this.snack.open(
        'Les transferts hors UE exigent des garanties documentées (Chap. V RGPD).',
        'OK', { duration: 4500 }
      );
      return;
    }

    this.submitting = true;
    const toIso = (s?: string): string | undefined => s ? new Date(s).toISOString() : undefined;
    const payload = {
      processorName: v.processorName.trim(),
      processorLegalEntity: v.processorLegalEntity?.trim() || undefined,
      processorContact: v.processorContact?.trim() || undefined,
      processorDpoContact: v.processorDpoContact?.trim() || undefined,
      processorCountry: v.processorCountry?.trim().toUpperCase() || undefined,
      servicesDescription: v.servicesDescription.trim(),
      subProcessorCategories: lines(v.subProcessorCategories),
      linkedProcessingActivityIds: lines(v.linkedProcessingActivityIds),
      thirdCountryTransfers: transfers,
      transferSafeguards: v.transferSafeguards?.trim() || undefined,
      contractDocumentUrl: v.contractDocumentUrl?.trim() || undefined,
      signedAt:       toIso(v.signedAt),
      effectiveFrom:  toIso(v.effectiveFrom),
      expirationDate: toIso(v.expirationDate),
      securityMeasures: v.securityMeasures?.trim() || undefined,
      breachNotificationCommitmentHours: v.breachNotificationCommitmentHours,
      auditRights: v.auditRights,
      auditRightsNotes: v.auditRightsNotes?.trim() || undefined,
      dataReturnOrDeletionTerms: v.dataReturnOrDeletionTerms?.trim() || undefined
    };

    const op$ = this.isEdit
      ? this.svc.edit(this.data!.agreement!.id, payload)
      : (() => {
          const createdByUserId = this.auth.snapshot()?.userId;
          if (!createdByUserId) {
            this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({ reference: v.reference.trim(), ...payload, createdByUserId });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => { this.snack.open(this.isEdit ? 'DPA mis à jour.' : 'DPA créé (DRAFT).', 'OK', { duration: 2500 }); this.dialogRef.close(a); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pa-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

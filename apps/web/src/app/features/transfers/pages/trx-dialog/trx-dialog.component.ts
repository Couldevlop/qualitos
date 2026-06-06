import { Component, Inject, Optional } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { requiresDerogationJustification, TransfersService } from '../../transfers.service';
import { TransferMechanism, TransferView } from '../../transfers.types';

export interface TrxDialogData { transfer?: TransferView; }

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
  selector: 'qos-trx-dialog',
  templateUrl: './trx-dialog.component.html',
  styleUrls: ['./trx-dialog.component.scss'],
  standalone: false
})
export class TrxDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  readonly mechanisms: { value: TransferMechanism; label: string; warn?: boolean }[] = [
    { value: 'ADEQUACY_DECISION',            label: $localize`:@@transfers.dialog.mech-adequacy:Décision d'adéquation (Art. 45)` },
    { value: 'STANDARD_CONTRACTUAL_CLAUSES', label: $localize`:@@transfers.dialog.mech-scc:Clauses contractuelles types — SCC 2021 (Art. 46.2.c-d)` },
    { value: 'BINDING_CORPORATE_RULES',      label: $localize`:@@transfers.dialog.mech-bcr:Règles d'entreprise contraignantes — BCR (Art. 47)` },
    { value: 'CODE_OF_CONDUCT',              label: $localize`:@@transfers.dialog.mech-coc:Code de conduite approuvé (Art. 46.2.e)` },
    { value: 'CERTIFICATION',                label: $localize`:@@transfers.dialog.mech-certification:Mécanisme de certification (Art. 46.2.f)` },
    { value: 'DEROGATION_ART49',             label: $localize`:@@transfers.dialog.mech-derogation:Dérogation — situation particulière (Art. 49) ⚠ exceptionnel`, warn: true }
  ];

  get dialogTitle(): string {
    return this.isEdit
      ? $localize`:@@transfers.dialog.title-edit:Modifier le transfert`
      : $localize`:@@transfers.dialog.title-create:Nouveau transfert international`;
  }

  get submitLabelText(): string {
    return this.isEdit
      ? $localize`:@@common.save:Enregistrer`
      : $localize`:@@transfers.dialog.submit-create:Créer (DRAFT)`;
  }

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: TransfersService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<TrxDialogComponent, TransferView>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: TrxDialogData | null
  ) {
    this.isEdit = !!data?.transfer;
    const t = data?.transfer;
    this.form = this.fb.nonNullable.group({
      reference: [
        { value: t?.reference ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(64),
         Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]
      ],
      recipientName:        [t?.recipientName ?? '',        [Validators.required, Validators.maxLength(250)]],
      recipientLegalEntity: [t?.recipientLegalEntity ?? '', [Validators.maxLength(250)]],
      recipientContact:     [t?.recipientContact ?? '',     [Validators.maxLength(250)]],
      destinationCountries: [listToLines(t?.destinationCountries)],
      mechanism: [(t?.mechanism ?? 'STANDARD_CONTRACTUAL_CLAUSES') as TransferMechanism, [Validators.required]],
      safeguardsDescription:   [t?.safeguardsDescription ?? '',   [Validators.maxLength(4000)]],
      safeguardsDocumentUrl:   ['', [Validators.maxLength(1024)]],
      derogationJustification: [t?.derogationJustification ?? '', [Validators.maxLength(4000)]],
      dataCategories: [listToLines(t?.dataCategories)],
      linkedProcessingActivityIds: [listToLines(t?.linkedProcessingActivityIds), [uuidLinesValidator()]],
      linkedProcessorAgreementIds: [listToLines(t?.linkedProcessorAgreementIds), [uuidLinesValidator()]]
    });

    // OWASP A03 — safeguardsDocumentUrl restreinte à https://
    this.form.controls.safeguardsDocumentUrl.valueChanges.subscribe(v => {
      if (!v) { this.form.controls.safeguardsDocumentUrl.setErrors(null); return; }
      if (!/^https:\/\/.+/.test(v)) this.form.controls.safeguardsDocumentUrl.setErrors({ httpsOnly: true });
    });
    this.form.controls.safeguardsDocumentUrl.setValue(t?.safeguardsDocumentUrl ?? '');
  }

  isDerogation(): boolean { return requiresDerogationJustification(this.form.controls.mechanism.value); }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();

    // OWASP A04 — Art. 49 requires explicit justification.
    if (requiresDerogationJustification(v.mechanism) && !v.derogationJustification?.trim()) {
      this.snack.open(
        $localize`:@@transfers.dialog.derogation-required:Une dérogation Art. 49 exige une justification explicite (intérêt vital, exécution contrat, motifs publics importants, …).`,
        $localize`:@@common.ok:OK`, { duration: 5000 }
      );
      return;
    }
    // OWASP A04 — Art. 46 (SCC/BCR/CoC/Cert) requires safeguardsDescription.
    if (v.mechanism !== 'ADEQUACY_DECISION'
        && v.mechanism !== 'DEROGATION_ART49'
        && !v.safeguardsDescription?.trim()) {
      this.snack.open(
        $localize`:@@transfers.dialog.safeguards-required:Les mécanismes Art. 46 (SCC / BCR / Code / Certification) exigent une description des garanties.`,
        $localize`:@@common.ok:OK`, { duration: 5000 }
      );
      return;
    }

    this.submitting = true;
    const payload = {
      recipientName: v.recipientName.trim(),
      recipientLegalEntity: v.recipientLegalEntity?.trim() || undefined,
      recipientContact: v.recipientContact?.trim() || undefined,
      destinationCountries: lines(v.destinationCountries),
      mechanism: v.mechanism,
      safeguardsDescription: v.safeguardsDescription?.trim() || undefined,
      safeguardsDocumentUrl: v.safeguardsDocumentUrl?.trim() || undefined,
      derogationJustification: v.derogationJustification?.trim() || undefined,
      dataCategories: lines(v.dataCategories),
      linkedProcessingActivityIds: lines(v.linkedProcessingActivityIds),
      linkedProcessorAgreementIds: lines(v.linkedProcessorAgreementIds)
    };

    const op$ = this.isEdit
      ? this.svc.edit(this.data!.transfer!.id, payload)
      : (() => {
          const createdByUserId = this.auth.snapshot()?.userId;
          if (!createdByUserId) {
            this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({ reference: v.reference.trim(), ...payload, createdByUserId });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: t => { this.snack.open(this.isEdit ? $localize`:@@transfers.dialog.updated:Transfert mis à jour.` : $localize`:@@transfers.dialog.created:Transfert créé (DRAFT).`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(t); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[trx-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@transfers.dialog.save-error:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

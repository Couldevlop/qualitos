import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import {
  NonConformityResponse,
  NonConformitySeverity,
  NonConformityStatus,
  SupplierResponse
} from '../../suppliers.types';

/**
 * Sans `nc`, la fenêtre crée une NC ; avec, elle la modifie.
 *
 * <p>`supplierId` absent : la NC s'ouvre depuis le registre transverse, qui ne
 * sait pas encore de quel fournisseur il s'agit — le choix se fait alors dans
 * la fenêtre, parmi `suppliers`. Depuis la fiche d'un fournisseur, il est connu
 * et n'est pas redemandé.
 */
export interface SuppliersNcDialogData {
  supplierId?: string;
  nc?: NonConformityResponse;
  /** Les fournisseurs proposés au choix, quand il reste à faire. */
  suppliers?: SupplierResponse[];
}

@Component({
  selector: 'qos-suppliers-nc-dialog',
  templateUrl: './suppliers-nc-dialog.component.html',
  styleUrls: ['./suppliers-nc-dialog.component.scss'],
  standalone: false
})
export class SuppliersNcDialogComponent {

  submitting = false;

  readonly severities: { value: NonConformitySeverity; label: string }[] = [
    { value: 'MINOR',    label: $localize`:@@suppliers.nc.severity-minor:Mineure` },
    { value: 'MAJOR',    label: $localize`:@@suppliers.nc.severity-major:Majeure` },
    { value: 'CRITICAL', label: $localize`:@@suppliers.nc.severity-critical:Critique` }
  ];

  /** Ouverte, clôturée ou annulée ; « En revue » reste proposé à une NC qui l'est déjà. */
  readonly statuses: { value: NonConformityStatus; label: string }[] = [
    { value: 'OPEN',     label: $localize`:@@suppliers.nc.status-open:Ouverte` },
    ...(this.data.nc?.status === 'IN_REVIEW'
      ? [{ value: 'IN_REVIEW' as NonConformityStatus, label: $localize`:@@suppliers.nc.status-review:En revue` }] : []),
    { value: 'RESOLVED', label: $localize`:@@suppliers.nc.status-closed:Clôturée` },
    { value: 'REJECTED', label: $localize`:@@suppliers.nc.status-canceled:Annulée` }
  ];

  readonly editing = !!this.data.nc;
  readonly createTitle = $localize`:@@suppliers.nc.title:Nouvelle non-conformité fournisseur`;
  readonly editTitle = $localize`:@@suppliers.nc.edit-title:Modifier la non-conformité fournisseur`;

  /** Le fournisseur est à choisir : la fenêtre s'ouvre depuis le registre transverse. */
  readonly choosingSupplier = !this.data.supplierId && !this.data.nc;
  readonly suppliers = this.data.suppliers ?? [];

  readonly form = this.fb.nonNullable.group({
    // Obligatoire quand il est à choisir : une NC sans fournisseur n'existe pas.
    supplierId: [this.data.supplierId ?? this.data.nc?.supplierId ?? '',
                 this.data.supplierId || this.data.nc ? [] : [Validators.required]],
    status:     ['OPEN' as NonConformityStatus, [Validators.required]],
    detectedOn: ['', [Validators.required]],
    severity:   ['MAJOR' as NonConformitySeverity, [Validators.required]],
    partNumber:   ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)]],
    partQuantity: [null as number | null, [Validators.required, Validators.min(1), Validators.max(1_000_000_000),
                                          Validators.pattern(/^\d+$/)]],
    lotReference: ['', [Validators.maxLength(100)]],
    description:  ['', [Validators.maxLength(1000)]],
    eightDRequested: [false],
    eightDReceived:  [false]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SuppliersService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SuppliersNcDialogComponent, NonConformityResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SuppliersNcDialogData
  ) {
    const nc = data.nc;
    if (nc) {
      this.form.patchValue({
        status: nc.status, detectedOn: nc.detectedOn, severity: nc.severity,
        partNumber: nc.partNumber ?? '', partQuantity: nc.partQuantity ?? null,
        lotReference: nc.lotReference ?? '', description: nc.description ?? '',
        eightDRequested: !!nc.eightDRequested, eightDReceived: !!nc.eightDReceived
      });
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    const body = {
      status: v.status,
      detectedOn: v.detectedOn,
      severity: v.severity,
      partNumber: v.partNumber.trim(),
      partQuantity: Number(v.partQuantity),
      eightDRequested: v.eightDRequested,
      eightDReceived: v.eightDReceived,
      lotReference: v.lotReference?.trim() || undefined,
      description:  v.description?.trim()  || undefined
    };
    const nc = this.data.nc;
    const supplierId = this.data.supplierId ?? nc?.supplierId ?? v.supplierId;
    // En modification, un champ vidé s'envoie vide : absent, le serveur le garderait.
    const call = nc
      ? this.svc.updateNc(supplierId, nc.id, {
          ...body, lotReference: v.lotReference.trim(), description: v.description.trim()
        })
      : this.svc.addNc(supplierId, body);
    call
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: nc => { this.snack.open($localize`:@@suppliers.nc.saved:NC fournisseur enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(nc); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-nc] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@suppliers.save-error:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}

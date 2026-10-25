import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { filter } from 'rxjs/operators';

import { SupplierDocumentOwnerType } from '../../suppliers.types';

export interface SupplierDocumentsDialogData {
  supplierId: string;
  ownerType: SupplierDocumentOwnerType;
  ownerId: string;
  /** Ce que porte les documents, en clair : « NC PN-7781-A », « Certificat iso-9001 ». */
  subject: string;
  lockReason?: string | null;
}

/**
 * Les documents d'une NC ou d'un certificat, dans une fenêtre. Elle se ferme
 * sur le nombre de documents s'il a changé, pour que la liste appelante le
 * mette à jour sans tout recharger ; sur rien sinon.
 */
@Component({
  selector: 'qos-supplier-documents-dialog',
  templateUrl: './supplier-documents-dialog.component.html',
  styleUrls: ['./supplier-documents-dialog.component.scss'],
  standalone: false
})
export class SupplierDocumentsDialogComponent {

  private count: number | null = null;

  constructor(
    private readonly dialogRef: MatDialogRef<SupplierDocumentsDialogComponent, number | undefined>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SupplierDocumentsDialogData
  ) {
    // Échap et le voile ferment aussi la fenêtre, mais sans résultat : le nombre
    // de documents se perdrait et la liste afficherait l'ancien. On les fait donc
    // passer par close(). Les deux flux s'achèvent avec la fenêtre.
    dialogRef.disableClose = true;
    dialogRef.backdropClick().subscribe(() => this.close());
    dialogRef.keydownEvents().pipe(filter(e => e.key === 'Escape')).subscribe(() => this.close());
  }

  get title(): string {
    return this.data.ownerType === 'CERTIFICATE'
      ? $localize`:@@suppliers.documents.title-certificate:Documents du certificat`
      : $localize`:@@suppliers.documents.title-nc:Documents de la non-conformité`;
  }

  changed(count: number): void { this.count = count; }

  close(): void { this.dialogRef.close(this.count ?? undefined); }
}

import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { SuppliersService } from '../../suppliers.service';
import {
  CATEGORIES_BY_OWNER,
  DOCUMENT_CATEGORIES,
  documentCategoryLabel,
  documentIcon,
  fileSize
} from '../../suppliers.labels';
import { SupplierDocument, SupplierDocumentCategory, SupplierDocumentOwnerType } from '../../suppliers.types';

/** Plafond par fichier, le même que le serveur : refuser tôt évite d'envoyer 10 Mo pour rien. */
export const SUPPLIER_DOCUMENT_MAX_BYTES = 10 * 1024 * 1024;

/**
 * Rôles autorisés à RETIRER un document : miroir de la règle générale des
 * suppressions côté serveur. L'autorité reste le serveur — on évite seulement
 * d'offrir un bouton qui répondrait 403.
 */
const ROLES_RETRAIT = ['ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN', 'QUALITY_MANAGER'];

/**
 * Documents d'une NC fournisseur (8D, preuves de CAPA) ou d'un certificat.
 * Les fichiers vont dans le stockage objet, jamais dans la base ; ils se
 * retrouvent aussi dans la vue « Documents fournisseurs » de la GED.
 */
@Component({
  selector: 'qos-supplier-documents-panel',
  templateUrl: './supplier-documents-panel.component.html',
  styleUrls: ['./supplier-documents-panel.component.scss'],
  standalone: false
})
export class SupplierDocumentsPanelComponent implements OnChanges {

  @Input({ required: true }) supplierId!: string;
  @Input({ required: true }) ownerType!: SupplierDocumentOwnerType;
  @Input({ required: true }) ownerId!: string;
  /** Raison du verrou (NC close), ou null si l'on peut joindre et retirer. */
  @Input() lockReason: string | null = null;
  /** Le nombre de documents après chaque ajout ou retrait. */
  @Output() readonly changed = new EventEmitter<number>();

  /** Les types admis par le serveur — le sélecteur de fichiers n'en propose pas d'autres. */
  readonly accept = [
    '.pdf', '.jpg', '.jpeg', '.png', '.webp', '.heic', '.docx', '.xlsx', '.pptx',
    'application/pdf', 'image/jpeg', 'image/png', 'image/webp', 'image/heic',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation'
  ].join(',');

  categories: { value: SupplierDocumentCategory; label: string }[] = [];
  category: SupplierDocumentCategory = 'OTHER';

  documents: SupplierDocument[] = [];
  loading = false;
  loadError: string | null = null;
  uploading = false;

  constructor(
    private readonly svc: SuppliersService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['ownerType'] && this.ownerType) {
      const allowed = CATEGORIES_BY_OWNER[this.ownerType];
      this.categories = DOCUMENT_CATEGORIES.filter(c => allowed.includes(c.value));
      this.category = allowed[0];
    }
    if ((changes['ownerId'] || changes['supplierId']) && this.ownerId && this.supplierId) this.load();
  }

  get canRemove(): boolean {
    return !this.lockReason && this.auth.hasAnyRole(ROLES_RETRAIT);
  }

  choose(input: HTMLInputElement): void {
    const file = input.files?.[0];
    // Vidé tout de suite : sans cela, rejoindre le même fichier après un refus
    // ne déclencherait aucun événement.
    input.value = '';
    if (!file || this.lockReason) return;
    if (file.size > SUPPLIER_DOCUMENT_MAX_BYTES) {
      this.snack.open($localize`:@@suppliers.documents.too-large:Fichier trop lourd : 10 Mo au plus.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.uploading = true;
    this.svc.uploadDocument(this.supplierId, this.ownerType, this.ownerId, file, this.category)
      .pipe(finalize(() => (this.uploading = false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@suppliers.documents.added:Document ajouté.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.load(true);
        },
        error: err => this.fail(err)
      });
  }

  remove(doc: SupplierDocument): void {
    const name = doc.originalFilename || $localize`:@@suppliers.documents.unnamed:document sans nom`;
    const data: ConfirmDialogData = {
      title: $localize`:@@suppliers.documents.delete-title:Retirer ce document ?`,
      message: $localize`:@@suppliers.documents.delete-message:« ${name}:name: » sera retiré. Le retrait est inscrit au journal d'audit.`,
      confirmLabel: $localize`:@@suppliers.documents.delete:Retirer`,
      destructive: true
    };
    this.dialog.open(ConfirmDialogComponent, { data, autoFocus: false, restoreFocus: true })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.svc.deleteDocument(this.supplierId, this.ownerType, this.ownerId, doc.id).subscribe({
          next: () => this.load(true),
          error: err => this.fail(err)
        });
      });
  }

  removeAria(doc: SupplierDocument): string {
    const name = doc.originalFilename || '';
    return $localize`:@@suppliers.documents.remove-aria:Retirer le document ${name}:name:`;
  }

  categoryLabel(c: SupplierDocumentCategory): string { return documentCategoryLabel(c); }
  icon(doc: SupplierDocument): string { return documentIcon(doc.contentType); }
  size(bytes: number): string { return fileSize(bytes); }
  trackById(_i: number, d: SupplierDocument): string { return d.id; }

  private load(notify = false): void {
    this.loading = true;
    this.loadError = null;
    this.svc.listDocuments(this.supplierId, this.ownerType, this.ownerId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: docs => {
          this.documents = docs;
          if (notify) this.changed.emit(docs.length);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[supplier-documents] load failed', err?.status);
          this.loadError = (err as { status?: number })?.status === 503
            ? $localize`:@@suppliers.documents.storage-off:Le stockage des documents est désactivé sur cet environnement.`
            : safeErrorMessage(err, $localize`:@@suppliers.documents.load-error:Les documents n'ont pas pu être chargés.`);
        }
      });
  }

  private fail(err: unknown): void {
    const status = (err as { status?: number })?.status;
    // eslint-disable-next-line no-console
    console.warn('[supplier-documents] failed', status);
    const message = status === 400
      ? $localize`:@@suppliers.documents.invalid:Fichier refusé : type non admis ou contenu qui ne correspond pas à son extension.`
      : status === 413
        ? $localize`:@@suppliers.documents.limit:Limite atteinte : 10 Mo par fichier, 20 documents et 50 Mo au total.`
        : status === 409
          ? $localize`:@@suppliers.documents.locked:Les documents sont verrouillés : la non-conformité est close.`
          : safeErrorMessage(err, $localize`:@@suppliers.documents.error:L'opération sur le document a échoué.`);
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 5000 });
  }
}

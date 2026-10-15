import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { AuditsService } from '../../audits.service';
import { AuditDocument } from '../../audits.types';

/** Plafond par fichier, le même que le serveur : refuser tôt évite d'envoyer 10 Mo pour rien. */
export const AUDIT_DOCUMENT_MAX_BYTES = 10 * 1024 * 1024;

/**
 * Rôles autorisés à RETIRER un document. Miroir de la règle générale des
 * suppressions côté serveur ; l'autorité reste le serveur — on évite seulement
 * d'offrir un bouton qui répondrait 403.
 */
const ROLES_RETRAIT = ['ADMIN', 'ADMIN_TENANT', 'SUPER_ADMIN', 'QUALITY_MANAGER'];

/**
 * Section « Documents » d'un audit : convocation, plan d'audit signé, photos,
 * relevés, rapport de l'organisme… Les fichiers vont dans le stockage objet
 * existant (le même que les preuves CAPA), jamais dans la base.
 */
@Component({
  selector: 'qos-audit-documents-panel',
  templateUrl: './audit-documents-panel.component.html',
  styleUrls: ['./audit-documents-panel.component.scss'],
  standalone: false
})
export class AuditDocumentsPanelComponent implements OnChanges {

  @Input({ required: true }) planId!: string;
  /** Raison du verrou (audit annulé, rapport émis), ou null si l'on peut joindre et retirer. */
  @Input() lockReason: string | null = null;

  /** Les types admis par le serveur — le sélecteur de fichiers n'en propose pas d'autres. */
  readonly accept = [
    '.pdf', '.jpg', '.jpeg', '.png', '.webp', '.heic', '.docx', '.xlsx', '.pptx',
    'application/pdf', 'image/jpeg', 'image/png', 'image/webp', 'image/heic',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation'
  ].join(',');

  documents: AuditDocument[] = [];
  loading = false;
  loadError: string | null = null;
  uploading = false;

  constructor(
    private readonly audits: AuditsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['planId'] && this.planId) this.load();
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
    if (file.size > AUDIT_DOCUMENT_MAX_BYTES) {
      this.snack.open($localize`:@@audits.documents.too-large:Fichier trop lourd : 10 Mo au plus.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.uploading = true;
    this.audits.uploadDocument(this.planId, file)
      .pipe(finalize(() => (this.uploading = false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@audits.documents.added:Document ajouté à l'audit.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.load();
        },
        error: err => this.fail(err)
      });
  }

  remove(doc: AuditDocument): void {
    const name = doc.originalFilename || $localize`:@@audits.documents.unnamed:document sans nom`;
    const data: ConfirmDialogData = {
      title: $localize`:@@audits.documents.delete-title:Retirer ce document ?`,
      message: $localize`:@@audits.documents.delete-message:« ${name}:name: » sera retiré de l'audit. Le retrait est inscrit au journal d'audit.`,
      confirmLabel: $localize`:@@audits.documents.delete:Retirer`,
      destructive: true
    };
    this.dialog.open(ConfirmDialogComponent, { data, autoFocus: false, restoreFocus: true })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.audits.deleteDocument(this.planId, doc.id).subscribe({
          next: () => this.load(),
          error: err => this.fail(err)
        });
      });
  }

  removeAria(doc: AuditDocument): string {
    const name = doc.originalFilename || '';
    return $localize`:@@audits.documents.remove-aria:Retirer le document ${name}:name:`;
  }

  icon(doc: AuditDocument): string {
    if (doc.contentType.startsWith('image/')) return 'image';
    if (doc.contentType === 'application/pdf') return 'picture_as_pdf';
    return 'description';
  }

  /** Taille lisible : 1,2 Mo plutôt que 1258291. */
  size(bytes: number): string {
    return bytes >= 1024 * 1024
      ? `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
      : `${Math.max(1, Math.round(bytes / 1024))} Ko`;
  }

  trackById(_i: number, d: AuditDocument): string { return d.id; }

  private load(): void {
    this.loading = true;
    this.loadError = null;
    this.audits.listDocuments(this.planId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: docs => (this.documents = docs),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audit-documents] load failed', err?.status);
          this.loadError = (err as { status?: number })?.status === 503
            ? $localize`:@@audits.documents.storage-off:Le stockage des documents est désactivé sur cet environnement.`
            : safeErrorMessage(err, $localize`:@@audits.documents.load-error:Les documents n'ont pas pu être chargés.`);
        }
      });
  }

  private fail(err: unknown): void {
    const status = (err as { status?: number })?.status;
    // eslint-disable-next-line no-console
    console.warn('[audit-documents] failed', status);
    const message = status === 400
      ? $localize`:@@audits.documents.invalid:Fichier refusé : type non admis ou contenu qui ne correspond pas à son extension.`
      : status === 413
        ? $localize`:@@audits.documents.limit:Limite atteinte : 10 Mo par fichier, 20 documents et 50 Mo par audit.`
        : status === 409
          ? $localize`:@@audits.documents.locked:Les documents de cet audit sont verrouillés (audit annulé ou rapport émis).`
          : safeErrorMessage(err, $localize`:@@audits.documents.error:L'opération sur le document a échoué.`);
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 5000 });
  }
}

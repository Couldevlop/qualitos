import { HttpEventType } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { FolderRow, buildFolderRows } from '../../documents.folders';
import {
  DISPLAY_STATUS_LABELS, DOCUMENT_TYPE_LABELS, VERSION_STATUS_LABELS, displayStatusOf, formatBytes
} from '../../documents.labels';
import { DocumentsService } from '../../documents.service';
import {
  DisplayStatus,
  DocumentFolder,
  DocumentProcess,
  DocumentResponse,
  DocumentType,
  DocumentVersionResponse,
  VersionStatus
} from '../../documents.types';
import { DocumentsEditDialogComponent } from '../documents-edit-dialog/documents-edit-dialog.component';
import { DocumentsRejectDialogComponent } from '../documents-reject-dialog/documents-reject-dialog.component';
import { DocumentsSubmitDialogComponent } from '../documents-submit-dialog/documents-submit-dialog.component';
import { DocumentsVersionDialogComponent } from '../documents-version-dialog/documents-version-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-documents-detail',
  templateUrl: './documents-detail.component.html',
  styleUrls: ['./documents-detail.component.scss'],
  standalone: false
})
export class DocumentsDetailComponent implements OnInit {

  readonly versionColumns = ['versionNumber', 'status', 'file', 'changeNote', 'authorId', 'updatedAt', 'actions'];
  readonly statusLabels = DISPLAY_STATUS_LABELS;

  folders: FolderRow[] = [];
  processes: DocumentProcess[] = [];
  /** La version dont le fichier part, et sa progression en pour cent. */
  uploading: { versionId: string; progress: number } | null = null;

  document$!: Observable<DocumentResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private documentId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: DocumentsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.svc.folders().pipe(catchError(() => of([] as DocumentFolder[])))
      .subscribe(f => this.folders = buildFolderRows(f));
    this.svc.processes().pipe(catchError(() => of([] as DocumentProcess[])))
      .subscribe(p => this.processes = p);
    this.document$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('doc-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        this.documentId = id;
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[documents-detail] get failed', err?.status, err?.error?.title);
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loadingState$.next(false))
          ))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openEdit(d: DocumentResponse): void {
    const ref = this.dialog.open(DocumentsEditDialogComponent, {
      data: { document: d, folders: this.folders, processes: this.processes }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  archive(d: DocumentResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@documents.detail.archive-confirm-title:Archiver le document ?`,
        message: '« ' + d.title + ' » sera marqué ARCHIVED — non éditable mais conservé pour audit.',
        confirmLabel: $localize`:@@documents.detail.archive:Archiver`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(d.id).subscribe({
        next: () => { this.snack.open($localize`:@@documents.detail.archived:Document archivé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@documents.detail.archive-failed:Archivage impossible.`)
      });
    });
  }

  openNewVersion(d: DocumentResponse): void {
    const next = (d.versions.reduce((m, v) => Math.max(m, v.versionNumber), 0) || 0) + 1;
    const ref = this.dialog.open(DocumentsVersionDialogComponent, {
      data: { documentId: d.id, nextVersionNumber: next },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(v => { if (v) this.refresh$.next(); });
  }

  /** Soumettre, c'est choisir ses signataires (ADR 0099) ; une version sans fichier ne part pas. */
  submitForReview(v: DocumentVersionResponse): void {
    if (!v.file) {
      this.snack.open($localize`:@@documents.detail.submit-needs-file:Joignez le fichier du document avant de soumettre la version.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.dialog.open(DocumentsSubmitDialogComponent, {
      data: { version: v }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(r => { if (r) this.refresh$.next(); });
  }

  /** Une revue d'avant l'ADR 0099, sans signataires : elle se décide encore par le droit d'approuver. */
  legacyReview(v: DocumentVersionResponse): boolean {
    return v.status === 'IN_REVIEW' && !v.signatories?.length;
  }

  approve(v: DocumentVersionResponse): void {
    const approverId = this.auth.snapshot()?.userId;
    if (!approverId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.svc.approve(v.documentId, v.id, { approverId }).subscribe({
      next: r => {
        // Avec un circuit, une approbation peut ne franchir qu'une étape (ADR 0080).
        this.snack.open(r.status === 'APPROVED'
          ? $localize`:@@documents.detail.approved:Version approuvée.`
          : $localize`:@@documents.detail.step-approved:Votre approbation est enregistrée : la validation continue.`,
        $localize`:@@common.ok:OK`, { duration: 2800 });
        this.refresh$.next();
      },
      error: err => this.fail(err, $localize`:@@documents.detail.approve-failed:Approbation impossible.`)
    });
  }

  reject(v: DocumentVersionResponse): void {
    this.dialog.open(DocumentsRejectDialogComponent, {
      data: { version: v }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(r => { if (r) this.refresh$.next(); });
  }

  /** Relit la fiche après une signature ou un refus. */
  refresh(): void {
    this.refresh$.next();
  }

  /**
   * Les versions qui demandent l'attention : celle en revue, un brouillon revenu d'un refus,
   * et la dernière version signée, dont les cartes gardent la trace des signatures.
   */
  attention(d: DocumentResponse): DocumentVersionResponse[] {
    const open = d.versions.filter(v => v.status === 'IN_REVIEW' || (v.status === 'DRAFT' && !!v.rejectionReason));
    const signed = d.versions
      .filter(v => (v.status === 'APPROVED' || v.status === 'PUBLISHED') && !!v.signatories?.length)
      .reduce<DocumentVersionResponse | null>((last, v) => !last || v.versionNumber > last.versionNumber ? v : last, null);
    return signed ? [...open, signed] : open;
  }

  trackById(_i: number, v: DocumentVersionResponse): string {
    return v.id;
  }

  publish(v: DocumentVersionResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Publier la version v' + v.versionNumber + ' ?',
        message: $localize`:@@documents.detail.publish-confirm-message:La version actuelle deviendra Obsolète.`,
        confirmLabel: $localize`:@@documents.detail.publish:Publier`,
        cancelLabel: $localize`:@@common.cancel:Annuler`,
        danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.publish(v.documentId, v.id).subscribe({
        next: () => { this.snack.open($localize`:@@documents.detail.published:Version publiée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@documents.detail.publish-failed:Publication impossible.`)
      });
    });
  }

  acknowledge(v: DocumentVersionResponse): void {
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.svc.acknowledge(v.documentId, v.id, { userId }).subscribe({
      next: () => this.snack.open($localize`:@@documents.detail.acknowledged:Lecture confirmée.`, $localize`:@@common.ok:OK`, { duration: 2500 }),
      error: err => this.fail(err, $localize`:@@documents.detail.acknowledge-failed:Acquittement impossible.`)
    });
  }

  // ---------- fichier d'une version ----------

  /** Joint ou remplace le fichier d'une version en brouillon. */
  uploadFile(v: DocumentVersionResponse, input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.uploading) return;
    this.uploading = { versionId: v.id, progress: 0 };
    this.svc.uploadFile(v.documentId, v.id, file).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total && this.uploading) {
          this.uploading.progress = Math.round(100 * event.loaded / event.total);
        } else if (event.type === HttpEventType.Response) {
          this.uploading = null;
          this.snack.open($localize`:@@documents.detail.file-uploaded:Fichier joint.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.refresh$.next();
        }
      },
      error: err => {
        this.uploading = null;
        this.fail(err, $localize`:@@documents.detail.file-upload-failed:Le fichier n'a pas pu être envoyé.`);
      }
    });
  }

  download(v: DocumentVersionResponse): void {
    this.svc.fileLink(v.documentId, v.id, true).subscribe({
      next: link => {
        const a = window.document.createElement('a');
        a.href = link.url;
        a.rel = 'noopener';
        a.click();
      },
      error: err => this.fail(err, $localize`:@@documents.panel.download-failed:Le fichier n'a pas pu être téléchargé. Réessayez.`)
    });
  }

  size(bytes: number): string {
    return formatBytes(bytes);
  }

  displayStatus(d: DocumentResponse): DisplayStatus {
    return displayStatusOf(d);
  }

  versionBadge(s: VersionStatus): string { return 'vbadge vbadge-' + s.toLowerCase(); }
  versionLabel(s: VersionStatus): string { return VERSION_STATUS_LABELS[s] ?? s; }
  typeLabel(t: DocumentType): string { return DOCUMENT_TYPE_LABELS[t]; }

  folderName(id: string | null | undefined): string | null {
    return id ? this.folders.find(r => r.folder.id === id)?.folder.name ?? null : null;
  }

  processName(id: string | null | undefined): string | null {
    return id ? this.processes.find(p => p.id === id)?.name ?? null : null;
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[documents-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}

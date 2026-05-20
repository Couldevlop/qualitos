import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { DocumentsService } from '../../documents.service';
import {
  DocumentResponse,
  DocumentStatus,
  DocumentType,
  DocumentVersionResponse,
  VersionStatus
} from '../../documents.types';
import { DocumentsEditDialogComponent } from '../documents-edit-dialog/documents-edit-dialog.component';
import { DocumentsVersionDialogComponent } from '../documents-version-dialog/documents-version-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-documents-detail',
  templateUrl: './documents-detail.component.html',
  styleUrls: ['./documents-detail.component.scss'],
  standalone: false
})
export class DocumentsDetailComponent implements OnInit {

  readonly versionColumns = ['versionNumber', 'status', 'changeNote', 'authorId', 'updatedAt', 'actions'];

  document$!: Observable<DocumentResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

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
    this.document$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('doc-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        this.documentId = id;
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[documents-detail] get failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  openEdit(d: DocumentResponse): void {
    const ref = this.dialog.open(DocumentsEditDialogComponent, {
      data: { document: d }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  archive(d: DocumentResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Archiver le document ?',
        message: '« ' + d.title + ' » sera marqué ARCHIVED — non éditable mais conservé pour audit.',
        confirmLabel: 'Archiver',
        cancelLabel: 'Annuler',
        danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(d.id).subscribe({
        next: () => { this.snack.open('Document archivé.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Archivage impossible.')
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

  submitForReview(v: DocumentVersionResponse): void {
    this.svc.submit(v.documentId, v.id).subscribe({
      next: () => { this.snack.open('Soumise à revue.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Soumission impossible.')
    });
  }

  approve(v: DocumentVersionResponse): void {
    const approverId = this.auth.snapshot()?.userId;
    if (!approverId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.svc.approve(v.documentId, v.id, { approverId }).subscribe({
      next: () => { this.snack.open('Version approuvée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Approbation impossible.')
    });
  }

  publish(v: DocumentVersionResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Publier la version v' + v.versionNumber + ' ?',
        message: 'La version actuellement publiée sera marquée OBSOLETE.',
        confirmLabel: 'Publier',
        cancelLabel: 'Annuler',
        danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.publish(v.documentId, v.id).subscribe({
        next: () => { this.snack.open('Version publiée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Publication impossible.')
      });
    });
  }

  acknowledge(v: DocumentVersionResponse): void {
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.svc.acknowledge(v.documentId, v.id, { userId }).subscribe({
      next: () => this.snack.open('Lecture confirmée.', 'OK', { duration: 2500 }),
      error: err => this.fail(err, 'Acquittement impossible.')
    });
  }

  statusBadge(s: DocumentStatus): string { return 'badge badge-' + s.toLowerCase(); }
  versionBadge(s: VersionStatus): string { return 'vbadge vbadge-' + s.toLowerCase(); }
  typeLabel(t: DocumentType): string {
    return ({
      POLICY: 'Politique', PROCEDURE: 'Procédure', WORK_INSTRUCTION: 'Mode opératoire',
      RECORD: 'Enregistrement', FORM: 'Formulaire', MANUAL: 'Manuel', OTHER: 'Autre'
    })[t];
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[documents-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}

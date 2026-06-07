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
      data: { document: d }, autoFocus: 'first-tabbable', restoreFocus: true,
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

  submitForReview(v: DocumentVersionResponse): void {
    this.svc.submit(v.documentId, v.id).subscribe({
      next: () => { this.snack.open($localize`:@@documents.detail.submitted:Soumise à revue.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@documents.detail.submit-failed:Soumission impossible.`)
    });
  }

  approve(v: DocumentVersionResponse): void {
    const approverId = this.auth.snapshot()?.userId;
    if (!approverId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.svc.approve(v.documentId, v.id, { approverId }).subscribe({
      next: () => { this.snack.open($localize`:@@documents.detail.approved:Version approuvée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@documents.detail.approve-failed:Approbation impossible.`)
    });
  }

  publish(v: DocumentVersionResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Publier la version v' + v.versionNumber + ' ?',
        message: $localize`:@@documents.detail.publish-confirm-message:La version actuellement publiée sera marquée OBSOLETE.`,
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

  statusBadge(s: DocumentStatus): string { return 'badge badge-' + s.toLowerCase(); }
  versionBadge(s: VersionStatus): string { return 'vbadge vbadge-' + s.toLowerCase(); }
  typeLabel(t: DocumentType): string {
    return ({
      POLICY: $localize`:@@documents.type.policy:Politique`,
      PROCEDURE: $localize`:@@documents.type.procedure:Procédure`,
      WORK_INSTRUCTION: $localize`:@@documents.type.work-instruction:Mode opératoire`,
      RECORD: $localize`:@@documents.type.record:Enregistrement`,
      FORM: $localize`:@@documents.type.form:Formulaire`,
      MANUAL: $localize`:@@documents.type.manual:Manuel`,
      OTHER: $localize`:@@documents.type.other:Autre`
    })[t];
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[documents-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}

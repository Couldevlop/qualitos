import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { NcService } from '../../nc.service';
import { NcPhoto, NcResponse, NcSeverity, NcStatus } from '../../nc.types';
import {
  NcResolveDialogComponent,
  NcResolveDialogData
} from '../nc-resolve-dialog/nc-resolve-dialog.component';

// OWASP A03 — refuse malformed UUIDs client-side. Demo mock ids ("nc-1"…)
// are also accepted so the page stays usable with useMockApi=true.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-nc-detail',
  templateUrl: './nc-detail.component.html',
  styleUrls: ['./nc-detail.component.scss'],
  standalone: false
})
export class NcDetailComponent implements OnInit {

  nc$!: Observable<NcResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  // --- photos (upload binaire, online-only) ---------------------------------
  photos$ = new BehaviorSubject<NcPhoto[]>([]);
  photosLoading$ = new BehaviorSubject<boolean>(false);
  uploading$ = new BehaviorSubject<boolean>(false);
  /** Bascule à true quand le backend renvoie 503 type 'storage-disabled'. */
  storageDisabled$ = new BehaviorSubject<boolean>(false);
  /** id de la photo en cours de suppression (désactive sa vignette). */
  deletingId$ = new BehaviorSubject<string | null>(null);
  /** État réseau pour désactiver le bouton d'ajout hors-ligne. */
  online$ = this.connectivity.online$;
  /** Tooltip du bouton d'ajout quand hors-ligne (interpolation non marquable i18n). */
  readonly photosOfflineTooltip = $localize`:@@nc.photos.offline-tooltip:Photos disponibles en ligne uniquement`;

  private ncId = '';
  private readonly reload$ = new Subject<void>();
  private isMockId(s: string): boolean { return /^nc-/.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: NcService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialog: MatDialog,
    private readonly connectivity: ConnectivityService
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/nc']);
      return;
    }
    this.ncId = raw;
    this.nc$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc.getNc(this.ncId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] getNc failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@nc.detail.not-found:Non-conformité introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
    this.loadPhotos();
  }

  // --- photos -----------------------------------------------------------------

  loadPhotos(): void {
    queueMicrotask(() => this.photosLoading$.next(true));
    this.svc.listPhotos(this.ncId)
      .pipe(finalize(() => this.photosLoading$.next(false)))
      .subscribe({
        next: photos => { this.photos$.next(photos); this.storageDisabled$.next(false); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] listPhotos failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.storageDisabled$.next(true); return; }
          // Échec non bloquant : la fiche reste utilisable, les photos juste absentes.
        }
      });
  }

  /** Déclenche l'<input type="file"> masqué (caméra / galerie). */
  triggerFilePicker(input: HTMLInputElement): void {
    if (this.uploading$.value) return;
    input.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    // Réinitialise l'input pour autoriser la re-sélection du même fichier.
    input.value = '';
    if (!file) return;
    this.uploadPhoto(file);
  }

  private uploadPhoto(file: File): void {
    this.uploading$.next(true);
    this.svc.uploadPhoto(this.ncId, file)
      .pipe(finalize(() => this.uploading$.next(false)))
      .subscribe({
        next: photo => {
          this.photos$.next([photo, ...this.photos$.value]);
          this.storageDisabled$.next(false);
          this.snack.open(
            $localize`:@@nc.photos.upload-success:Photo ajoutée.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] uploadPhoto failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.storageDisabled$.next(true); return; }
          const msg =
            err?.status === 413 || err?.status === 400
              ? $localize`:@@nc.photos.upload-rejected:Fichier refusé — image (JPEG, PNG, WebP, HEIC) de 10 Mo maximum.`
              : err?.status === 409
              ? $localize`:@@nc.photos.upload-closed:Impossible d'ajouter une photo à une non-conformité clôturée ou annulée.`
              : safeErrorMessage(err, $localize`:@@nc.photos.upload-error:Échec de l'ajout de la photo.`);
          this.snack.open(msg, 'OK', { duration: 4000 });
        }
      });
  }

  deletePhoto(photo: NcPhoto): void {
    if (this.deletingId$.value) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@nc.photos.delete-confirm-title:Supprimer cette photo ?`,
        message: $localize`:@@nc.photos.delete-confirm-message:La photo sera définitivement supprimée du stockage. Cette action est irréversible.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.deletingId$.next(photo.id);
      this.svc.deletePhoto(this.ncId, photo.id)
        .pipe(finalize(() => this.deletingId$.next(null)))
        .subscribe({
          next: () => {
            this.photos$.next(this.photos$.value.filter(p => p.id !== photo.id));
            this.snack.open(
              $localize`:@@nc.photos.delete-success:Photo supprimée.`,
              $localize`:@@common.ok:OK`, { duration: 2000 });
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[nc-detail] deletePhoto failed', err?.status, err?.error?.type);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@nc.photos.delete-error:Échec de la suppression de la photo.`),
              'OK', { duration: 4000 });
          }
        });
    });
  }

  /**
   * 503 + ProblemDetail dont le type contient 'storage-disabled' → message UX
   * dédié, pas une erreur brute. Le backend émet le type en URI complète
   * (https://qualitos.io/errors/storage-disabled) — détection par inclusion.
   */
  private isStorageDisabled(err: unknown): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    const type = e?.error?.type;
    return e?.status === 503 && typeof type === 'string' && type.includes('storage-disabled');
  }

  /** Le bouton d'ajout est masqué dès que la NC est en état terminal. */
  canAddPhoto(status: NcStatus): boolean {
    return !this.isTerminal(status);
  }

  goBack(): void {
    this.router.navigate(['/nc']);
  }

  photoList(photoUrls?: string): string[] {
    if (!photoUrls) return [];
    return photoUrls.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  }

  startAnalysis(): void { this.transition('start-analysis'); }
  defineAction(): void { this.transition('define-action'); }
  close(): void { this.transition('close'); }

  cancel(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@nc.detail.cancel-confirm-title:Annuler cette non-conformité ?`,
        message: $localize`:@@nc.detail.cancel-confirm-message:La non-conformité sera marquée comme annulée. Cette décision est traçable.`,
        confirmLabel: $localize`:@@common.cancel:Annuler`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.transition('cancel');
    });
  }

  openResolve(n: NcResponse): void {
    const data: NcResolveDialogData = { ncId: n.id, reference: n.reference };
    this.dialog
      .open(NcResolveDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(resolved => { if (resolved) this.reload$.next(); });
  }

  escalateToCapa(): void {
    if (this.acting$.value) return;
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.snack.open(
        $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@nc.detail.escalate-confirm-title:Escalader vers une CAPA ?`,
        message: $localize`:@@nc.detail.escalate-confirm-message:Une action corrective/préventive (CAPA) sera créée et liée à cette non-conformité.`,
        confirmLabel: $localize`:@@nc.detail.escalate:Escalader CAPA`
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.acting$.next(true);
      this.svc.escalateToCapa(this.ncId, { ownerId })
        .pipe(finalize(() => this.acting$.next(false)))
        .subscribe({
          next: () => {
            this.snack.open(
              $localize`:@@nc.detail.escalate-success:CAPA créée et liée à la non-conformité.`,
              $localize`:@@common.ok:OK`, { duration: 2500 });
            this.reload$.next();
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[nc-detail] escalate failed', err?.status, err?.error?.title);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@nc.detail.escalate-error:Erreur lors de l'escalade.`),
              'OK', { duration: 4000 });
          }
        });
    });
  }

  private transition(action: 'start-analysis' | 'define-action' | 'close' | 'cancel'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start-analysis' ? this.svc.startAnalysis(this.ncId)
      : action === 'define-action' ? this.svc.defineAction(this.ncId)
      : action === 'close' ? this.svc.close(this.ncId)
      : this.svc.cancel(this.ncId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@nc.detail.transition-success:Statut mis à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[nc-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@nc.detail.transition-error:Erreur lors de la transition.`),
          'OK', { duration: 4000 });
      }
    });
  }

  isTerminal(status: NcStatus): boolean {
    return status === 'CLOSED' || status === 'CANCELLED';
  }

  canStartAnalysis(status: NcStatus): boolean { return status === 'OPEN'; }
  canDefineAction(status: NcStatus): boolean { return status === 'UNDER_ANALYSIS'; }
  canResolve(status: NcStatus): boolean { return status === 'ACTION_DEFINED'; }
  canClose(status: NcStatus): boolean { return status === 'RESOLVED'; }
  canCancel(status: NcStatus): boolean { return !this.isTerminal(status); }
  canEscalate(status: NcStatus): boolean { return !this.isTerminal(status); }

  statusBadgeClass(status: NcStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  severityBadgeClass(severity: NcSeverity): string {
    return 'sev sev-' + severity.toLowerCase();
  }
}

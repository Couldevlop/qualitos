import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse, PdcaPhase, PdcaStatus, PdcaStepEvidence, PdcaStepResponse } from '../../pdca.types';
import { PdcaStepDialogComponent, PdcaStepDialogData } from '../pdca-step-dialog/pdca-step-dialog.component';

// OWASP A03 — defense-in-depth: the backend re-validates the UUID, but we
// also refuse malformed route params client-side to avoid any chance of
// open-redirect / path-traversal-style abuse if the value ever lands in
// URLs we construct from it later.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-pdca-detail',
  templateUrl: './pdca-detail.component.html',
  styleUrls: ['./pdca-detail.component.scss'],
  standalone: false
})
export class PdcaDetailComponent implements OnInit {

  // La preuve suit immédiatement l'échéance : « pour quand » puis « et voici
  // que c'est fait ». Les rejeter en fin de ligne aurait éloigné la pièce de la
  // date qu'elle justifie.
  readonly stepColumns = ['phase', 'title', 'status', 'dueDate', 'evidence', 'updatedAt'];

  cycle$!: Observable<PdcaCycleResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  // --- preuves d'étape (§3.1, ADR 0061) ---------------------------------------
  // Une étape déclarée faite sans document ne prouve rien : elle affirme.

  /** Pièce de chaque étape, indexée par identifiant d'étape — une par étape. */
  readonly stepEvidences$ = new BehaviorSubject<Map<string, PdcaStepEvidence>>(new Map());

  /** Étape dont la pièce arrive ou s'en va : la ligne est engagée, pas acquise. */
  readonly busyEvidenceStepId$ = new BehaviorSubject<string | null>(null);

  /**
   * Vrai quand le stockage objet est coupé sur l'environnement. La colonne
   * affiche alors son état plutôt qu'un bouton qui ne peut rien faire.
   */
  readonly evidenceStorageDisabled$ = new BehaviorSubject<boolean>(false);

  /** Étape visée par le sélecteur de fichier partagé entre toutes les lignes. */
  private pendingEvidenceStepId: string | null = null;

  private cycleId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly pdca: PdcaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/pdca']);
      return;
    }
    this.cycleId = raw;
    this.cycle$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.pdca.getCycle(this.cycleId).pipe(
        catchError(err => {
          // OWASP A09 — do not echo backend error.detail to the UI: it can
          // disclose stack traces, internal class names, or DB hints. Log
          // technical info to console for ops only.
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] getCycle failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@pdca.detail.not-found:Cycle introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
    this.loadStepEvidences();
  }

  // --- preuves d'étape (§3.1, ADR 0061) ---------------------------------------

  /**
   * Chargement non bloquant : le tableau reste lisible même si le stockage est
   * coupé — la colonne « Preuve » affiche alors son état plutôt qu'une erreur en
   * travers de la fiche.
   */
  loadStepEvidences(): void {
    this.pdca.listStepEvidences(this.cycleId).subscribe({
      next: list => {
        const byStep = new Map<string, PdcaStepEvidence>();
        for (const e of list) {
          byStep.set(e.stepId, e);
        }
        this.stepEvidences$.next(byStep);
        this.evidenceStorageDisabled$.next(false);
      },
      error: err => {
        // OWASP A09 — le détail technique va en console, jamais à l'écran.
        // eslint-disable-next-line no-console
        console.warn('[pdca-detail] listStepEvidences failed', err?.status, err?.error?.type);
        if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); }
      }
    });
  }

  /** Pièce d'une étape, ou {@code undefined} quand la ligne n'en porte pas encore. */
  stepEvidence(stepId: string): PdcaStepEvidence | undefined {
    return this.stepEvidences$.value.get(stepId);
  }

  /**
   * Une pièce se joint tant que le cycle vit, que le stockage répond, que
   * l'étape n'en porte pas déjà une et qu'aucun échange n'est en cours sur
   * cette ligne.
   */
  canAttachStepEvidence(stepId: string, cycleStatus: PdcaStatus): boolean {
    return !this.isTerminal(cycleStatus)
        && !this.evidenceStorageDisabled$.value
        && !this.stepEvidence(stepId)
        && this.busyEvidenceStepId$.value !== stepId;
  }

  /**
   * Ouvre le sélecteur de fichier, unique pour toutes les lignes, en mémorisant
   * l'étape visée. Le champ est remis à zéro avant ouverture : sans cela,
   * rechoisir le même fichier après un échec ne déclencherait aucun événement.
   */
  triggerStepEvidencePicker(input: HTMLInputElement, stepId: string): void {
    if (this.busyEvidenceStepId$.value) { return; }
    input.value = '';
    this.pendingEvidenceStepId = stepId;
    input.click();
  }

  onStepEvidenceSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    const stepId = this.pendingEvidenceStepId;
    this.pendingEvidenceStepId = null;
    input.value = '';
    if (!file || !stepId) { return; }

    this.busyEvidenceStepId$.next(stepId);
    this.pdca.uploadStepEvidence(this.cycleId, stepId, file)
      .pipe(finalize(() => this.busyEvidenceStepId$.next(null)))
      .subscribe({
        next: evidence => {
          const next = new Map(this.stepEvidences$.value);
          next.set(stepId, evidence);
          this.stepEvidences$.next(next);
          this.snack.open(
            $localize`:@@pdca.evidence.added:Preuve jointe à l'étape.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] uploadStepEvidence failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); return; }
          this.snack.open(this.evidenceRefusal(err), 'OK', { duration: 5000 });
        }
      });
  }

  /**
   * Le retrait passe par une confirmation : c'est le seul geste qui fait
   * disparaître une preuve d'un dossier d'audit, et il est traçable.
   */
  removeStepEvidence(stepId: string, evidence: PdcaStepEvidence): void {
    if (this.busyEvidenceStepId$.value) { return; }
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@pdca.evidence.remove-title:Retirer la preuve de cette étape ?`,
        message: $localize`:@@pdca.evidence.remove-message:La pièce sera définitivement retirée. Une étape déclarée faite sans preuve s'affirme faite sans le démontrer : ce retrait est traçable.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) { return; }
      this.busyEvidenceStepId$.next(stepId);
      this.pdca.deleteStepEvidence(this.cycleId, stepId, evidence.id)
        .pipe(finalize(() => this.busyEvidenceStepId$.next(null)))
        .subscribe({
          next: () => {
            const next = new Map(this.stepEvidences$.value);
            next.delete(stepId);
            this.stepEvidences$.next(next);
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[pdca-detail] deleteStepEvidence failed', err?.status, err?.error?.type);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@pdca.evidence.remove-error:Échec du retrait de la preuve.`),
              'OK', { duration: 4000 });
          }
        });
    });
  }

  /** Icône par famille de document : on reconnaît un PDF d'un tableur d'un coup d'œil. */
  evidenceIcon(contentType: string): string {
    if (contentType === 'application/pdf') { return 'picture_as_pdf'; }
    if (contentType.startsWith('image/')) { return 'image'; }
    if (contentType.includes('spreadsheet')) { return 'table_chart'; }
    if (contentType.includes('wordprocessing')) { return 'description'; }
    return 'attach_file';
  }

  attachEvidenceAria(title: string): string {
    return $localize`:@@pdca.evidence.attach-aria:Joindre une preuve à l'étape : ${title}:title:`;
  }

  removeEvidenceAria(title: string): string {
    return $localize`:@@pdca.evidence.remove-aria:Retirer la preuve de l'étape : ${title}:title:`;
  }

  /**
   * Traduction des refus de téléversement. Chaque refus a sa raison : les
   * confondre reviendrait à dire « non » sans dire quoi corriger.
   */
  private evidenceRefusal(err: unknown): string {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    return e?.status === 413
      ? $localize`:@@pdca.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`
      : e?.status === 400
      ? $localize`:@@pdca.evidence.rejected:Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.`
      : e?.status === 409
      ? $localize`:@@pdca.evidence.limit:Cette étape porte déjà sa preuve, ou le cycle est clos.`
      : e?.status === 404
      ? $localize`:@@pdca.evidence.gone:Cette étape n'existe plus — recharge la fiche.`
      : safeErrorMessage(err, $localize`:@@pdca.evidence.error:Échec de l'ajout de la preuve.`);
  }

  /**
   * 503 + ProblemDetail de type 'storage-disabled' → le stockage est coupé sur
   * cet environnement. Message dédié plutôt qu'une erreur brute qui ferait
   * croire à un bug.
   */
  private isStorageDisabled(err: unknown): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    const type = e?.error?.type;
    return e?.status === 503 && typeof type === 'string' && type.includes('storage-disabled');
  }

  goBack(): void {
    this.router.navigate(['/pdca']);
  }

  openAddStep(currentPhase: PdcaStatus): void {
    const defaultPhase: PdcaPhase | undefined =
      currentPhase === 'PLAN' || currentPhase === 'DO'
        || currentPhase === 'CHECK' || currentPhase === 'ACT'
        ? currentPhase
        : undefined;
    const data: PdcaStepDialogData = { cycleId: this.cycleId, defaultPhase };
    this.dialog
      .open(PdcaStepDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(step => {
        if (step) {
          this.reload$.next();
        }
      });
  }

  advance(currentStatus: PdcaStatus): void {
    if (this.acting$.value) return;
    if (currentStatus === 'COMPLETED' || currentStatus === 'CANCELLED') return;
    this.acting$.next(true);
    this.pdca
      .advanceCycle(this.cycleId)
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@pdca.detail.advanced:Cycle avancé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] advance failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@pdca.detail.advance-error:Erreur lors de l'avancement.`),
            'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    this.pdca
      .cancelCycle(this.cycleId)
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@pdca.detail.cancelled:Cycle annulé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] cancel failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@pdca.detail.cancel-error:Erreur lors de l'annulation.`),
            'OK', { duration: 4000 });
        }
      });
  }

  isTerminal(status: PdcaStatus): boolean {
    return status === 'COMPLETED' || status === 'CANCELLED';
  }

  statusBadge(status: PdcaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  stepStatusBadge(status: PdcaStepResponse['status']): string {
    return 'badge badge-' + status.toLowerCase();
  }

  phaseColor(phase: PdcaPhase): string {
    return 'phase phase-' + phase.toLowerCase();
  }
}

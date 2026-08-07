import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { CapaService } from '../../capa.service';
import {
  CapaEditDialogComponent,
  CapaEditDialogData
} from '../capa-edit-dialog/capa-edit-dialog.component';
import { CapaActionResponse, CapaActionStatus, CapaCaseResponse, CapaCriticity, CapaEvidence, CapaStatus, SuggestedAction } from '../../capa.types';
import {
  CapaActionDialogComponent,
  CapaActionDialogData
} from '../capa-action-dialog/capa-action-dialog.component';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-capa-detail',
  templateUrl: './capa-detail.component.html',
  styleUrls: ['./capa-detail.component.scss'],
  standalone: false
})
export class CapaDetailComponent implements OnInit {

  readonly actionColumns = ['title', 'status', 'dueDate', 'completedAt', 'advance'];

  readonly notFoundLabel = $localize`:@@capa.detail.not-found:Cas introuvable`;
  readonly analysingLabel = $localize`:@@capa.detail.analysing:Analyse…`;
  readonly suggestLabel = $localize`:@@capa.detail.suggest:Suggérer (IA)`;
  readonly addLabel = $localize`:@@common.add:Ajouter`;

  case$!: Observable<CapaCaseResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  // Suggestions d'actions par l'IA (non persistées tant que non ajoutées).
  suggestions: SuggestedAction[] = [];
  suggesting = false;
  addingKey: string | null = null;

  // --- preuves du dossier (§4.2, ISO 9001 §10.2) ------------------------------
  /** Bornes tenues par le serveur ; l'écran les énonce au lieu de les faire découvrir. */
  readonly maxEvidences = 10;
  readonly maxEvidenceBytes = 10 * 1024 * 1024;

  evidences$ = new BehaviorSubject<CapaEvidence[]>([]);
  evidencesLoading$ = new BehaviorSubject<boolean>(false);
  uploadingEvidence$ = new BehaviorSubject<boolean>(false);
  /** Vrai quand le serveur répond 503 : le stockage binaire est coupé sur cet environnement. */
  evidenceStorageDisabled$ = new BehaviorSubject<boolean>(false);
  /** Identifiant de la pièce en cours de retrait (désactive sa ligne). */
  removingEvidenceId$ = new BehaviorSubject<string | null>(null);

  private caseId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  // Ancré des DEUX côtés et jeu de caractères restreint : un simple préfixe
  // laissait passer « capa-1/../../secrets » (traversée de chemin — OWASP A03).
  private isMockId(s: string): boolean { return /^capa-[a-z0-9-]{1,64}$/i.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly capa: CapaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/capa']);
      return;
    }
    this.caseId = raw;
    this.case$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.capa.getCase(this.caseId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] getCase failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@capa.detail.case-not-found:Cas CAPA introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
    this.loadEvidences();
  }

  // --- preuves du dossier ------------------------------------------------------

  /** Chargement non bloquant : la fiche reste utilisable si les preuves manquent. */
  loadEvidences(): void {
    queueMicrotask(() => this.evidencesLoading$.next(true));
    this.capa.listEvidences(this.caseId)
      .pipe(finalize(() => this.evidencesLoading$.next(false)))
      .subscribe({
        next: evidences => {
          this.evidences$.next(evidences);
          this.evidenceStorageDisabled$.next(false);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] listEvidences failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); }
        }
      });
  }

  triggerEvidencePicker(input: HTMLInputElement): void {
    if (this.uploadingEvidence$.value) return;
    input.click();
  }

  onEvidenceSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    // Réinitialise pour autoriser la re-sélection du même fichier.
    input.value = '';
    if (!file) return;
    this.uploadEvidence(file);
  }

  private uploadEvidence(file: File): void {
    this.uploadingEvidence$.next(true);
    this.capa.uploadEvidence(this.caseId, file)
      .pipe(finalize(() => this.uploadingEvidence$.next(false)))
      .subscribe({
        next: evidence => {
          this.evidences$.next([...this.evidences$.value, evidence]);
          this.evidenceStorageDisabled$.next(false);
          this.snack.open(
            $localize`:@@capa.evidence.added:Preuve ajoutée au dossier.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] uploadEvidence failed', err?.status, err?.error?.type);
          if (this.isStorageDisabled(err)) { this.evidenceStorageDisabled$.next(true); return; }
          // Chaque refus a sa raison : les confondre reviendrait à dire « non »
          // sans dire quoi corriger.
          const msg =
            err?.status === 413
              ? $localize`:@@capa.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`
              : err?.status === 400
              ? $localize`:@@capa.evidence.rejected:Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.`
              : err?.status === 409
              ? $localize`:@@capa.evidence.limit:Le dossier a atteint sa limite de preuves, ou il est clôturé.`
              : safeErrorMessage(err, $localize`:@@capa.evidence.error:Échec de l'ajout de la preuve.`);
          this.snack.open(msg, 'OK', { duration: 5000 });
        }
      });
  }

  removeEvidence(evidence: CapaEvidence): void {
    if (this.removingEvidenceId$.value) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@capa.evidence.remove-title:Retirer cette preuve ?`,
        message: $localize`:@@capa.evidence.remove-message:La pièce sera définitivement retirée du dossier. Un dossier d'audit se justifie par ses preuves : ce retrait est traçable.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.removingEvidenceId$.next(evidence.id);
      this.capa.deleteEvidence(this.caseId, evidence.id)
        .pipe(finalize(() => this.removingEvidenceId$.next(null)))
        .subscribe({
          next: () => {
            this.evidences$.next(this.evidences$.value.filter(e => e.id !== evidence.id));
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-detail] deleteEvidence failed', err?.status, err?.error?.type);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@capa.evidence.remove-error:Échec du retrait de la preuve.`),
              'OK', { duration: 4000 });
          }
        });
    });
  }

  /** Le dépôt se ferme sur un dossier clos ou rejeté, comme pour les actions. */
  canAddEvidence(status: CapaStatus): boolean {
    return status !== 'CLOSED' && status !== 'REJECTED'
        && this.evidences$.value.length < this.maxEvidences;
  }

  /** Compteur affiché : l'utilisateur voit la borne approcher au lieu de la heurter. */
  evidenceCountLabel(): string {
    return this.evidences$.value.length + ' / ' + this.maxEvidences;
  }

  /** Poids lisible — les octets ne disent rien à personne. */
  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1).replace('.', ',') + ' Mo';
  }

  /** Icône par famille de document : on reconnaît un PDF d'un tableur d'un coup d'œil. */
  evidenceIcon(contentType: string): string {
    if (contentType === 'application/pdf') return 'picture_as_pdf';
    if (contentType.startsWith('image/')) return 'image';
    if (contentType.includes('spreadsheet')) return 'table_chart';
    if (contentType.includes('wordprocessing')) return 'description';
    return 'attach_file';
  }

  /**
   * 503 + ProblemDetail de type 'storage-disabled' → le stockage est coupé sur cet
   * environnement. Message dédié plutôt qu'une erreur brute qui ferait croire à un bug.
   */
  private isStorageDisabled(err: unknown): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    const type = e?.error?.type;
    return e?.status === 503 && typeof type === 'string' && type.includes('storage-disabled');
  }

  goBack(): void {
    this.router.navigate(['/capa']);
  }

  openEdit(c: CapaCaseResponse): void {
    const data: CapaEditDialogData = { capa: c };
    this.dialog
      .open(CapaEditDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => { if (updated) this.reload$.next(); });
  }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deleteCase(title: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@capa.detail.delete-title:Supprimer ce cas CAPA ?`,
        message: $localize`:@@capa.detail.delete-message:« ${title}:title: » et toutes ses actions seront supprimés définitivement.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.capa.deleteCase(this.caseId).subscribe({
        next: () => {
          this.snack.open($localize`:@@capa.detail.deleted:Cas supprimé.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.router.navigate(['/capa']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
    });
  }

  openAddAction(): void {
    const data: CapaActionDialogData = { caseId: this.caseId };
    this.dialog
      .open(CapaActionDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(action => {
        if (action) this.reload$.next();
      });
  }

  // ---- Suggestion d'actions par l'IA (§4.2) ----

  suggestActions(): void {
    this.suggesting = true;
    this.suggestions = [];
    this.capa.suggestActions(this.caseId).subscribe({
      next: list => {
        this.suggestions = list;
        this.suggesting = false;
        if (!list.length) {
          this.snack.open($localize`:@@capa.detail.no-suggestion:Aucune action exploitable — précisez le problème.`, $localize`:@@common.ok:OK`, { duration: 3000 });
        }
      },
      error: err => {
        this.suggesting = false;
        this.snack.open(
          safeErrorMessage(err, $localize`:@@capa.detail.suggestion-unavailable:Suggestion IA indisponible (ai-service / Ollama).`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
  }

  // ---- Avancement du statut des actions (ANO-011, §4.2 / ISO 9001 §10.2) ----

  private static readonly ACTION_FLOW: Record<CapaActionStatus, CapaActionStatus | null> = {
    PENDING: 'IN_PROGRESS',
    IN_PROGRESS: 'DONE',
    DONE: null
  };

  /** Statut suivant d'une action (null si déjà DONE). */
  nextActionStatus(s: CapaActionStatus): CapaActionStatus | null {
    return CapaDetailComponent.ACTION_FLOW[s];
  }

  /** Une action est avançable si elle n'est pas DONE et que la CAPA n'est pas terminale. */
  canAdvanceAction(a: CapaActionResponse, caseStatus: CapaStatus): boolean {
    return a.status !== 'DONE' && !this.isTerminal(caseStatus) && !this.acting$.value;
  }

  /** Libellé du bouton selon le prochain statut. */
  advanceActionLabel(s: CapaActionStatus): string {
    return s === 'PENDING'
      ? $localize`:@@capa.detail.action-start:Démarrer`
      : $localize`:@@capa.detail.action-complete:Terminer`;
  }

  /** Fait avancer une action vers son statut suivant (le titre est renvoyé — requis backend). */
  advanceAction(a: CapaActionResponse): void {
    const next = this.nextActionStatus(a.status);
    if (!next || this.acting$.value) {
      return;
    }
    this.acting$.next(true);
    this.capa.updateAction(this.caseId, a.id, { title: a.title, status: next })
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@capa.detail.action-advanced:Action mise à jour.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] advanceAction failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@capa.detail.action-advance-error:Mise à jour de l'action impossible.`),
            $localize`:@@common.close:Fermer`, { duration: 4000 });
        }
      });
  }

  addSuggestion(s: SuggestedAction): void {
    this.addingKey = s.title;
    this.capa.addAction(this.caseId, { title: s.title, description: s.description }).subscribe({
      next: () => {
        this.addingKey = null;
        this.suggestions = this.suggestions.filter(x => x !== s);
        this.snack.open($localize`:@@capa.detail.action-added:Action ajoutée à la CAPA.`, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        this.addingKey = null;
        this.snack.open(safeErrorMessage(err, $localize`:@@common.add-failed:Ajout impossible.`), $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }

  dismissSuggestions(): void { this.suggestions = []; }

  start(): void { this.transition('start'); }
  resolve(): void { this.transition('resolve'); }
  reject(): void { this.transition('reject'); }

  /**
   * ISO 9001 §10.2 — once a CAPA is RESOLVED, the auditor must verify that
   * the action was actually effective at the planned horizon (typically
   * 3 / 6 / 12 months). Only allowed in RESOLVED state; transitions to
   * CLOSED on positive verification.
   */
  verifyEffectiveness(effective: boolean): void {
    if (this.acting$.value) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: effective
          ? $localize`:@@capa.detail.confirm-effective-title:Confirmer efficacité ?`
          : $localize`:@@capa.detail.confirm-not-effective-title:Confirmer non-efficacité ?`,
        message: effective
          ? $localize`:@@capa.detail.confirm-effective-message:Tu confirmes que les actions ont eu l'effet attendu. Le cas sera clôturé (CLOSED).`
          : $localize`:@@capa.detail.confirm-not-effective-message:Tu signales que les actions n'ont pas été efficaces. Le cas reste RESOLVED — il faudra rouvrir ou créer un nouveau CAPA.`,
        confirmLabel: effective
          ? $localize`:@@capa.detail.yes-effective:Oui, efficace`
          : $localize`:@@capa.detail.yes-not-effective:Oui, non efficace`,
        destructive: !effective
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.acting$.next(true);
      this.capa.verifyEffectiveness(this.caseId, effective)
        .pipe(finalize(() => this.acting$.next(false)))
        .subscribe({
          next: () => {
            this.snack.open(
              effective
                ? $localize`:@@capa.detail.effectiveness-validated:Efficacité validée — cas clôturé.`
                : $localize`:@@capa.detail.not-effectiveness-recorded:Non-efficacité enregistrée.`,
              $localize`:@@common.ok:OK`, { duration: 2500 }
            );
            this.reload$.next();
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-detail] effectiveness failed', err?.status, err?.error?.title);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@capa.detail.verification-error:Erreur lors de la vérification.`),
              $localize`:@@common.ok:OK`, { duration: 4000 }
            );
          }
        });
    });
  }

  canVerifyEffectiveness(s: CapaStatus): boolean {
    return s === 'RESOLVED';
  }

  private transition(action: 'start' | 'resolve' | 'reject'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start' ? this.capa.startCase(this.caseId)
      : action === 'resolve' ? this.capa.resolveCase(this.caseId)
      : this.capa.rejectCase(this.caseId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const msg = action === 'start'
          ? $localize`:@@capa.detail.case-started:Cas démarré.`
          : action === 'resolve'
            ? $localize`:@@capa.detail.case-resolved:Cas résolu.`
            : $localize`:@@capa.detail.case-rejected:Cas rejeté.`;
        this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@capa.detail.transition-error:Erreur lors de la transition.`),
          $localize`:@@common.ok:OK`, { duration: 4000 }
        );
      }
    });
  }

  canStart(s: CapaStatus): boolean { return s === 'OPEN'; }
  canResolve(s: CapaStatus): boolean { return s === 'IN_PROGRESS'; }
  canReject(s: CapaStatus): boolean { return s === 'OPEN' || s === 'IN_PROGRESS'; }
  isTerminal(s: CapaStatus): boolean { return s === 'CLOSED' || s === 'REJECTED'; }

  effectivenessLabel(verified: boolean): string {
    return verified
      ? $localize`:@@capa.detail.effectiveness-verified:Vérifiée ✓`
      : $localize`:@@capa.detail.effectiveness-not-effective:Non efficace ✗`;
  }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
  actionBadge(s: 'PENDING' | 'IN_PROGRESS' | 'DONE'): string { return 'badge badge-' + s.toLowerCase(); }
}

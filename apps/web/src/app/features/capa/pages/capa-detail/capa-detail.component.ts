import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { AccountNames } from '../../../../core/directory/directory-names';
import { DirectoryService } from '../../../../core/directory/directory.service';
import {
  capaCriticityLabel, capaOwnerLabel, capaSourceLabel, unnamedOwners
} from '../../capa.labels';
import { CapaService } from '../../capa.service';
import {
  CapaEditDialogComponent,
  CapaEditDialogData
} from '../capa-edit-dialog/capa-edit-dialog.component';
import {
  CapaActionResponse, CapaActionStatus, CapaCaseResponse, CapaCriticity,
  CapaEvidence, CapaStatus, ClosureBlocker
} from '../../capa.types';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-capa-detail',
  templateUrl: './capa-detail.component.html',
  styleUrls: ['./capa-detail.component.scss'],
  standalone: false
})
export class CapaDetailComponent implements OnInit {


  readonly notFoundLabel = $localize`:@@capa.detail.not-found:Cas introuvable`;

  /** Le nom du pilote d'un dossier ancien qui ne l'a pas recopié, lu dans l'annuaire (ADR 0091). */
  private readonly names = new AccountNames(this.directory);

  case$!: Observable<CapaCaseResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

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
    private readonly snack: MatSnackBar,
    private readonly directory: DirectoryService
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
      tap(c => { if (c) this.names.resolve(unnamedOwners([c])); }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
    // Seules les preuves DU DOSSIER se chargent ici. Celles des actions sont
    // lues par le tableau, qui les montre et les gere (ADR 0117).
    this.loadEvidences();
  }

  /** Le nom du pilote, jamais son identifiant (ADR 0088). */
  ownerLabel(c: CapaCaseResponse): string {
    return capaOwnerLabel(c, id => this.names.get(id));
  }

  sourceLabel(c: CapaCaseResponse): string {
    return capaSourceLabel(c.sourceType);
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
          this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 5000 });
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
              $localize`:@@common.ok:OK`, { duration: 4000 });
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

  /**
   * Traduction des refus de téléversement, partagée par les deux niveaux.
   * Chaque refus a sa raison : les confondre reviendrait à dire « non » sans
   * dire quoi corriger.
   */
  private evidenceRefusal(err: unknown): string {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const e = err as any;
    return e?.status === 413
      ? $localize`:@@capa.evidence.too-large:Pièce trop lourde — 10 Mo au maximum.`
      : e?.status === 400
      ? $localize`:@@capa.evidence.rejected:Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.`
      : e?.status === 409
      ? $localize`:@@capa.action-evidence.limit:Cette action porte déjà sa preuve, ou le dossier est clôturé.`
      : e?.status === 404
      ? $localize`:@@capa.action-evidence.gone:Cette action n'existe plus — recharge la fiche.`
      : safeErrorMessage(err, $localize`:@@capa.evidence.error:Échec de l'ajout de la preuve.`);
  }

  /**
   * Relit le dossier après qu'une action a bougé dans le tableau.
   *
   * <p>Le statut du dossier se déduit de ses actions (ADR 0117) : terminer la
   * dernière le résout, et l'en-tête doit le dire tout de suite. Sans cette
   * relecture, la fiche afficherait « en cours » au-dessus d'un tableau dont
   * toutes les lignes sont faites.
   */
  reloadCase(): void {
    this.reload$.next();
  }

  // ---------------------------------------------------------------------------
  // L'AVANCEMENT DU DOSSIER — et non une liste d'actions.
  //
  // Une CAPA EST une action : elle s'ouvre avec la sienne, dont l'intitulé, la
  // nature, le responsable et l'échéance sont ceux du dossier (ADR 0088). La
  // fiche montrait pourtant un tableau d'actions dont l'unique ligne répétait
  // mot pour mot l'en-tête au-dessus d'elle : une liste d'actions à l'intérieur
  // d'une action.
  //
  // Le tableau est parti. Ce qu'il portait d'irremplaçable — faire avancer le
  // travail, puisque le statut du dossier s'en déduit (ADR 0117) — est ici, sur
  // le dossier lui-même. Sans quoi un dossier ouvert hors d'un écart resterait
  // ouvert pour toujours : plus rien n'aurait pu faire avancer son action.
  // ---------------------------------------------------------------------------

  /**
   * L'action du dossier, quand il en porte exactement une.
   *
   * <p>{@code null} dans les deux autres cas, qui ne sont pas la même chose et
   * que la fiche distingue : aucune action (un dossier ouvert par l'ancien
   * point d'entrée, antérieur à la règle « une CAPA naît avec son action ») et
   * plusieurs (des actions ajoutées depuis la fiche d'un écart, ADR 0115).
   * Dans aucun des deux la fiche ne peut parler de « l'action » au singulier.
   */
  uniqueAction(c: CapaCaseResponse): CapaActionResponse | null {
    return c.actions?.length === 1 ? c.actions[0] : null;
  }

  /** Combien d'actions le dossier porte — pour le dire sans les énumérer. */
  actionCount(c: CapaCaseResponse): number {
    return c.actions?.length ?? 0;
  }

  canAdvance(c: CapaCaseResponse): boolean {
    const action = this.uniqueAction(c);
    return !!action && action.status !== 'DONE'
      && !this.isTerminal(c.status) && !this.acting$.value;
  }

  advanceLabel(c: CapaCaseResponse): string {
    return this.uniqueAction(c)?.status === 'PENDING'
      ? $localize`:@@capa.detail.action-start:Démarrer`
      : $localize`:@@capa.detail.action-complete:Terminer`;
  }

  actionStatusLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.action-status.pending:À faire`
      : status === 'IN_PROGRESS'
        ? $localize`:@@capa.action-status.in-progress:En cours`
        : $localize`:@@capa.action-status.done:Faite`;
  }

  actionBadge(status: CapaActionStatus): string {
    return 'badge badge-' + status.toLowerCase().replace('_', '-');
  }

  /**
   * Fait avancer le travail du dossier d'un cran : à faire → en cours → faite.
   *
   * <p>Seul le statut part. Renvoyer l'intitulé le réécrirait à l'identique et
   * écraserait une correction faite entre-temps par quelqu'un d'autre.
   */
  advance(c: CapaCaseResponse): void {
    const action = this.uniqueAction(c);
    if (!action || !this.canAdvance(c)) {
      return;
    }
    const suivant: CapaActionStatus = action.status === 'PENDING' ? 'IN_PROGRESS' : 'DONE';
    this.acting$.next(true);
    this.capa.updateAction(c.id, action.id, { status: suivant }).pipe(
      finalize(() => this.acting$.next(false))
    ).subscribe({
      next: () => {
        this.snack.open($localize`:@@capa.detail.action-advanced:Action mise à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reloadCase();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[capa-detail] advance failed',
          (err as { status?: number })?.status);
        this.snack.open(
          safeErrorMessage(err,
            $localize`:@@capa.detail.action-advance-error:Mise à jour de l'action impossible.`),
          $localize`:@@common.close:Fermer`, { duration: 4000 });
      }
    });
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

  reject(): void { this.rejeter(); }

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

  /**
   * Phrase du motif, construite ICI et non reçue du serveur : elle doit se dire
   * dans la langue de l'utilisateur, et un message serveur ne se traduit pas.
   */
  blockerLabel(b: ClosureBlocker): string {
    switch (b.code) {
      case 'NO_ACTION':
        return $localize`:@@capa.blocker.no-action:Aucune action n'est enregistrée : il n'y a rien dont vérifier l'efficacité.`;
      case 'ACTIONS_NOT_DONE':
        return b.count === 1
          ? $localize`:@@capa.blocker.actions-one:1 action reste à terminer.`
          : $localize`:@@capa.blocker.actions-many:${b.count}:count: actions restent à terminer.`;
      case 'CONTAINMENT_ONLY':
        // Seul un dossier antérieur peut en arriver là : une CAPA s'ouvre
        // désormais corrective ou préventive, et ne reçoit pas d'autre action.
        return $localize`:@@capa.blocker.containment-only-single:Le dossier ne porte qu'une mesure d'endiguement : elle arrête l'effet sans supprimer la cause. Ouvrez une CAPA corrective pour traiter la cause.`;
      case 'OPEN_NON_CONFORMITIES':
        return b.count === 1
          ? $localize`:@@capa.blocker.nc-one:1 non-conformité liée est encore ouverte.`
          : $localize`:@@capa.blocker.nc-many:${b.count}:count: non-conformités liées sont encore ouvertes.`;
      default:
        // Code inconnu (serveur plus récent que l'écran) : mieux vaut une phrase
        // générique qu'un bouton actif dont on ignore pourquoi il échouera.
        return $localize`:@@capa.blocker.unknown:Un prérequis de clôture n'est pas satisfait.`;
    }
  }

  /** Vrai dès qu'un motif subsiste — c'est ce qui éteint le bouton de clôture. */
  blocksClosure(c: CapaCaseResponse): boolean {
    return (c.closureBlockers?.length ?? 0) > 0;
  }

  /**
   * Infobulle du bouton éteint. Vide quand rien ne bloque : une infobulle qui
   * répète le libellé du bouton n'apprend rien et gêne le survol.
   */
  closureTooltip(c: CapaCaseResponse): string {
    return this.blocksClosure(c)
      ? (c.closureBlockers ?? []).map(b => this.blockerLabel(b)).join(' ')
      : '';
  }

  /**
   * L'abandon du dossier — la seule transition qui reste un geste.
   *
   * <p>« Démarrer » et « Résoudre » l'accompagnaient, et ne décidaient rien : le
   * serveur refusait déjà de résoudre tant qu'une action restait à faire, si
   * bien qu'on demandait à un humain de recopier une conclusion déjà tirée.
   * Le statut du dossier se déduit maintenant de ses actions (ADR 0117) ;
   * renoncer, lui, ne se déduit de rien.
   */
  private rejeter(): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    this.capa.rejectCase(this.caseId)
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: () => {
          this.snack.open(
            $localize`:@@capa.detail.case-rejected:Cas rejeté.`,
            $localize`:@@common.ok:OK`, { duration: 2000 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-detail] reject failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@capa.detail.transition-error:Erreur lors de la transition.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  canReject(s: CapaStatus): boolean { return s === 'OPEN' || s === 'IN_PROGRESS'; }
  isTerminal(s: CapaStatus): boolean { return s === 'CLOSED' || s === 'REJECTED'; }

  effectivenessLabel(verified: boolean): string {
    return verified
      ? $localize`:@@capa.detail.effectiveness-verified:Vérifiée ✓`
      : $localize`:@@capa.detail.effectiveness-not-effective:Non efficace ✗`;
  }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
  readonly criticityLabel = capaCriticityLabel;
}

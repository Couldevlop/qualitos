import { Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, QueryList, ViewChildren } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { combineLatest, of, Subscription } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { roleLabel } from '../../../../core/authz/authz.labels';
import { AuthzService } from '../../../../core/authz/authz.service';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitDecision, CircuitRun, CircuitStep } from '../../../../core/circuits/circuit.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SIGNATORY_DECISION_LABELS } from '../../documents.labels';
import { DocumentsService } from '../../documents.service';
import { DocumentSignatory, DocumentVersionResponse } from '../../documents.types';

export type StepState = 'done' | 'current' | 'todo' | 'rejected';

/**
 * Où en est une version dans son circuit de validation.
 *
 * <p>Depuis l'ADR 0099, une version part à la signature de 1 à 3 personnes
 * choisies à la soumission : une carte par signataire, comme la validation
 * d'un MOC. Seul le signataire dont c'est le tour voit Approuver / Rejeter ;
 * le rejet demande un motif et renvoie la version en brouillon.
 *
 * <p>Une revue d'avant l'ADR 0099 garde son circuit par rôles (ADR 0080) :
 * <p>Pour une version en revue : les étapes, celles franchies, celle en cours
 * avec ses approbations, et à qui revient la décision — à l'utilisateur, s'il
 * porte le rôle de l'étape et n'a pas encore décidé. Pour une version revenue
 * en brouillon : la raison du dernier refus.
 */
@Component({
  selector: 'qos-document-circuit',
  templateUrl: './document-circuit.component.html',
  styleUrls: ['./document-circuit.component.scss'],
  standalone: false
})
export class DocumentCircuitComponent implements OnChanges, OnDestroy {

  @Input({ required: true }) version!: DocumentVersionResponse;
  /** Une signature ou un rejet enregistré : la fiche se relit. */
  @Output() readonly changed = new EventEmitter<DocumentVersionResponse>();

  readonly commentMax = 1000;
  /** Le commentaire de la signature, ou le motif du rejet. */
  comment = '';
  /** Le signataire a choisi de rejeter : le champ du motif est ouvert. */
  rejecting = false;
  /** Rejet confirmé sans motif : le champ le réclame. */
  reasonAsked = false;
  busy = false;
  @ViewChildren('motif') private motifs?: QueryList<ElementRef<HTMLTextAreaElement>>;

  run: CircuitRun | null = null;
  /** Vrai une fois la lecture faite : avant, on n'affirme rien. */
  loaded = false;
  failed = false;
  private myRoles: string[] = [];
  private sub?: Subscription;

  constructor(
    private readonly circuits: CircuitService,
    private readonly authz: AuthzService,
    private readonly auth: AuthService,
    private readonly documents: DocumentsService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnChanges(): void {
    this.sub?.unsubscribe();
    this.run = null;
    this.loaded = false;
    this.failed = false;
    this.cancelReject();
    if (this.version?.status !== 'IN_REVIEW') return;
    // Des signataires : tout est dans la version, rien d'autre à lire.
    if (this.signed) {
      this.loaded = true;
      return;
    }
    this.sub = combineLatest([
      this.circuits.run('document-version', this.version.id).pipe(
        catchError(() => {
          this.failed = true;
          return of(null);
        })),
      this.authz.me().pipe(map(me => me.roles))
    ]).subscribe(([run, roles]) => {
      // Un passage terminé appartient à une soumission précédente : la revue en cours n'en a pas.
      this.run = run?.status === 'IN_PROGRESS' ? run : null;
      this.myRoles = roles;
      this.loaded = true;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get inReview(): boolean {
    return this.version?.status === 'IN_REVIEW';
  }

  get rejected(): boolean {
    return this.version?.status === 'DRAFT' && !!this.version.rejectionReason;
  }

  state(i: number): StepState {
    const r = this.run!;
    if (r.status === 'REJECTED' && i === r.currentStep) return 'rejected';
    if (r.status === 'APPROVED' || i < r.currentStep) return 'done';
    return i === r.currentStep ? 'current' : 'todo';
  }

  approvalsAt(i: number): number {
    return this.run?.decisions.filter(d => d.stepIndex === i && d.approved).length ?? 0;
  }

  decisionsAt(i: number): CircuitDecision[] {
    return this.run?.decisions.filter(d => d.stepIndex === i) ?? [];
  }

  get current(): CircuitStep | null {
    return this.run ? this.run.steps[this.run.currentStep] ?? null : null;
  }

  private get me(): string | undefined {
    return this.auth.snapshot()?.userId;
  }

  mine(d: CircuitDecision): boolean {
    return !!this.me && d.actorId === this.me;
  }

  /** Ce que l'utilisateur doit savoir de son rôle dans la revue en cours. */
  get turn(): 'author' | 'decided' | 'yours' | 'waiting' | null {
    if (!this.run || !this.current) return null;
    const me = this.me;
    if (me && me === this.version.authorId) return 'author';
    if (me && this.run.decisions.some(d => d.actorId === me)) return 'decided';
    return this.myRoles.includes(this.current.roleCode) ? 'yours' : 'waiting';
  }

  // ---------- circuit de signature (ADR 0099) ----------

  get signatories(): DocumentSignatory[] {
    return this.version?.signatories ?? [];
  }

  get signed(): boolean {
    return this.signatories.length > 0;
  }

  /** Le signataire attendu : le premier en attente, tant que la version est en revue. */
  get awaited(): DocumentSignatory | null {
    return this.inReview ? this.signatories.find(s => s.decision === 'PENDING') ?? null : null;
  }

  /** Ce que l'utilisateur doit savoir de sa place dans la signature. */
  get signTurn(): 'yours' | 'author' | 'waiting' | null {
    const next = this.awaited;
    if (!next) return null;
    if (next.userId === this.me) return 'yours';
    return this.me === this.version.authorId ? 'author' : 'waiting';
  }

  canSign(s: DocumentSignatory): boolean {
    return this.signTurn === 'yours' && this.awaited === s;
  }

  isAwaited(s: DocumentSignatory): boolean {
    return this.awaited === s;
  }

  decisionText(s: DocumentSignatory): string {
    return SIGNATORY_DECISION_LABELS[s.decision];
  }

  signatoryLabel(s: DocumentSignatory): string {
    return $localize`:@@documents.signatory.label:Signataire ${s.position}:position:`;
  }

  /** Vrai une fois le rejet confirmé sans motif, tant que le motif reste vide. */
  get missingReason(): boolean {
    return this.reasonAsked && !this.comment.trim();
  }

  approve(): void {
    this.sign('APPROVED');
  }

  /** Ouvre le champ du motif et y place le curseur, une fois rendu. */
  startReject(): void {
    this.rejecting = true;
    setTimeout(() => this.motifs?.first?.nativeElement.focus());
  }

  /** Revient aux deux choix ; le motif saisi est oublié. */
  cancelReject(): void {
    this.rejecting = false;
    this.reasonAsked = false;
    this.comment = '';
  }

  reject(champ?: HTMLTextAreaElement): void {
    if (!this.comment.trim()) {
      this.reasonAsked = true;
      champ?.focus();
      return;
    }
    this.sign('REJECTED');
  }

  private sign(decision: 'APPROVED' | 'REJECTED'): void {
    const me = this.me;
    if (this.busy || !me) return;
    this.busy = true;
    const v = this.version;
    const texte = this.comment.trim();
    const call = decision === 'APPROVED'
      ? this.documents.approve(v.documentId, v.id, { approverId: me, ...(texte ? { comment: texte } : {}) })
      : this.documents.reject(v.documentId, v.id, texte);
    call.subscribe({
      next: r => {
        this.busy = false;
        this.cancelReject();
        this.snack.open(decision === 'REJECTED'
          ? $localize`:@@documents.signature.rejected:Rejet enregistré : la version revient en brouillon chez son auteur.`
          : r.status === 'APPROVED'
            ? $localize`:@@documents.detail.approved:Version approuvée.`
            : $localize`:@@documents.signature.signed:Signature enregistrée : le signataire suivant est invité à signer.`,
        $localize`:@@common.ok:OK`, { duration: 3000 });
        this.changed.emit(r);
      },
      error: err => {
        this.busy = false;
        this.snack.open(safeErrorMessage(err, $localize`:@@documents.signature.failed:La signature n'a pas été enregistrée.`),
          $localize`:@@common.ok:OK`, { duration: 5000 });
      }
    });
  }

  trackBySignatory(_i: number, s: DocumentSignatory): number {
    return s.position;
  }

  roleText(code: string): string {
    return roleLabel(code);
  }

  trackByIndex(i: number): number {
    return i;
  }
}

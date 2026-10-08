import { Component, Input, OnChanges, OnDestroy } from '@angular/core';
import { combineLatest, of, Subscription } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { roleLabel } from '../../../../core/authz/authz.labels';
import { AuthzService } from '../../../../core/authz/authz.service';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitDecision, CircuitRun, CircuitStep } from '../../../../core/circuits/circuit.types';
import { DocumentVersionResponse } from '../../documents.types';

export type StepState = 'done' | 'current' | 'todo' | 'rejected';

/**
 * Où en est une version dans son circuit de validation (ADR 0080).
 *
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

  run: CircuitRun | null = null;
  /** Vrai une fois la lecture faite : avant, on n'affirme rien. */
  loaded = false;
  failed = false;
  private myRoles: string[] = [];
  private sub?: Subscription;

  constructor(
    private readonly circuits: CircuitService,
    private readonly authz: AuthzService,
    private readonly auth: AuthService
  ) {}

  ngOnChanges(): void {
    this.sub?.unsubscribe();
    this.run = null;
    this.loaded = false;
    this.failed = false;
    if (this.version?.status !== 'IN_REVIEW') return;
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

  roleText(code: string): string {
    return roleLabel(code);
  }

  trackByIndex(i: number): number {
    return i;
  }
}

import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { Observable, Subscription, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { roleLabel } from '../../../../core/authz/authz.labels';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitRun, CircuitStep } from '../../../../core/circuits/circuit.types';
import { DocumentResponse, DocumentVersionResponse } from '../../documents.types';

export type RowState = 'done' | 'current' | 'todo' | 'rejected';

/** Un temps du parcours d'une version : sa rédaction, une étape de validation, sa diffusion. */
export interface ValidationRow {
  kind: 'draft' | 'step' | 'publish';
  label: string;
  /** Le rôle qui tient l'étape ; absent en approbation simple. */
  roleCode: string | null;
  state: RowState;
  /** Les personnes qui ont fait ce temps : l'auteur, les approbateurs. */
  actors: string[];
  at: string | null;
  /** Où en est une étape à plusieurs approbations. */
  approvals: { done: number; needed: number } | null;
}

/**
 * Ce que l'on sait du circuit d'une version : son passage, s'il en a un, ou
 * pour un brouillon les étapes qu'il suivra une fois soumis.
 */
export interface CircuitFacts {
  run: CircuitRun | null;
  plan: CircuitStep[] | null;
}

const LABEL_DRAFT = $localize`:@@documents.validation.draft:Rédaction`;
const LABEL_SIMPLE = $localize`:@@documents.validation.simple:Approbation`;
const LABEL_PUBLISH = $localize`:@@documents.validation.publish:Diffusion et prise de connaissance`;

/**
 * Le circuit de validation d'une version dans la fiche GED : la rédaction, les
 * étapes réglées par l'administrateur (ADR 0080) avec qui a validé et quand,
 * puis la diffusion et le suivi de la prise de connaissance.
 *
 * <p>Pour une version en revue ou validée, les étapes sont celles de son
 * passage, figées à la soumission ; pour un brouillon, celles qu'il suivra.
 * Sans circuit, une seule approbation suffit et le parcours le dit.
 */
@Component({
  selector: 'qos-document-validation',
  templateUrl: './document-validation.component.html',
  styleUrls: ['./document-validation.component.scss'],
  standalone: false
})
export class DocumentValidationComponent implements OnChanges, OnDestroy {

  @Input({ required: true }) document!: DocumentResponse;
  @Input({ required: true }) version!: DocumentVersionResponse;
  /** Lecteurs ayant confirmé la version en vigueur ; `null` tant qu'on ne le sait pas. */
  @Input() acknowledgments: number | null = null;

  rows: ValidationRow[] = [];
  loaded = false;
  private facts: CircuitFacts = { run: null, plan: null };
  private sub?: Subscription;

  constructor(private readonly circuits: CircuitService) {}

  ngOnChanges(changes: SimpleChanges): void {
    const before: DocumentVersionResponse | undefined = changes['version']?.previousValue;
    const relire = !!changes['version']
      && (before?.id !== this.version?.id || before?.status !== this.version?.status);
    if (relire) {
      this.load();
    } else {
      this.rows = validationRows(this.version, this.facts);
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  /** La diffusion d'une version à lecture obligatoire suit qui l'a lue. */
  get tracksReading(): boolean {
    return this.document.mandatoryRead && this.version.id === this.document.currentVersionId;
  }

  get readers(): number {
    return this.acknowledgments ?? 0;
  }

  roleText(code: string): string {
    return roleLabel(code);
  }

  trackByIndex(i: number): number {
    return i;
  }

  private load(): void {
    this.sub?.unsubscribe();
    this.loaded = false;
    this.facts = { run: null, plan: null };
    this.rows = validationRows(this.version, this.facts);
    this.sub = this.read().subscribe(facts => {
      this.facts = facts;
      this.rows = validationRows(this.version, facts);
      this.loaded = true;
    });
  }

  /** Un brouillon montre le circuit qu'il suivra ; une version soumise, son passage. */
  private read(): Observable<CircuitFacts> {
    if (this.version.status === 'DRAFT') {
      return this.circuits.circuit('document-version').pipe(
        map(c => ({ run: null, plan: c.enabled !== false && c.steps.length ? c.steps : null })),
        catchError(() => of({ run: null, plan: null })));
    }
    return this.circuits.run('document-version', this.version.id).pipe(
      map(run => ({ run, plan: null })),
      catchError(() => of({ run: null, plan: null })));
  }
}

/** Le parcours d'une version, du brouillon à sa diffusion. */
export function validationRows(v: DocumentVersionResponse, facts: CircuitFacts): ValidationRow[] {
  const submitted = v.status !== 'DRAFT';
  const rows: ValidationRow[] = [{
    kind: 'draft', label: LABEL_DRAFT, roleCode: null,
    state: submitted ? 'done' : 'current',
    actors: [v.authorId], at: v.createdAt, approvals: null
  }];
  rows.push(...stepRows(v, facts));

  const published = v.status === 'PUBLISHED' || v.status === 'OBSOLETE';
  rows.push({
    kind: 'publish', label: LABEL_PUBLISH, roleCode: null,
    state: published ? 'done' : v.status === 'APPROVED' ? 'current' : 'todo',
    actors: [], at: published ? v.publishedAt ?? null : null, approvals: null
  });
  return rows;
}

function stepRows(v: DocumentVersionResponse, { run, plan }: CircuitFacts): ValidationRow[] {
  // Le passage qui compte : celui en cours pendant la revue, celui qui a abouti ensuite.
  const own = run && (v.status === 'IN_REVIEW' ? run.status === 'IN_PROGRESS'
    : v.status !== 'DRAFT' && run.status === 'APPROVED') ? run : null;

  if (own) {
    return own.steps.map((s, i) => {
      const approvals = own.decisions.filter(d => d.stepIndex === i && d.approved);
      const last = approvals[approvals.length - 1];
      return {
        kind: 'step', label: s.name, roleCode: s.roleCode,
        state: own.status === 'APPROVED' || i < own.currentStep ? 'done'
          : i === own.currentStep ? 'current' : 'todo',
        actors: approvals.map(d => d.actorId), at: last?.at ?? null,
        approvals: { done: approvals.length, needed: s.minApprovals }
      } as ValidationRow;
    });
  }
  if (v.status === 'DRAFT' && plan) {
    return plan.map(s => ({
      kind: 'step', label: s.name, roleCode: s.roleCode, state: 'todo', actors: [], at: null,
      approvals: { done: 0, needed: s.minApprovals }
    } as ValidationRow));
  }
  // Approbation simple : une personne ayant le droit d'approuver suffit.
  const approved = v.status !== 'DRAFT' && v.status !== 'IN_REVIEW';
  return [{
    kind: 'step', label: LABEL_SIMPLE, roleCode: null,
    state: approved ? 'done' : v.status === 'IN_REVIEW' ? 'current' : 'todo',
    actors: approved && v.approvedBy ? [v.approvedBy] : [], at: approved ? v.approvedAt ?? null : null,
    approvals: null
  }];
}

import { SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitRun } from '../../../../core/circuits/circuit.types';
import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { PersonNameModule } from '../../../../shared/person-name/person-name.module';
import { SharedModule } from '../../../../shared/shared.module';
import { DocumentResponse, DocumentVersionResponse } from '../../documents.types';
import { DocumentValidationComponent, validationRows } from './document-validation.component';

const version = (o: Partial<DocumentVersionResponse> = {}): DocumentVersionResponse => ({
  id: 'v1', documentId: 'd1', versionNumber: 2, status: 'IN_REVIEW', authorId: 'autrice',
  createdAt: '2026-10-01T08:00:00Z', updatedAt: '2026-10-01T08:00:00Z', ...o
});

const STEPS = [
  { name: 'Validateur 1', roleCode: 'QUALITY_MANAGER', minApprovals: 2 },
  { name: 'Validateur 2', roleCode: 'QUALITY_DIRECTOR', minApprovals: 1 }
];

const passage = (o: Partial<CircuitRun> = {}): CircuitRun => ({
  id: 'r1', subject: 'document-version', subjectId: 'v1', status: 'IN_PROGRESS', currentStep: 0,
  steps: STEPS,
  decisions: [{ stepIndex: 0, actorId: 'alice', approved: true, comment: null, at: '2026-10-02T09:00:00Z' }],
  startedAt: '2026-10-02T08:00:00Z', endedAt: null, ...o
});

const states = (rows: { state: string }[]) => rows.map(r => r.state);

describe('validationRows', () => {

  it('un brouillon annonce la signature : ses signataires se choisiront à la soumission', () => {
    const rows = validationRows(version({ status: 'DRAFT' }), { run: null });
    expect(rows.map(r => r.label)).toEqual(['Rédaction', 'Signature', 'Diffusion et prise de connaissance']);
    expect(states(rows)).toEqual(['current', 'todo', 'todo']);
    expect(rows[0].actors).toEqual(['autrice']);
    expect(rows[1].note).toContain('choisis à la soumission');
  });

  it('avec des signataires : une ligne chacun, dans l’ordre, l’attendu en cours', () => {
    const sig = (position: number, userId: string, decision: 'PENDING' | 'APPROVED' | 'REJECTED', decidedAt: string | null = null) =>
      ({ position, userId, name: userId, decision, decidedAt, comment: null });
    const rows = validationRows(version({ signatories: [
      sig(1, 'alice', 'APPROVED', '2026-10-02T09:00:00Z'), sig(2, 'bob', 'PENDING'), sig(3, 'chloe', 'PENDING')] }),
      { run: passage() });
    expect(rows.map(r => r.label)).toEqual(['Rédaction', 'Signataire 1', 'Signataire 2', 'Signataire 3',
      'Diffusion et prise de connaissance']);
    expect(states(rows)).toEqual(['done', 'done', 'current', 'todo', 'todo']);
    expect(rows[1].actors).toEqual(['alice']);
    expect(rows[1].at).toBe('2026-10-02T09:00:00Z');

    const approuvee = validationRows(version({ status: 'APPROVED', signatories: [sig(1, 'alice', 'APPROVED')] }), { run: null });
    expect(states(approuvee)).toEqual(['done', 'done', 'current']);
    const rejetee = validationRows(version({ status: 'OBSOLETE', signatories: [sig(1, 'alice', 'REJECTED')] }), { run: null });
    expect(rejetee[1].state).toBe('rejected');
  });

  it('en revue, suit le passage : qui a validé, et l’étape en cours', () => {
    const rows = validationRows(version(), { run: passage() });
    expect(states(rows)).toEqual(['done', 'current', 'todo', 'todo']);
    expect(rows[1].actors).toEqual(['alice']);
    expect(rows[1].at).toBe('2026-10-02T09:00:00Z');
    expect(rows[1].approvals).toEqual({ done: 1, needed: 2 });
    expect(rows[2].roleCode).toBe('QUALITY_DIRECTOR');
  });

  it('une version publiée a franchi toutes les étapes de son passage', () => {
    const rows = validationRows(version({ status: 'PUBLISHED', publishedAt: '2026-10-05T00:00:00Z' }),
      { run: passage({ status: 'APPROVED', currentStep: 1 }) });
    expect(states(rows)).toEqual(['done', 'done', 'done', 'done']);
    expect(rows[3].at).toBe('2026-10-05T00:00:00Z');
  });

  it('une version approuvée attend sa diffusion', () => {
    const rows = validationRows(version({ status: 'APPROVED' }),
      { run: passage({ status: 'APPROVED', currentStep: 1 }) });
    expect(states(rows)).toEqual(['done', 'done', 'done', 'current']);
  });

  it('sans circuit, une seule approbation : en attente, puis faite par son approbateur', () => {
    const enRevue = validationRows(version(), { run: null });
    expect(states(enRevue)).toEqual(['done', 'current', 'todo']);
    expect(enRevue[1].note).toContain("droit d'approuver");

    const rows = validationRows(version({ status: 'APPROVED', approvedBy: 'bob', approvedAt: '2026-10-03T00:00:00Z' }),
      { run: null });
    expect(rows[1].label).toBe('Approbation');
    expect(rows[1].roleCode).toBeNull();
    expect(rows[1].actors).toEqual(['bob']);
    expect(rows[1].at).toBe('2026-10-03T00:00:00Z');

    const sansTrace = validationRows(version({ status: 'OBSOLETE' }), { run: null });
    expect(sansTrace[1].actors).toEqual([]);
    expect(sansTrace[2].at).toBeNull();
  });

  it('un passage refusé ou d’une autre soumission ne décrit pas la revue en cours', () => {
    expect(states(validationRows(version(), { run: passage({ status: 'REJECTED' }) })))
      .toEqual(['done', 'current', 'todo']);
    expect(states(validationRows(version({ status: 'PUBLISHED' }), { run: passage() })))
      .toEqual(['done', 'done', 'done']);
    expect(states(validationRows(version({ status: 'DRAFT' }), { run: passage({ status: 'APPROVED' }) })))
      .toEqual(['current', 'todo', 'todo']);
  });
});

describe('DocumentValidationComponent', () => {
  let fixture: ComponentFixture<DocumentValidationComponent>;
  let component: DocumentValidationComponent;
  let circuits: jasmine.SpyObj<CircuitService>;

  const doc = (o: Partial<DocumentResponse> = {}): DocumentResponse => ({
    id: 'd1', tenantId: 't', code: 'PR-001', title: 'Procédure', type: 'PROCEDURE', status: 'ACTIVE',
    ownerId: 'o', mandatoryRead: true, currentVersionId: 'v1',
    createdAt: '', updatedAt: '', versions: [], ...o
  });

  async function setup(run: CircuitRun | null | 'echec' = passage()): Promise<void> {
    circuits = jasmine.createSpyObj<CircuitService>('CircuitService', ['run']);
    circuits.run.and.returnValue(run === 'echec' ? throwError(() => new Error('503')) : of(run));
    await TestBed.configureTestingModule({
      declarations: [DocumentValidationComponent],
      imports: [SharedModule, PersonNameModule],
      providers: [provideDirectoryStub(), { provide: CircuitService, useValue: circuits }]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentValidationComponent);
    component = fixture.componentInstance;
  }

  function show(v: DocumentVersionResponse, d = doc()): void {
    const before = component.version;
    component.document = d;
    component.version = v;
    component.ngOnChanges({ version: new SimpleChange(before, v, before === undefined) });
    fixture.detectChanges();
  }

  const host = (): HTMLElement => fixture.nativeElement as HTMLElement;

  it('lit le passage d’une revue d’avant les signataires et le rend du haut vers le bas', async () => {
    await setup();
    show(version());
    expect(circuits.run).toHaveBeenCalledWith('document-version', 'v1');
    expect(component.loaded).toBeTrue();
    expect(host().querySelectorAll('.temps').length).toBe(4);
    expect(host().querySelector('[data-test="temps-1"]')!.getAttribute('aria-current')).toBe('step');
    expect(host().textContent).toContain('1 approbation(s) sur 2');
  });

  it('un brouillon ou une version à signataires n’a rien à lire', async () => {
    await setup();
    show(version({ status: 'DRAFT' }));
    show(version({ id: 'v2', signatories: [
      { position: 1, userId: 'alice', name: 'Alice', decision: 'PENDING', decidedAt: null, comment: null }] }));
    expect(circuits.run).not.toHaveBeenCalled();
    expect(component.loaded).toBeTrue();
    expect(host().querySelector('[data-test="note"]')).toBeNull();
    expect(component.rows.map(r => r.label)).toEqual(['Rédaction', 'Signataire 1', 'Diffusion et prise de connaissance']);
  });

  it('une lecture en échec retombe sur l’approbation simple', async () => {
    await setup('echec');
    show(version({ id: 'v2' }));
    expect(component.rows.length).toBe(3);
    expect(component.loaded).toBeTrue();
  });

  it('ne relit que si la version ou son statut change', async () => {
    await setup();
    const v = version();
    show(v);
    show({ ...v, changeNote: 'retouche' });
    expect(circuits.run).toHaveBeenCalledTimes(1);
    component.acknowledgments = 3;
    component.ngOnChanges({ acknowledgments: new SimpleChange(null, 3, false) });
    expect(circuits.run).toHaveBeenCalledTimes(1);
    show({ ...v, status: 'APPROVED' });
    expect(circuits.run).toHaveBeenCalledTimes(2);
  });

  it('suit la prise de connaissance de la version en vigueur', async () => {
    await setup(passage({ status: 'APPROVED', currentStep: 1 }));
    component.acknowledgments = 4;
    show(version({ status: 'PUBLISHED', publishedAt: '2026-10-05T00:00:00Z' }));
    expect(component.tracksReading).toBeTrue();
    expect(host().querySelector('[data-test="lectures"]')!.textContent).toContain('4 lecteur(s)');

    component.acknowledgments = null;
    expect(component.readers).toBe(0);
    show(version({ id: 'v9', status: 'PUBLISHED' }), doc({ currentVersionId: 'v1' }));
    expect(component.tracksReading).toBeFalse();
    expect(host().querySelector('[data-test="lectures"]')).toBeNull();
  });

  it('montre la raison du dernier refus d’un brouillon', async () => {
    await setup();
    show(version({ status: 'DRAFT', rejectionReason: 'Section 4 incomplète' }));
    expect(host().querySelector('[data-test="refus"]')!.textContent).toContain('Section 4 incomplète');
    expect(component.trackByIndex(2)).toBe(2);
    expect(component.roleText('QUALITY_DIRECTOR')).toBeTruthy();
  });

  it('se désabonne en partant', async () => {
    await setup();
    show(version());
    component.ngOnDestroy();
    expect(component).toBeTruthy();
  });
});

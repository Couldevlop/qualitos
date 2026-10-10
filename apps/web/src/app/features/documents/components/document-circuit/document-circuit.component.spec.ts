import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitRun } from '../../../../core/circuits/circuit.types';
import { SharedModule } from '../../../../shared/shared.module';
import { DocumentVersionResponse } from '../../documents.types';
import { DocumentCircuitComponent } from './document-circuit.component';

describe('DocumentCircuitComponent', () => {

  let fixture: ComponentFixture<DocumentCircuitComponent>;
  let component: DocumentCircuitComponent;
  let circuits: jasmine.SpyObj<CircuitService>;
  let authz: jasmine.SpyObj<AuthzService>;
  let moi = 'alice';

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const q = (test: string): HTMLElement | null => hote().querySelector(`[data-test="${test}"]`);

  const version = (o: Partial<DocumentVersionResponse> = {}): DocumentVersionResponse => ({
    id: 'v1', documentId: 'd1', versionNumber: 2, status: 'IN_REVIEW', authorId: 'autrice',
    createdAt: '', updatedAt: '', ...o
  });

  const passage = (o: Partial<CircuitRun> = {}): CircuitRun => ({
    id: 'r1', subject: 'document-version', subjectId: 'v1', status: 'IN_PROGRESS', currentStep: 1,
    steps: [
      { name: 'Relecture', roleCode: 'QUALITY_MANAGER', minApprovals: 2 },
      { name: 'Signature', roleCode: 'QUALITY_DIRECTOR', minApprovals: 1 },
      { name: 'Archivage', roleCode: 'AUDITOR', minApprovals: 1 }
    ],
    decisions: [
      { stepIndex: 0, actorId: 'alice', approved: true, comment: 'RAS', at: '2026-10-08T08:00:00Z' },
      { stepIndex: 0, actorId: 'bob', approved: true, comment: null, at: '2026-10-08T09:00:00Z' }
    ],
    startedAt: '2026-10-08T07:00:00Z', endedAt: null, ...o
  });

  async function setup(v: DocumentVersionResponse, run: CircuitRun | null | 'echec',
                       roles: string[] = ['QUALITY_DIRECTOR']): Promise<void> {
    circuits = jasmine.createSpyObj<CircuitService>('CircuitService', ['run']);
    circuits.run.and.returnValue(run === 'echec' ? throwError(() => new Error('503')) : of(run));
    authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['me']);
    authz.me.and.returnValue(of({ userId: moi, roles, permissions: [] }));
    await TestBed.configureTestingModule({
      declarations: [DocumentCircuitComponent],
      imports: [SharedModule],
      providers: [
        { provide: CircuitService, useValue: circuits },
        { provide: AuthzService, useValue: authz },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: moi }) } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentCircuitComponent);
    component = fixture.componentInstance;
    component.version = v;
    component.ngOnChanges();
    fixture.detectChanges();
  }

  beforeEach(() => moi = 'chloe');

  it('montre les étapes franchies, la courante et celles à venir', async () => {
    await setup(version(), passage());

    expect(circuits.run).toHaveBeenCalledWith('document-version', 'v1');
    expect(q('etape-0')!.classList).toContain('etape--done');
    expect(q('etape-1')!.classList).toContain('etape--current');
    expect(q('etape-1')!.getAttribute('aria-current')).toBe('step');
    expect(q('etape-2')!.classList).toContain('etape--todo');
    expect(q('etape-0')!.textContent).toContain('2 sur 2');
    expect(q('etape-0')!.textContent).toContain('« RAS »');
  });

  it('dit à l’utilisateur que c’est son tour s’il porte le rôle de l’étape', async () => {
    await setup(version(), passage());
    expect(component.turn).toBe('yours');
    expect(q('tour')!.classList).toContain('tour--yours');
  });

  it('sinon, dit qui est attendu', async () => {
    await setup(version(), passage(), ['USER']);
    expect(component.turn).toBe('waiting');
    expect(q('tour')!.textContent).toContain('Directeur qualité');
  });

  it('reconnaît l’auteur et celui qui a déjà décidé', async () => {
    moi = 'autrice';
    await setup(version(), passage());
    expect(component.turn).toBe('author');

    moi = 'alice';
    component.ngOnChanges();
    expect(component.turn).toBe('decided');
    expect(component.mine(component.run!.decisions[0])).toBeTrue();
  });

  it('sans circuit en cours, annonce l’approbation simple', async () => {
    await setup(version(), passage({ status: 'REJECTED', endedAt: 'x' }));
    expect(component.run).toBeNull();
    expect(q('simple')).not.toBeNull();
    expect(component.turn).toBeNull();
  });

  it('une lecture en échec se dit', async () => {
    await setup(version(), 'echec');
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
  });

  it('une version refusée montre sa raison, sans lire de circuit', async () => {
    await setup(version({ status: 'DRAFT', rejectionReason: 'Section 4 incomplète', rejectedAt: '2026-10-08T10:00:00Z' }), null);

    expect(circuits.run).not.toHaveBeenCalled();
    expect(q('refus')!.textContent).toContain('Section 4 incomplète');
    expect(q('circuit')).toBeNull();
  });

  it('un passage approuvé marque toutes les étapes franchies ; un refus marque la sienne', async () => {
    await setup(version(), passage());
    component.run = passage({ status: 'APPROVED' });
    expect([0, 1, 2].map(i => component.state(i))).toEqual(['done', 'done', 'done']);
    component.run = passage({ status: 'REJECTED' });
    expect(component.state(1)).toBe('rejected');
    expect(component.approvalsAt(1)).toBe(0);
  });

  it('se désabonne en partant', async () => {
    await setup(version(), passage());
    component.ngOnDestroy();
    expect(component).toBeTruthy();
  });
});

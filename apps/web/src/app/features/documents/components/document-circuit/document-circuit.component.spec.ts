import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { CircuitRun } from '../../../../core/circuits/circuit.types';
import { SharedModule } from '../../../../shared/shared.module';
import { DocumentsService } from '../../documents.service';
import { DocumentSignatory, DocumentVersionResponse } from '../../documents.types';
import { DocumentCircuitComponent } from './document-circuit.component';

describe('DocumentCircuitComponent', () => {

  let fixture: ComponentFixture<DocumentCircuitComponent>;
  let component: DocumentCircuitComponent;
  let circuits: jasmine.SpyObj<CircuitService>;
  let authz: jasmine.SpyObj<AuthzService>;
  let documents: jasmine.SpyObj<DocumentsService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
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
    documents = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['approve', 'reject']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [DocumentCircuitComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: DocumentsService, useValue: documents },
        { provide: MatSnackBar, useValue: snack },
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

  // ---------- circuit de signature (ADR 0099) ----------

  const sig = (position: number, userId: string, o: Partial<DocumentSignatory> = {}): DocumentSignatory => ({
    position, userId, name: 'Personne ' + position, decision: 'PENDING', decidedAt: null, comment: null, ...o
  });
  const signee = (o: Partial<DocumentVersionResponse> = {}): DocumentVersionResponse => version({
    signatories: [
      sig(1, 'alice', { decision: 'APPROVED', decidedAt: '2026-10-20T08:00:00Z', comment: 'Conforme' }),
      sig(2, 'chloe'),
      sig(3, 'bob')
    ], ...o
  });

  it('avec des signataires : une carte par signataire, sans lire de circuit par rôles', async () => {
    await setup(signee(), null);

    expect(circuits.run).not.toHaveBeenCalled();
    expect(q('circuit')).toBeNull();
    expect(q('signature')).not.toBeNull();
    expect(q('signataire-1')!.textContent).toContain('Signataire 1');
    expect(q('signataire-1')!.textContent).toContain('Personne 1');
    expect(q('signataire-1')!.textContent).toContain('Conforme');
    expect(q('etat-1')!.textContent).toContain('Approuvé');
    expect(q('etat-1')!.classList).toContain('etat--APPROVED');
    expect(q('etat-2')!.textContent).toContain('En attente');
    expect(q('signataire-2')!.getAttribute('aria-current')).toBe('step');
    expect(q('signataire-3')!.getAttribute('aria-current')).toBeNull();
  });

  it('seul le signataire dont c’est le tour voit Approuver et Rejeter', async () => {
    await setup(signee(), null);
    expect(component.signTurn).toBe('yours');
    expect(q('signataire-2')!.querySelector('[data-test="approuver"]')).not.toBeNull();
    expect(q('signataire-3')!.querySelector('[data-test="approuver"]')).toBeNull();
    expect(component.canSign(component.signatories[2])).toBeFalse();
  });

  it('les autres lisent qui est attendu ; l’auteur, qu’on le valide', async () => {
    moi = 'bob';
    await setup(signee(), null);
    expect(component.signTurn).toBe('waiting');
    expect(q('tour-signature')!.textContent).toContain('Personne 2');
    expect(q('approuver')).toBeNull();

    component.version = signee({ authorId: 'bob' });
    expect(component.signTurn).toBe('author');
  });

  it('approuver signe avec le commentaire, prévient et relit la fiche', async () => {
    await setup(signee(), null);
    const suite = signee();
    documents.approve.and.returnValue(of(suite));
    const relu = jasmine.createSpy('changed');
    component.changed.subscribe(relu);
    component.comment = '  Vu  ';

    (q('approuver') as HTMLButtonElement).click();

    expect(documents.approve).toHaveBeenCalledOnceWith('d1', 'v1', { approverId: 'chloe', comment: 'Vu' });
    expect(snack.open.calls.mostRecent().args[0]).toContain('signataire suivant');
    expect(relu).toHaveBeenCalledWith(suite);
    expect(component.comment).toBe('');
    expect(component.busy).toBeFalse();
  });

  it('la dernière signature approuve la version ; sans commentaire, rien n’en part', async () => {
    await setup(signee(), null);
    documents.approve.and.returnValue(of(version({ status: 'APPROVED' })));
    component.approve();
    expect(documents.approve).toHaveBeenCalledOnceWith('d1', 'v1', { approverId: 'chloe' });
    expect(snack.open.calls.mostRecent().args[0]).toContain('Version approuvée');
  });

  it('rejeter demande un motif avant de partir', async () => {
    await setup(signee(), null);
    (q('rejeter') as HTMLButtonElement).click();
    fixture.detectChanges();
    await new Promise(r => setTimeout(r));

    expect(component.rejecting).toBeTrue();
    expect(document.activeElement).toBe(q('motif'));
    (q('confirmer-rejet') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(documents.reject).not.toHaveBeenCalled();
    expect(q('motif-requis')).not.toBeNull();
    expect(q('motif')!.getAttribute('aria-invalid')).toBe('true');
  });

  it('un rejet motivé renvoie en brouillon', async () => {
    await setup(signee(), null);
    const refusee = version({ status: 'DRAFT' });
    documents.reject.and.returnValue(of(refusee));
    const relu = jasmine.createSpy('changed');
    component.changed.subscribe(relu);
    component.startReject();
    component.comment = ' Section 4 ';

    component.reject();

    expect(documents.reject).toHaveBeenCalledOnceWith('d1', 'v1', 'Section 4');
    expect(snack.open.calls.mostRecent().args[0]).toContain('brouillon');
    expect(relu).toHaveBeenCalledWith(refusee);
    expect(component.rejecting).toBeFalse();
  });

  it('annuler le rejet oublie le motif', async () => {
    await setup(signee(), null);
    component.startReject();
    component.comment = 'x';
    component.reasonAsked = true;
    component.cancelReject();
    expect([component.rejecting, component.reasonAsked, component.comment]).toEqual([false, false, '']);
  });

  it('une signature refusée par le serveur le dit et laisse décider à nouveau', async () => {
    await setup(signee(), null);
    documents.approve.and.returnValue(throwError(() => ({ status: 403 })));
    component.approve();
    expect(snack.open).toHaveBeenCalled();
    expect(component.busy).toBeFalse();
  });

  it('pas de double envoi, ni sans session', async () => {
    await setup(signee(), null);
    component.busy = true;
    component.approve();
    component.busy = false;
    moi = '';
    component.approve();
    expect(documents.approve).not.toHaveBeenCalled();
  });

  it('une version rejetée garde ses cartes, sans boutons ni tour', async () => {
    await setup(signee({ status: 'DRAFT', rejectionReason: 'Section 4', rejectedAt: '2026-10-20T09:00:00Z',
      signatories: [sig(1, 'chloe', { decision: 'REJECTED', comment: 'Section 4' })] }), null);

    expect(q('refus')).not.toBeNull();
    expect(q('signature')).not.toBeNull();
    expect(q('etat-1')!.classList).toContain('etat--REJECTED');
    expect(component.awaited).toBeNull();
    expect(component.signTurn).toBeNull();
    expect(q('approuver')).toBeNull();
    expect(component.trackBySignatory(0, component.signatories[0])).toBe(1);
  });

  it('se désabonne en partant', async () => {
    await setup(version(), passage());
    component.ngOnDestroy();
    expect(component).toBeTruthy();
  });
});

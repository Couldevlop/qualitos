import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { defer, of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  ApprovalResponse,
  ChangeResponse,
  ChangeSummary,
  ImpactResponse
} from '../../changes.types';
import { ChangesDetailComponent } from './changes-detail.component';

/**
 * L'écran est le poste de pilotage du workflow §4.8 : soumission, décisions
 * multi-niveaux, implémentation, impacts. Chaque action destructrice passe par une
 * confirmation, et l'état affiché est toujours rechargé depuis le serveur.
 */
describe('ChangesDetailComponent', () => {
  let component: ChangesDetailComponent;
  let fixture: ComponentFixture<ChangesDetailComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;
  let routeId: string;

  const ID = 'chg-1';
  const base = `${environment.apiBaseUrl}/api/v1/changes`;

  const change = (over: Partial<ChangeResponse> = {}): ChangeResponse => ({
    id: ID, tenantId: 't1', code: 'CHG-2026-014',
    title: 'Procédure stérilisation autoclave 4',
    description: 'Alignement sur ISO 13485 §7.5.7.',
    type: 'DOCUMENT', priority: 'HIGH', status: 'UNDER_REVIEW',
    requesterUserId: 'u1', impactSummary: 'PROC-STER-004 + formation.',
    riskAssessment: 'Risque résiduel faible.',
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z', ...over
  });

  const approval = (over: Partial<ApprovalResponse> = {}): ApprovalResponse => ({
    id: 'a1', tenantId: 't1', changeId: ID, approverUserId: 'qa-manager',
    approvalLevel: 1, decision: 'PENDING', createdAt: '2026-07-01T08:00:00Z', ...over
  });

  const impact = (over: Partial<ImpactResponse> = {}): ImpactResponse => ({
    id: 'i1', tenantId: 't1', changeId: ID, targetType: 'DOCUMENT',
    targetId: 'd1e2f3a4-1111-2222-3333-444455556666',
    createdAt: '2026-07-01T08:00:00Z', ...over
  });

  const summaryOf = (approvals: ApprovalResponse[], impacts: ImpactResponse[]): ChangeSummary => ({
    changeId: ID, status: 'UNDER_REVIEW', totalApprovers: approvals.length,
    approved: approvals.filter(a => a.decision === 'APPROVED').length,
    rejected: approvals.filter(a => a.decision === 'REJECTED').length,
    pending: approvals.filter(a => a.decision === 'PENDING').length,
    impactCount: impacts.length, approvals, impacts
  });

  /** Une passe de chargement : la demande, sa synthèse, ses approbations, ses impacts. */
  function flushDetail(
    c: ChangeResponse = change(),
    approvals: ApprovalResponse[] = [],
    impacts: ImpactResponse[] = []
  ): void {
    http.expectOne(`${base}/${ID}`).flush(c);
    http.expectOne(`${base}/${ID}/summary`).flush(summaryOf(approvals, impacts));
    http.expectOne(`${base}/${ID}/approvals`).flush(approvals);
    http.expectOne(`${base}/${ID}/impacts`).flush(impacts);
    fixture.detectChanges();
  }

  function stubDialog(result: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(result)
    } as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    routeId = ID;

    await TestBed.configureTestingModule({
      declarations: [ChangesDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: defer(() => of(convertToParamMap({ id: routeId }))) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangesDetailComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('affiche la demande, sa synthèse d\'approbation et ses impacts', () => {
    fixture.detectChanges();
    flushDetail(change(), [approval(), approval({ id: 'a2', approverUserId: 'reg', decision: 'APPROVED' })],
      [impact({ notes: 'MAJ procédure' })]);

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('CHG-2026-014');
    expect(el.querySelector('h1')?.textContent).toContain('autoclave 4');
    expect(el.querySelectorAll('.stat-card').length).toBe(5);
    expect(component.summary?.totalApprovers).toBe(2);
    expect(component.summary?.approved).toBe(1);
    expect(el.querySelector('.approvals-card')?.textContent).toContain('qa-manager');
    expect(el.querySelector('.impacts-card')?.textContent).toContain('MAJ procédure');
  });

  it('n\'appelle pas le serveur sur un identifiant qui n\'a pas la forme attendue', (done) => {
    routeId = '../../admin';
    fixture.detectChanges();
    http.expectNone(() => true);

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('Identifiant');
      done();
    });
  });

  it('reste lisible quand la demande échoue et que les annexes tombent aussi', (done) => {
    fixture.detectChanges();
    http.expectOne(`${base}/${ID}`).flush({ title: 'x' }, { status: 404, statusText: 'Not Found' });
    http.expectOne(`${base}/${ID}/summary`).flush({ title: 'x' }, { status: 500, statusText: 'Server Error' });
    http.expectOne(`${base}/${ID}/approvals`).flush({ title: 'x' }, { status: 500, statusText: 'Server Error' });
    http.expectOne(`${base}/${ID}/impacts`).flush({ title: 'x' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.summary).toBeNull();
    expect(component.approvals).toEqual([]);
    expect(component.impacts).toEqual([]);
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')).toBeNull();

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('Erreur lors du chargement');
      done();
    });
  });

  it('soumet une demande en brouillon puis recharge son état', () => {
    fixture.detectChanges();
    flushDetail(change({ status: 'DRAFT' }));

    component.submitForReview(change({ status: 'DRAFT' }));
    const req = http.expectOne(`${base}/${ID}/submit`);
    expect(req.request.method).toBe('POST');
    req.flush(change({ status: 'SUBMITTED' }));

    flushDetail(change({ status: 'SUBMITTED' }));
    expect((fixture.nativeElement as HTMLElement).querySelector('.meta-row')?.textContent)
      .toContain('SUBMITTED');
  });

  it('ne recharge pas quand la soumission est refusée', () => {
    fixture.detectChanges();
    flushDetail(change({ status: 'DRAFT' }));

    component.submitForReview(change({ status: 'DRAFT' }));
    http.expectOne(`${base}/${ID}/submit`)
      .flush({ title: 'invalid' }, { status: 409, statusText: 'Conflict' });

    http.expectNone(`${base}/${ID}`);
    expect((fixture.nativeElement as HTMLElement).querySelector('.meta-row')?.textContent)
      .toContain('DRAFT');
  });

  it('n\'annule la demande qu\'après confirmation', () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(false);
    component.cancel(change());
    http.expectNone(`${base}/${ID}/cancel`);

    dialog.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    component.cancel(change());
    http.expectOne(`${base}/${ID}/cancel`).flush(change({ status: 'CANCELLED' }));
    flushDetail(change({ status: 'CANCELLED' }));
    expect((fixture.nativeElement as HTMLElement).querySelector('.meta-row')?.textContent)
      .toContain('CANCELLED');
  });

  it('supprime la demande puis revient à la liste', () => {
    fixture.detectChanges();
    flushDetail();

    const nav = spyOn(TestBed.inject(Router), 'navigate');
    stubDialog(true);
    component.remove(change());

    const req = http.expectOne(`${base}/${ID}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(nav).toHaveBeenCalledWith(['/changes']);
  });

  it('ne quitte pas l\'écran si la suppression est refusée', () => {
    fixture.detectChanges();
    flushDetail();

    const nav = spyOn(TestBed.inject(Router), 'navigate');
    stubDialog(true);
    component.remove(change());
    http.expectOne(`${base}/${ID}`).flush({ title: 'nope' }, { status: 403, statusText: 'Forbidden' });

    expect(nav).not.toHaveBeenCalled();
  });

  it('retire un approbateur après confirmation et le fait disparaître du tableau', () => {
    fixture.detectChanges();
    flushDetail(change(), [approval()]);
    expect(component.approvals.length).toBe(1);

    const dialog = stubDialog(false);
    component.removeApprover(change(), approval());
    http.expectNone(`${base}/${ID}/approvers/qa-manager`);
    expect(component.approvals.length).toBe(1);

    dialog.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    component.removeApprover(change(), approval());
    const req = http.expectOne(`${base}/${ID}/approvers/qa-manager`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(component.approvals).toEqual([]);

    flushDetail();
    expect((fixture.nativeElement as HTMLElement).querySelector('.approvals-card')?.textContent)
      .toContain('Aucun approbateur');
  });

  it('détache un impact après confirmation', () => {
    fixture.detectChanges();
    flushDetail(change(), [], [impact()]);
    expect(component.impacts.length).toBe(1);

    stubDialog(true);
    component.removeImpact(change(), impact());
    const req = http.expectOne(`${base}/${ID}/impacts/i1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(component.impacts).toEqual([]);
    flushDetail();
  });

  it('conserve l\'impact quand le serveur refuse de le détacher', () => {
    fixture.detectChanges();
    flushDetail(change(), [], [impact()]);

    stubDialog(true);
    component.removeImpact(change(), impact());
    http.expectOne(`${base}/${ID}/impacts/i1`)
      .flush({ title: 'nope' }, { status: 403, statusText: 'Forbidden' });

    expect(component.impacts.length).toBe(1);
    http.expectNone(`${base}/${ID}`);
  });

  it('recharge après une décision, une implémentation ou un ajout, jamais après une annulation du dialogue', () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(approval({ decision: 'APPROVED' }));
    component.decide(change(), 'APPROVED');
    expect(dialog).toHaveBeenCalled();
    flushDetail(change({ status: 'APPROVED' }));

    dialog.and.returnValue({ afterClosed: () => of(change({ status: 'IMPLEMENTED' })) } as MatDialogRef<unknown>);
    component.openImplement(change({ status: 'APPROVED' }));
    flushDetail(change({ status: 'IMPLEMENTED' }));

    dialog.and.returnValue({ afterClosed: () => of(impact()) } as MatDialogRef<unknown>);
    component.openAddImpact(change());
    flushDetail(change(), [], [impact()]);

    dialog.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.decide(change(), 'REJECTED');
    component.openImplement(change());
    component.openAddApprover(change());
    component.openAddImpact(change());
    component.openEdit(change());
    http.expectNone(`${base}/${ID}`);
  });

  it('ajoute un approbateur et le voit apparaître après rechargement', () => {
    fixture.detectChanges();
    flushDetail();

    stubDialog(approval());
    component.openAddApprover(change());
    flushDetail(change(), [approval()]);

    expect(component.approvals.map(a => a.approverUserId)).toEqual(['qa-manager']);
    expect((fixture.nativeElement as HTMLElement).querySelector('.approvals-card')?.textContent)
      .toContain('PENDING');
  });

  it('traduit les types de demande et les entités impactées', () => {
    expect(component.typeLabel('DOCUMENT')).toBe('Document');
    expect(component.typeLabel('IT_SYSTEM')).toBe('Système IT');
    expect(component.typeLabel('ORGANIZATIONAL')).toBe('Organisationnel');
    expect(component.typeLabel('OTHER')).toBe('Autre');

    expect(component.targetLabel('TRAINING_PATH')).toBe('Parcours');
    expect(component.targetLabel('IOT_DEVICE')).toBe('IoT');
    expect(component.targetLabel('FMEA_PROJECT')).toBe('FMEA');
    expect(component.targetLabel('PDCA_CYCLE')).toBe('PDCA');
    expect(component.targetLabel('STANDARD')).toBe('Norme');
    expect(component.targetLabel('SUPPLIER')).toBe('Fournisseur');
    expect(component.targetLabel('OTHER')).toBe('Autre');
  });

  it('dérive les classes de badge du statut, de la priorité et de la décision', () => {
    expect(component.statusBadge('IMPLEMENTED')).toBe('badge badge-implemented');
    expect(component.priorityBadge('LOW')).toBe('prio prio-low');
    expect(component.decisionBadge('REJECTED')).toBe('dbadge dbadge-rejected');
    expect(component.decisionBadge('PENDING')).toBe('dbadge dbadge-pending');
  });
});

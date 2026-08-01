import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { defer, of } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Complaint, ComplaintResponseEntry, SpringPage } from '../../complaints.types';
import { ComplaintDetailComponent } from './complaint-detail.component';

describe('ComplaintDetailComponent', () => {
  let component: ComplaintDetailComponent;
  let fixture: ComponentFixture<ComplaintDetailComponent>;
  let http: HttpTestingController;
  let currentUser: AuthUser | null;
  let routeId: string;

  const ID = '3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f';
  const base = `${environment.apiBaseUrl}/api/v1/complaints`;

  const complaint = (over: Partial<Complaint> = {}): Complaint => ({
    id: ID, tenantId: 't1', code: 'REC-1', channel: 'EMAIL',
    customerName: 'Acme', customerEmail: 'qa@acme.test', customerExternalId: null,
    subject: 'Colis endommagé', description: 'Carton écrasé à la livraison.',
    severity: 'HIGH', category: 'DELIVERY', status: 'UNDER_INVESTIGATION',
    supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: '2026-07-01T08:00:00Z', firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: 'u1', createdAt: '2026-07-01T08:00:00Z', updatedAt: null, ...over
  });

  const entry = (over: Partial<ComplaintResponseEntry> = {}): ComplaintResponseEntry => ({
    id: 'r1', tenantId: 't1', complaintId: ID, authorUserId: 'u1', channel: 'EMAIL',
    body: 'Nous revenons vers vous.', internalNote: false,
    sentAt: '2026-07-02T09:00:00Z', createdAt: '2026-07-02T09:00:00Z', ...over
  });

  const pageOf = <T>(content: T[]): SpringPage<T> => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 100
  });

  /** Une passe de chargement = la réclamation + son fil de messages. */
  function flushDetail(c: Complaint = complaint(), responses: ComplaintResponseEntry[] = []): void {
    http.expectOne(`${base}/${ID}`).flush(c);
    http.expectOne(r => r.url === `${base}/${ID}/responses`).flush(pageOf(responses));
    fixture.detectChanges();
  }

  function stubDialog(result: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(result)
    } as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    routeId = ID;
    currentUser = {
      userId: '11111111-1111-1111-1111-111111111111',
      tenantId: 't1', displayName: 'QM', roles: ['quality_manager']
    };

    await TestBed.configureTestingModule({
      declarations: [ComplaintDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        // `defer` : l'identifiant est lu au moment de l'abonnement, ce qui permet
        // à un test de le remplacer avant la première détection de changements.
        { provide: ActivatedRoute, useValue: { paramMap: defer(() => of(convertToParamMap({ id: routeId }))) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ComplaintDetailComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('affiche la référence et l\'objet du dossier', () => {
    fixture.detectChanges();
    flushDetail();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('REC-1');
    expect(el.querySelector('h1')?.textContent).toContain('Colis endommagé');
  });

  it('avertit que la résolution attend une réponse au client', () => {
    fixture.detectChanges();
    flushDetail();
    expect(component.awaitingResponse(complaint())).toBeTrue();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.banner-warn')?.textContent).toContain('résolution');
  });

  it('n\'appelle pas le serveur sur un identifiant invalide', (done) => {
    routeId = 'pas-un-uuid';
    fixture.detectChanges();
    http.expectNone(() => true);

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('Identifiant');
      done();
    });
  });

  it('n\'offre que les transitions valides dans l\'état courant', () => {
    const investigating = complaint();
    expect(component.canRespond(investigating)).toBeTrue();
    expect(component.canReject(investigating)).toBeTrue();
    expect(component.canResolve(investigating)).toBeFalse();
    expect(component.canClose(investigating)).toBeFalse();
    expect(component.canReopen(investigating)).toBeFalse();
    expect(component.canRate(investigating)).toBeFalse();

    const resolved = complaint({ status: 'RESOLVED', firstResponseAt: '2026-07-02T09:00:00Z' });
    expect(component.canClose(resolved)).toBeTrue();
    expect(component.canReopen(resolved)).toBeTrue();
    expect(component.canRate(resolved)).toBeTrue();
    expect(component.canReject(resolved)).toBeFalse();

    const closed = complaint({ status: 'CLOSED' });
    expect(component.canEdit(closed)).toBeFalse();
    expect(component.canAssign(closed)).toBeFalse();
    expect(component.canRespond(closed)).toBeFalse();
  });

  it('conditionne la suppression au statut ET au rôle porté par le JWT', () => {
    expect(component.canDelete(complaint({ status: 'RECEIVED' }))).toBeTrue();
    expect(component.canDelete(complaint({ status: 'RESOLVED' }))).toBeFalse();

    currentUser = { userId: 'u2', tenantId: 't1', displayName: 'Op', roles: ['user'] };
    expect(component.canDelete(complaint({ status: 'RECEIVED' }))).toBeFalse();
    expect(component.canDeleteNote(entry({ internalNote: true }))).toBeFalse();

    currentUser = { userId: 'u3', tenantId: 't1', displayName: 'Adm', roles: ['ADMIN_TENANT'] };
    expect(component.canDeleteNote(entry({ internalNote: true }))).toBeTrue();
    expect(component.canDeleteNote(entry({ internalNote: false }))).toBeFalse();
  });

  it('clôture après confirmation puis recharge le dossier', () => {
    fixture.detectChanges();
    const resolved = complaint({ status: 'RESOLVED', firstResponseAt: '2026-07-02T09:00:00Z' });
    flushDetail(resolved);

    stubDialog(true);
    component.close(resolved);

    http.expectOne(`${base}/${ID}/close`).flush(complaint({ status: 'CLOSED' }));
    flushDetail(complaint({ status: 'CLOSED' }));
    expect(component.pending).toBeFalse();
  });

  it('ne fait rien si la confirmation est refusée', () => {
    fixture.detectChanges();
    const resolved = complaint({ status: 'RESOLVED' });
    flushDetail(resolved);

    stubDialog(false);
    component.close(resolved);
    http.expectNone(`${base}/${ID}/close`);
    expect(component.pending).toBeFalse();
  });

  it('rouvre un dossier clôturé', () => {
    fixture.detectChanges();
    const closed = complaint({ status: 'CLOSED' });
    flushDetail(closed);

    stubDialog(true);
    component.reopen(closed);
    const req = http.expectOne(`${base}/${ID}/reopen`);
    expect(req.request.method).toBe('POST');
    req.flush(complaint({ status: 'UNDER_INVESTIGATION' }));
    flushDetail();
    expect((fixture.nativeElement as HTMLElement).querySelector('.badge')?.textContent)
      .toContain('investigation');
  });

  it('supprime le dossier puis revient à la liste', () => {
    fixture.detectChanges();
    const received = complaint({ status: 'RECEIVED' });
    flushDetail(received);

    const nav = spyOn(TestBed.inject(Router), 'navigate');
    stubDialog(true);
    component.remove(received);

    http.expectOne(`${base}/${ID}`).flush(null);
    expect(nav).toHaveBeenCalledWith(['/complaints']);
  });

  it('supprime une note interne puis recharge le fil', () => {
    fixture.detectChanges();
    const note = entry({ id: 'r9', internalNote: true });
    flushDetail(complaint(), [note]);

    stubDialog(true);
    component.deleteResponse(complaint(), note);
    const req = http.expectOne(`${base}/${ID}/responses/r9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    flushDetail();
    expect(component.pending).toBeFalse();
  });

  it('recharge après une action de dialogue, et pas si elle est annulée', () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(complaint({ status: 'REJECTED' }));
    component.runAction(complaint(), 'REJECT');
    expect(dialog).toHaveBeenCalled();
    flushDetail(complaint({ status: 'REJECTED' }));

    dialog.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.edit(complaint());
    http.expectNone(`${base}/${ID}`);
  });

  it('ouvre le dialogue de réponse et recharge quand un message est ajouté', () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(entry());
    component.respond(complaint());
    expect(dialog).toHaveBeenCalled();

    flushDetail(complaint({ firstResponseAt: '2026-07-02T09:00:00Z', status: 'RESPONDED' }), [entry()]);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.msg__body')?.textContent).toContain('Nous revenons vers vous');
    expect(el.querySelector('.banner-warn')).toBeNull();
  });

  it('remonte un message lisible quand la transition est refusée', () => {
    fixture.detectChanges();
    const resolved = complaint({ status: 'RESOLVED' });
    flushDetail(resolved);

    stubDialog(true);
    component.close(resolved);
    http.expectOne(`${base}/${ID}/close`)
      .flush({ title: 'nope' }, { status: 409, statusText: 'Conflict' });

    expect(component.pending).toBeFalse();
    // Aucun rechargement n'est déclenché : la vue garde l'état serveur connu.
    http.expectNone(`${base}/${ID}`);
  });

  it('associe un ton aux statuts, gravités et notes de satisfaction', () => {
    expect(component.statusTone('RECEIVED')).toBe('info');
    expect(component.statusTone('UNDER_INVESTIGATION')).toBe('warn');
    expect(component.statusTone('REOPENED')).toBe('warn');
    expect(component.statusTone('RESPONDED')).toBe('info');
    expect(component.statusTone('RESOLVED')).toBe('success');
    expect(component.statusTone('CLOSED')).toBe('neutral');
    expect(component.statusTone('REJECTED')).toBe('danger');

    expect(component.severityTone('LOW')).toBe('success');
    expect(component.severityTone('MEDIUM')).toBe('info');
    expect(component.severityTone('HIGH')).toBe('warn');
    expect(component.severityTone('CRITICAL')).toBe('danger');

    expect(component.satisfactionTone(null)).toBe('neutral');
    expect(component.satisfactionTone(10)).toBe('success');
    expect(component.satisfactionTone(7)).toBe('warn');
    expect(component.satisfactionTone(3)).toBe('danger');
  });

  it('identifie les messages pour le trackBy', () => {
    expect(component.trackByEntry(0, entry({ id: 'r42' }))).toBe('r42');
  });
});

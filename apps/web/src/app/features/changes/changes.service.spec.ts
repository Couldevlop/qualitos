import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ChangesService } from './changes.service';
import { ApprovalResponse, ChangeResponse, ImpactResponse } from './changes.types';

/**
 * Le service porte deux implémentations derrière la même signature : le mode démo
 * hors-ligne (`useMockApi`) et l'API réelle. Les deux partent dans le bundle, les
 * deux sont donc vérifiées — en particulier les invariants de statut du workflow
 * d'approbation, que le mode démo rejoue localement.
 */
describe('ChangesService (mode démo hors-ligne)', () => {
  let service: ChangesService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ChangesService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded change requests', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filtre par statut puis par type', (done) => {
    service.list(0, 50, 'DRAFT').subscribe(drafts => {
      expect(drafts.content.every(c => c.status === 'DRAFT')).toBeTrue();
      service.list(0, 50, undefined, 'IT_SYSTEM').subscribe(itChanges => {
        expect(itChanges.content.map(c => c.code)).toEqual(['CHG-2026-015']);
        done();
      });
    });
  });

  it('creates a DRAFT change request with default priority', (done) => {
    service.create({
      code: 'C', title: 'Change X', type: 'PROCESS', requesterUserId: 'u'
    }).subscribe(c => {
      expect(c.status).toBe('DRAFT');
      expect(c.priority).toBe('MEDIUM');
      done();
    });
  });

  it('respecte la priorité choisie à la création', (done) => {
    service.create({
      code: 'C2', title: 'Urgent', type: 'EQUIPMENT', requesterUserId: 'u', priority: 'CRITICAL'
    }).subscribe(c => {
      expect(c.priority).toBe('CRITICAL');
      done();
    });
  });

  it('addApprover then lists approvals', (done) => {
    service.create({ code: 'AP', title: 'Approvable', type: 'PROCESS', requesterUserId: 'u' })
      .subscribe(c => {
        service.addApprover(c.id, { approverUserId: 'mgr' }).subscribe(a => {
          expect(a.decision).toBe('PENDING');
          expect(a.approvalLevel).toBe(1);
          service.listApprovals(c.id).subscribe(approvals => {
            expect(approvals.length).toBeGreaterThan(0);
            done();
          });
        });
      });
  });

  it('addImpact then lists impacts', (done) => {
    service.create({ code: 'IM', title: 'Impactful', type: 'PROCESS', requesterUserId: 'u' })
      .subscribe(c => {
        service.addImpact(c.id, { targetType: 'DOCUMENT', targetId: 'doc-1' }).subscribe(() => {
          service.listImpacts(c.id).subscribe(impacts => {
            expect(impacts.length).toBeGreaterThan(0);
            done();
          });
        });
      });
  });

  it('passe la demande en revue puis l\'implémente', (done) => {
    service.submit('chg-3').subscribe(submitted => {
      expect(submitted.status).toBe('SUBMITTED');
      service.implement('chg-3', { implementedAt: '2026-08-01' }).subscribe(done_ => {
        expect(done_.status).toBe('IMPLEMENTED');
        expect(done_.implementedAt).toBe('2026-08-01');
        done();
      });
    });
  });

  it('conserve le motif quand la demande est annulée', (done) => {
    service.cancel('chg-2', 'Budget gelé').subscribe(c => {
      expect(c.status).toBe('CANCELLED');
      expect(c.rejectionReason).toBe('Budget gelé');
      done();
    });
  });

  it('reflète le statut parent sur la décision des approbateurs', (done) => {
    // « chg-1 » est semé avec un approbateur APPROVED et un PENDING.
    service.decide('chg-1', { approverUserId: 'reg-officer', decision: 'APPROVED' }).subscribe(() => {
      service.get('chg-1').subscribe(approved => {
        expect(approved.status).toBe('APPROVED');

        service.decide('chg-1', { approverUserId: 'qa-manager', decision: 'PENDING' }).subscribe(() => {
          service.get('chg-1').subscribe(underReview => {
            // Un avis retiré fait retomber la demande en cours d'examen.
            expect(underReview.status).toBe('UNDER_REVIEW');

            service.decide('chg-1', {
              approverUserId: 'qa-manager', decision: 'REJECTED', comment: 'Risque non couvert'
            }).subscribe(() => {
              service.get('chg-1').subscribe(rejected => {
                // Un seul rejet suffit à bloquer la demande, et il est motivé.
                expect(rejected.status).toBe('REJECTED');
                expect(rejected.rejectionReason).toBe('Risque non couvert');
                done();
              });
            });
          });
        });
      });
    });
  });

  it('agrège la synthèse d\'une demande à partir de ses approbations et impacts', (done) => {
    service.addImpact('chg-1', { targetType: 'PDCA_CYCLE', targetId: 'pdca-9' }).subscribe(() => {
      service.summary('chg-1').subscribe(s => {
        expect(s.changeId).toBe('chg-1');
        expect(s.status).toBe('UNDER_REVIEW');
        expect(s.totalApprovers).toBe(2);
        expect(s.approved).toBe(1);
        expect(s.pending).toBe(1);
        expect(s.rejected).toBe(0);
        expect(s.impactCount).toBe(1);
        expect(s.impacts.length).toBe(1);
        done();
      });
    });
  });

  it('retire un approbateur et un impact de la demande', (done) => {
    service.addImpact('chg-1', { targetType: 'STANDARD', targetId: 'iso-9001' }).subscribe(im => {
      service.removeApprover('chg-1', 'qa-manager').subscribe(() => {
        service.removeImpact('chg-1', im.id).subscribe(() => {
          service.summary('chg-1').subscribe(s => {
            expect(s.approvals.map(a => a.approverUserId)).toEqual(['reg-officer']);
            expect(s.impactCount).toBe(0);
            done();
          });
        });
      });
    });
  });

  it('met à jour les champs éditables de la demande', (done) => {
    service.update('chg-2', { title: 'Migration LMS — phase 2', priority: 'HIGH' }).subscribe(c => {
      expect(c.title).toBe('Migration LMS — phase 2');
      expect(c.priority).toBe('HIGH');
      expect(c.code).toBe('CHG-2026-015');
      done();
    });
  });

  it('supprime la demande avec ses approbations et ses impacts', (done) => {
    service.delete('chg-1').subscribe(() => {
      service.list().subscribe(page => {
        expect(page.content.some(c => c.id === 'chg-1')).toBeFalse();
        service.listApprovals('chg-1').subscribe(approvals => {
          expect(approvals).toEqual([]);
          done();
        });
      });
    });
  });
});

describe('ChangesService (API réelle)', () => {
  let service: ChangesService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/changes`;
  const ID = 'c1b2c3d4-1111-2222-3333-444455556666';

  const change: ChangeResponse = {
    id: ID, tenantId: 't1', code: 'CHG-2026-014', title: 'Procédure stérilisation',
    type: 'DOCUMENT', priority: 'HIGH', status: 'UNDER_REVIEW',
    requesterUserId: 'u1', createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z'
  };

  const approval: ApprovalResponse = {
    id: 'a1', tenantId: 't1', changeId: ID, approverUserId: 'u2',
    approvalLevel: 1, decision: 'PENDING', createdAt: '2026-07-01T08:00:00Z'
  };

  const impact: ImpactResponse = {
    id: 'i1', tenantId: 't1', changeId: ID, targetType: 'DOCUMENT',
    targetId: 'doc-1', createdAt: '2026-07-01T08:00:00Z'
  };

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ChangesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('pagine sans envoyer de filtre vide', (done) => {
    service.list().subscribe(page => {
      expect(page.totalElements).toBe(1);
      done();
    });
    const req = http.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('type')).toBeFalse();
    req.flush({ content: [change], totalElements: 1, totalPages: 1, number: 0, size: 50 });
  });

  it('transmet statut et type quand l\'utilisateur filtre', (done) => {
    service.list(1, 10, 'APPROVED', 'SUPPLIER').subscribe(() => done());
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('status')).toBe('APPROVED');
    expect(req.request.params.get('type')).toBe('SUPPLIER');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 });
  });

  it('lit, crée, met à jour et supprime une demande', (done) => {
    service.get(ID).subscribe(c => expect(c.code).toBe('CHG-2026-014'));
    http.expectOne(`${base}/${ID}`).flush(change);

    service.create({ code: 'CHG-1', title: 'T', type: 'PROCESS', requesterUserId: 'u1' }).subscribe();
    const post = http.expectOne(base);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.code).toBe('CHG-1');
    post.flush(change);

    service.update(ID, { title: 'Revu' }).subscribe();
    const patch = http.expectOne(`${base}/${ID}`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ title: 'Revu' });
    patch.flush(change);

    service.delete(ID).subscribe(() => done());
    const del = http.expectOne(`${base}/${ID}`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('soumet la demande pour revue', (done) => {
    service.submit(ID).subscribe(() => done());
    const req = http.expectOne(`${base}/${ID}/submit`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ ...change, status: 'SUBMITTED' });
  });

  it('passe le motif d\'annulation en paramètre de requête, et rien sans motif', (done) => {
    service.cancel(ID, 'Budget gelé').subscribe(() => {
      service.cancel(ID).subscribe(() => done());
      const plain = http.expectOne(`${base}/${ID}/cancel`);
      expect(plain.request.params.has('reason')).toBeFalse();
      plain.flush({ ...change, status: 'CANCELLED' });
    });
    const withReason = http.expectOne(r => r.url === `${base}/${ID}/cancel`);
    expect(withReason.request.params.get('reason')).toBe('Budget gelé');
    withReason.flush({ ...change, status: 'CANCELLED' });
  });

  it('marque la demande implémentée avec sa date', (done) => {
    service.implement(ID, { implementedAt: '2026-08-01' }).subscribe(() => done());
    const req = http.expectOne(`${base}/${ID}/implement`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ implementedAt: '2026-08-01' });
    req.flush({ ...change, status: 'IMPLEMENTED' });
  });

  it('lit la synthèse d\'avancement des approbations', (done) => {
    service.summary(ID).subscribe(s => {
      expect(s.pending).toBe(2);
      done();
    });
    http.expectOne(`${base}/${ID}/summary`).flush({
      changeId: ID, status: 'UNDER_REVIEW', totalApprovers: 3, approved: 1,
      rejected: 0, pending: 2, impactCount: 0, approvals: [], impacts: []
    });
  });

  it('gère les approbateurs et leurs décisions sur les routes dédiées', (done) => {
    service.listApprovals(ID).subscribe(list => expect(list.length).toBe(1));
    http.expectOne(`${base}/${ID}/approvals`).flush([approval]);

    service.addApprover(ID, { approverUserId: 'u2', approvalLevel: 2 }).subscribe();
    const post = http.expectOne(`${base}/${ID}/approvers`);
    expect(post.request.body).toEqual({ approverUserId: 'u2', approvalLevel: 2 });
    post.flush(approval);

    service.decide(ID, { approverUserId: 'u2', decision: 'REJECTED', comment: 'Non' }).subscribe();
    const decision = http.expectOne(`${base}/${ID}/decisions`);
    expect(decision.request.method).toBe('POST');
    expect(decision.request.body).toEqual({ approverUserId: 'u2', decision: 'REJECTED', comment: 'Non' });
    decision.flush({ ...approval, decision: 'REJECTED' });

    service.removeApprover(ID, 'u2').subscribe(() => done());
    const del = http.expectOne(`${base}/${ID}/approvers/u2`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('gère les impacts sur les routes dédiées', (done) => {
    service.listImpacts(ID).subscribe(list => expect(list.length).toBe(1));
    http.expectOne(`${base}/${ID}/impacts`).flush([impact]);

    service.addImpact(ID, { targetType: 'DOCUMENT', targetId: 'doc-1', notes: 'MAJ requise' }).subscribe();
    const post = http.expectOne(`${base}/${ID}/impacts`);
    expect(post.request.body).toEqual({ targetType: 'DOCUMENT', targetId: 'doc-1', notes: 'MAJ requise' });
    post.flush(impact);

    service.removeImpact(ID, 'i1').subscribe(() => done());
    const del = http.expectOne(`${base}/${ID}/impacts/i1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('propage le refus du serveur au lieu de le masquer', (done) => {
    service.submit(ID).subscribe({
      next: () => fail('une transition refusée ne doit pas produire de demande'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });
    http.expectOne(`${base}/${ID}/submit`)
      .flush({ title: 'invalid state' }, { status: 409, statusText: 'Conflict' });
  });
});

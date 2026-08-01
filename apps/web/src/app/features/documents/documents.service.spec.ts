import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { DocumentsService } from './documents.service';
import { DocumentResponse, DocumentVersionResponse } from './documents.types';

describe('DocumentsService (mock mode)', () => {
  let service: DocumentsService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DocumentsService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded documents', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a document with an initial DRAFT version', (done) => {
    service.create({
      code: 'X', title: 'Doc', type: 'PROCEDURE', ownerId: 'u', mandatoryRead: false
    }).subscribe(d => {
      expect(d.status).toBe('ACTIVE');
      expect(d.versions.length).toBe(1);
      expect(d.versions[0].status).toBe('DRAFT');
      done();
    });
  });

  it('createVersion increments versionNumber', (done) => {
    service.create({ code: 'V', title: 'Versioned', type: 'POLICY', ownerId: 'u', mandatoryRead: false })
      .subscribe(d => {
        service.createVersion(d.id, { authorId: 'u', changeNote: 'v2' }).subscribe(v => {
          expect(v.versionNumber).toBe(2);
          expect(v.status).toBe('DRAFT');
          done();
        });
      });
  });

  it('submit -> approve -> publish lifecycle', (done) => {
    service.create({ code: 'L', title: 'Lifecycle', type: 'POLICY', ownerId: 'u', mandatoryRead: false })
      .subscribe(d => {
        const v = d.versions[0];
        service.submit(d.id, v.id).subscribe(sub => {
          expect(sub.status).toBe('IN_REVIEW');
          service.approve(d.id, v.id, { approverId: 'mgr' }).subscribe(app => {
            expect(app.status).toBe('APPROVED');
            expect(app.approvedBy).toBe('mgr');
            service.publish(d.id, v.id).subscribe(pub => {
              expect(pub.status).toBe('PUBLISHED');
              done();
            });
          });
        });
      });
  });

  it('archive sets ARCHIVED status', (done) => {
    service.archive('doc-3').subscribe(d => {
      expect(d.status).toBe('ARCHIVED');
      done();
    });
  });

  it('acknowledge returns an acknowledgment record', (done) => {
    service.acknowledge('doc-1', 'ver-1', { userId: 'u' }).subscribe(ack => {
      expect(ack.userId).toBe('u');
      expect(ack.versionId).toBe('ver-1');
      done();
    });
  });

  it('ne renvoie que les documents du statut demandé', (done) => {
    service.archive('doc-3').subscribe(() => {
      service.list(0, 20, 'ARCHIVED').subscribe(page => {
        expect(page.content.length).toBe(1);
        expect(page.content[0].id).toBe('doc-3');
        done();
      });
    });
  });

  it('met à jour les métadonnées du document sans toucher aux versions', (done) => {
    service.update('doc-2', { title: 'Procédure audits internes v2', mandatoryRead: true })
      .subscribe(d => {
        expect(d.title).toBe('Procédure audits internes v2');
        expect(d.mandatoryRead).toBeTrue();
        expect(d.versions.length).toBe(1);
        done();
      });
  });

  it('retombe sur le premier document quand l\'identifiant à mettre à jour est inconnu', (done) => {
    service.update('inconnu', { title: 'ignoré' }).subscribe(d => {
      expect(d.id).toBe('doc-1');
      expect(d.title).toBe('Politique Qualité 2026');
      done();
    });
  });

  it('l\'archivage d\'un identifiant inconnu n\'archive aucun document existant', (done) => {
    service.archive('inconnu').subscribe(d => {
      expect(d.id).toBe('doc-1');
      expect(d.status).toBe('ACTIVE');
      done();
    });
  });

  it('updateVersion n\'écrase que les champs transmis', (done) => {
    service.updateVersion('doc-1', 'ver-1', { changeNote: 'Correction typo' }).subscribe(v => {
      expect(v.changeNote).toBe('Correction typo');
      expect(v.content).toBe('Engagement direction… (extrait démo)');
      done();
    });
  });

  it('updateVersion accepte contenu et URI de contenu', (done) => {
    service.updateVersion('doc-1', 'ver-1', { content: 'Nouveau corps', contentUri: 's3://doc/1' })
      .subscribe(v => {
        expect(v.content).toBe('Nouveau corps');
        expect(v.contentUri).toBe('s3://doc/1');
        done();
      });
  });

  it('updateVersion sur une version inconnue retombe sur une version de repli', (done) => {
    service.updateVersion('doc-1', 'inconnue', { changeNote: 'x' }).subscribe(v => {
      expect(v.id).toBe('ver-1');
      expect(v.changeNote).toBe('Mise à jour annuelle 2026');
      done();
    });
  });

  it('publier une version rend obsolète la version publiée précédente (une seule en vigueur)', (done) => {
    service.createVersion('doc-1', { authorId: 'u', changeNote: 'v3' }).subscribe(v3 => {
      service.publish('doc-1', v3.id).subscribe(published => {
        expect(published.status).toBe('PUBLISHED');
        service.get('doc-1').subscribe(d => {
          expect(d.currentVersionId).toBe(v3.id);
          expect(d.versions.find(v => v.id === 'ver-1')?.status).toBe('OBSOLETE');
          done();
        });
      });
    });
  });

  it('approuver une version inconnue ne modifie aucune version existante', (done) => {
    service.approve('doc-1', 'inconnue', { approverId: 'mgr' }).subscribe(v => {
      expect(v.id).toBe('ver-1');
      expect(v.approvedBy).toBe('demo-user');
      done();
    });
  });

  it('publier une version inconnue ne change pas la version courante', (done) => {
    service.publish('doc-1', 'inconnue').subscribe(() => {
      service.get('doc-1').subscribe(d => {
        expect(d.currentVersionId).toBe('ver-1');
        done();
      });
    });
  });

  it('soumettre une version inconnue ne fait basculer aucun statut', (done) => {
    service.submit('doc-3', 'inconnue').subscribe(() => {
      service.get('doc-3').subscribe(d => {
        expect(d.versions[0].status).toBe('DRAFT');
        done();
      });
    });
  });

  it('enregistre l\'ancrage blockchain sur la version', (done) => {
    service.setBlockchainTx('doc-1', 'ver-1', '0xabc123').subscribe(v => {
      expect(v.blockchainTxHash).toBe('0xabc123');
      done();
    });
  });

  it('l\'ancrage d\'une version inconnue ne pose aucun hash à tort', (done) => {
    service.setBlockchainTx('doc-1', 'inconnue', '0xdead').subscribe(v => {
      expect(v.id).toBe('ver-1');
      expect(v.blockchainTxHash).toBeUndefined();
      done();
    });
  });

  it('compte les acquittements de lecture obligatoire', (done) => {
    service.countAcknowledgments('doc-1', 'ver-1').subscribe(r => {
      expect(r.count).toBe(0);
      done();
    });
  });
});

/**
 * Mode API réelle (celui de production) : la GED signe des preuves d'audit,
 * donc chaque verbe/URL doit être vérifié — un PATCH parti sur la mauvaise
 * sous-ressource publierait ou approuverait la mauvaise version.
 */
describe('DocumentsService (API réelle)', () => {
  let service: DocumentsService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/documents`;

  const version = (over: Partial<DocumentVersionResponse> = {}): DocumentVersionResponse => ({
    id: 'v1', documentId: 'd1', versionNumber: 1, status: 'DRAFT', authorId: 'u1',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', ...over
  });

  const doc = (over: Partial<DocumentResponse> = {}): DocumentResponse => ({
    id: 'd1', tenantId: 't1', code: 'PR-001', title: 'Procédure', type: 'PROCEDURE',
    status: 'ACTIVE', ownerId: 'u1', mandatoryRead: false,
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    versions: [version()], ...over
  });

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DocumentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('pagine sans filtre de statut par défaut', () => {
    service.list(1, 10).subscribe();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 });
  });

  it('transmet le filtre ARCHIVED au serveur', () => {
    service.list(0, 20, 'ARCHIVED').subscribe();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('ARCHIVED');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('lit un document par identifiant', (done) => {
    service.get('d1').subscribe(d => {
      expect(d.code).toBe('PR-001');
      done();
    });
    http.expectOne(`${base}/d1`).flush(doc());
  });

  it('crée un document en POST sur la collection', () => {
    service.create({ code: 'PR-002', title: 'T', type: 'POLICY', ownerId: 'u1', mandatoryRead: true })
      .subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.code).toBe('PR-002');
    expect(req.request.body.mandatoryRead).toBeTrue();
    req.flush(doc());
  });

  it('met à jour les métadonnées en PATCH', () => {
    service.update('d1', { title: 'Nouveau titre' }).subscribe();
    const req = http.expectOne(`${base}/d1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ title: 'Nouveau titre' });
    req.flush(doc({ title: 'Nouveau titre' }));
  });

  it('archive via une sous-ressource dédiée, jamais par DELETE (conservation pour audit)', () => {
    service.archive('d1').subscribe();
    const req = http.expectOne(`${base}/d1/archive`);
    expect(req.request.method).toBe('PATCH');
    req.flush(doc({ status: 'ARCHIVED' }));
  });

  it('crée une version sous la ressource du document', () => {
    service.createVersion('d1', { authorId: 'u1', changeNote: 'v2' }).subscribe();
    const req = http.expectOne(`${base}/d1/versions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ authorId: 'u1', changeNote: 'v2' });
    req.flush(version({ id: 'v2', versionNumber: 2 }));
  });

  it('met à jour une version en PATCH', () => {
    service.updateVersion('d1', 'v1', { content: 'corps' }).subscribe();
    const req = http.expectOne(`${base}/d1/versions/v1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ content: 'corps' });
    req.flush(version({ content: 'corps' }));
  });

  it('soumet une version à revue', (done) => {
    service.submit('d1', 'v1').subscribe(v => {
      expect(v.status).toBe('IN_REVIEW');
      done();
    });
    const req = http.expectOne(`${base}/d1/versions/v1/submit`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({});
    req.flush(version({ status: 'IN_REVIEW' }));
  });

  it('approuve en transmettant l\'approbateur (traçabilité ISO 9001 §7.5.2)', () => {
    service.approve('d1', 'v1', { approverId: 'mgr' }).subscribe();
    const req = http.expectOne(`${base}/d1/versions/v1/approve`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ approverId: 'mgr' });
    req.flush(version({ status: 'APPROVED', approvedBy: 'mgr' }));
  });

  it('publie une version approuvée', () => {
    service.publish('d1', 'v1').subscribe();
    const req = http.expectOne(`${base}/d1/versions/v1/publish`);
    expect(req.request.method).toBe('PATCH');
    req.flush(version({ status: 'PUBLISHED' }));
  });

  it('remonte le refus serveur quand la version n\'est pas approuvée', (done) => {
    service.publish('d1', 'v1').subscribe({
      next: () => fail('une publication refusée ne doit pas produire de valeur'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });
    http.expectOne(`${base}/d1/versions/v1/publish`)
      .flush({ title: 'not approved' }, { status: 409, statusText: 'Conflict' });
  });

  it('pose le hash d\'ancrage blockchain sur la version', () => {
    service.setBlockchainTx('d1', 'v1', '0xfeed').subscribe();
    const req = http.expectOne(`${base}/d1/versions/v1/blockchain`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ txHash: '0xfeed' });
    req.flush(version({ blockchainTxHash: '0xfeed' }));
  });

  it('enregistre l\'acquittement de lecture en POST', () => {
    service.acknowledge('d1', 'v1', { userId: 'u9' }).subscribe();
    const req = http.expectOne(`${base}/d1/versions/v1/acknowledge`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'u9' });
    req.flush({ id: 'ack1', versionId: 'v1', userId: 'u9', acknowledgedAt: '2026-07-02T00:00:00Z' });
  });

  it('compte les acquittements en GET', (done) => {
    service.countAcknowledgments('d1', 'v1').subscribe(r => {
      expect(r.count).toBe(12);
      done();
    });
    const req = http.expectOne(`${base}/d1/versions/v1/acknowledgments/count`);
    expect(req.request.method).toBe('GET');
    req.flush({ count: 12 });
  });
});

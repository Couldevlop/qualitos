import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { DocumentsService } from './documents.service';

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
});

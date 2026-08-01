import { HttpRequest, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { StandardsExtrasService } from './standards-extras.service';
import {
  AnchorableEvent, CoverageCell, CoverageMatrixResponse, MockAuditReport, SpringPage,
  StandardAdoption, StandardCatalogEntry
} from './standards-extras.types';

/**
 * Les trois capacités couvertes ici (§8.9, §8.4 onglet 7, §11.3) étaient exposées
 * par l'API sans aucun consommateur : ces tests figent le contrat client.
 */
describe('StandardsExtrasService', () => {
  let service: StandardsExtrasService;
  let http: HttpTestingController;

  const standards = `${environment.apiBaseUrl}/api/v1/standards`;
  const blockchain = `${environment.apiBaseUrl}/api/v1/blockchain`;
  const auditEvents = `${environment.apiBaseUrl}/api/v1/audit/events`;

  const cell = (source: string, clause: string, target: string,
                targets: CoverageCell['targets']): CoverageCell =>
    ({ sourceStandardCode: source, sourceClauseCode: clause, targetStandardCode: target, targets });

  const matrix = (over: Partial<CoverageMatrixResponse> = {}): CoverageMatrixResponse => ({
    tenantId: 't1', standardCodes: ['iso-9001', 'iso-14001'], cells: [],
    totalSourceClauses: 0, totalMappings: 0, reuseRatioPercent: 0, ...over
  });

  const page = <T>(content: T[]): SpringPage<T> =>
    ({ content, totalElements: content.length, number: 0, size: content.length });

  const report = (over: Partial<MockAuditReport> = {}): MockAuditReport => ({
    id: 'r1', adoptionId: 'a1', standardId: 's1', standardCode: 'iso-9001',
    standardName: 'Management de la qualité', readiness: 72, majorCount: 1, minorCount: 2,
    observationCount: 3, questionCount: 30, questions: [], gaps: [], remediationPlan: [],
    aiProvider: 'mistral', createdByUserId: 'u1', createdAt: '2026-07-01T10:00:00Z', ...over
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(StandardsExtrasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- §8.9 — matrice de co-couverture --------------------------------------

  it('omet le paramètre codes quand la sélection est vide (le serveur retombe sur les normes adoptées)', (done) => {
    service.coverageMatrix([]).subscribe(() => done());
    const req = http.expectOne((r: HttpRequest<unknown>) =>
      r.url === `${standards}/coverage-matrix`);
    expect(req.request.params.has('codes')).toBeFalse();
    req.flush(matrix());
  });

  it('transmet les codes sélectionnés en une liste séparée par des virgules', (done) => {
    service.coverageMatrix(['iso-9001', 'iso-14001']).subscribe(() => done());
    const req = http.expectOne((r: HttpRequest<unknown>) =>
      r.url === `${standards}/coverage-matrix`);
    expect(req.request.params.get('codes')).toBe('iso-9001,iso-14001');
    req.flush(matrix());
  });

  it('écarte les codes que le serveur rejetterait plutôt que de provoquer un 400', (done) => {
    service.coverageMatrix(['ISO 9001', 'iso-14001']).subscribe(() => done());
    const req = http.expectOne((r: HttpRequest<unknown>) =>
      r.url === `${standards}/coverage-matrix`);
    expect(req.request.params.get('codes')).toBe('iso-14001');
    req.flush(matrix());
  });

  it('pivote les cellules en lignes complètes : une cellule par colonne, trous matérialisés', (done) => {
    service.coverageOverview([]).subscribe(overview => {
      expect(overview.columns).toEqual(['iso-9001', 'iso-14001', 'iso-45001']);
      expect(overview.rows.length).toBe(1);
      const row = overview.rows[0];
      expect(row.cells.length).toBe(3);
      expect(row.cells[0].self).toBeTrue();
      expect(row.cells[1].coverages.map(c => c.clauseCode)).toEqual(['5.2']);
      expect(row.cells[2].coverages.length).toBe(0);
      done();
    });

    http.expectOne((r: HttpRequest<unknown>) => r.url === `${standards}/coverage-matrix`).flush(
      matrix({
        standardCodes: ['iso-9001', 'iso-14001', 'iso-45001'],
        cells: [cell('iso-9001', '5.2', 'iso-14001',
          [{ clauseCode: '5.2', relation: 'EQUIVALENT', confidence: 95 }])]
      }));
  });

  it('ne compte comme mutualisée qu\'une relation EQUIVALENT ou COVERS', (done) => {
    service.coverageOverview([]).subscribe(overview => {
      const [equivalent, related] = overview.rows;
      expect(equivalent.sourceClauseCode).toBe('5.2');
      expect(equivalent.sharedCount).toBe(1);
      expect(related.sourceClauseCode).toBe('7.4');
      expect(related.sharedCount).toBe(0);
      expect(overview.sharedClauseCount).toBe(1);
      done();
    });

    http.expectOne((r: HttpRequest<unknown>) => r.url === `${standards}/coverage-matrix`).flush(
      matrix({
        cells: [
          cell('iso-9001', '5.2', 'iso-14001',
            [{ clauseCode: '5.2', relation: 'EQUIVALENT', confidence: 95 }]),
          cell('iso-9001', '7.4', 'iso-14001',
            [{ clauseCode: '7.4', relation: 'RELATED', confidence: 40 }])
        ]
      }));
  });

  it('trie les clauses naturellement : 9.2 avant 10.1', (done) => {
    service.coverageOverview([]).subscribe(overview => {
      expect(overview.rows.map(r => r.sourceClauseCode)).toEqual(['9.2', '10.1']);
      done();
    });

    http.expectOne((r: HttpRequest<unknown>) => r.url === `${standards}/coverage-matrix`).flush(
      matrix({
        cells: [
          cell('iso-9001', '10.1', 'iso-14001',
            [{ clauseCode: '10.1', relation: 'COVERS', confidence: 80 }]),
          cell('iso-9001', '9.2', 'iso-14001',
            [{ clauseCode: '9.2', relation: 'COVERS', confidence: 80 }])
        ]
      }));
  });

  // ---- Normes & adoptions ----------------------------------------------------

  it('ne demande jamais plus de 100 normes : au-delà le serveur rabote en silence', (done) => {
    service.selectableStandards().subscribe(list => {
      expect(list.map(s => s.code)).toEqual(['iso-14001', 'iso-9001']);
      done();
    });
    const req = http.expectOne((r: HttpRequest<unknown>) => r.url === standards);
    expect(req.request.params.get('size')).toBe('100');
    req.flush(page<StandardCatalogEntry>([
      { id: '2', code: 'iso-14001', fullName: 'Environnement', family: 'HLS' },
      { id: '1', code: 'iso-9001', fullName: 'Qualité', family: 'HLS' }
    ]));
  });

  it('exclut du sélecteur les codes que la matrice refuserait', (done) => {
    service.selectableStandards().subscribe(list => {
      expect(list.map(s => s.code)).toEqual(['iso-9001']);
      done();
    });
    http.expectOne((r: HttpRequest<unknown>) => r.url === standards).flush(
      page<StandardCatalogEntry>([
        { id: '1', code: 'iso-9001', fullName: 'Qualité', family: 'HLS' },
        { id: '2', code: 'FDA 21 CFR 11', fullName: 'Signatures', family: null }
      ]));
  });

  it('déplie la page des adoptions du tenant', (done) => {
    service.adoptions().subscribe(list => {
      expect(list.length).toBe(1);
      expect(list[0].standardCode).toBe('iso-9001');
      done();
    });
    http.expectOne((r: HttpRequest<unknown>) => r.url === `${standards}/adoptions`).flush(
      page<StandardAdoption>([
        { id: 'a1', standardId: 's1', standardCode: 'iso-9001',
          standardName: 'Qualité', status: 'IN_PROGRESS' }
      ]));
  });

  // ---- §8.4 onglet 7 — audit blanc IA ---------------------------------------

  it('lance un audit blanc IA sur la route dédiée, distincte de l\'audit blanc « règles »', (done) => {
    service.runMockAudit('a1').subscribe(r => {
      expect(r.id).toBe('r1');
      done();
    });
    const req = http.expectOne(`${standards}/adoptions/a1/audit-blanc-ia`);
    expect(req.request.method).toBe('POST');
    req.flush(report());
  });

  it('présente l\'historique du plus récent au plus ancien', (done) => {
    service.mockAuditHistory('a1').subscribe(runs => {
      expect(runs.map(r => r.id)).toEqual(['recent', 'ancien']);
      done();
    });
    http.expectOne(`${standards}/adoptions/a1/audit-blanc-ia`).flush([
      report({ id: 'ancien', createdAt: '2026-01-01T00:00:00Z' }),
      report({ id: 'recent', createdAt: '2026-07-01T00:00:00Z' })
    ]);
  });

  it('relit une exécution précise', (done) => {
    service.mockAuditReport('a1', 'r1').subscribe(r => {
      expect(r.standardCode).toBe('iso-9001');
      done();
    });
    http.expectOne(`${standards}/adoptions/a1/audit-blanc-ia/r1`).flush(report());
  });

  // ---- §11.3 — ancrage & vérification ---------------------------------------

  it('borne la taille du lot aux limites du serveur [1, 1000]', (done) => {
    service.anchorBatch(5000).subscribe(() => done());
    const req = http.expectOne((r: HttpRequest<unknown>) =>
      r.url === `${blockchain}/anchor/run`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('batchSize')).toBe('1000');
    req.flush({
      tenantId: 't1', batchSize: 0, merkleRoot: null, blockchainTxRef: null,
      eventIds: [], firstSequenceNo: 0, lastSequenceNo: 0, anchoredAt: '2026-07-01T00:00:00Z'
    });
  });

  it('vérifie une preuve par son hash, débarrassé des espaces de copier-coller', (done) => {
    service.verifyAnchor('  abc123  ').subscribe(v => {
      expect(v.status).toBe('VERIFIED');
      done();
    });
    const req = http.expectOne((r: HttpRequest<unknown>) => r.url === `${blockchain}/verify`);
    expect(req.request.params.get('hash')).toBe('abc123');
    req.flush({ status: 'VERIFIED', detail: 'ok', txRef: 'tx-1', merkleRoot: 'root-1' });
  });

  it('ne demande jamais plus de 100 événements d\'audit', (done) => {
    service.recentAuditEvents(500).subscribe(list => {
      expect(list.length).toBe(1);
      done();
    });
    const req = http.expectOne((r: HttpRequest<unknown>) => r.url === auditEvents);
    expect(req.request.params.get('size')).toBe('100');
    req.flush(page<AnchorableEvent>([
      { id: 'e1', sequenceNo: 42, occurredAt: '2026-07-01T00:00:00Z', action: 'CAPA_CREATED',
        resourceType: 'CAPA', integrityHash: 'a'.repeat(64), blockchainTxRef: null }
    ]));
  });
});

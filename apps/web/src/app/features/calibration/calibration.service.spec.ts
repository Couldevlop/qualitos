import { HttpErrorResponse, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CalibrationService, addDays, dueStateOf, toLocalDate } from './calibration.service';
import {
  EquipmentResponse, EquipmentSummary, MsaResponse, PlanResponse, RecordResponse
} from './calibration.types';
import { SpringPage } from '../pdca/pdca.types';

/**
 * `/api/v1/calibration` (15 routes) n'avait aucun consommateur : les échéances de
 * calibration — l'information critique du module §4.10 — n'étaient consultables
 * qu'en appelant l'API à la main.
 */
describe('CalibrationService', () => {
  let service: CalibrationService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/calibration`;

  const equipment = (id: string, over: Partial<EquipmentResponse> = {}): EquipmentResponse => ({
    id, tenantId: 't1', code: 'EQ-' + id, name: 'Pied à coulisse ' + id,
    manufacturer: null, model: null, serialNumber: null, location: null,
    status: 'ACTIVE', critical: false, iotDeviceId: null, ownerUserId: null,
    createdBy: 'u1', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over
  });

  const plan = (equipmentId: string, over: Partial<PlanResponse> = {}): PlanResponse => ({
    id: 'p-' + equipmentId, tenantId: 't1', equipmentId,
    frequencyMonths: 12, procedureReference: null, tolerance: null, accreditationRef: null,
    lastCalibratedOn: null, nextDueOn: '2026-09-01', overdue: false,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over
  });

  const record = (id: string, over: Partial<RecordResponse> = {}): RecordResponse => ({
    id, tenantId: 't1', equipmentId: 'e1', performedOn: '2026-05-01',
    performedByUserId: null, performedByOrg: null, result: 'PASS',
    measurements: null, certificateReference: null, nextDueOverride: null,
    createdAt: '2026-05-01T00:00:00Z', ...over
  });

  const msa = (id: string, over: Partial<MsaResponse> = {}): MsaResponse => ({
    id, tenantId: 't1', equipmentId: 'e1', type: 'GAGE_R_R', performedOn: '2026-05-01',
    studyValue: 12.5, passingThreshold: 30, result: 'PASS', notes: null,
    createdBy: 'u1', createdAt: '2026-05-01T00:00:00Z', ...over
  });

  const summary = (over: Partial<EquipmentSummary> = {}): EquipmentSummary => ({
    equipmentId: 'e1', status: 'ACTIVE', critical: false,
    lastCalibratedOn: '2026-05-01', nextDueOn: '2027-05-01', overdue: false,
    lastResult: 'PASS', passRecords: 1, failRecords: 0, conditionalRecords: 0,
    msaPass: 1, msaFail: 0, ...over
  });

  const page = <T>(content: T[]): SpringPage<T> => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CalibrationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- Équipement ----------------------------------------------------------

  it('liste les équipements avec pagination', (done) => {
    service.listEquipment(2, 10).subscribe(p => {
      expect(p.content.length).toBe(1);
      done();
    });
    const req = http.expectOne(r => r.url === `${base}/equipment`);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([equipment('e1')]));
  });

  it('ajoute le filtre de statut seulement quand il est fourni', (done) => {
    service.listEquipment(0, 50, 'OUT_OF_SERVICE').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/equipment`);
    expect(req.request.params.get('status')).toBe('OUT_OF_SERVICE');
    req.flush(page([]));
  });

  it('lit un équipement', (done) => {
    service.getEquipment('e1').subscribe(e => {
      expect(e.id).toBe('e1');
      done();
    });
    http.expectOne(`${base}/equipment/e1`).flush(equipment('e1'));
  });

  it('crée un équipement', (done) => {
    service.createEquipment({ code: 'EQ-1', name: 'Pied', critical: true, createdBy: 'u1' })
      .subscribe(e => { expect(e.id).toBe('e1'); done(); });
    const req = http.expectOne(`${base}/equipment`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ code: 'EQ-1', name: 'Pied', critical: true, createdBy: 'u1' });
    req.flush(equipment('e1'));
  });

  it('met à jour un équipement en PATCH', (done) => {
    service.updateEquipment('e1', { name: 'Nouveau nom' }).subscribe(() => done());
    const req = http.expectOne(`${base}/equipment/e1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ name: 'Nouveau nom' });
    req.flush(equipment('e1', { name: 'Nouveau nom' }));
  });

  it('change le statut par la route dédiée', (done) => {
    service.setStatus('e1', 'RETIRED').subscribe(e => {
      expect(e.status).toBe('RETIRED');
      done();
    });
    const req = http.expectOne(`${base}/equipment/e1/status/RETIRED`);
    expect(req.request.method).toBe('POST');
    req.flush(equipment('e1', { status: 'RETIRED' }));
  });

  it('supprime un équipement', (done) => {
    service.deleteEquipment('e1').subscribe(() => done());
    const req = http.expectOne(`${base}/equipment/e1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('lit la synthèse d’un équipement', (done) => {
    service.summary('e1').subscribe(s => {
      expect(s.passRecords).toBe(1);
      done();
    });
    http.expectOne(`${base}/equipment/e1/summary`).flush(summary());
  });

  // ---- Plan ----------------------------------------------------------------

  it('lit le plan d’un équipement', (done) => {
    service.getPlan('e1').subscribe(p => {
      expect(p?.frequencyMonths).toBe(12);
      done();
    });
    http.expectOne(`${base}/equipment/e1/plan`).flush(plan('e1'));
  });

  it('traduit le 404 du plan en absence de plan, pas en erreur', (done) => {
    service.getPlan('e1').subscribe(p => {
      expect(p).toBeNull();
      done();
    });
    http.expectOne(`${base}/equipment/e1/plan`)
      .flush({}, { status: 404, statusText: 'Not Found' });
  });

  it('propage les autres erreurs du plan', (done) => {
    service.getPlan('e1').subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(500);
        done();
      }
    });
    http.expectOne(`${base}/equipment/e1/plan`)
      .flush({}, { status: 500, statusText: 'Server Error' });
  });

  it('enregistre le plan en PUT (upsert)', (done) => {
    service.upsertPlan('e1', { frequencyMonths: 6, firstDueOn: '2026-12-01' })
      .subscribe(() => done());
    const req = http.expectOne(`${base}/equipment/e1/plan`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ frequencyMonths: 6, firstDueOn: '2026-12-01' });
    req.flush(plan('e1', { frequencyMonths: 6 }));
  });

  it('supprime le plan', (done) => {
    service.deletePlan('e1').subscribe(() => done());
    const req = http.expectOne(`${base}/equipment/e1/plan`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('interroge les plans échus avec et sans date de coupure', (done) => {
    service.overduePlans().subscribe();
    const noCutoff = http.expectOne(r => r.url === `${base}/plans/overdue`);
    expect(noCutoff.request.params.has('cutoff')).toBeFalse();
    noCutoff.flush(page([]));

    service.overduePlans('2026-12-31', 1, 20).subscribe(() => done());
    const withCutoff = http.expectOne(r => r.url === `${base}/plans/overdue`);
    expect(withCutoff.request.params.get('cutoff')).toBe('2026-12-31');
    expect(withCutoff.request.params.get('page')).toBe('1');
    expect(withCutoff.request.params.get('size')).toBe('20');
    withCutoff.flush(page([]));
  });

  // ---- Enregistrements et MSA ------------------------------------------------

  it('liste et ajoute des enregistrements de calibration', (done) => {
    service.listRecords('e1').subscribe(p => expect(p.content.length).toBe(1));
    const list = http.expectOne(r => r.url === `${base}/equipment/e1/records`);
    expect(list.request.params.get('size')).toBe('100');
    list.flush(page([record('r1')]));

    service.addRecord('e1', { performedOn: '2026-06-01', result: 'FAIL' })
      .subscribe(r => { expect(r.id).toBe('r2'); done(); });
    const add = http.expectOne(r => r.url === `${base}/equipment/e1/records`
      && r.method === 'POST');
    expect(add.request.body).toEqual({ performedOn: '2026-06-01', result: 'FAIL' });
    add.flush(record('r2', { result: 'FAIL' }));
  });

  it('liste et ajoute des études MSA', (done) => {
    service.listMsa('e1', 0, 25).subscribe(p => expect(p.content.length).toBe(1));
    const list = http.expectOne(r => r.url === `${base}/equipment/e1/msa`);
    expect(list.request.params.get('size')).toBe('25');
    list.flush(page([msa('m1')]));

    service.addMsa('e1', {
      type: 'BIAS', performedOn: '2026-06-01', studyValue: 0.4,
      result: 'MARGINAL', createdBy: 'u1'
    }).subscribe(m => { expect(m.id).toBe('m2'); done(); });
    const add = http.expectOne(r => r.url === `${base}/equipment/e1/msa` && r.method === 'POST');
    expect(add.request.body.studyValue).toBe(0.4);
    add.flush(msa('m2', { type: 'BIAS', result: 'MARGINAL' }));
  });

  // ---- Vue de liste -----------------------------------------------------------

  it('joint les échéances à risque aux équipements et trie les retards en tête', (done) => {
    service.overview(null, 0, 50, 60).subscribe(o => {
      expect(o.rows.map(r => r.equipment.id)).toEqual(['e2', 'e3', 'e1']);
      expect(o.rows[0].due).toBe('OVERDUE');
      expect(o.rows[1].due).toBe('DUE_SOON');
      expect(o.rows[2].due).toBe('OK');
      expect(o.rows[2].plan).toBeNull();
      expect(o.overdueCount).toBe(1);
      expect(o.dueSoonCount).toBe(1);
      expect(o.page.totalElements).toBe(3);
      done();
    });

    const list = http.expectOne(r => r.url === `${base}/equipment`);
    expect(list.request.params.has('status')).toBeFalse();
    list.flush(page([
      equipment('e1', { code: 'A-1' }),
      equipment('e3', { code: 'C-3' }),
      equipment('e2', { code: 'B-2' })
    ]));

    const atRisk = http.expectOne(r => r.url === `${base}/plans/overdue`);
    expect(atRisk.request.params.get('size')).toBe('500');
    expect(atRisk.request.params.get('cutoff')).toBe(toLocalDate(addDays(new Date(), 60)));
    atRisk.flush(page([
      plan('e2', { overdue: true, nextDueOn: '2026-01-01' }),
      plan('e3', { overdue: false, nextDueOn: '2026-08-15' })
    ]));
  });

  it('transmet le filtre de statut de la vue de liste', (done) => {
    service.overview('RETIRED', 1, 25, 30).subscribe(o => {
      expect(o.rows).toEqual([]);
      expect(o.overdueCount).toBe(0);
      expect(o.dueSoonCount).toBe(0);
      done();
    });
    const list = http.expectOne(r => r.url === `${base}/equipment`);
    expect(list.request.params.get('status')).toBe('RETIRED');
    expect(list.request.params.get('page')).toBe('1');
    list.flush(page([]));
    http.expectOne(r => r.url === `${base}/plans/overdue`).flush(page([]));
  });

  // ---- Vue de détail -------------------------------------------------------------

  it('assemble la fiche : équipement, synthèse, plan, enregistrements et MSA', (done) => {
    service.detail('e1').subscribe(d => {
      expect(d.equipment.id).toBe('e1');
      expect(d.summary.lastResult).toBe('PASS');
      expect(d.plan?.frequencyMonths).toBe(12);
      expect(d.records.length).toBe(1);
      expect(d.msa.length).toBe(1);
      done();
    });

    http.expectOne(`${base}/equipment/e1`).flush(equipment('e1'));
    http.expectOne(`${base}/equipment/e1/summary`).flush(summary());
    http.expectOne(`${base}/equipment/e1/plan`).flush(plan('e1'));
    http.expectOne(r => r.url === `${base}/equipment/e1/records`).flush(page([record('r1')]));
    http.expectOne(r => r.url === `${base}/equipment/e1/msa`).flush(page([msa('m1')]));
  });

  it('assemble la fiche même sans plan configuré', (done) => {
    service.detail('e1').subscribe(d => {
      expect(d.plan).toBeNull();
      expect(d.records).toEqual([]);
      done();
    });

    http.expectOne(`${base}/equipment/e1`).flush(equipment('e1'));
    http.expectOne(`${base}/equipment/e1/summary`).flush(summary({ nextDueOn: null }));
    http.expectOne(`${base}/equipment/e1/plan`).flush({}, { status: 404, statusText: 'Not Found' });
    http.expectOne(r => r.url === `${base}/equipment/e1/records`).flush(page([]));
    http.expectOne(r => r.url === `${base}/equipment/e1/msa`).flush(page([]));
  });

  // ---- Fonctions pures ---------------------------------------------------------------

  it('dueStateOf s’aligne sur le drapeau serveur et non sur l’horloge du poste', () => {
    expect(dueStateOf(null)).toBe('OK');
    expect(dueStateOf(plan('e1', { overdue: true }))).toBe('OVERDUE');
    expect(dueStateOf(plan('e1', { overdue: false }))).toBe('DUE_SOON');
  });

  it('toLocalDate formate en date locale, sans bascule UTC', () => {
    // 31 décembre 23 h locales : `toISOString()` renverrait le 1er janvier
    // pour tout fuseau à l'est de Greenwich.
    expect(toLocalDate(new Date(2026, 11, 31, 23, 30))).toBe('2026-12-31');
    expect(toLocalDate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('addDays décale sans muter la date source', () => {
    const source = new Date(2026, 0, 30);
    const shifted = addDays(source, 5);
    expect(toLocalDate(shifted)).toBe('2026-02-04');
    expect(toLocalDate(source)).toBe('2026-01-30');
  });
});

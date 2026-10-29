import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { defer, of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  AssignmentResponse,
  CapabilityResponse,
  DmaicProjectResponse,
  MeasureResponse
} from '../../dmaic.types';
import { DmaicDetailComponent } from './dmaic-detail.component';
import { STUB_PERSON, provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { PersonNameModule } from '../../../../shared/person-name/person-name.module';

/** La fiche de résolution a ses propres tests : ici, seul son onglet compte. */
@Component({ selector: 'qos-dmaic-resolution', template: '<p data-test="fiche">fiche {{ projectId }} {{ projectPhase }}</p><ng-content></ng-content>', standalone: false })
class ResolutionStubComponent {
  @Input() projectId = '';
  @Input() problemStatement: string | null = null;
  @Input() projectTitle = '';
  @Input() projectPhase: string | null = null;
  @Input() spec: unknown = null;
  @Output() missingChange = new EventEmitter<string[]>();
}

/**
 * L'écran pilote les transitions du projet (avancer / pause / reprendre / annuler)
 * et la capabilité : chaque action doit être confirmée quand elle est destructrice,
 * ne rien envoyer quand elle est refusée, et recharger l'état serveur ensuite.
 */
describe('DmaicDetailComponent', () => {
  let component: DmaicDetailComponent;
  let fixture: ComponentFixture<DmaicDetailComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;
  let routeId: string;
  let routeQuery: Record<string, string>;

  const ID = 'dmaic-1';
  const base = `${environment.apiBaseUrl}/api/v1/dmaic`;

  const project = (over: Partial<DmaicProjectResponse> = {}): DmaicProjectResponse => ({
    id: ID, tenantId: 't1', title: 'Rebut ligne A',
    problemStatement: 'Rebut à 3,2 % contre 1,5 % visés.',
    goalStatement: 'Revenir à 1,2 % avant fin de trimestre.',
    phase: 'MEASURE', status: 'ACTIVE', blackBeltId: 'bb',
    specLowerLimit: 9.95, specUpperLimit: 10.05, specTarget: 10, specUnit: 'mm',
    measureCount: 0, pokaYokeCount: 0,
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z', ...over
  });

  const capability: CapabilityResponse = {
    sampleSize: 40, mean: 10.01, stdDev: 0.01, cp: 1.7, cpk: 1.45, sigmaLevel: 4.35,
    interpretation: 'Processus capable.', warnings: []
  };

  const measure = (over: Partial<MeasureResponse> = {}): MeasureResponse => ({
    id: 'm1', projectId: ID, value: 10.02, createdAt: '2026-07-02T08:00:00Z', ...over
  });

  const assignment = (over: Partial<AssignmentResponse> = {}): AssignmentResponse => ({
    id: 'as1', projectId: ID, deviceId: 'pk-1', deviceCode: 'PK-INT-001',
    deviceName: 'Verrouillage capot', deviceType: 'PREVENTION', status: 'PROPOSED',
    createdAt: '2026-07-02T08:00:00Z', updatedAt: '2026-07-02T08:00:00Z', ...over
  });

  /** Une passe de chargement = le projet, puis ses mesures, ses Poka-Yoke et sa capabilité (relus à chaque fois). */
  function flushDetail(
    p: DmaicProjectResponse = project(),
    cap: CapabilityResponse | null = capability,
    measures: MeasureResponse[] = [],
    assignments: AssignmentResponse[] = []
  ): void {
    http.expectOne(`${base}/projects/${ID}`).flush(p);
    http.expectOne(`${base}/projects/${ID}/measures`).flush(measures);
    http.expectOne(`${base}/projects/${ID}/pokayoke`).flush(assignments);
    const capReq = http.expectOne(`${base}/projects/${ID}/capability`);
    if (cap) {
      capReq.flush(cap);
    } else {
      capReq.flush({ title: 'nope' }, { status: 422, statusText: 'Unprocessable Entity' });
    }
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
    routeQuery = {};

    await TestBed.configureTestingModule({
      declarations: [DmaicDetailComponent, ResolutionStubComponent],
      imports: [PersonNameModule, SharedModule, UiModule, NoopAnimationsModule],
      providers: [provideDirectoryStub(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        // `defer` : l'identifiant est lu à l'abonnement, un test peut donc le
        // remplacer avant la première détection de changements.
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: defer(() => of(convertToParamMap({ id: routeId }))),
            get snapshot() { return { queryParamMap: convertToParamMap(routeQuery) }; }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DmaicDetailComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('affiche le titre, la phase et la capabilité du projet', () => {
    fixture.detectChanges();
    flushDetail();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Rebut ligne A');
    expect(el.querySelector('.meta-row')?.textContent).toContain('Mesurer');
    expect(el.querySelector('.cap-grid')?.textContent).toContain('40');
    expect(component.capability?.cpk).toBe(1.45);
  });

  it('nomme le Black Belt et le champion, jamais par leur identifiant (ADR 0092)', () => {
    fixture.detectChanges();
    flushDetail(project({ blackBeltId: STUB_PERSON.accountId, championId: '33333333-3333-4333-8333-333333333333' }));
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-test="dmaic-black-belt"]')?.textContent).toContain(STUB_PERSON.displayName);
    // Un compte que l'annuaire ne connaît pas se dit par un libellé neutre.
    expect(el.querySelector('[data-test="dmaic-champion"]')?.textContent).toContain('Personne inconnue');
    expect(el.textContent).not.toContain(STUB_PERSON.accountId);
    expect(el.textContent).not.toContain('33333333-3333');
  });

  it('n\'appelle pas le serveur sur un identifiant qui n\'a pas la forme attendue', (done) => {
    routeId = 'javascript:alert(1)';
    fixture.detectChanges();
    http.expectNone(() => true);

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('Identifiant');
      done();
    });
  });

  it('accepte un identifiant UUID', () => {
    routeId = 'a1b2c3d4-1111-2222-3333-444455556666';
    fixture.detectChanges();
    http.expectOne(`${base}/projects/${routeId}`).flush(project({ id: routeId }));
    http.expectOne(`${base}/projects/${routeId}/measures`).flush([]);
    http.expectOne(`${base}/projects/${routeId}/pokayoke`).flush([]);
    http.expectOne(`${base}/projects/${routeId}/capability`).flush(capability);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')).not.toBeNull();
  });

  it('affiche un message sûr quand le projet est introuvable', (done) => {
    fixture.detectChanges();
    http.expectOne(`${base}/projects/${ID}`)
      .flush({ title: 'not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    // Aucune capabilité n'est demandée pour un projet qui n'existe pas.
    http.expectNone(`${base}/projects/${ID}/capability`);
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')).toBeNull();

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('Erreur lors du chargement');
      done();
    });
  });

  it('n\'affiche aucun indicateur quand le calcul de capabilité échoue', () => {
    fixture.detectChanges();
    flushDetail(project(), null);

    expect(component.capability).toBeNull();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.cap-grid')).toBeNull();
    expect(el.querySelector('.capability-card')?.textContent).toContain('Données insuffisantes');
  });

  it('situe la phase courante dans le déroulé DMAIC', () => {
    expect(component.phaseIndex('DEFINE')).toBe(0);
    expect(component.phaseIndex('CONTROL')).toBe(4);

    fixture.detectChanges();
    flushDetail(project({ phase: 'ANALYZE' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-test="fiche"]')?.textContent).toContain('ANALYZE');
  });

  it('avance la phase puis recharge l\'état serveur', () => {
    fixture.detectChanges();
    flushDetail();

    component.advance(project());
    const req = http.expectOne(`${base}/projects/${ID}/advance`);
    expect(req.request.method).toBe('PATCH');
    req.flush(project({ phase: 'ANALYZE' }));

    flushDetail(project({ phase: 'ANALYZE' }));
    expect((fixture.nativeElement as HTMLElement).querySelector('.meta-row')?.textContent)
      .toContain('Analyser');
  });

  it('met en pause puis reprend le projet', () => {
    fixture.detectChanges();
    flushDetail();

    component.hold(project());
    http.expectOne(`${base}/projects/${ID}/hold`).flush(project({ status: 'ON_HOLD' }));
    flushDetail(project({ status: 'ON_HOLD' }));

    component.resume(project({ status: 'ON_HOLD' }));
    http.expectOne(`${base}/projects/${ID}/resume`).flush(project());
    flushDetail();
    expect((fixture.nativeElement as HTMLElement).querySelector('.meta-row')?.textContent)
      .toContain('Actif');
  });

  it('ne recharge pas et reste sur l\'état connu quand la transition est refusée', () => {
    fixture.detectChanges();
    flushDetail();

    component.advance(project({ phase: 'CONTROL' }));
    http.expectOne(`${base}/projects/${ID}/advance`)
      .flush({ title: 'invalid transition' }, { status: 409, statusText: 'Conflict' });

    http.expectNone(`${base}/projects/${ID}`);
    expect((fixture.nativeElement as HTMLElement).querySelector('.meta-row')?.textContent)
      .toContain('Mesurer');
  });

  it('n\'annule le projet qu\'après confirmation explicite', () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(false);
    component.cancel(project());
    http.expectNone(`${base}/projects/${ID}/cancel`);

    dialog.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    component.cancel(project());
    http.expectOne(`${base}/projects/${ID}/cancel`).flush(project({ status: 'CANCELLED' }));
    flushDetail(project({ status: 'CANCELLED' }));
    expect((fixture.nativeElement as HTMLElement).querySelector('.meta-row')?.textContent)
      .toContain('Annulé');
  });

  it('supprime le projet puis revient à la liste', () => {
    fixture.detectChanges();
    flushDetail();

    const nav = spyOn(TestBed.inject(Router), 'navigate');
    stubDialog(true);
    component.remove(project());

    const req = http.expectOne(`${base}/projects/${ID}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(nav).toHaveBeenCalledWith(['/dmaic']);
  });

  it('ne quitte pas l\'écran quand la suppression est refusée par le serveur', () => {
    fixture.detectChanges();
    flushDetail();

    const nav = spyOn(TestBed.inject(Router), 'navigate');
    stubDialog(true);
    component.remove(project());
    http.expectOne(`${base}/projects/${ID}`)
      .flush({ title: 'forbidden' }, { status: 403, statusText: 'Forbidden' });

    expect(nav).not.toHaveBeenCalled();
  });

  it("relit les mesures et la capabilité après l'ajout d'une mesure", () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(measure({ id: 'm-new', value: 9.99 }));
    component.openAddMeasure();
    // La capabilité dépend de l'échantillon : elle est recalculée après l'ajout.
    flushDetail(project({ measureCount: 1 }), { ...capability, sampleSize: 41 }, [measure({ id: 'm-new', value: 9.99 })]);
    expect(component.measures.map(m => m.id)).toEqual(['m-new']);
    expect(component.capability?.sampleSize).toBe(41);

    // Dialogue fermé sans saisie : aucune requête, aucun rechargement.
    dialog.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.openAddMeasure();
    http.expectNone(`${base}/projects/${ID}`);
  });

  it('affiche les mesures et les assignations lues sur le serveur', () => {
    fixture.detectChanges();
    flushDetail(project(), capability,
      [measure({ id: 'm1', value: 9.99, subgroupId: 'g1' })],
      [assignment({ note: 'pilote atelier' })]);

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.measures-card')?.textContent).toContain('9.99');
    expect(el.querySelectorAll('.empty').length).toBe(0);
  });

  it('supprime une mesure après confirmation et la retire du tableau', () => {
    fixture.detectChanges();
    flushDetail(project(), capability, [measure({ id: 'm1' }), measure({ id: 'm2', value: 10.04 })]);

    stubDialog(true);
    component.removeMeasure(measure({ id: 'm1' }));
    const req = http.expectOne(`${base}/projects/${ID}/measures/m1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(component.measures.map(m => m.id)).toEqual(['m2']);
    flushDetail(project(), capability, [measure({ id: 'm2', value: 10.04 })]);
  });

  it('conserve la mesure quand sa suppression est refusée', () => {
    fixture.detectChanges();
    flushDetail(project(), capability, [measure({ id: 'm1' })]);

    stubDialog(true);
    component.removeMeasure(measure({ id: 'm1' }));
    http.expectOne(`${base}/projects/${ID}/measures/m1`)
      .flush({ title: 'nope' }, { status: 403, statusText: 'Forbidden' });

    expect(component.measures.length).toBe(1);
    http.expectNone(`${base}/projects/${ID}`);
  });

  it('assigne un Poka-Yoke au projet et recharge ses compteurs', () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(assignment());
    component.openAddPokaYoke();
    flushDetail(project({ pokaYokeCount: 1 }), capability, [], [assignment()]);
    expect(component.assignments.map(a => a.deviceCode)).toEqual(['PK-INT-001']);

    dialog.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.openAddPokaYoke();
    http.expectNone(`${base}/projects/${ID}`);
  });

  it('détache une assignation après confirmation et la retire du tableau', () => {
    fixture.detectChanges();
    flushDetail(project(), capability, [],
      [assignment({ id: 'as1' }), assignment({ id: 'as2', deviceCode: 'PK-VIS-002' })]);

    const dialog = stubDialog(false);
    component.removeAssignment(assignment({ id: 'as1' }));
    http.expectNone(`${base}/projects/${ID}/pokayoke/as1`);
    expect(component.assignments.length).toBe(2);

    dialog.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    component.removeAssignment(assignment({ id: 'as1' }));
    const req = http.expectOne(`${base}/projects/${ID}/pokayoke/as1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(component.assignments.map(a => a.id)).toEqual(['as2']);
    flushDetail(project(), capability, [], [assignment({ id: 'as2', deviceCode: 'PK-VIS-002' })]);
  });

  it('recharge après une modification, et pas quand le dialogue est annulé', () => {
    fixture.detectChanges();
    flushDetail();

    const dialog = stubDialog(project({ title: 'Titre revu' }));
    component.openEdit(project());
    flushDetail(project({ title: 'Titre revu' }));
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Titre revu');

    dialog.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.openEdit(project());
    http.expectNone(`${base}/projects/${ID}`);
  });

  it('dérive les classes de badge de la phase, du statut et de l\'assignation', () => {
    expect(component.phaseBadge('IMPROVE')).toBe('phase phase-improve');
    expect(component.statusBadge('COMPLETED')).toBe('badge badge-completed');
    expect(component.assignmentBadge('IN_DESIGN')).toBe('assign assign-in_design');
    expect(component.assignmentBadge('VERIFIED')).toBe('assign assign-verified');
  });

  it('affiche toujours la fiche, à la phase du projet', () => {
    fixture.detectChanges();
    flushDetail(project({ phase: 'MEASURE' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-test="fiche"]')?.textContent).toContain('MEASURE');
  });

  it('bloque le passage à la phase suivante tant que la fiche est incomplète', () => {
    fixture.detectChanges();
    flushDetail();
    component.onMissing(['DEF-01']);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('[data-test="avancer"]') as HTMLButtonElement;
    expect(btn.disabled).toBeTrue();
    component.onMissing([]);
    fixture.detectChanges();
    expect(btn.disabled).toBeFalse();
  });
});

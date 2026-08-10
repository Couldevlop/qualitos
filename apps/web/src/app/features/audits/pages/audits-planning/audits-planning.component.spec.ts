import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditPlanningEntry } from '../../audits.types';
import { AuditsPlanningComponent } from './audits-planning.component';

/**
 * Planning des audits (§4.4).
 *
 * Ce qui se vérifie ici, au-delà de l'affichage : que le décompte affiché vient
 * bien du SERVEUR (jamais recalculé depuis l'horloge du poste), que le filtre de
 * type part réellement dans la requête, et qu'un échec vide la table au lieu de
 * laisser un planning périmé sous une bannière d'erreur.
 */
describe('AuditsPlanningComponent', () => {
  let component: AuditsPlanningComponent;
  let fixture: ComponentFixture<AuditsPlanningComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;
  let subs: Subscription;

  const endpoint = `${environment.apiBaseUrl}/api/v1/audits/planning`;

  class FakeConnectivity {
    private readonly subject = new Subject<boolean>();
    readonly online$ = this.subject.asObservable();
    isOnline(): boolean { return true; }
  }

  const entry = (over: Partial<AuditPlanningEntry> = {}): AuditPlanningEntry => ({
    id: 'a1',
    title: 'Audit interne ISO 9001',
    type: 'INTERNAL',
    status: 'PLANNED',
    standard: 'ISO_9001',
    leadAuditorId: 'u1',
    scheduledDate: '2026-07-15',
    daysUntil: 30,
    overdue: false,
    reminderSent: false,
    ...over
  });

  function start(): void {
    fixture.detectChanges();
    subs.add(component.entries$.subscribe());
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    // Mode connecté : seul moyen d'observer et de COMPTER les requêtes émises.
    environment.useMockApi = false;
    subs = new Subscription();

    await TestBed.configureTestingModule({
      declarations: [AuditsPlanningComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
        { provide: ConnectivityService, useClass: FakeConnectivity }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditsPlanningComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    subs.unsubscribe();
    environment.useMockApi = prevMock;
  });

  // ---- Contrat d'affichage ------------------------------------------------------

  it('affiche les colonnes attendues d’un planning', () => {
    expect(component.displayedColumns)
      .toEqual(['scheduledDate', 'countdown', 'title', 'type', 'standard', 'reminder']);
  });

  it('propose les six types, « Tous » en tête, interne et externe en premier', () => {
    expect(component.types.map(t => t.value))
      .toEqual(['', 'INTERNAL', 'EXTERNAL', 'SUPPLIER', 'LPA', 'CERTIFICATION', 'SURVEILLANCE']);
  });

  // ---- Urgence -------------------------------------------------------------------

  it('classe l’urgence sur le seuil de rappel du serveur, pas sur un seuil maison', () => {
    // 30 jours ici DOIT valoir 30 jours côté serveur : deux seuils divergents
    // afficheraient « rien ne presse » sur un audit dont le rappel vient de partir.
    expect(component.urgency(entry({ overdue: true, daysUntil: -3 }))).toBe('overdue');
    expect(component.urgency(entry({ daysUntil: 0 }))).toBe('due');
    expect(component.urgency(entry({ daysUntil: 30 }))).toBe('due');
    expect(component.urgency(entry({ daysUntil: 31 }))).toBe('soon');
    expect(component.urgency(entry({ daysUntil: 60 }))).toBe('soon');
    expect(component.urgency(entry({ daysUntil: 61 }))).toBe('later');
  });

  it('formule le décompte en clair, retard compris, sans signe négatif', () => {
    expect(component.countdownLabel(entry({ daysUntil: 0 }))).toContain('ujourd');
    expect(component.countdownLabel(entry({ daysUntil: 1 }))).toContain('emain');
    expect(component.countdownLabel(entry({ daysUntil: 12 }))).toContain('12');
    const late = component.countdownLabel(entry({ overdue: true, daysUntil: -3 }));
    expect(late).toContain('3');
    expect(late).not.toContain('-3');
    expect(component.countdownLabel(entry({ overdue: true, daysUntil: -1 }))).toContain('1');
  });

  // ---- Chargement ----------------------------------------------------------------

  it('charge l’horizon par défaut sans filtre de type', () => {
    start();

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('horizonDays')).toBe('90');
    expect(req.request.params.has('type')).toBeFalse();
    req.flush([]);
  });

  it('transmet le type choisi au serveur au lieu de filtrer côté client', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush([]);

    component.typeFilter.setValue('EXTERNAL');

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('type')).toBe('EXTERNAL');
    req.flush([]);
  });

  it('recharge une seule fois au changement d’horizon', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush([]);

    component.horizonFilter.setValue(365);

    // `expectOne` échoue s'il part DEUX requêtes : c'est la régression visée.
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('horizonDays')).toBe('365');
    req.flush([]);
  });

  it('compte les retards et les échéances proches pour l’en-tête', () => {
    start();

    http.expectOne(r => r.url === endpoint).flush([
      entry({ id: 'a1', overdue: true, daysUntil: -5 }),
      entry({ id: 'a2', daysUntil: 12 }),
      entry({ id: 'a3', daysUntil: 30 }),
      entry({ id: 'a4', daysUntil: 80 })
    ]);

    expect(component.overdueCount).toBe(1);
    expect(component.approachingCount).toBe(2);
  });

  it('vide la table quand le chargement échoue, plutôt que d’afficher un planning périmé', () => {
    const emitted: AuditPlanningEntry[][] = [];
    fixture.detectChanges();
    subs.add(component.entries$.subscribe(rows => emitted.push(rows)));

    http.expectOne(r => r.url === endpoint)
      .flush({ detail: 'SQLException at line 42' }, { status: 500, statusText: 'Server Error' });

    expect(emitted[emitted.length - 1]).toEqual([]);
    expect(component.overdueCount).toBe(0);
  });

  it('n’expose jamais le détail technique d’une erreur serveur', done => {
    fixture.detectChanges();
    subs.add(component.entries$.subscribe());
    http.expectOne(r => r.url === endpoint)
      .flush({ detail: 'SQLException at line 42' }, { status: 500, statusText: 'Server Error' });

    subs.add(component.error$.subscribe(err => {
      if (err) {
        expect(err).not.toContain('SQLException');
        done();
      }
    }));
  });

  // ---- Navigation -----------------------------------------------------------------

  it('ouvre la fiche de l’audit depuis une ligne', () => {
    const nav = spyOn(TestBed.inject(Router), 'navigate');

    component.open(entry({ id: 'a-7' }));

    expect(nav).toHaveBeenCalledWith(['/audits', 'a-7']);
  });

  it('revient à la liste complète des plans', () => {
    const nav = spyOn(TestBed.inject(Router), 'navigate');

    component.backToList();

    expect(nav).toHaveBeenCalledWith(['/audits']);
  });
});

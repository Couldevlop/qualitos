import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { filter, take } from 'rxjs/operators';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaCaseResponse } from '../../capa.types';
import { CapaListComponent } from './capa-list.component';

describe('CapaListComponent', () => {
  let component: CapaListComponent;
  let fixture: ComponentFixture<CapaListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [CapaListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('CAPA');
  });

  it('exposes the canonical CAPA statuses', () => {
    expect(component.statuses).toEqual(['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED']);
  });

  it('declares the expected displayed columns', () => {
    expect(component.displayedColumns).toEqual(['title', 'type', 'criticity', 'status', 'dueDate']);
  });

  it('computes badge classes from status and criticity', () => {
    expect(component.statusBadge('IN_PROGRESS')).toBe('badge badge-in_progress');
    expect(component.criticityBadge('CRITICAL')).toBe('crit crit-critical');
  });

  it('clamps page index and size on onPage', () => {
    component.onPage({ pageIndex: -3, pageSize: 999 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);

    component.onPage({ pageIndex: 2, pageSize: 0 } as PageEvent);
    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(1);
  });

  it('navigates to the case detail on openCase', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openCase({ id: 'capa-42' } as never);
    expect(nav).toHaveBeenCalledWith(['/capa', 'capa-42']);
  });
});

/**
 * Chargement de la liste contre l'API réelle : filtre, pagination et surtout
 * le chemin d'erreur, qui doit rester lisible sans divulguer le détail serveur
 * (OWASP A09).
 */
describe('CapaListComponent (chargement API)', () => {
  let component: CapaListComponent;
  let fixture: ComponentFixture<CapaListComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  const aCase = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: 'c1', tenantId: 't1', title: 'Cas', type: 'CORRECTIVE', criticity: 'HIGH',
    status: 'OPEN', sourceType: 'AUDIT', ownerId: 'u1',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    actions: [], ...over
  });

  const page = (content: CapaCaseResponse[], totalElements = content.length) =>
    ({ content, totalElements, totalPages: 1, number: 0, size: 20 });

  /** Le dialogue de création est remplacé : on ne teste ici que la réaction de la liste. */
  function stubDialog(result: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(result)
    } as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    await TestBed.configureTestingModule({
      declarations: [CapaListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    component.ngOnInit();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('charge la première page sans filtre au démarrage', (done) => {
    component.cases$.subscribe(list => {
      expect(list.length).toBe(1);
      expect(component.totalElements).toBe(37);
      done();
    });

    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([aCase()], 37));
  });

  it('relance la requête avec le statut choisi dans le filtre', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([aCase()]));

    component.statusFilter.setValue('REJECTED');
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('REJECTED');
    req.flush(page([]));
  });

  it('repasse sans filtre quand le statut est remis à vide', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.statusFilter.setValue('OPEN');
    http.expectOne(r => r.url === base).flush(page([]));

    component.statusFilter.setValue('');
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([]));
  });

  it('recharge la page demandée par le paginateur', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.onPage({ pageIndex: 3, pageSize: 50 } as PageEvent);
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('50');
    req.flush(page([]));
  });

  it('affiche un message générique sur erreur serveur, sans détail technique', async () => {
    component.cases$.subscribe();
    const errorPromise = firstValueFrom(component.error$.pipe(filter(Boolean), take(1)));

    http.expectOne(r => r.url === base)
      .flush({ title: 'NullPointerException at CapaService.java:42' },
             { status: 500, statusText: 'Server Error' });

    const message = await errorPromise;
    expect(message).toContain('Erreur serveur');
    expect(message).not.toContain('NullPointer');
  });

  it('signale un accès refusé sans laisser la vue en chargement', async () => {
    component.cases$.subscribe();
    const errorPromise = firstValueFrom(component.error$.pipe(filter(Boolean), take(1)));

    http.expectOne(r => r.url === base).flush({}, { status: 403, statusText: 'Forbidden' });

    expect(await errorPromise).toContain('droits');
    expect(await firstValueFrom(component.loading$)).toBeFalse();
  });

  it('revient en première page et recharge après une création', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.onPage({ pageIndex: 4, pageSize: 20 } as PageEvent);
    http.expectOne(r => r.url === base).flush(page([]));

    expect(component.pageIndex).toBe(4);

    stubDialog(aCase({ id: 'nouveau' }));
    component.openCreate();
    expect(component.pageIndex).toBe(0);

    // Retour page 0 puis rafraîchissement : switchMap annule la requête intermédiaire,
    // seule la dernière atteint le serveur.
    const pending = http.match(r => r.url === base).filter(r => !r.cancelled);
    expect(pending.length).toBe(1);
    expect(pending[0].request.params.get('page')).toBe('0');
    pending[0].flush(page([aCase({ id: 'nouveau' })]));
  });

  it('ne recharge rien quand la création est annulée', () => {
    component.cases$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    stubDialog(undefined);
    component.openCreate();
    http.expectNone(r => r.url === base);
    expect(component.pageIndex).toBe(0);
  });
});

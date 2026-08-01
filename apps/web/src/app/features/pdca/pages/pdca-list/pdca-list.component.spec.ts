import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Subscription, of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { PdcaCycleResponse, SpringPage } from '../../pdca.types';
import { PdcaListComponent } from './pdca-list.component';

describe('PdcaListComponent', () => {
  let component: PdcaListComponent;
  let fixture: ComponentFixture<PdcaListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PdcaListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        // PdcaListComponent gained Router injection in the row-click feature.
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(PdcaListComponent);
    component = fixture.componentInstance;
  });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Cycles PDCA');
  });

  it('exposes the canonical PDCA statuses', () => {
    expect(component.statuses).toEqual(['PLAN', 'DO', 'CHECK', 'ACT', 'COMPLETED', 'CANCELLED']);
  });

  it('computes badge class from status', () => {
    expect(component.statusBadgeClass('DO')).toBe('badge badge-do');
  });
});

/**
 * Le registre des cycles pilote la roue de Deming : filtre de phase, pagination
 * bornée (OWASP A03) et navigation vers la fiche doivent refléter exactement ce
 * qui est demandé au serveur.
 */
describe('PdcaListComponent — chargement, filtre et pagination', () => {
  let component: PdcaListComponent;
  let fixture: ComponentFixture<PdcaListComponent>;
  let http: HttpTestingController;
  let router: Router;
  let sub: Subscription;
  let emitted: PdcaCycleResponse[][];
  let prevMock: boolean;

  const endpoint = `${environment.apiBaseUrl}/api/v1/pdca/cycles`;

  function cycle(over: Partial<PdcaCycleResponse> = {}): PdcaCycleResponse {
    return {
      id: 'c1', tenantId: 't1', title: 'Réduction des rebuts', description: 'Ligne 3',
      status: 'DO', ownerId: 'u1', createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-02T00:00:00Z', steps: [], ...over
    };
  }

  function page(content: PdcaCycleResponse[], totalElements = content.length): SpringPage<PdcaCycleResponse> {
    return { content, totalElements, totalPages: 1, number: 0, size: 20 };
  }

  /** Monte le composant puis s'abonne au flux (la table est masquée au montage). */
  function start(): void {
    fixture.detectChanges();
    emitted = [];
    sub = component.cycles$.subscribe(rows => emitted.push(rows));
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;

    await TestBed.configureTestingModule({
      declarations: [PdcaListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PdcaListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    sub?.unsubscribe();
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('charge la première page sans filtre et mémorise le total pour le paginateur', () => {
    start();
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([cycle(), cycle({ id: 'c2' })], 7));

    expect(emitted[0].length).toBe(2);
    expect(component.totalElements).toBe(7);
  });

  it('rend une ligne par cycle avec son nombre d\'étapes', fakeAsync(() => {
    fixture.detectChanges();
    tick();                     // deferredView publie loading=false → la table se monte
    fixture.detectChanges();

    http.expectOne(r => r.url === endpoint).flush(page([
      cycle({ steps: [] }),
      cycle({ id: 'c2', title: 'MTTR P1', steps: [
        { id: 's1', cycleId: 'c2', phase: 'PLAN', title: 'Pareto', status: 'DONE',
          createdAt: '', updatedAt: '' }
      ] })
    ]));
    tick();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const rows = el.querySelectorAll('table.cycles-table tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('0');
    expect(rows[1].textContent).toContain('MTTR P1');
    expect(el.querySelector('.empty')).toBeNull();
  }));

  it('affiche l\'état vide quand aucun cycle ne correspond au filtre', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    http.expectOne(r => r.url === endpoint).flush(page([]));
    tick();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.empty')).toBeTruthy();
  }));

  it('relance une requête filtrée sur la phase choisie, puis la retire sur « Tous »', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.statusFilter.setValue('CHECK');
    let req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('status')).toBe('CHECK');
    req.flush(page([cycle({ status: 'CHECK' })]));

    component.statusFilter.setValue('');
    req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([]));
  });

  it('borne la pagination demandée au serveur (page négative, taille excessive)', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.onPage({ pageIndex: -1, pageSize: 10000, length: 0 } as PageEvent);

    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('size')).toBe('100');
    req.flush(page([]));
  });

  it('relève une taille de page nulle à 1 plutôt que d\'envoyer size=0', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.onPage({ pageIndex: 1, pageSize: 0, length: 0 } as PageEvent);

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('size')).toBe('1');
    expect(req.request.params.get('page')).toBe('1');
    req.flush(page([]));
  });

  it('publie un message d\'erreur sûr et masque la table quand le chargement échoue', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    http.expectOne(r => r.url === endpoint).flush(
      { detail: 'java.sql.SQLException: relation pdca_cycle does not exist' },
      { status: 500, statusText: 'Server Error' });
    tick();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const banner = el.querySelector('.state-row.error');
    expect(banner?.textContent).toContain('Erreur serveur — réessayez dans un instant.');
    expect(banner?.textContent).not.toContain('SQLException');
    expect(el.querySelector('table.cycles-table')).toBeNull();
  }));

  it('ouvre la fiche du cycle cliqué', () => {
    start();
    component.openCycle(cycle({ id: 'c9' }));
    expect(router.navigate).toHaveBeenCalledWith(['/pdca', 'c9']);
    http.expectOne(r => r.url === endpoint).flush(page([]));
  });

  it('après création, revient à la première page et recharge la liste', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));
    component.onPage({ pageIndex: 2, pageSize: 20, length: 60 } as PageEvent);
    http.expectOne(r => r.url === endpoint).flush(page([]));

    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(cycle({ id: 'new' })) } as never);
    component.openCreate();

    expect(component.pageIndex).toBe(0);
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    req.flush(page([cycle({ id: 'new' })]));
    expect(emitted[emitted.length - 1][0].id).toBe('new');
  });

  it('ne recharge pas la liste quand la création est abandonnée', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.openCreate();

    http.expectNone(r => r.url === endpoint);
  });
});

import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Subscription, of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { InMemoryQueueStore, OfflineQueueStore } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcPage, NcResponse } from '../../nc.types';
import { NcListComponent } from './nc-list.component';

describe('NcListComponent', () => {
  let component: NcListComponent;
  let fixture: ComponentFixture<NcListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [NcListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NcListComponent);
    component = fixture.componentInstance;
  });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Non-conformités');
  });

  it('exposes the canonical NC statuses', () => {
    expect(component.statuses)
      .toEqual(['OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'CLOSED', 'CANCELLED']);
  });

  it('ne propose pas RESOLVED au filtre : c\'est un etat de passage', () => {
    // Une NC resolue est cloturee dans la foulee ; proposer ce statut renvoyait
    // presque toujours une liste vide. Les NC resolues se lisent sous CLOSED.
    expect(component.statuses).not.toContain('RESOLVED');
  });

  it('affiche la colonne Detecte par apres le statut', () => {
    expect(component.displayedColumns).toEqual([
      'reference', 'title', 'category', 'severity', 'status', 'reporter', 'detectedAt'
    ]);
  });

  it('exposes the canonical NC severities and categories', () => {
    expect(component.severities).toEqual(['MINOR', 'MAJOR', 'CRITICAL']);
    expect(component.categories).toContain('SAFETY');
    expect(component.categories.length).toBe(7);
  });

  it('computes badge classes from status and severity', () => {
    expect(component.statusBadgeClass('UNDER_ANALYSIS')).toBe('badge badge-under_analysis');
    expect(component.severityBadgeClass('CRITICAL')).toBe('sev sev-critical');
  });
});

/**
 * Le registre NC est le point d'entrée terrain : filtres, pagination bornée
 * (OWASP A03) et lignes cliquables doivent rester cohérents avec ce que le
 * serveur reçoit réellement.
 */
describe('NcListComponent — chargement, filtres et pagination', () => {
  let component: NcListComponent;
  let fixture: ComponentFixture<NcListComponent>;
  let http: HttpTestingController;
  let router: Router;
  let sub: Subscription;
  let emitted: NcResponse[][];
  let prevMock: boolean;

  const endpoint = `${environment.apiBaseUrl}/api/v1/nc`;

  function nc(over: Partial<NcResponse> = {}): NcResponse {
    return {
      id: 'a1', reference: 'NC-2026-1001', title: 'Étiquetage manquant',
      category: 'PROCESS', severity: 'MAJOR', status: 'OPEN', origin: 'INTERNAL',
      detectedAt: '2026-07-01T00:00:00Z', createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-01T00:00:00Z', ...over
    };
  }

  function page(content: NcResponse[], totalElements = content.length): NcPage {
    return { content, totalElements, totalPages: 1, number: 0, size: 20 };
  }

  /** Monte le composant puis s'abonne au flux (la table est masquée au montage). */
  function start(): void {
    fixture.detectChanges();
    emitted = [];
    sub = component.ncs$.subscribe(rows => emitted.push(rows));
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;

    await TestBed.configureTestingModule({
      declarations: [NcListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NcListComponent);
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
    req.flush(page([nc(), nc({ id: 'a2', reference: 'NC-2026-1002' })], 42));

    expect(emitted[0].length).toBe(2);
    expect(component.totalElements).toBe(42);
  });

  it('rend une ligne par non-conformité une fois la page chargée', fakeAsync(() => {
    fixture.detectChanges();
    tick();                     // deferredView publie loading=false → la table se monte
    fixture.detectChanges();

    http.expectOne(r => r.url === endpoint)
      .flush(page([nc(), nc({ id: 'a2', reference: 'NC-2026-1002', pendingSync: true })]));
    tick();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('table.t tbody tr').length).toBe(2);
    expect(el.textContent).toContain('NC-2026-1001');
    // la ligne non synchronisée est signalée et neutralisée
    expect(el.querySelectorAll('.pending').length).toBe(1);
    expect(el.querySelectorAll('tr.row-disabled').length).toBe(1);
    expect(el.querySelector('.empty')).toBeNull();
  }));

  it('rend une colonne « Détecté par » nommant l\'auteur du signalement', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    http.expectOne(r => r.url === endpoint).flush(page([
      nc({ reporterName: 'Amina Dridi', reporterId: 'u-42' }),
      nc({ id: 'a2', reference: 'NC-2026-1002' })
    ]));
    tick();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const entetes = Array.from(el.querySelectorAll('table.t th'))
      .map(th => (th.textContent ?? '').trim());
    expect(entetes).toContain('Détecté par');

    const index = component.displayedColumns.indexOf('reporter');
    const lignes = Array.from(el.querySelectorAll('table.t tbody tr'));
    const cellule = (i: number) =>
      (lignes[i].querySelectorAll('td')[index].textContent ?? '').trim();

    expect(cellule(0)).toBe('Amina Dridi');
    // Signalement antérieur à cette donnée : un tiret, jamais l'UUID du compte,
    // qui ne désignerait personne dans une liste.
    expect(cellule(1)).toBe('—');
    expect(el.textContent).not.toContain('u-42');
  }));

  it('affiche l\'état vide quand aucune NC ne correspond au filtre', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    http.expectOne(r => r.url === endpoint).flush(page([]));
    tick();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('table.t tbody tr').length).toBe(0);
    expect(el.querySelector('.empty')).toBeTruthy();
  }));

  it('relance une requête filtrée à chaque changement de filtre', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.statusFilter.setValue('OPEN');
    let req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('status')).toBe('OPEN');
    req.flush(page([nc()]));

    component.severityFilter.setValue('CRITICAL');
    req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('status')).toBe('OPEN');
    expect(req.request.params.get('severity')).toBe('CRITICAL');
    req.flush(page([]));

    component.categoryFilter.setValue('SAFETY');
    req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('category')).toBe('SAFETY');
    req.flush(page([]));
  });

  it('revenir à « Tous » retire le filtre de la requête', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.statusFilter.setValue('CLOSED');
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.statusFilter.setValue('');
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([]));
  });

  it('borne la pagination demandée au serveur (page négative, taille excessive)', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.onPage({ pageIndex: -3, pageSize: 5000, length: 0 } as PageEvent);

    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('100');
    req.flush(page([]));
  });

  it('respecte une pagination légitime', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.onPage({ pageIndex: 2, pageSize: 50, length: 120 } as PageEvent);

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('50');
    req.flush(page([]));
  });

  it('publie un message d\'erreur sûr et vide la liste quand le chargement échoue', fakeAsync(() => {
    start();
    const seen: (string | null)[] = [];
    component.error$.subscribe(m => seen.push(m));

    http.expectOne(r => r.url === endpoint)
      .flush({ detail: 'org.postgresql.util.PSQLException' },
        { status: 500, statusText: 'Server Error' });
    tick();

    // La liste est vidée plutôt que laissée en l'état : ne rien émettre
    // laissait les lignes PRÉCÉDENTES à l'écran, périmées, sous la bannière
    // d'erreur. Le gabarit masque par ailleurs la table tant qu'une erreur est
    // affichée, donc aucune mention « aucun résultat » ne vient tromper.
    expect(emitted[emitted.length - 1]).toEqual([]);
    expect(component.totalElements).toBe(0);
    const last = seen[seen.length - 1];
    expect(last).toBe('Erreur serveur — réessayez dans un instant.');
    expect(last).not.toContain('PSQL');             // OWASP A09 : pas de détail technique
  }));

  it('remet le bandeau d\'erreur à zéro avant chaque nouveau chargement', fakeAsync(() => {
    start();
    const seen: (string | null)[] = [];
    component.error$.subscribe(m => seen.push(m));

    http.expectOne(r => r.url === endpoint).flush({}, { status: 500, statusText: 'Server Error' });
    tick();
    expect(seen[seen.length - 1]).toBeTruthy();

    component.statusFilter.setValue('OPEN');
    http.expectOne(r => r.url === endpoint).flush(page([nc()]));
    tick();
    expect(seen[seen.length - 1]).toBeNull();
  }));

  it('ouvre la fiche d\'une NC synchronisée', () => {
    start();
    component.openNc(nc({ id: 'a9' }));
    expect(router.navigate).toHaveBeenCalledWith(['/nc', 'a9']);
    http.expectOne(r => r.url === endpoint).flush(page([]));
  });

  it('n\'ouvre pas une NC encore en attente de synchronisation (pas d\'id serveur)', () => {
    start();
    component.openNc(nc({ id: 'offline-1', pendingSync: true }));
    expect(router.navigate).not.toHaveBeenCalled();
    http.expectOne(r => r.url === endpoint).flush(page([]));
  });

  it('après création, revient à la première page et recharge la liste', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));
    component.onPage({ pageIndex: 3, pageSize: 20, length: 100 } as PageEvent);
    http.expectOne(r => r.url === endpoint).flush(page([]));

    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(nc({ id: 'new' })) } as never);
    component.openCreate();

    expect(component.pageIndex).toBe(0);
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    req.flush(page([nc({ id: 'new' })]));
    expect(emitted[emitted.length - 1][0].id).toBe('new');
  });

  it('ne recharge pas la liste quand la déclaration est abandonnée', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.openCreate();

    http.expectNone(r => r.url === endpoint);
  });
});

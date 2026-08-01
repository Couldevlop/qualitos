import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { Subscription, of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CircleResponse } from '../../circles.types';
import { CirclesListComponent } from './circles-list.component';

/**
 * Liste des cercles de qualité (§3.3).
 *
 * Le point vérifié au-delà de l'affichage est le nombre de requêtes : la liste
 * est alimentée par un `combineLatest` dont CHAQUE source déclenche un
 * rechargement. Pousser deux sources pour une seule création partirait en double
 * appel, avec une course dont la réponse la plus ancienne peut sortir gagnante.
 */
describe('CirclesListComponent', () => {
  let component: CirclesListComponent;
  let fixture: ComponentFixture<CirclesListComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;
  let subs: Subscription;

  const endpoint = `${environment.apiBaseUrl}/api/v1/circles`;

  const page = (content: Partial<CircleResponse>[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  /** La liste n'est chargée que si quelqu'un souscrit : le template le fait via `async`. */
  function start(): void {
    fixture.detectChanges();
    subs.add(component.circles$.subscribe());
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    // Mode connecté : c'est le seul moyen de COMPTER les requêtes émises.
    environment.useMockApi = false;
    subs = new Subscription();

    await TestBed.configureTestingModule({
      declarations: [CirclesListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CirclesListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    subs.unsubscribe();
    environment.useMockApi = prevMock;
  });

  // ---- Contrat d'affichage -----------------------------------------------------

  it('expose les statuts et colonnes du référentiel', () => {
    expect(component.statuses).toEqual(['ACTIVE', 'PAUSED', 'ARCHIVED']);
    expect(component.displayedColumns)
      .toEqual(['name', 'status', 'members', 'meetings', 'proposals', 'updatedAt']);
  });

  it('dérive la classe de pastille du statut', () => {
    expect(component.badge('PAUSED')).toBe('badge badge-paused');
    expect(component.badge('ACTIVE')).toBe('badge badge-active');
  });

  it('navigue vers la fiche du cercle ouvert', () => {
    const nav = spyOn(TestBed.inject(Router), 'navigate');

    component.openCircle({ id: 'circ-5' } as CircleResponse);

    expect(nav).toHaveBeenCalledWith(['/circles', 'circ-5']);
  });

  // ---- Chargement --------------------------------------------------------------

  it('charge la première page et retient le total', () => {
    start();

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    req.flush(page([{ id: 'c1' }, { id: 'c2' }]));

    expect(component.totalElements).toBe(2);
  });

  it('affiche un message sûr et vide la liste quand le chargement échoue', () => {
    const emitted: CircleResponse[][] = [];
    fixture.detectChanges();
    subs.add(component.circles$.subscribe(rows => emitted.push(rows)));

    http.expectOne(r => r.url === endpoint)
      .flush({ detail: 'SQLException at line 42' }, { status: 500, statusText: 'Server Error' });

    // Le détail technique ne doit pas fuiter vers l'utilisateur.
    expect(emitted[emitted.length - 1]).toEqual([]);
  });

  // ---- Pagination ----------------------------------------------------------------

  it('borne la pagination côté client, le serveur revalidant de son côté', () => {
    component.onPage({ pageIndex: -1, pageSize: 9999 } as PageEvent);

    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('recharge une seule fois au changement de page', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));

    component.onPage({ pageIndex: 2, pageSize: 20 } as PageEvent);

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('2');
    req.flush(page([]));
  });

  // ---- Création ------------------------------------------------------------------

  it('après création, revient à la première page et ne recharge QU\'UNE fois', () => {
    start();
    http.expectOne(r => r.url === endpoint).flush(page([]));
    component.onPage({ pageIndex: 3, pageSize: 20 } as PageEvent);
    http.expectOne(r => r.url === endpoint).flush(page([]));

    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of({ id: 'nouveau' }) } as never);
    component.openCreate();

    expect(component.pageIndex).toBe(0);
    // `expectOne` échoue s'il y a DEUX requêtes : c'est exactement la
    // régression que ce test empêche.
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    req.flush(page([{ id: 'nouveau' }]));
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

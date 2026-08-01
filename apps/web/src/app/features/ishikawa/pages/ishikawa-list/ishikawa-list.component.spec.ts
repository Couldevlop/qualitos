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
import { IshikawaDiagramResponse } from '../../ishikawa.types';
import { IshikawaListComponent } from './ishikawa-list.component';

/**
 * Liste des diagrammes d'Ishikawa (§3.5).
 *
 * Le point vérifié au-delà de l'affichage est le nombre de requêtes : la liste
 * est alimentée par un `combineLatest` dont CHAQUE source déclenche un
 * rechargement. Pousser deux sources pour une seule création partirait en double
 * appel, avec une course dont la réponse la plus ancienne peut sortir gagnante.
 */
describe('IshikawaListComponent', () => {
  let component: IshikawaListComponent;
  let fixture: ComponentFixture<IshikawaListComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;
  let subs: Subscription;

  const endpoint = `${environment.apiBaseUrl}/api/v1/ishikawa/diagrams`;

  const page = (content: Partial<IshikawaDiagramResponse>[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  /** La liste n'est chargée que si quelqu'un souscrit : le template le fait via `async`. */
  function start(): void {
    fixture.detectChanges();
    subs.add(component.diagrams$.subscribe());
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    // Mode connecté : c'est le seul moyen de COMPTER les requêtes émises.
    environment.useMockApi = false;
    subs = new Subscription();

    await TestBed.configureTestingModule({
      declarations: [IshikawaListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IshikawaListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    subs.unsubscribe();
    environment.useMockApi = prevMock;
  });

  // ---- Contrat d'affichage -----------------------------------------------------

  it('expose les statuts et colonnes du référentiel', () => {
    expect(component.statuses).toEqual(['DRAFT', 'IN_REVIEW', 'VALIDATED', 'ARCHIVED']);
    expect(component.displayedColumns)
      .toEqual(['problem', 'mode', 'status', 'causes', 'updatedAt']);
  });

  it('dérive la classe de pastille du statut', () => {
    expect(component.badgeClass('IN_REVIEW')).toBe('badge badge-in_review');
    expect(component.badgeClass('VALIDATED')).toBe('badge badge-validated');
  });

  it('navigue vers la fiche du diagramme ouvert', () => {
    const nav = spyOn(TestBed.inject(Router), 'navigate');

    component.openDiagram({ id: 'ish-5' } as IshikawaDiagramResponse);

    expect(nav).toHaveBeenCalledWith(['/ishikawa', 'ish-5']);
  });

  // ---- Chargement --------------------------------------------------------------

  it('charge la première page et retient le total', () => {
    start();

    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    req.flush(page([{ id: 'i1' }, { id: 'i2' }]));

    expect(component.totalElements).toBe(2);
  });

  it('vide la liste quand le chargement échoue', () => {
    const emitted: IshikawaDiagramResponse[][] = [];
    fixture.detectChanges();
    subs.add(component.diagrams$.subscribe(rows => emitted.push(rows)));

    http.expectOne(r => r.url === endpoint)
      .flush({ detail: 'SQLException at line 42' }, { status: 500, statusText: 'Server Error' });

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

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
import { DocumentResponse } from '../../documents.types';
import { DocumentsListComponent } from './documents-list.component';

function doc(overrides: Partial<DocumentResponse> = {}): DocumentResponse {
  return {
    id: 'doc-1', tenantId: 't', code: 'PR-001', title: 'Procédure', type: 'PROCEDURE',
    status: 'ACTIVE', mandatoryRead: false, currentVersionId: 'v2',
    versions: [
      { id: 'v1', versionNumber: 1, status: 'SUPERSEDED' } as never,
      { id: 'v2', versionNumber: 2, status: 'APPROVED' } as never
    ],
    createdAt: '2026-06-01T00:00:00Z', updatedAt: '2026-06-01T00:00:00Z',
    ...overrides
  } as DocumentResponse;
}

describe('DocumentsListComponent', () => {
  let component: DocumentsListComponent;
  let fixture: ComponentFixture<DocumentsListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [DocumentsListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing and defaults the filter to ACTIVE', () => {
    fixture.detectChanges();
    expect(component.statusFilter.value).toBe('ACTIVE');
    expect(component.statuses).toEqual(['ACTIVE', 'ARCHIVED']);
  });

  it('declares the expected displayed columns', () => {
    expect(component.displayedColumns)
      .toEqual(['code', 'title', 'type', 'currentVersion', 'status', 'mandatoryRead']);
  });

  it('resolves the current version from currentVersionId', () => {
    const v = component.currentVersion(doc());
    expect(v.number).toBe(2);
    expect(v.status).toBe('APPROVED');
  });

  it('falls back to the last version when currentVersionId is unknown', () => {
    const v = component.currentVersion(doc({ currentVersionId: 'missing' }));
    expect(v.number).toBe(2);
  });

  it('builds badge classes for version, status and type', () => {
    expect(component.versionBadge('APPROVED')).toBe('vbadge vbadge-approved');
    expect(component.versionBadge(undefined)).toBe('');
    expect(component.statusBadge('ARCHIVED')).toBe('badge badge-archived');
    expect(component.typeBadge('PROCEDURE')).toBe('tbadge tbadge-procedure');
  });

  it('clamps pagination bounds on onPage', () => {
    component.onPage({ pageIndex: -1, pageSize: 5000 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the document detail on open', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.open(doc({ id: 'doc-9' }));
    expect(nav).toHaveBeenCalledWith(['/documents', 'doc-9']);
  });

  it('n\'affiche aucune version quand le document n\'en a encore aucune', () => {
    const v = component.currentVersion(doc({ versions: [], currentVersionId: undefined }));
    expect(v.number).toBeUndefined();
    expect(v.status).toBeUndefined();
    expect(component.versionBadge(v.status)).toBe('');
  });
});

/**
 * Chargement contre l'API réelle : le filtre par défaut (ACTIVE) masque les
 * documents archivés — un archivé qui réapparaîtrait dans la liste courante
 * serait une régression réglementaire (§4.1).
 */
describe('DocumentsListComponent (chargement API)', () => {
  let component: DocumentsListComponent;
  let fixture: ComponentFixture<DocumentsListComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/documents`;

  const page = (content: DocumentResponse[], totalElements = content.length) =>
    ({ content, totalElements, totalPages: 1, number: 0, size: 20 });

  function stubDialog(result: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(result)
    } as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    await TestBed.configureTestingModule({
      declarations: [DocumentsListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    component.ngOnInit();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('demande d\'emblée les seuls documents actifs', (done) => {
    component.documents$.subscribe(list => {
      expect(list.length).toBe(1);
      expect(component.totalElements).toBe(12);
      done();
    });

    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(page([doc()], 12));
  });

  it('bascule sur les archives quand le filtre change', () => {
    component.documents$.subscribe();
    http.expectOne(r => r.url === base).flush(page([doc()]));

    component.statusFilter.setValue('ARCHIVED');
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('ARCHIVED');
    req.flush(page([doc({ status: 'ARCHIVED' })]));
  });

  it('supprime le filtre de statut quand on demande tout le référentiel', () => {
    component.documents$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.statusFilter.setValue('');
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([]));
  });

  it('recharge la page demandée par le paginateur', () => {
    component.documents$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.onPage({ pageIndex: 2, pageSize: 50 } as PageEvent);
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('50');
    req.flush(page([]));
  });

  it('remonte un message générique sur erreur serveur sans exposer la trace', async () => {
    component.documents$.subscribe();
    const errorPromise = firstValueFrom(component.error$.pipe(filter(Boolean), take(1)));

    http.expectOne(r => r.url === base)
      .flush({ detail: 'constraint uk_documents_code violated' },
             { status: 500, statusText: 'Server Error' });

    const message = await errorPromise;
    expect(message).toContain('Erreur serveur');
    expect(message).not.toContain('constraint');
    expect(await firstValueFrom(component.loading$)).toBeFalse();
  });

  it('revient en première page et recharge après une création', () => {
    component.documents$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    component.onPage({ pageIndex: 3, pageSize: 20 } as PageEvent);
    http.expectOne(r => r.url === base).flush(page([]));

    stubDialog(doc({ id: 'doc-neuf' }));
    component.openCreate();
    expect(component.pageIndex).toBe(0);

    const pending = http.match(r => r.url === base).filter(r => !r.cancelled);
    expect(pending.length).toBe(1);
    expect(pending[0].request.params.get('page')).toBe('0');
    pending[0].flush(page([doc({ id: 'doc-neuf' })]));
  });

  it('ne recharge rien quand la création est annulée', () => {
    component.documents$.subscribe();
    http.expectOne(r => r.url === base).flush(page([]));

    stubDialog(undefined);
    component.openCreate();
    http.expectNone(r => r.url === base);
  });
});

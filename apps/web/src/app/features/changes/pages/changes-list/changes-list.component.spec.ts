import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ChangeRequestType, ChangeResponse } from '../../changes.types';
import { ChangesListComponent } from './changes-list.component';

describe('ChangesListComponent', () => {
  let component: ChangesListComponent;
  let fixture: ComponentFixture<ChangesListComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/changes`;

  const change = (over: Partial<ChangeResponse> = {}): ChangeResponse => ({
    id: 'chg-1', tenantId: 't1', code: 'CHG-2026-014',
    title: 'Procédure stérilisation autoclave 4',
    type: 'DOCUMENT', priority: 'HIGH', status: 'UNDER_REVIEW',
    requesterUserId: 'u1', createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z', ...over
  });

  function flushList(content: ChangeResponse[] = [change()]): void {
    http.expectOne(r => r.url === base)
      .flush({ content, totalElements: content.length, totalPages: 1, number: 0, size: 20 });
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
    await TestBed.configureTestingModule({
      declarations: [ChangesListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ChangesListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('expose les sept statuts et les sept types du référentiel', () => {
    expect(component.statuses).toEqual([
      'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'IMPLEMENTED', 'CANCELLED'
    ]);
    expect(component.types).toEqual([
      'DOCUMENT', 'PROCESS', 'EQUIPMENT', 'SUPPLIER', 'IT_SYSTEM', 'ORGANIZATIONAL', 'OTHER'
    ]);
    expect(component.displayedColumns)
      .toEqual(['code', 'title', 'type', 'priority', 'status', 'plannedFor']);
  });

  it('traduit chaque type sans laisser de libellé technique à l\'écran', () => {
    const labels = component.types.map(t => component.typeLabel(t));
    expect(labels).toEqual([
      'Document', 'Processus', 'Équipement', 'Fournisseur', 'Système IT', 'Organisationnel', 'Autre'
    ]);
    // Aucun libellé ne doit rester en SCREAMING_SNAKE_CASE.
    expect(labels.some(l => l.includes('_'))).toBeFalse();
  });

  it('dérive les classes de badge du statut, de la priorité et du type', () => {
    expect(component.statusBadge('UNDER_REVIEW')).toBe('badge badge-under_review');
    expect(component.priorityBadge('CRITICAL')).toBe('prio prio-critical');
    expect(component.typeBadge('IT_SYSTEM')).toBe('tbadge tbadge-it_system');
  });

  it('borne la pagination reçue du paginator', () => {
    component.onPage({ pageIndex: -3, pageSize: 0 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(1);
  });

  it('ouvre le détail de la demande cliquée', () => {
    const nav = spyOn(TestBed.inject(Router), 'navigate');
    component.open(change({ id: 'chg-42' }));
    expect(nav).toHaveBeenCalledWith(['/changes', 'chg-42']);
  });

  it('demande la première page sans filtre et affiche les demandes reçues', () => {
    fixture.detectChanges();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('type')).toBeFalse();
    req.flush({
      content: [change(), change({ id: 'chg-2', code: 'CHG-2026-015', title: 'Migration LMS' })],
      totalElements: 12, totalPages: 1, number: 0, size: 20
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.textContent).toContain('CHG-2026-014');
    expect(component.totalElements).toBe(12);
  });

  it('rejoue le filtrage côté serveur pour le statut puis le type', () => {
    fixture.detectChanges();
    flushList();

    component.statusFilter.setValue('APPROVED');
    const byStatus = http.expectOne(r => r.url === base);
    expect(byStatus.request.params.get('status')).toBe('APPROVED');
    expect(byStatus.request.params.has('type')).toBeFalse();
    byStatus.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    component.typeFilter.setValue('SUPPLIER');
    const byBoth = http.expectOne(r => r.url === base);
    expect(byBoth.request.params.get('status')).toBe('APPROVED');
    expect(byBoth.request.params.get('type')).toBe('SUPPLIER');
    byBoth.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    component.statusFilter.setValue('');
    const typeOnly = http.expectOne(r => r.url === base);
    expect(typeOnly.request.params.has('status')).toBeFalse();
    expect(typeOnly.request.params.get('type')).toBe('SUPPLIER');
    typeOnly.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('recharge avec les bornes appliquées quand le paginator change', () => {
    fixture.detectChanges();
    flushList();

    component.onPage({ pageIndex: 4, pageSize: 250 } as PageEvent);
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('4');
    expect(req.request.params.get('size')).toBe('100');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 4, size: 100 });
  });

  it('affiche un message sûr et garde la table vide quand le serveur refuse l\'accès', (done) => {
    fixture.detectChanges();
    http.expectOne(r => r.url === base)
      .flush({ title: 'forbidden' }, { status: 403, statusText: 'Forbidden' });

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('droits');
      fixture.detectChanges();
      expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(0);
      done();
    });
  });

  it('revient en première page après une création, et ne recharge pas si le dialogue est annulé', () => {
    fixture.detectChanges();
    flushList();

    component.onPage({ pageIndex: 2, pageSize: 20 } as PageEvent);
    http.expectOne(r => r.url === base)
      .flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 20 });

    const dialog = stubDialog(change({ id: 'chg-9' }));
    component.openCreate();

    // Remise à zéro de la page ET rafraîchissement : la requête utile est la dernière.
    const pending = http.match(r => r.url === base);
    const reloaded = pending[pending.length - 1];
    expect(reloaded.request.params.get('page')).toBe('0');
    expect(component.pageIndex).toBe(0);
    reloaded.flush({ content: [change({ id: 'chg-9' })], totalElements: 1, totalPages: 1, number: 0, size: 20 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(1);

    dialog.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.openCreate();
    http.expectNone(r => r.url === base);
  });

  it('affiche le libellé traduit du type dans la colonne dédiée', () => {
    fixture.detectChanges();
    flushList([change({ type: 'IT_SYSTEM' as ChangeRequestType })]);
    expect((fixture.nativeElement as HTMLElement).querySelector('tbody tr')?.textContent)
      .toContain('Système IT');
  });
});

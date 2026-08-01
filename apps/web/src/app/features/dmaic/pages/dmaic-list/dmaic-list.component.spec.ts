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
import { DmaicProjectResponse } from '../../dmaic.types';
import { DmaicListComponent } from './dmaic-list.component';

describe('DmaicListComponent', () => {
  let component: DmaicListComponent;
  let fixture: ComponentFixture<DmaicListComponent>;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/dmaic/projects`;

  const project = (over: Partial<DmaicProjectResponse> = {}): DmaicProjectResponse => ({
    id: 'dm-1', tenantId: 't1', title: 'Rebut ligne A',
    phase: 'MEASURE', status: 'ACTIVE', blackBeltId: 'bb',
    measureCount: 3, pokaYokeCount: 1,
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z', ...over
  });

  /** Une passe de chargement : intercepte la requête de liste et rend le résultat. */
  function flushList(content: DmaicProjectResponse[] = [project()]): void {
    const req = http.expectOne(r => r.url === base);
    req.flush({ content, totalElements: content.length, totalPages: 1, number: 0, size: 20 });
    fixture.detectChanges();
  }

  function stubDialog(result: unknown): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(result)
    } as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    // Les tests exercent le chemin API réel : c'est celui qui tourne chez un tenant.
    environment.useMockApi = false;
    await TestBed.configureTestingModule({
      declarations: [DmaicListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DmaicListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('renders without throwing', () => {
    fixture.detectChanges();
    flushList();
    expect(component).toBeTruthy();
  });

  it('exposes the canonical statuses and the five DMAIC phases', () => {
    expect(component.statuses).toEqual(['ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED']);
    expect(component.phases).toEqual(['DEFINE', 'MEASURE', 'ANALYZE', 'IMPROVE', 'CONTROL']);
  });

  it('declares the expected displayed columns', () => {
    expect(component.displayedColumns)
      .toEqual(['title', 'phase', 'status', 'measureCount', 'pokaYokeCount', 'targetCompletionDate']);
  });

  it('builds phase and status badge classes', () => {
    expect(component.phaseBadge('ANALYZE')).toBe('phase phase-analyze');
    expect(component.statusBadge('ON_HOLD')).toBe('badge badge-on_hold');
  });

  it('clamps pagination on onPage', () => {
    component.onPage({ pageIndex: -5, pageSize: 1000 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the project detail on open', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.open({ id: 'dm-3' } as never);
    expect(nav).toHaveBeenCalledWith(['/dmaic', 'dm-3']);
  });

  it('demande la première page sans filtre et affiche les projets reçus', () => {
    fixture.detectChanges();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('phase')).toBeFalse();
    req.flush({
      content: [project(), project({ id: 'dm-2', title: 'Temps de cycle' })],
      totalElements: 7, totalPages: 4, number: 0, size: 20
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.textContent).toContain('Rebut ligne A');
    // Le total vient du serveur, pas du nombre de lignes de la page courante.
    expect(component.totalElements).toBe(7);
  });

  it('rejoue le filtrage côté serveur quand statut puis phase sont choisis', () => {
    fixture.detectChanges();
    flushList();

    component.statusFilter.setValue('COMPLETED');
    const filtered = http.expectOne(r => r.url === base);
    expect(filtered.request.params.get('status')).toBe('COMPLETED');
    expect(filtered.request.params.has('phase')).toBeFalse();
    filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    component.phaseFilter.setValue('CONTROL');
    const both = http.expectOne(r => r.url === base);
    expect(both.request.params.get('status')).toBe('COMPLETED');
    expect(both.request.params.get('phase')).toBe('CONTROL');
    both.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('revient à « tous » quand le filtre est remis à vide', () => {
    fixture.detectChanges();
    flushList();

    component.statusFilter.setValue('ACTIVE');
    http.expectOne(r => r.url === base)
      .flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    component.statusFilter.setValue('');
    const cleared = http.expectOne(r => r.url === base);
    expect(cleared.request.params.has('status')).toBeFalse();
    cleared.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('recharge avec les bornes appliquées quand le paginator change', () => {
    fixture.detectChanges();
    flushList();

    component.onPage({ pageIndex: 3, pageSize: 500 } as PageEvent);
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('100');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 3, size: 100 });
  });

  it('affiche un message sûr et garde la table vide quand le serveur échoue', (done) => {
    fixture.detectChanges();
    http.expectOne(r => r.url === base)
      .flush({ title: 'boom' }, { status: 500, statusText: 'Server Error' });

    component.error$.subscribe(err => {
      if (!err) return;
      expect(err).toContain('Erreur serveur');
      fixture.detectChanges();
      expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(0);
      done();
    });
  });

  it('recharge la première page après une création, et rien si le dialogue est annulé', () => {
    fixture.detectChanges();
    flushList();

    component.onPage({ pageIndex: 2, pageSize: 20 } as PageEvent);
    http.expectOne(r => r.url === base)
      .flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 20 });

    const dialog = stubDialog(project({ id: 'dm-9' }));
    component.openCreate();
    expect(dialog).toHaveBeenCalled();

    // Un projet créé apparaît en tête de liste : on revient à la page 1.
    // (le composant émet la remise à zéro de page ET le rafraîchissement, d'où
    // une requête annulée en amont de celle qui sert réellement la vue.)
    const pending = http.match(r => r.url === base);
    const reloaded = pending[pending.length - 1];
    expect(reloaded.request.params.get('page')).toBe('0');
    expect(component.pageIndex).toBe(0);
    reloaded.flush({ content: [project({ id: 'dm-9' })], totalElements: 1, totalPages: 1, number: 0, size: 20 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(1);

    dialog.and.returnValue({ afterClosed: () => of(undefined) } as MatDialogRef<unknown>);
    component.openCreate();
    http.expectNone(r => r.url === base);
  });
});

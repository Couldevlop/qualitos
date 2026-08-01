import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { KpisService } from '../../kpis.service';
import { KpiResponse } from '../../kpis.types';
import { KpisListComponent } from './kpis-list.component';

describe('KpisListComponent', () => {
  let component: KpisListComponent;
  let fixture: ComponentFixture<KpisListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [KpisListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(KpisListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing and defaults the status filter to ACTIVE', () => {
    fixture.detectChanges();
    expect(component.statusFilter.value).toBe('ACTIVE');
    expect(component.statuses).toEqual(['DRAFT', 'ACTIVE', 'ARCHIVED']);
  });

  it('declares the expected displayed columns', () => {
    expect(component.displayedColumns)
      .toEqual(['code', 'name', 'category', 'direction', 'target', 'frequency', 'status']);
  });

  it('renders a direction label that distinguishes higher vs lower is better', () => {
    expect(component.directionLabel('HIGHER_IS_BETTER')).not.toBe(component.directionLabel('LOWER_IS_BETTER'));
  });

  it('maps direction to up/down badge', () => {
    expect(component.directionBadge('HIGHER_IS_BETTER')).toBe('dir dir-up');
    expect(component.directionBadge('LOWER_IS_BETTER')).toBe('dir dir-down');
  });

  it('builds the status badge class', () => {
    expect(component.statusBadge('ARCHIVED')).toBe('badge badge-archived');
  });

  it('clamps pagination on onPage', () => {
    component.onPage({ pageIndex: -4, pageSize: 600 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the KPI detail on open', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.open({ id: 'kpi-3' } as never);
    expect(nav).toHaveBeenCalledWith(['/kpis', 'kpi-3']);
  });
});

/**
 * Le service est ici un double : c'est le seul moyen d'exercer les filtres
 * réellement transmis au serveur et les états d'erreur du catalogue.
 */
describe('KpisListComponent (service doublé)', () => {
  let fixture: ComponentFixture<KpisListComponent>;
  let component: KpisListComponent;
  let svc: jasmine.SpyObj<KpisService>;
  /** Valeur renvoyée par le dialogue de création. */
  let dialogResult: unknown;

  const kpi = (over: Partial<KpiResponse> = {}): KpiResponse => ({
    id: 'kpi-1', tenantId: 't1', code: 'first-pass-yield', name: 'First Pass Yield',
    description: 'Bons du premier coup', category: 'Qualité', unit: '%',
    direction: 'HIGHER_IS_BETTER', frequency: 'WEEKLY', targetValue: 98,
    status: 'ACTIVE', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  function build(): void {
    fixture = TestBed.createComponent(KpisListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** `deferredView` livre ses états via `asyncScheduler` : un tour de boucle suffit. */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<KpisService>('KpisService', ['list']);
    svc.list.and.returnValue(of(page([kpi()])));
    dialogResult = { id: 'kpi-2' };

    await TestBed.configureTestingModule({
      declarations: [KpisListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: KpisService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        provideRouter([])
      ]
    }).compileComponents();
  });

  it('charge le catalogue actif et alimente le tableau', () => {
    build();
    expect(svc.list).toHaveBeenCalledWith(0, 20, 'ACTIVE', undefined);
    expect(component.totalElements).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('First Pass Yield');
  });

  it('transmet le statut choisi et l’omet pour « Tous »', () => {
    build();
    component.statusFilter.setValue('ARCHIVED');
    expect(svc.list).toHaveBeenCalledWith(0, 20, 'ARCHIVED', undefined);

    component.statusFilter.setValue('');
    expect(svc.list).toHaveBeenCalledWith(0, 20, undefined, undefined);
  });

  it('nettoie la catégorie saisie et ignore une saisie blanche', () => {
    build();
    component.categoryFilter.setValue('  CAPA ');
    expect(svc.list).toHaveBeenCalledWith(0, 20, 'ACTIVE', 'CAPA');

    component.categoryFilter.setValue('   ');
    expect(svc.list).toHaveBeenCalledWith(0, 20, 'ACTIVE', undefined);
  });

  it('recharge la page demandée après un changement de pagination', () => {
    build();
    component.onPage({ pageIndex: 2, pageSize: 50 } as PageEvent);
    expect(svc.list).toHaveBeenCalledWith(2, 50, 'ACTIVE', undefined);
  });

  it('revient à la première page après une création confirmée', () => {
    build();
    component.onPage({ pageIndex: 3, pageSize: 20 } as PageEvent);
    const before = svc.list.calls.count();
    component.openCreate();
    expect(component.pageIndex).toBe(0);
    expect(svc.list.calls.count()).toBeGreaterThan(before);
    expect(svc.list.calls.mostRecent().args[0]).toBe(0);
  });

  it('ne recharge rien quand la création est annulée', () => {
    build();
    dialogResult = undefined;
    const before = svc.list.calls.count();
    component.openCreate();
    expect(svc.list.calls.count()).toBe(before);
  });

  it('affiche un bandeau et vide le tableau quand le catalogue échoue', async () => {
    svc.list.and.returnValue(throwError(() => ({ status: 500 })));
    build();
    await settle();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.banner-error')?.textContent).toContain('Erreur serveur');
    expect(el.querySelectorAll('tbody tr').length).toBe(0);
  });

  it('retire l’indicateur de chargement une fois la réponse reçue', async () => {
    build();
    await settle();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-info')).toBeNull();
  });
});

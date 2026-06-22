import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
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

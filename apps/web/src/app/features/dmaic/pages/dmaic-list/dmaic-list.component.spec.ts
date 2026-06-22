import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DmaicListComponent } from './dmaic-list.component';

describe('DmaicListComponent', () => {
  let component: DmaicListComponent;
  let fixture: ComponentFixture<DmaicListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
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
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing', () => {
    fixture.detectChanges();
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
});

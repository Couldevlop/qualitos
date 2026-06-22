import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsListComponent } from './audits-list.component';

describe('AuditsListComponent', () => {
  let component: AuditsListComponent;
  let fixture: ComponentFixture<AuditsListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [AuditsListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(AuditsListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('exposes the canonical audit statuses and columns', () => {
    expect(component.statuses).toEqual(['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']);
    expect(component.displayedColumns)
      .toEqual(['title', 'type', 'standard', 'status', 'score', 'scheduledDate']);
  });

  it('builds the status badge class', () => {
    expect(component.statusBadge('COMPLETED')).toBe('badge badge-completed');
  });

  it('maps the score to high/mid/low buckets and a neutral class when missing', () => {
    expect(component.scoreClass(undefined)).toBe('score');
    expect(component.scoreClass(90)).toBe('score score-high');
    expect(component.scoreClass(85)).toBe('score score-high');
    expect(component.scoreClass(75)).toBe('score score-mid');
    expect(component.scoreClass(70)).toBe('score score-mid');
    expect(component.scoreClass(50)).toBe('score score-low');
  });

  it('clamps pagination on onPage', () => {
    component.onPage({ pageIndex: -2, pageSize: 250 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the plan detail on openPlan', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openPlan({ id: 'aud-7' } as never);
    expect(nav).toHaveBeenCalledWith(['/audits', 'aud-7']);
  });
});

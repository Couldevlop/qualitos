import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FivesListComponent } from './fives-list.component';

describe('FivesListComponent', () => {
  let component: FivesListComponent;
  let fixture: ComponentFixture<FivesListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [FivesListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(FivesListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('exposes the canonical 5S audit statuses and columns', () => {
    expect(component.statuses).toEqual(['DRAFT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']);
    expect(component.displayedColumns).toEqual(['zone', 'status', 'score', 'scheduledAt', 'updatedAt']);
  });

  it('builds the status badge class', () => {
    expect(component.badgeClass('IN_PROGRESS')).toBe('badge badge-in_progress');
  });

  it('maps the 5S score to high/mid/low buckets', () => {
    expect(component.scoreClass(undefined)).toBe('score');
    expect(component.scoreClass(80)).toBe('score score-high');
    expect(component.scoreClass(79)).toBe('score score-mid');
    expect(component.scoreClass(60)).toBe('score score-mid');
    expect(component.scoreClass(59)).toBe('score score-low');
  });

  it('clamps pagination on onPage', () => {
    component.onPage({ pageIndex: -1, pageSize: 777 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the audit detail on openAudit', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openAudit({ id: '5s-8' } as never);
    expect(nav).toHaveBeenCalledWith(['/fives', '5s-8']);
  });
});

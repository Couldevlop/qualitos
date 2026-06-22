import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CirclesListComponent } from './circles-list.component';

describe('CirclesListComponent', () => {
  let component: CirclesListComponent;
  let fixture: ComponentFixture<CirclesListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
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
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('exposes the canonical circle statuses and columns', () => {
    expect(component.statuses).toEqual(['ACTIVE', 'PAUSED', 'ARCHIVED']);
    expect(component.displayedColumns)
      .toEqual(['name', 'status', 'members', 'meetings', 'proposals', 'updatedAt']);
  });

  it('builds the status badge class', () => {
    expect(component.badge('PAUSED')).toBe('badge badge-paused');
  });

  it('clamps pagination on onPage', () => {
    component.onPage({ pageIndex: -1, pageSize: 9999 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the circle detail on openCircle', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openCircle({ id: 'circ-5' } as never);
    expect(nav).toHaveBeenCalledWith(['/circles', 'circ-5']);
  });
});

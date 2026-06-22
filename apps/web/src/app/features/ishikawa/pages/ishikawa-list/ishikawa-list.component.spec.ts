import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IshikawaListComponent } from './ishikawa-list.component';

describe('IshikawaListComponent', () => {
  let component: IshikawaListComponent;
  let fixture: ComponentFixture<IshikawaListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
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
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders without throwing', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('exposes the canonical statuses and columns', () => {
    expect(component.statuses).toEqual(['DRAFT', 'IN_REVIEW', 'VALIDATED', 'ARCHIVED']);
    expect(component.displayedColumns).toEqual(['problem', 'mode', 'status', 'causes', 'updatedAt']);
  });

  it('builds the status badge class', () => {
    expect(component.badgeClass('VALIDATED')).toBe('badge badge-validated');
    expect(component.badgeClass('IN_REVIEW')).toBe('badge badge-in_review');
  });

  it('clamps pagination on onPage', () => {
    component.onPage({ pageIndex: -2, pageSize: 500 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);
  });

  it('navigates to the diagram detail on openDiagram', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openDiagram({ id: 'ish-2' } as never);
    expect(nav).toHaveBeenCalledWith(['/ishikawa', 'ish-2']);
  });
});

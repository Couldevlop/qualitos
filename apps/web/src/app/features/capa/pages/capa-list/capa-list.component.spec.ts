import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaListComponent } from './capa-list.component';

describe('CapaListComponent', () => {
  let component: CapaListComponent;
  let fixture: ComponentFixture<CapaListComponent>;
  let prevMock: boolean;

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    await TestBed.configureTestingModule({
      declarations: [CapaListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CapaListComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('CAPA');
  });

  it('exposes the canonical CAPA statuses', () => {
    expect(component.statuses).toEqual(['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED']);
  });

  it('declares the expected displayed columns', () => {
    expect(component.displayedColumns).toEqual(['title', 'type', 'criticity', 'status', 'dueDate']);
  });

  it('computes badge classes from status and criticity', () => {
    expect(component.statusBadge('IN_PROGRESS')).toBe('badge badge-in_progress');
    expect(component.criticityBadge('CRITICAL')).toBe('crit crit-critical');
  });

  it('clamps page index and size on onPage', () => {
    component.onPage({ pageIndex: -3, pageSize: 999 } as PageEvent);
    expect(component.pageIndex).toBe(0);
    expect(component.pageSize).toBe(100);

    component.onPage({ pageIndex: 2, pageSize: 0 } as PageEvent);
    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(1);
  });

  it('navigates to the case detail on openCase', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.openCase({ id: 'capa-42' } as never);
    expect(nav).toHaveBeenCalledWith(['/capa', 'capa-42']);
  });
});

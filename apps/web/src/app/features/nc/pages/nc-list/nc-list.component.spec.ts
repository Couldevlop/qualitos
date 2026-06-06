import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { SharedModule } from '../../../../shared/shared.module';
import { NcListComponent } from './nc-list.component';

describe('NcListComponent', () => {
  let component: NcListComponent;
  let fixture: ComponentFixture<NcListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [NcListComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NcListComponent);
    component = fixture.componentInstance;
  });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Non-conformités');
  });

  it('exposes the canonical NC statuses', () => {
    expect(component.statuses).toEqual(['OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'RESOLVED', 'CLOSED', 'CANCELLED']);
  });

  it('exposes the canonical NC severities and categories', () => {
    expect(component.severities).toEqual(['MINOR', 'MAJOR', 'CRITICAL']);
    expect(component.categories).toContain('SAFETY');
    expect(component.categories.length).toBe(7);
  });

  it('computes badge classes from status and severity', () => {
    expect(component.statusBadgeClass('UNDER_ANALYSIS')).toBe('badge badge-under_analysis');
    expect(component.severityBadgeClass('CRITICAL')).toBe('sev sev-critical');
  });
});

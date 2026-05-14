import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { PdcaListComponent } from './pdca-list.component';

describe('PdcaListComponent', () => {
  let component: PdcaListComponent;
  let fixture: ComponentFixture<PdcaListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PdcaListComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(PdcaListComponent);
    component = fixture.componentInstance;
  });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Cycles PDCA');
  });

  it('exposes the canonical PDCA statuses', () => {
    expect(component.statuses).toEqual(['PLAN', 'DO', 'CHECK', 'ACT', 'COMPLETED', 'CANCELLED']);
  });

  it('computes badge class from status', () => {
    expect(component.statusBadgeClass('DO')).toBe('badge badge-do');
  });
});

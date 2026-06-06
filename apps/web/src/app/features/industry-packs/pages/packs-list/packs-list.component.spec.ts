import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { IndustryPacksService } from '../../industry-packs.service';
import { ActivationResponse, PacksPage } from '../../industry-packs.types';
import { PacksListComponent } from './packs-list.component';

describe('PacksListComponent', () => {
  let component: PacksListComponent;
  let fixture: ComponentFixture<PacksListComponent>;
  let svc: jasmine.SpyObj<IndustryPacksService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const user: AuthUser = {
    userId: 'u-1', tenantId: 't-1', displayName: 'Demo', roles: ['admin']
  };

  const richManifest = JSON.stringify({
    sectors: ['construction'], standards: ['iso-19650', 'iso-9001'],
    richKpis: [{ kpiId: 'k1', name: 'KPI 1' }],
    ishikawaTemplates: [{ problemArchetype: 'P', branches: { man: ['c'] } }],
    pokaYokeLibrary: [{ id: 'pk', name: 'PK' }],
    glossary: { BIM: 'def' }
  });

  const page: PacksPage = {
    content: [
      { id: '1', code: 'construction', name: 'BTP & Construction', version: '1.0.0', tags: ['btp'], manifestJson: richManifest },
      { id: '2', code: 'manufacturing', name: 'Industrie', version: '1.0.0', tags: ['shop-floor'], manifestJson: JSON.stringify({ kpis: ['oee'] }) }
    ],
    totalElements: 2, totalPages: 1, number: 0, size: 50
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj<IndustryPacksService>('IndustryPacksService',
      ['list', 'myActivations', 'activate', 'deactivate']);
    // parseManifest is used directly from the real service; we provide a real one.
    const real = new IndustryPacksService(null as never);
    svc.list.and.returnValue(of(page));
    svc.myActivations.and.returnValue(of([
      { id: 'a1', tenantId: 't-1', packCode: 'manufacturing', status: 'ACTIVE' } as ActivationResponse
    ]));
    (svc as unknown as { parseManifest: typeof real.parseManifest }).parseManifest =
      real.parseManifest.bind(real);

    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [PacksListComponent],
      imports: [SharedModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: IndustryPacksService, useValue: svc },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MatSnackBar, useValue: snack },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PacksListComponent);
    component = fixture.componentInstance;
  });

  it('renders one premium card per pack', () => {
    fixture.detectChanges();
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('.pack-card');
    expect(cards.length).toBe(2);
  });

  it('crosses /my activations to mark the active pack with a badge', () => {
    fixture.detectChanges();
    const active = component.views.find(v => v.pack.code === 'manufacturing');
    expect(active?.active).toBeTrue();
    const inactive = component.views.find(v => v.pack.code === 'construction');
    expect(inactive?.active).toBeFalse();

    const badges = (fixture.nativeElement as HTMLElement).querySelectorAll('.badge.active');
    expect(badges.length).toBe(1);
  });

  it('shows rich counters for rich packs', () => {
    fixture.detectChanges();
    const rich = component.views.find(v => v.pack.code === 'construction');
    expect(rich?.manifest.rich).toBeTrue();
    expect(rich?.manifest.kpis.length).toBe(1);
    expect(rich?.manifest.standards.length).toBe(2);
    expect(rich?.manifest.ishikawaTemplates.length).toBe(1);
  });

  it('filters cards by free-text query', () => {
    fixture.detectChanges();
    component.query = 'construction';
    component.applyFilter();
    expect(component.filtered.length).toBe(1);
    expect(component.filtered[0].pack.code).toBe('construction');
  });

  it('activates an inactive pack and shows provisioning counters in the snackbar', () => {
    svc.activate.and.returnValue(of({
      id: 'a2', tenantId: 't-1', packCode: 'construction', status: 'ACTIVE',
      kpisCreated: 7, kpisSkipped: 2
    } as ActivationResponse));
    fixture.detectChanges();

    const target = component.views.find(v => v.pack.code === 'construction')!;
    component.activate(target, new Event('click'));

    expect(svc.activate).toHaveBeenCalledWith('construction', { activatedBy: 'u-1' });
    expect(target.active).toBeTrue();
    expect(snack.open).toHaveBeenCalled();
    const message = snack.open.calls.mostRecent().args[0] as string;
    expect(message).toContain('7');
    expect(message).toContain('2');
  });

  it('hides management buttons when the user has no admin role', () => {
    TestBed.resetTestingModule();
    // canManage is computed in ngOnInit from roles — verify the gating logic.
    const c = Object.create(PacksListComponent.prototype) as PacksListComponent;
    const auth = { snapshot: () => ({ ...user, roles: ['quality_manager'] }) } as unknown as AuthService;
    (c as unknown as { auth: AuthService }).auth = auth;
    (c as unknown as { computeRole: () => void })['computeRole']();
    expect(c.canManage).toBeFalse();
  });
});

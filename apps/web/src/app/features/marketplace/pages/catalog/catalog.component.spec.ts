import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { MarketplaceService } from '../../marketplace.service';
import { InstallationView, MarketplacePackView } from '../../marketplace.types';
import { CatalogComponent } from './catalog.component';

describe('CatalogComponent', () => {
  let component: CatalogComponent;
  let fixture: ComponentFixture<CatalogComponent>;
  let svc: jasmine.SpyObj<MarketplaceService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const user: AuthUser = { userId: 'u-1', tenantId: 't-1', displayName: 'D', roles: ['admin_tenant'] };

  const packs: MarketplacePackView[] = [
    { id: 'p1', packId: 'iso-13485', version: '1.0', publisher: 'Acme', title: 'Pack MedTech',
      description: 'd', sector: 'healthcare', norms: ['iso-13485'], priceCents: 12000,
      currency: 'EUR', status: 'PUBLISHED', manifestUrl: 'https://x/y', ratingAvg: 4, ratingCount: 3 },
    { id: 'p2', packId: 'haccp', version: '2.0', publisher: 'Foo', title: 'Pack HACCP',
      sector: 'agro', norms: ['iso-22000'], priceCents: 0, currency: 'EUR',
      status: 'PUBLISHED', manifestUrl: 'https://x/z', ratingAvg: 0, ratingCount: 0 }
  ];
  const mine: InstallationView[] = [
    { id: 'i1', tenantId: 't-1', marketplacePackId: 'p2', packId: 'haccp',
      packVersion: '2.0', status: 'INSTALLED' }
  ];

  beforeEach(async () => {
    svc = jasmine.createSpyObj<MarketplaceService>('MarketplaceService',
      ['listPublished', 'myInstallations', 'install', 'uninstall', 'rate']);
    svc.listPublished.and.returnValue(of(packs));
    svc.myInstallations.and.returnValue(of(mine));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CatalogComponent],
      imports: [SharedModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: MarketplaceService, useValue: svc },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(true) }) } },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    component = fixture.componentInstance;
  });

  it('renders one card per published pack', () => {
    fixture.detectChanges();
    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('.pack-card');
    expect(cards.length).toBe(2);
  });

  it('marks installed pack with a badge from /my', () => {
    fixture.detectChanges();
    const installed = component.entries.find(e => e.pack.id === 'p2');
    expect(installed?.installation).toBeTruthy();
    const badges = (fixture.nativeElement as HTMLElement).querySelectorAll('.badge.installed');
    expect(badges.length).toBe(1);
  });

  it('shows "Gratuit" for a zero-price pack', () => {
    fixture.detectChanges();
    const free = component.entries.find(e => e.pack.id === 'p2')!;
    expect(component.priceLabel(free)).toBe('Gratuit');
    const paid = component.entries.find(e => e.pack.id === 'p1')!;
    expect(component.priceLabel(paid)).toContain('120.00');
  });

  it('filters by free-text query', () => {
    fixture.detectChanges();
    component.query = 'medtech';
    component.applyFilter();
    expect(component.filtered.length).toBe(1);
    expect(component.filtered[0].pack.id).toBe('p1');
  });

  it('installs an uninstalled pack', () => {
    svc.install.and.returnValue(of(
      { id: 'i2', tenantId: 't-1', marketplacePackId: 'p1', packId: 'iso-13485',
        packVersion: '1.0', status: 'INSTALLED' }));
    fixture.detectChanges();
    const target = component.entries.find(e => e.pack.id === 'p1')!;
    component.install(target, new Event('click'));
    expect(svc.install).toHaveBeenCalledWith('p1');
    expect(target.installation).toBeTruthy();
    expect(snack.open).toHaveBeenCalled();
  });

  it('uninstalls an installed pack after confirm', () => {
    svc.uninstall.and.returnValue(of(
      { ...mine[0], status: 'UNINSTALLED' }));
    fixture.detectChanges();
    const target = component.entries.find(e => e.pack.id === 'p2')!;
    component.uninstall(target, new Event('click'));
    expect(svc.uninstall).toHaveBeenCalledWith('i1');
    expect(target.installation).toBeUndefined();
  });

  it('rates an installed pack', () => {
    svc.rate.and.returnValue(of({ ...packs[1], ratingAvg: 5, ratingCount: 1 }));
    fixture.detectChanges();
    const target = component.entries.find(e => e.pack.id === 'p2')!;
    component.rate(target, 5, new Event('click'));
    expect(svc.rate).toHaveBeenCalledWith('p2', 5);
    expect(target.pack.ratingCount).toBe(1);
  });

  it('hides management buttons for a non-admin role', () => {
    const c = Object.create(CatalogComponent.prototype) as CatalogComponent;
    const auth = { snapshot: () => ({ ...user, roles: ['quality_manager'] }) } as unknown as AuthService;
    (c as unknown as { auth: AuthService }).auth = auth;
    (c as unknown as { computeRole: () => void })['computeRole']();
    expect(c.canManage).toBeFalse();
  });
});

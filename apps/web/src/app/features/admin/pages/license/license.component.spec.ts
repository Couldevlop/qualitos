import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { EditionService } from '../../../../core/edition/edition.service';
import { EditionView, SAAS } from '../../../../core/edition/edition.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TenantModulesService } from '../../tenant-modules.service';
import { LicenseComponent } from './license.component';

describe('LicenseComponent', () => {

  let fixture: ComponentFixture<LicenseComponent>;
  let component: LicenseComponent;

  const q = (test: string): HTMLElement | null =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-test="${test}"]`);

  const onprem = (o: Partial<EditionView> = {}): EditionView => ({
    ...SAAS, edition: 'ONPREM', licenseStatus: 'VALID', customer: 'Hôpital Saint-Louis', licenseId: 'LIC-2026-001',
    tier: 'ENTERPRISE', modules: ['dmaic', 'standards'], maxUsers: 40, activeUsers: 37,
    expiresAt: '2027-10-01T00:00:00Z', graceEndsAt: '2027-10-31T00:00:00Z', daysLeft: 300, ...o
  });

  async function setup(view: EditionView, catalogueEchoue = false): Promise<void> {
    const edition = jasmine.createSpyObj<EditionService>('EditionService', ['current']);
    edition.current.and.returnValue(of(view));
    const modules = jasmine.createSpyObj<TenantModulesService>('TenantModulesService', ['catalog']);
    modules.catalog.and.returnValue(catalogueEchoue ? throwError(() => new Error('503')) : of([
      { code: 'dmaic', name: 'DMAIC + Poka-Yoke', category: 'methodes', minimumTier: 'STANDARD', dependencies: [], coreModule: false },
      { code: 'standards', name: 'Standards Hub', category: 'transverse', minimumTier: 'PRO', dependencies: [], coreModule: false }
    ]));
    await TestBed.configureTestingModule({
      declarations: [LicenseComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [provideRouter([]), { provide: EditionService, useValue: edition },
        { provide: TenantModulesService, useValue: modules }]
    }).compileComponents();
    fixture = TestBed.createComponent(LicenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('en SaaS, dit que la licence ne s’applique pas', async () => {
    await setup(SAAS);
    expect(q('saas')).not.toBeNull();
    expect(q('etat')).toBeNull();
  });

  it('montre le client, l’état, les places occupées et les modules nommés', async () => {
    await setup(onprem());
    expect(q('etat')!.textContent).toContain('Valide');
    expect(q('utilisateurs')!.textContent).toContain('37');
    expect(q('utilisateurs')!.textContent).toContain('40');
    expect(q('modules')!.textContent).toContain('DMAIC + Poka-Yoke');
    expect(component.usage(onprem())).toBe(93);
  });

  it('une licence tous modules et sans plafond le dit simplement', async () => {
    await setup(onprem({ modules: ['*'], maxUsers: 0 }));
    expect(q('tous')).not.toBeNull();
    expect(q('illimite')).not.toBeNull();
    expect(component.usage(onprem({ maxUsers: 0 }))).toBe(0);
  });

  it('une licence absente dit pourquoi l’installation est en lecture seule', async () => {
    await setup(onprem({ licenseStatus: 'MISSING', licenseId: null, customer: null, reason: 'Aucune licence installée.' }));
    expect(q('raison')!.textContent).toContain('Aucune licence');
    expect(q('etat')!.textContent).toContain('Absente');
  });

  it('les états et leurs tons', async () => {
    await setup(SAAS);
    const etats = ['VALID', 'GRACE', 'EXPIRED', 'NOT_YET_VALID', 'INVALID', 'MISSING', 'NOT_REQUIRED'] as const;
    expect(etats.map(s => component.statusText(s)).every(t => t.length > 0)).toBeTrue();
    expect(component.tone('VALID')).toBe('ok');
    expect(component.tone('GRACE')).toBe('warning');
    expect(component.tone('EXPIRED')).toBe('blocked');
  });

  it('sans catalogue, les modules gardent leur code', async () => {
    await setup(onprem(), true);
    expect(q('modules')!.textContent).toContain('dmaic');
  });
});

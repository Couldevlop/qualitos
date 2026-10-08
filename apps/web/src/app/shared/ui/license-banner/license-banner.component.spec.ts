import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { EditionService } from '../../../core/edition/edition.service';
import { EditionView, SAAS } from '../../../core/edition/edition.types';
import { UiModule } from '../ui.module';
import { LicenseBannerComponent, noticeFor } from './license-banner.component';

describe('LicenseBannerComponent', () => {

  const date = (iso: string | null) => iso ?? '';
  const onprem = (o: Partial<EditionView>): EditionView => ({
    ...SAAS, edition: 'ONPREM', licenseStatus: 'VALID', expiresAt: '2027-10-01', graceEndsAt: '2027-10-31',
    daysLeft: 200, ...o
  });

  it('se tait en SaaS et pendant la vie d’une licence', () => {
    expect(noticeFor(SAAS, date)).toBeNull();
    expect(noticeFor(onprem({ daysLeft: 200 }), date)).toBeNull();
    expect(noticeFor(onprem({ daysLeft: null }), date)).toBeNull();
    expect(noticeFor(onprem({ licenseStatus: 'NOT_REQUIRED' }), date)).toBeNull();
  });

  it('prévient un mois avant l’échéance, puis pendant la grâce', () => {
    expect(noticeFor(onprem({ daysLeft: 12 }), date)!.tone).toBe('info');
    const grace = noticeFor(onprem({ licenseStatus: 'GRACE' }), date)!;
    expect(grace.tone).toBe('warning');
    expect(grace.message).toContain('2027-10-31');
  });

  it('dit la lecture seule, quelle qu’en soit la cause', () => {
    for (const s of ['EXPIRED', 'NOT_YET_VALID', 'MISSING', 'INVALID'] as const) {
      const n = noticeFor(onprem({ licenseStatus: s }), date)!;
      expect(n.tone).withContext(s).toBe('blocked');
      expect(n.message).withContext(s).toContain('lecture seule');
    }
  });

  describe('dans la page', () => {
    let fixture: ComponentFixture<LicenseBannerComponent>;

    async function setup(view: EditionView): Promise<void> {
      const edition = jasmine.createSpyObj<EditionService>('EditionService', ['current']);
      edition.current.and.returnValue(of(view));
      await TestBed.configureTestingModule({
        imports: [UiModule],
        providers: [provideRouter([]), { provide: EditionService, useValue: edition }]
      }).compileComponents();
      fixture = TestBed.createComponent(LicenseBannerComponent);
      fixture.detectChanges();
    }

    const bandeau = () => (fixture.nativeElement as HTMLElement).querySelector('[data-test="license-banner"]');

    it('rien en SaaS', async () => {
      await setup(SAAS);
      expect(bandeau()).toBeNull();
    });

    it('une alerte quand l’installation est en lecture seule', async () => {
      await setup(onprem({ licenseStatus: 'MISSING' }));
      expect(bandeau()!.getAttribute('role')).toBe('alert');
      expect(bandeau()!.textContent).toContain('Voir la licence');
    });

    it('un statut discret pour l’échéance qui approche', async () => {
      await setup(onprem({ daysLeft: 5 }));
      expect(bandeau()!.getAttribute('role')).toBe('status');
    });
  });
});

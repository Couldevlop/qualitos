import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ModuleRow } from '../../admin.types';
import { TenantModulesService } from '../../tenant-modules.service';
import { TenantModulesComponent } from './tenant-modules.component';

/**
 * Qui décide du périmètre. Le socle standard est acquis d'office ; au-delà, c'est
 * l'éditeur de la plateforme qui ouvre les modules, pas le client. L'écran doit
 * donc rester CONSULTABLE par l'administrateur du tenant — c'est ainsi qu'il sait
 * de quoi il dispose — mais ne lui proposer aucune action : afficher des boutons
 * que le serveur refusera en 403 est une promesse qu'on ne tient pas.
 */
describe('TenantModulesComponent (qui peut agir)', () => {
  let fixture: ComponentFixture<TenantModulesComponent>;
  let component: TenantModulesComponent;
  let auth: jasmine.SpyObj<AuthService>;

  const user = (roles: string[]): AuthUser => ({
    userId: 'u1', tenantId: 't1', displayName: 'Demo', roles
  });

  const row = (code: string, core = false): ModuleRow => ({
    entry: {
      code, name: code.toUpperCase(), category: 'transverse',
      minimumTier: 'STANDARD', dependencies: [], coreModule: core
    },
    activation: null
  });

  beforeEach(async () => {
    const svc = jasmine.createSpyObj<TenantModulesService>(
      'TenantModulesService',
      ['overview', 'catalog', 'summary', 'activate', 'startTrial', 'convertTrial',
       'suspend', 'resume', 'disable', 'changeTier']);
    svc.overview.and.returnValue(of({
      rows: [row('pdca', true), row('iot')],
      summary: {
        tenantId: 't1', tenantTier: 'STANDARD', totalActivations: 0, enabledCount: 0,
        trialCount: 0, activeCount: 0, suspendedCount: 0, expiredCount: 0,
        disabledCount: 0, activations: []
      }
    }));

    auth = jasmine.createSpyObj<AuthService>('AuthService', ['user']);
    auth.user.and.returnValue(of(user(['admin_tenant'])));

    await TestBed.configureTestingModule({
      declarations: [TenantModulesComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TenantModulesService, useValue: svc },
        { provide: AuthService, useValue: auth }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TenantModulesComponent);
    component = fixture.componentInstance;
  });

  it('refuse les actions à l’administrateur du tenant', done => {
    fixture.detectChanges();

    component.canManage$.subscribe(can => {
      expect(can).toBeFalse();
      done();
    });
  });

  it('ouvre les actions au super administrateur', done => {
    auth.user.and.returnValue(of(user(['super_admin'])));
    fixture.detectChanges();

    component.canManage$.subscribe(can => {
      expect(can).toBeTrue();
      done();
    });
  });

  it('refuse les actions à un utilisateur sans rôle d’administration', done => {
    auth.user.and.returnValue(of(user(['quality_manager'])));
    fixture.detectChanges();

    component.canManage$.subscribe(can => {
      expect(can).toBeFalse();
      done();
    });
  });

  it('reste consultable : la liste est chargée quel que soit le rôle', () => {
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('PDCA');
    expect(texte).toContain('IOT');
  });
});

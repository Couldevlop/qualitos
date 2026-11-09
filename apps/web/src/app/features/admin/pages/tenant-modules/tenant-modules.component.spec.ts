import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ModuleRow, TenantModuleSummary } from '../../admin.types';
import { TenantModulesService } from '../../tenant-modules.service';
import { ModuleGroupRow, TenantModulesComponent } from './tenant-modules.component';

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
  let svc: jasmine.SpyObj<TenantModulesService>;

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

  const resume: TenantModuleSummary = {
    tenantId: 't1', tenantTier: 'STANDARD', totalActivations: 0, enabledCount: 0,
    trialCount: 0, activeCount: 0, suspendedCount: 0, expiredCount: 0,
    disabledCount: 0, activations: []
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj<TenantModulesService>(
      'TenantModulesService',
      ['overview', 'catalog', 'summary', 'activate', 'startTrial', 'convertTrial',
       'suspend', 'resume', 'disable', 'changeTier']);
    svc.overview.and.returnValue(of({ rows: [row('pdca', true), row('iot')], summary: resume }));

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
    expect(texte).toContain('Hub IoT');
  });

  it('affiche les modules, catégories et paliers traduits, jamais leurs codes', () => {
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('Cycles PDCA');
    expect(texte).toContain('Modules transverses');
    expect(texte).not.toContain('transverse ');
    expect(texte).not.toContain('STANDARD');
    expect(texte).toContain('Standard');
  });

  // ---------- socle et additionnels ----------

  describe('le tableau sépare le socle des modules additionnels', () => {

    it('pose un en-tête devant chaque bloc, socle d’abord', done => {
      fixture.detectChanges();

      component.groupedRows$.subscribe(lignes => {
        expect(lignes.length).toBe(4);
        const socle = lignes[0] as ModuleGroupRow;
        const extra = lignes[2] as ModuleGroupRow;
        expect(socle.group).toBe('CORE');
        expect(socle.count).toBe(1);
        expect(extra.group).toBe('EXTRA');
        expect(extra.count).toBe(1);
        // Les modules gardent l'ordre du catalogue à l'intérieur de leur bloc.
        expect((lignes[1] as ModuleRow).entry.code).toBe('pdca');
        expect((lignes[3] as ModuleRow).entry.code).toBe('iot');
        done();
      });
    });

    it('n’annonce pas un bloc vide', done => {
      // Un en-tête au-dessus de rien se lit comme un chargement incomplet.
      // Avant le premier affichage : la vue d'ensemble n'est demandée qu'une
      // fois (shareReplay refCount:false), un changement plus tard ne repartirait pas.
      svc.overview.and.returnValue(of({ rows: [row('iot')], summary: resume }));
      fixture.detectChanges();

      component.groupedRows$.subscribe(lignes => {
        expect(lignes.length).toBe(2);
        expect((lignes[0] as ModuleGroupRow).group).toBe('EXTRA');
        done();
      });
    });

    it('dit, à l’écran, ce que chaque bloc engage', () => {
      fixture.detectChanges();
      const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

      expect(texte).toContain('Socle');
      expect(texte).toContain('Livré avec la plateforme');
      expect(texte).toContain('Modules additionnels');
      expect(texte).toContain('client par client');
    });

    it('reconnaît un en-tête de ligne de module', () => {
      expect(component.estEnTete(0, { group: 'CORE', label: 'Socle', hint: '', count: 1 }))
        .toBeTrue();
      expect(component.estEnTete(1, row('iot'))).toBeFalse();
    });
  });
});

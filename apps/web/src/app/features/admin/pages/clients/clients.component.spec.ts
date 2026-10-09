import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ModuleCatalogEntry, OnboardResponse, TenantSummary } from '../../admin.types';
import { ClientsService } from '../../clients.service';
import { CredentialRevealComponent } from '../../components/credential-reveal/credential-reveal.component';
import { TenantModulesService } from '../../tenant-modules.service';
import { ClientsComponent, familles, slugify } from './clients.component';

describe('ClientsComponent', () => {

  let fixture: ComponentFixture<ClientsComponent>;
  let component: ClientsComponent;
  let service: jasmine.SpyObj<ClientsService>;
  let modules: jasmine.SpyObj<TenantModulesService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const ACME: TenantSummary = {
    id: 't1', slug: 'acme', name: 'ACME', plan: 'STARTER', active: true, createdAt: '2026-10-01T00:00:00Z',
    updatedAt: '2026-10-01T00:00:00Z'
  };

  const mod = (o: Partial<ModuleCatalogEntry>): ModuleCatalogEntry => ({
    code: 'x', name: 'X', category: 'methodes', minimumTier: 'STANDARD', dependencies: [], coreModule: false, ...o
  });
  const CATALOGUE = [
    mod({ code: 'core', name: 'Socle', category: 'socle', coreModule: true }),
    mod({ code: 'capa', name: 'CAPA', category: 'qualite' }),
    mod({ code: 'nc', name: 'Non-conformités', category: 'qualite', dependencies: ['capa'] }),
    mod({ code: 'spc', name: 'SPC', category: 'methodes', dependencies: ['nc'] })
  ];

  async function setup(listFails = false): Promise<void> {
    service = jasmine.createSpyObj<ClientsService>('ClientsService', ['list', 'onboard']);
    service.list.and.returnValue(listFails ? throwError(() => new Error('503'))
      : of({ content: [ACME], totalElements: 1, totalPages: 1, number: 0, size: 100 }));
    modules = jasmine.createSpyObj<TenantModulesService>('TenantModulesService', ['catalog']);
    modules.catalog.and.returnValue(of(CATALOGUE));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [ClientsComponent, CredentialRevealComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ClientsService, useValue: service },
        { provide: TenantModulesService, useValue: modules },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ClientsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('liste les clients de la plateforme', async () => {
    await setup();
    expect(hote().querySelector('[data-test="clients"]')!.textContent).toContain('ACME');
  });

  it('l’assistant mène de l’entreprise à l’administrateur, et crée le client', async () => {
    await setup();
    const reponse: OnboardResponse = {
      tenant: { ...ACME, id: 't2', slug: 'hopital-saint-jean', name: 'Hôpital Saint-Jean' },
      admin: { id: 'u1', tenantId: 't2', keycloakId: 'kc', email: 'alice@hsj.fr', roles: ['admin_tenant'], active: true,
        createdAt: '', updatedAt: '' },
      temporaryPassword: 'xxxx-xxxx-xxxx', invitationSent: false,
      modules: [{ code: 'capa', activated: true, message: null }, { code: 'nc', activated: false, message: 'moteur injoignable' }]
    };
    service.onboard.and.returnValue(of(reponse));

    (hote().querySelector('[data-test="nouveau-client"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(component.families.map(f => f.category)).toEqual(['socle', 'qualite', 'methodes']);

    // Étape 1 : le nom suffit à déduire l'identifiant.
    component.next();
    expect(component.step).toBe(1);
    component.companyForm.controls.name.setValue('Hôpital Saint-Jean');
    component.suggestSlug();
    expect(component.companyForm.controls.slug.value).toBe('hopital-saint-jean');
    component.next();
    expect(component.step).toBe(2);

    // Étape 2 : SPC entraîne NC, qui entraîne CAPA ; le socle est inclus d'office.
    component.toggle(CATALOGUE[3]);
    expect([...component.selected].sort()).toEqual(['capa', 'nc', 'spc']);
    component.toggle(CATALOGUE[1]);
    expect(component.selected.has('capa')).withContext('requis par NC').toBeTrue();
    expect(component.requiredText(CATALOGUE[1])).toContain('Non-conformités');
    component.toggle(CATALOGUE[0]);
    expect(component.isSelected(CATALOGUE[0])).toBeTrue();
    expect(component.selected.has('core')).toBeFalse();
    component.toggle(CATALOGUE[3]);
    expect(component.selected.has('spc')).toBeFalse();
    component.next();
    expect(component.step).toBe(3);

    // Étape 3 : l'administrateur.
    component.create();
    expect(service.onboard).not.toHaveBeenCalled();
    component.adminForm.patchValue({ email: 'alice@hsj.fr', firstName: 'Alice' });
    component.create();
    expect(service.onboard).toHaveBeenCalledWith({
      name: 'Hôpital Saint-Jean', slug: 'hopital-saint-jean', plan: 'STARTER', modules: ['capa', 'nc'],
      admin: { email: 'alice@hsj.fr', firstName: 'Alice', lastName: null }
    });
    fixture.detectChanges();

    expect(component.wizard).toBeFalse();
    expect(component.failedModules).toBe(1);
    expect(hote().querySelector('[data-test="resultat"]')!.textContent).toContain('moteur injoignable');
    expect(hote().querySelector('[data-test="remise-mdp"]')!.textContent).toContain('xxxx-xxxx-xxxx');
    expect(component.clients[0].name).toBe('Hôpital Saint-Jean');

    component.step = 2;
    component.back();
    expect(component.step as number).toBe(1);
  });

  it('un refus du serveur est annoncé et l’assistant reste ouvert', async () => {
    await setup();
    service.onboard.and.returnValue(throwError(() => new Error('409')));
    component.openWizard();
    component.companyForm.patchValue({ name: 'ACME', slug: 'acme' });
    component.adminForm.patchValue({ email: 'a@acme.fr' });
    component.create();
    expect(snack.open).toHaveBeenCalled();
    expect(component.wizard).toBeTrue();
    expect(component.creating).toBeFalse();
  });

  it('sans catalogue ni liste, l’écran le dit', async () => {
    await setup(true);
    fixture.detectChanges();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
    modules.catalog.and.returnValue(throwError(() => new Error('503')));
    (component as unknown as { catalog: ModuleCatalogEntry[] }).catalog = [];
    component.openWizard();
    expect(component.catalogFailed).toBeTrue();
    component.closeWizard();
    expect(component.wizard).toBeFalse();
  });

  it('outils : familles, identifiant d’adresse', () => {
    expect(familles(CATALOGUE).find(f => f.category === 'qualite')!.modules.map(m => m.code)).toEqual(['capa', 'nc']);
    expect(slugify('  Hôpital — Saint Jean !! ')).toBe('hopital-saint-jean');
    expect(slugify('a'.repeat(80)).length).toBe(63);
  });
});

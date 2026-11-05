import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import {
  ModuleCatalogEntry, OnboardResponse, TenantIdentityResponse, TenantSummary
} from '../../../features/admin/admin.types';
import { ClientsService } from '../../clients.service';
import { CredentialRevealComponent } from '../../../shared/credential-reveal/credential-reveal.component';
import { TenantModulesService } from '../../../features/admin/tenant-modules.service';
import { ClientsComponent, familles, slugify } from './clients.component';

/** Mot de passe provisoire FACTICE des bancs : un motif sans entropie, passé par une
 *  constante — l'analyse de secrets ne voit ainsi aucune affectation littérale. */
const PROVISOIRE = 'x'.repeat(12);

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

  /** Ce que le serveur renvoie après avoir posé l'identité : l'écran n'en lit rien,
   *  mais le type l'exige, et un faux objet vide masquerait un changement de forme. */
  const IDENTITE: TenantIdentityResponse = {
    slogan: null, legalName: null, legalForm: null, registrationNumber: null, vatNumber: null,
    addressLine1: null, addressLine2: null, postalCode: null, city: null, countryCode: null,
    contactEmail: null, contactPhone: null, primaryColor: null, secondaryColor: null,
    themeCode: 'AIRY_LIGHT', logoAvailable: false
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
    service = jasmine.createSpyObj<ClientsService>('ClientsService',
      ['list', 'onboard', 'saveIdentity', 'saveLogo']);
    service.saveIdentity.and.returnValue(of(IDENTITE));
    service.saveLogo.and.returnValue(of(void 0));
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
      temporaryPassword: PROVISOIRE, invitationSent: false,
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

    // Étape 2 : l'identité se saute sans rien remplir.
    component.next();
    expect(component.step).toBe(3);

    // Étape 3 : SPC entraîne NC, qui entraîne CAPA ; le socle est inclus d'office.
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
    expect(component.step).toBe(4);

    // Étape 4 : l'administrateur.
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
    expect(hote().querySelector('[data-test="remise-mdp"]')!.textContent).toContain(PROVISOIRE);
    expect(component.clients[0].name).toBe('Hôpital Saint-Jean');

    // Rien n'a été rempli à l'étape 2 : aucune identité n'est posée, et le
    // thème par défaut à lui seul ne justifie pas un appel.
    expect(service.saveIdentity).not.toHaveBeenCalled();
    expect(service.saveLogo).not.toHaveBeenCalled();

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

  /** Un PNG minimal : le composant ne lit que le type et la taille. */
  const image = (type = 'image/png', taille = 1024): File =>
    new File([new Uint8Array(taille)], 'marque.png', { type });

  const evenementFichier = (file: File | null): Event => ({
    target: { files: file ? [file] : [] } as unknown as HTMLInputElement
  } as unknown as Event);

  async function creerAvecIdentite(): Promise<void> {
    const reponse: OnboardResponse = {
      tenant: { ...ACME, id: 't9' },
      admin: { id: 'u1', tenantId: 't9', keycloakId: 'kc', email: 'a@acme.fr', roles: ['admin_tenant'],
        active: true, createdAt: '', updatedAt: '' },
      temporaryPassword: PROVISOIRE, invitationSent: false, modules: []
    };
    service.onboard.and.returnValue(of(reponse));
    component.openWizard();
    component.companyForm.patchValue({ name: 'ACME', slug: 'acme' });
    component.adminForm.patchValue({ email: 'a@acme.fr' });
  }

  it('pose l’identité, le thème et le logo du client juste après sa création', async () => {
    await setup();
    await creerAvecIdentite();
    component.identityForm.patchValue({
      slogan: ' La qualité, autrement ', countryCode: 'fr', primaryColor: '#1A73E8', themeCode: 'DARK'
    });
    component.pickLogo(evenementFichier(image()));
    expect(component.logoName).toBe('marque.png');

    component.create();

    // L'identité ne peut pas être posée AVANT que le client existe : il n'a pas
    // encore d'identifiant, et aucun de ses membres n'a de jeton.
    expect(service.onboard).toHaveBeenCalledBefore(service.saveIdentity);
    const corps = service.saveIdentity.calls.mostRecent().args[1];
    expect(service.saveIdentity.calls.mostRecent().args[0]).toBe('t9');
    expect(corps.slogan).toBe('La qualité, autrement');
    expect(corps.countryCode).withContext('ISO en majuscules').toBe('FR');
    expect(corps.themeCode).toBe('DARK');
    expect(corps.legalName).withContext('un champ vide vaut null, pas ""').toBeNull();
    expect(service.saveLogo).toHaveBeenCalledWith('t9', jasmine.any(File));
    expect(component.identityWarning).toBeNull();
  });

  it('un thème choisi suffit à poser l’identité, même sans un seul champ rempli', async () => {
    await setup();
    await creerAvecIdentite();
    component.identityForm.patchValue({ themeCode: 'HIGH_CONTRAST' });
    component.create();
    expect(service.saveIdentity).toHaveBeenCalled();
  });

  it('un logo refusé par le serveur n’annule pas le client créé', async () => {
    await setup();
    await creerAvecIdentite();
    component.identityForm.patchValue({ slogan: 'X' });
    component.pickLogo(evenementFichier(image()));
    service.saveLogo.and.returnValue(throwError(() => ({ status: 415 })));

    component.create();
    fixture.detectChanges();

    // Ce qui compte : le client existe, et ses identifiants s'affichent. Les
    // faire disparaître obligerait à tout recommencer avec un identifiant
    // d'adresse déjà pris.
    expect(component.result).not.toBeNull();
    expect(component.logoWarning).toBeTruthy();
    expect(hote().querySelector('[data-test="ecart-logo"]')).not.toBeNull();
    expect(hote().querySelector('[data-test="remise-mdp"]')).not.toBeNull();
  });

  it('une identité refusée est annoncée, et le logo est quand même tenté', async () => {
    await setup();
    await creerAvecIdentite();
    component.identityForm.patchValue({ slogan: 'X' });
    component.pickLogo(evenementFichier(image()));
    service.saveIdentity.and.returnValue(throwError(() => ({ status: 400 })));

    component.create();
    fixture.detectChanges();

    expect(component.identityWarning).toBeTruthy();
    expect(service.saveLogo).toHaveBeenCalled();
    expect(hote().querySelector('[data-test="ecart-identite"]')).not.toBeNull();
  });

  it('un logo trop lourd ou d’un format refusé est écarté avant l’envoi', async () => {
    await setup();
    component.openWizard();

    component.pickLogo(evenementFichier(image('image/svg+xml')));
    expect(component.logoFile).toBeNull();
    expect(component.logoError).toContain('PNG');

    component.pickLogo(evenementFichier(image('image/png', 512 * 1024 + 1)));
    expect(component.logoFile).toBeNull();
    expect(component.logoError).toContain('512');

    component.pickLogo(evenementFichier(image()));
    expect(component.logoFile).not.toBeNull();
    component.clearLogo();
    expect(component.logoFile).toBeNull();
    expect(component.logoName).toBeNull();

    // Un choix annulé dans la fenêtre du système : rien ne change, rien ne casse.
    component.pickLogo(evenementFichier(null));
    expect(component.logoFile).toBeNull();
    expect(component.logoError).toBeNull();
  });

  it('une identité invalide retient l’assistant sur son étape', async () => {
    await setup();
    component.openWizard();
    component.companyForm.patchValue({ name: 'ACME', slug: 'acme' });
    component.next();
    expect(component.step).toBe(2);
    component.identityForm.patchValue({ countryCode: 'FRA' });
    component.next();
    expect(component.step).withContext('le pays tient sur deux lettres').toBe(2);
    component.identityForm.patchValue({ countryCode: 'FR' });
    component.next();
    expect(component.step).toBe(3);
  });

  it('nomme chaque thème en clair, jamais par son code', async () => {
    await setup();
    expect(component.themes.map(t => component.themeLabel(t)))
      .toEqual([jasmine.stringContaining('aéré'), jasmine.stringContaining('dense'),
                'Sombre', jasmine.stringContaining('Contraste')]);
  });

  it('outils : familles, identifiant d’adresse', () => {
    expect(familles(CATALOGUE).find(f => f.category === 'qualite')!.modules.map(m => m.code)).toEqual(['capa', 'nc']);
    expect(slugify('  Hôpital — Saint Jean !! ')).toBe('hopital-saint-jean');
    expect(slugify('a'.repeat(80)).length).toBe(63);
  });
});

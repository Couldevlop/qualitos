import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatMenuModule } from '@angular/material/menu';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { PartnerShellComponent } from './partner-shell.component';

/**
 * La coquille de la console d'un cabinet.
 *
 * <p>Ce que ces tests tiennent : la console d'un cabinet ne donne accès à AUCUN
 * écran — ni métier, ni d'administration. C'est ce qui se perd à la première
 * reprise, en ajoutant une entrée « pratique » vers les non-conformités d'un
 * client : un consultant n'a pas de client, et un tel lien ouvrirait une page
 * dont les données ne sont celles de personne.
 */
describe('PartnerShellComponent', () => {
  let fixture: ComponentFixture<PartnerShellComponent>;
  let component: PartnerShellComponent;

  const user: AuthUser = {
    userId: 'u-9',
    displayName: 'claire.consultante',
    roles: ['CONSULTANT']
  } as AuthUser;

  const auth = jasmine.createSpyObj<AuthService>('AuthService', ['user', 'logout']);

  beforeEach(async () => {
    auth.user.and.returnValue(of(user));
    auth.logout.calls.reset();

    await TestBed.configureTestingModule({
      declarations: [PartnerShellComponent],
      imports: [SharedModule, UiModule, MatMenuModule, NoopAnimationsModule],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }]
    }).compileComponents();

    fixture = TestBed.createComponent(PartnerShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('porte un repère qui dit qu’on n’est pas dans son propre espace', () => {
    expect(fixture.nativeElement.textContent as string).toContain('Console cabinet');
  });

  it('n’ouvre aucun lien de navigation', () => {
    // La vérification qui compte. La coquille ne porte qu'un bandeau : le seul
    // chemin est le portefeuille, servi par la route enfant. Toute ancre
    // apparaissant ici serait un accès qu'un consultant n'a pas.
    const liens = fixture.nativeElement.querySelectorAll('a[routerLink], a[href]');
    expect(liens.length).toBe(0);
  });

  it('montre le compte connecté', () => {
    const nom = fixture.nativeElement.querySelector('.qos-partner__user-name') as HTMLElement;
    expect(nom.textContent!.trim()).toBe('claire.consultante');
  });

  it('réduit le nom du compte à deux initiales', () => {
    expect(component.initials('claire.consultante')).toBe('CC');
    expect(component.initials('Claire Martin')).toBe('CM');
    expect(component.initials('claire')).toBe('C');
    // Un nom fait d'espaces ne doit pas produire d'exception dans l'en-tête.
    expect(component.initials('   ')).toBe('');
  });

  it('déconnecte par le service d’authentification', () => {
    component.logout();
    expect(auth.logout).toHaveBeenCalledTimes(1);
  });
});

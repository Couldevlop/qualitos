import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatMenuModule } from '@angular/material/menu';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { PlatformShellComponent } from './platform-shell.component';

/**
 * La coquille de la console de l'éditeur.
 *
 * <p>Ce que ces tests tiennent : la console ne contient QUE de l'administration
 * de plateforme. C'était la demande, et c'est ce qui se perd à la première
 * reprise — il suffit d'ajouter une entrée « pratique » vers un écran qualité
 * pour que la frontière devienne décorative.
 */
describe('PlatformShellComponent', () => {
  let fixture: ComponentFixture<PlatformShellComponent>;
  let component: PlatformShellComponent;

  const user: AuthUser = {
    userId: 'u-1',
    displayName: 'super.admin',
    roles: ['SUPER_ADMIN']
  } as AuthUser;

  const auth = jasmine.createSpyObj<AuthService>('AuthService', ['user', 'logout']);

  beforeEach(async () => {
    auth.user.and.returnValue(of(user));
    auth.logout.calls.reset();

    await TestBed.configureTestingModule({
      declarations: [PlatformShellComponent],
      imports: [SharedModule, UiModule, MatMenuModule, NoopAnimationsModule],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }]
    }).compileComponents();

    fixture = TestBed.createComponent(PlatformShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const liens = (): string[] => Array.from(
    fixture.nativeElement.querySelectorAll('.qos-platform__link-label') as NodeListOf<HTMLElement>)
    .map(el => el.textContent!.trim());

  it('n’affiche que des entrées d’administration de plateforme', () => {
    expect(liens()).toEqual(['Clients', 'Catalogue des modules']);
  });

  it('ne mène vers aucun écran qualité', () => {
    // La vérification qui compte : toutes les destinations sont sous /platform.
    // Un lien vers /nc ou /capa, même « pratique », ferait sortir l’éditeur de
    // sa console vers des données qui ne sont celles de personne.
    const routes = component.entries.map(e => e.route);
    expect(routes.every(r => r.startsWith('/platform/'))).toBeTrue();
  });

  it('ne liste aucune entrée qui ne mène nulle part', () => {
    // Les quatre surfaces prévues (cabinets, paramétrage central, piste
    // d’audit, statistiques) arrivent dans leurs lots. Les annoncer avant
    // produirait quatre liens morts, et un lien mort dans une console
    // d’administration se lit comme une panne.
    expect(component.entries.every(e => e.route && e.label && e.hint)).toBeTrue();
  });

  it('porte un repère qui dit qu’on est chez l’éditeur', () => {
    const texte = fixture.nativeElement.textContent as string;
    expect(texte).toContain('Console éditeur');
    expect(texte).toContain('Administration de la plateforme');
  });

  it('dit explicitement que les données d’un client ne se consultent pas ici', () => {
    const portee = fixture.nativeElement.querySelector('.qos-platform__scope') as HTMLElement;
    expect(portee.textContent).toContain('n’ouvre aucun écran qualité');
  });

  it('montre le compte connecté et ses rôles', () => {
    const nom = fixture.nativeElement.querySelector('.qos-platform__user-name') as HTMLElement;
    expect(nom.textContent!.trim()).toBe('super.admin');
  });

  it('réduit le nom du compte à deux initiales', () => {
    expect(component.initials('super.admin')).toBe('SA');
    expect(component.initials('Marie Dupont')).toBe('MD');
    expect(component.initials('editeur')).toBe('E');
    // Un nom fait d’espaces ne doit pas produire d’exception dans l’en-tête.
    expect(component.initials('   ')).toBe('');
  });

  it('déconnecte par le service d’authentification', () => {
    component.logout();
    expect(auth.logout).toHaveBeenCalledTimes(1);
  });
});

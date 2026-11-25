import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Subject, of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { EditionService } from '../../../../core/edition/edition.service';
import { Edition } from '../../../../core/edition/edition.types';
import { AdminShellComponent } from './admin-shell.component';

/**
 * L'enveloppe de la console d'administration : deux onglets, le menu de
 * l'onglet courant, et aucune adresse perdue.
 */
describe('AdminShellComponent', () => {
  let fixture: ComponentFixture<AdminShellComponent>;
  let composant: AdminShellComponent;
  let evenements: Subject<unknown>;
  let urlCourante: string;
  let donneeDeLaRoute: Record<string, unknown> | undefined;
  /** Aucune route enfant encore activee : l'enveloppe se rend avant elle. */
  let sansEnfant = false;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const routes = (): string[] => composant.groupesCourants
    .flatMap(g => g.items.map(i => i.route));

  /**
   * Monte l'ecran pour une adresse donnee. `onglet` absent simule une route
   * non etiquetee : l'enveloppe doit alors deduire l'onglet de l'adresse.
   */
  async function monter(url: string, onglet?: string, edition: Edition = 'ONPREM'): Promise<void> {
    urlCourante = url;
    donneeDeLaRoute = onglet ? { onglet } : undefined;
    sansEnfant = false;
    evenements = new Subject<unknown>();

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [AdminShellComponent],
      imports: [SharedModule, UiModule, MatTabsModule, MatTooltipModule, NoopAnimationsModule],
      providers: [
        {
          provide: Router,
          useValue: {
            get url(): string { return urlCourante; },
            events: evenements.asObservable(),
            createUrlTree: () => ({}),
            serializeUrl: () => urlCourante
          }
        },
        {
          provide: ActivatedRoute,
          useValue: {
            get firstChild() {
              return sansEnfant ? null : { snapshot: { data: donneeDeLaRoute } };
            }
          }
        },
        { provide: EditionService, useValue: { edition: () => of(edition) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminShellComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => composant?.ngOnDestroy());

  it("l onglet courant se lit dans la DONNEE de la route, pas dans l adresse", async () => {
    await monter('/admin/messagerie', 'parametrage');

    expect(composant.ongletCourant).toBe('parametrage');
  });

  it("une route non etiquetee retrouve son onglet par l adresse", async () => {
    // Le repli existe pour qu'un ecran ajoute sans son `data.onglet` n'allume
    // pas le mauvais onglet : il est retrouve par l'adresse qu'il occupe.
    await monter('/admin/roles');

    expect(composant.ongletCourant).toBe('utilisateurs');
  });

  it("l enveloppe rendue AVANT sa route enfant trouve quand meme son onglet", async () => {
    // L'ordre d'activation n'est pas garanti : l'enveloppe peut se rendre alors
    // que `firstChild` est encore nul. Sans repli, la lecture de `data.onglet`
    // levait et la console ne s'affichait pas du tout.
    await monter('/admin/webhooks', 'parametrage');
    sansEnfant = true;
    composant.ngOnInit();
    fixture.detectChanges();

    expect(composant.ongletCourant).toBe('parametrage');
    expect(routes()).toContain('/admin/webhooks');
  });

  it("une adresse inconnue retombe sur le PREMIER onglet, jamais sur rien", async () => {
    await monter('/admin/inexistant');

    expect(composant.ongletCourant).toBe('utilisateurs');
  });

  it("le menu montre les ecrans de l onglet courant, et eux seuls", async () => {
    await monter('/admin/team', 'utilisateurs');

    expect(routes()).toEqual(['/admin/team', '/admin/roles', '/admin/cabinets']);
  });

  it("l onglet du parametrage porte les dix-sept reglages", async () => {
    await monter('/admin/modules', 'parametrage');

    expect(routes()).toContain('/admin/messagerie');
    expect(routes()).toContain('/admin/listes');
    expect(routes()).toContain('/admin/audit-log');
    expect(routes()).not.toContain('/admin/team');
  });

  it('la licence ne figure au menu qu en installation sur site (ADR 0082)', async () => {
    await monter('/admin/modules', 'parametrage', 'ONPREM');
    expect(routes()).withContext('on-premise').toContain('/admin/licence');

    await monter('/admin/modules', 'parametrage', 'SAAS');
    expect(routes()).withContext('SaaS').not.toContain('/admin/licence');
  });

  it("les connecteurs sont signales comme SORTANT de la console", async () => {
    // Leur route est hors de `/admin` : sans ce signalement, l'utilisateur
    // verrait la barre d'onglets disparaitre sans comprendre pourquoi.
    await monter('/admin/modules', 'parametrage');
    const connecteurs = composant.groupesCourants
      .flatMap(g => g.items).find(i => i.route === '/connectors')!;

    expect(connecteurs.horsConsole).withContext('marque absente').toBeTrue();
    expect(hote().querySelectorAll('.lien__sortie').length).toBe(1);
  });

  it("l entree courante est marquee, et une seule", async () => {
    await monter('/admin/messagerie', 'parametrage');

    const actives = hote().querySelectorAll('.lien--actif');
    expect(actives.length).toBe(1);
    expect(actives[0].textContent).toContain('Messagerie');
  });

  it('une sous-adresse garde son entree allumee', async () => {
    await monter('/admin/listes/qualite', 'parametrage');

    expect(composant.estActif({ label: '', route: '/admin/listes', icon: '' })).toBeTrue();
    // Et ne confond pas deux routes dont l'une prefixe l'autre par les lettres.
    expect(composant.estActif({ label: '', route: '/admin/liste', icon: '' })).toBeFalse();
  });

  it("passer d un onglet a l autre allume le bon, sans remonter l ecran", async () => {
    // Le geste qui revele la famille « l'ecran perd ce que le serveur a garde » :
    // naviguer DANS la console ne la reconstruit pas, l'onglet doit donc se
    // relire a chaque fin de navigation -- sinon il resterait sur le premier.
    await monter('/admin/team', 'utilisateurs');
    expect(composant.ongletCourant).toBe('utilisateurs');

    urlCourante = '/admin/messagerie';
    donneeDeLaRoute = { onglet: 'parametrage' };
    evenements.next(new NavigationEnd(1, urlCourante, urlCourante));
    fixture.detectChanges();

    expect(composant.ongletCourant).toBe('parametrage');
    expect(routes()).toContain('/admin/messagerie');
    expect(routes()).not.toContain('/admin/team');
  });

  it("les deux onglets sont rendus comme des LIENS, pas des panneaux", async () => {
    await monter('/admin/team', 'utilisateurs');
    const liens = hote().querySelectorAll('.console__onglets a');

    expect(liens.length).toBe(2);
    expect(liens[0].textContent).toContain('Gestion utilisateurs');
    expect(liens[1].textContent).toContain('Paramétrage');
  });
});

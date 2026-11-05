import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { ModuleCatalogEntry } from '../../../features/admin/admin.types';
import { TenantModulesService } from '../../../features/admin/tenant-modules.service';
import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import { FAMILY_LABELS, PlatformModulesComponent } from './platform-modules.component';

/**
 * Le catalogue des modules, vu par l'éditeur.
 *
 * <p>La source est le catalogue du moteur qualité, pas une liste tenue dans
 * l'écran : ces tests vérifient que ce qui s'affiche vient bien de lui, y
 * compris l'ordre des familles — un tri alphabétique mettrait « Intégrations »
 * avant « Méthodes », alors que le catalogue va des méthodes aux intégrations.
 */
describe('PlatformModulesComponent', () => {
  let fixture: ComponentFixture<PlatformModulesComponent>;
  let component: PlatformModulesComponent;
  let modules: jasmine.SpyObj<TenantModulesService>;

  const entree = (code: string, name: string, category: string,
                  deps: string[] = [], core = false): ModuleCatalogEntry => ({
    code, name, category, minimumTier: core ? 'FREE' : 'STANDARD',
    dependencies: deps, coreModule: core
  });

  const catalogue: ModuleCatalogEntry[] = [
    entree('pdca', 'PDCA cycles', 'methodes', [], true),
    entree('dmaic', 'DMAIC + Poka-Yoke', 'methodes'),
    entree('capa', 'CAPA', 'transverse', [], true),
    entree('risk', 'Risk / FMEA', 'transverse', ['capa']),
    entree('itsm', 'ITSM connectors', 'integrations')
  ];

  beforeEach(async () => {
    modules = jasmine.createSpyObj<TenantModulesService>('TenantModulesService', ['catalog']);
    modules.catalog.and.returnValue(of(catalogue));

    await TestBed.configureTestingModule({
      declarations: [PlatformModulesComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [{ provide: TenantModulesService, useValue: modules }]
    }).compileComponents();

    fixture = TestBed.createComponent(PlatformModulesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('groupe les modules par famille, dans l’ordre du catalogue', () => {
    expect(component.families.map(f => f.key)).toEqual(['methodes', 'transverse', 'integrations']);
    expect(component.families[0].entries.map(e => e.code)).toEqual(['pdca', 'dmaic']);
  });

  it('nomme chaque famille en français plutôt que par son code', () => {
    expect(component.families.map(f => f.label))
      .toEqual([FAMILY_LABELS['methodes'], FAMILY_LABELS['transverse'], FAMILY_LABELS['integrations']]);
  });

  it('affiche sous son code brut une famille que le serveur ajouterait', () => {
    // Le serveur peut livrer une catégorie que cet écran ne connaît pas encore.
    // Montrer le code est laid mais honnête ; une famille muette ferait
    // disparaître les modules qu’elle contient.
    modules.catalog.and.returnValue(of([entree('x', 'Nouveau', 'quantique')]));
    component.reload();
    expect(component.families[0].label).toBe('quantique');
  });

  it('compte les modules et ceux acquis d’office', () => {
    expect(component.total).toBe(5);
    // C'est ce qui évite de chercher un module cœur dans la liste de ce qu'on
    // accorde : il est déjà là.
    expect(component.coreCount).toBe(2);
  });

  it('rend les dépendances par leur nom, pas par leur code', () => {
    // « Exige : CAPA » se comprend ; « Exige : capa » demande de connaître les
    // codes internes.
    expect(component.labelOf('capa')).toBe('CAPA');
  });

  it('montre le code brut d’une dépendance absente du catalogue', () => {
    // Une incohérence du serveur doit se voir, pas se taire.
    expect(component.labelOf('inexistant')).toBe('inexistant');
  });

  it('affiche les modules et leurs paliers', () => {
    const texte = fixture.nativeElement.textContent as string;
    expect(texte).toContain('PDCA cycles');
    expect(texte).toContain('Risk / FMEA');
    expect(texte).toContain('STANDARD');
  });

  it('dit quelles dépendances un module exige, dans le gabarit', () => {
    const risk = fixture.nativeElement.querySelector('[data-code="risk"]') as HTMLElement;
    expect(risk.textContent).toContain('CAPA');
  });

  it('n’affiche pas de palier pour un module acquis d’office', () => {
    const pdca = fixture.nativeElement.querySelector('[data-code="pdca"]') as HTMLElement;
    expect(pdca.querySelector('.module__palier')).toBeNull();
    expect(pdca.querySelector('.module__socle')).not.toBeNull();
  });

  it('rend l’erreur lisible quand le moteur qualité ne répond pas', () => {
    modules.catalog.and.returnValue(throwError(() => ({ status: 503 })));
    component.reload();
    fixture.detectChanges();

    expect(component.error).toBeTruthy();
    expect(component.families).toEqual([]);
    // Et le compteur retombe : garder l'ancien total ferait croire que le
    // catalogue est encore chargé.
    expect(component.total).toBe(0);
    expect(fixture.nativeElement.querySelector('[data-test="erreur"]')).not.toBeNull();
  });

  it('efface l’erreur précédente à un rechargement réussi', () => {
    modules.catalog.and.returnValue(throwError(() => ({ status: 503 })));
    component.reload();
    modules.catalog.and.returnValue(of(catalogue));
    component.reload();

    expect(component.error).toBeNull();
    expect(component.total).toBe(5);
  });

  it('annonce un catalogue vide plutôt que de rester muet', () => {
    modules.catalog.and.returnValue(of([]));
    component.reload();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-test="vide"]')).not.toBeNull();
  });

  it('retombe à l’état de repos après le chargement', done => {
    component.loading$.subscribe(loading => {
      expect(loading).toBeFalse();
      done();
    });
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { APQP_PHASES } from '../../apqp.reference';
import { ApqpOverviewComponent } from './apqp-overview.component';

/**
 * Le cycle APQP, dont le schéma EST l'interface.
 *
 * <p>Deux choses se vérifient ici. La forme en V d'abord : la phase 3 est le
 * point bas, les phases 1 et 5 les extrémités hautes — c'est ce que le schéma
 * a à dire, et une grille mal câblée le dirait faux sans qu'aucun autre test
 * ne s'en aperçoive. Et l'ordre du DOM ensuite : les cinq phases doivent y
 * rester dans leur ordre de lecture, quelle que soit la disposition visuelle,
 * sans quoi un lecteur d'écran entend le cycle dans le désordre.
 *
 * <p>Le reste tient à l'URL : la phase ouverte y vit, donc un lien se partage,
 * et le retour du navigateur referme au lieu de quitter l'écran.
 */
describe('ApqpOverviewComponent', () => {

  let fixture: ComponentFixture<ApqpOverviewComponent>;
  let component: ApqpOverviewComponent;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let router: Router;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  async function setup(phase?: string): Promise<void> {
    params = new BehaviorSubject(convertToParamMap(phase ? { phase } : {}));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpOverviewComponent],
      imports: [SharedModule, UiModule, RouterTestingModule, NoopAnimationsModule],
      providers: [{ provide: ActivatedRoute, useValue: { paramMap: params.asObservable() } }]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(ApqpOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('dessine les cinq phases en V : 3 au point bas, 1 et 5 aux extrémités', async () => {
    // La forme n'est pas décorative — elle dit que le milieu du V est le point
    // où tout se joue avant de pouvoir remonter.
    await setup();

    const niveaux = APQP_PHASES.map(p => component.niveau(p));
    expect(niveaux).toEqual([1, 2, 3, 2, 1]);
  });

  it('garde les phases dans l’ordre de lecture, quelle que soit la disposition', async () => {
    // La grille les place en V ; le DOM, lui, reste la séquence 1→5, sinon un
    // lecteur d'écran entend le cycle dans le désordre.
    await setup();

    const rangs = Array.from(hote().querySelectorAll('.jalon__rang'))
      .map(e => (e as HTMLElement).textContent!.trim());
    expect(rangs).toEqual(['1', '2', '3', '4', '5']);
  });

  it('n’ouvre rien sur /apqp, et invite à choisir', async () => {
    await setup();

    expect(component.choisie).toBeUndefined();
    expect(hote().querySelector('.detail')).toBeNull();
    expect(hote().querySelector('.invite')).not.toBeNull();
  });

  it('ouvre la phase que porte l’URL, avec ses livrables', async () => {
    await setup('validation');

    expect(component.choisie!.numero).toBe(4);
    expect(hote().querySelectorAll('.livrables li').length)
      .toBe(APQP_PHASES[3].livrables.length);
    expect(hote().querySelector('.invite')).toBeNull();
  });

  it('suit un changement de segment sans être recréé', async () => {
    await setup('planification');
    expect(component.choisie!.slug).toBe('planification');

    params.next(convertToParamMap({ phase: 'production-serie' }));
    fixture.detectChanges();

    expect(component.choisie!.numero).toBe(5);
  });

  it('referme la phase quand on reclique sur son jalon', async () => {
    // Recliquer doit défaire, pas laisser l'écran dans le même état : c'est ce
    // qu'attend quiconque a déjà refermé un accordéon.
    await setup('validation');

    component.basculer(APQP_PHASES[3]);
    expect(router.navigate).toHaveBeenCalledWith(['/apqp']);
  });

  it('ouvre une autre phase quand on clique sur un autre jalon', async () => {
    await setup('validation');

    component.basculer(APQP_PHASES[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/apqp', 'planification']);
  });

  it('revient au schéma quand le segment n’existe pas', async () => {
    // Lien périmé ou faute de frappe : un écran vide n'expliquerait rien.
    await setup('phase-inventee');

    expect(router.navigate).toHaveBeenCalledWith(['/apqp']);
    expect(component.choisie).toBeUndefined();
  });

  it('nomme la cinquième phase « Production série et retour d’expérience »', async () => {
    await setup();
    expect(APQP_PHASES[4].titre).toContain('Production série');
    expect(APQP_PHASES[4].slug).toBe('production-serie');
  });
});

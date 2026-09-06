import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { APQP_PHASES } from '../../apqp.reference';
import { ApqpOverviewComponent } from './apqp-overview.component';

/**
 * La vue d'ensemble du cycle APQP.
 *
 * <p>L'ordre EST la méthode : une phase s'appuie sur la précédente, et c'est la
 * seule chose que cet écran doit transmettre avant qu'on entre dans le détail.
 * Ce qui se vérifie ici tient donc à la séquence — cinq phases, dans leur rang,
 * chacune menant à sa propre adresse.
 */
describe('ApqpOverviewComponent', () => {

  let fixture: ComponentFixture<ApqpOverviewComponent>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ApqpOverviewComponent],
      imports: [SharedModule, UiModule, RouterTestingModule, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpOverviewComponent);
    fixture.detectChanges();
  });

  it('montre les cinq phases dans leur ordre', () => {
    const rangs = Array.from(hote().querySelectorAll('.phase__rang'))
      .map(e => (e as HTMLElement).textContent!.trim());

    expect(rangs).toEqual(['1', '2', '3', '4', '5']);
  });

  it('fait de chaque phase un lien vers sa propre adresse', () => {
    // On entre dans une phase EN CLIQUANT sur le schéma : un dessin qu'on ne
    // peut que regarder obligerait à repasser par le menu latéral.
    const liens = Array.from(hote().querySelectorAll('a.phase'))
      .map(a => a.getAttribute('href'));

    expect(liens).toEqual(APQP_PHASES.map(p => '/apqp/' + p.slug));
  });

  it('rend la séquence comme une liste ordonnée, pas comme cinq blocs', () => {
    // Un lecteur d'écran doit entendre « 1 sur 5 », sans quoi l'enchaînement —
    // tout le propos de l'APQP — ne lui parvient pas.
    expect(hote().querySelector('ol.cycle')).not.toBeNull();
    expect(hote().querySelectorAll('ol.cycle > li').length).toBe(5);
  });

  it('annonce le poids de chaque phase par son nombre de livrables', () => {
    const comptes = Array.from(hote().querySelectorAll('.phase__compte'))
      .map(e => parseInt((e as HTMLElement).textContent!.trim(), 10));

    expect(comptes).toEqual(APQP_PHASES.map(p => p.livrables.length));
  });

  it('relie les phases par quatre flèches, jamais cinq', () => {
    // La dernière phase ne mène nulle part : une flèche après elle promettrait
    // une sixième étape.
    expect(hote().querySelectorAll('.cycle__lien').length).toBe(4);
  });
});

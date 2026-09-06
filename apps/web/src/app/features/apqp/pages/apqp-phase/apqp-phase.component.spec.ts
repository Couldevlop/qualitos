import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { APQP_PHASES } from '../../apqp.reference';
import { ApqpPhaseComponent } from './apqp-phase.component';

/**
 * Une phase du cycle APQP.
 *
 * <p>Deux exigences se croisent. La phase vient de l'URL, donc un lien vers une
 * phase se partage et se met en favori — et le composant doit suivre un
 * changement de segment sans être recréé, sinon passer à la phase suivante
 * laisserait l'écran sur la précédente. Et un segment inconnu, lien périmé ou
 * faute de frappe, ramène au cycle plutôt qu'à un écran vide.
 */
describe('ApqpPhaseComponent', () => {

  let fixture: ComponentFixture<ApqpPhaseComponent>;
  let component: ApqpPhaseComponent;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let router: Router;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  async function setup(slug: string): Promise<void> {
    params = new BehaviorSubject(convertToParamMap({ phase: slug }));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpPhaseComponent],
      imports: [SharedModule, UiModule, RouterTestingModule, NoopAnimationsModule],
      providers: [{ provide: ActivatedRoute, useValue: { paramMap: params.asObservable() } }]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(ApqpPhaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('affiche la phase désignée par l’URL, avec ses livrables', async () => {
    await setup('validation');

    expect(component.phase!.numero).toBe(4);
    expect(hote().querySelectorAll('.livrables__liste li').length)
      .toBe(APQP_PHASES[3].livrables.length);
  });

  it('suit un changement de segment sans être recréé', async () => {
    // Passer d'une phase à la suivante ne détruit pas le composant : un
    // instantané de l'URL laisserait l'écran sur la phase précédente.
    await setup('planification');
    expect(component.phase!.slug).toBe('planification');

    params.next(convertToParamMap({ phase: 'conception-produit' }));
    fixture.detectChanges();

    expect(component.phase!.slug).toBe('conception-produit');
  });

  it('ramène au cycle quand le segment n’existe pas', async () => {
    // Lien périmé ou faute de frappe : un écran vide n'expliquerait rien.
    await setup('phase-inventee');

    expect(router.navigate).toHaveBeenCalledWith(['/apqp']);
    expect(component.phase).toBeUndefined();
  });

  it('propose la phase suivante et la précédente, aux bons endroits', async () => {
    await setup('conception-processus');

    expect(component.precedente!.slug).toBe('conception-produit');
    expect(component.suivante!.slug).toBe('validation');
  });

  it('n’offre pas de précédente en phase 1, ni de suivante en phase 5', async () => {
    // Une flèche vers une sixième phase promettrait ce qui n'existe pas.
    await setup('planification');
    expect(component.precedente).toBeUndefined();
    expect(component.suivante!.numero).toBe(2);

    await setup('retour-experience');
    expect(component.precedente!.numero).toBe(4);
    expect(component.suivante).toBeUndefined();
  });
});

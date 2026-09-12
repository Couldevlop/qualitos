import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApqpDeliverable, ApqpPhase } from '../../apqp.types';
import { ApqpPpapSummaryComponent, LignePpap } from './apqp-ppap-summary.component';

/**
 * Le dossier PPAP sous le schéma.
 *
 * <p>Ce que ce banc tient : la section AGRÈGE le cycle et ne tient aucune liste
 * propre — une liste normative à part divergerait dès que le client adapte son
 * cycle. Et le compte affiché est celui du serveur, pas un recalcul local qui
 * pourrait le démentir.
 */
describe('ApqpPpapSummaryComponent', () => {

  let fixture: ComponentFixture<ApqpPpapSummaryComponent>;
  let composant: ApqpPpapSummaryComponent;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ApqpPpapSummaryComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpPpapSummaryComponent);
    composant = fixture.componentInstance;
  });

  function livrable(partiel: Partial<ApqpDeliverable>): ApqpDeliverable {
    return {
      id: partiel.label ?? 'd1',
      position: 1,
      label: 'Livrable',
      ppap: false,
      kind: 'ATTACHMENT',
      done: false,
      evidenceCount: 0,
      ...partiel
    };
  }

  function phase(position: number, titre: string, livrables: ApqpDeliverable[]): ApqpPhase {
    return {
      id: `p${position}`,
      position,
      level: position,
      title: titre,
      purpose: null,
      question: null,
      deliverables: livrables
    };
  }

  it('agrège les livrables étoilés de toutes les phases, dans l\'ordre du cycle', () => {
    composant.phases = [
      phase(1, 'Process Design & Development', [
        livrable({ label: 'Process flow diagram', ppap: true }),
        livrable({ label: 'Floor plan layout' })
      ]),
      phase(2, 'Product and Process Validation', [
        livrable({ label: 'MSA', ppap: true })
      ])
    ];
    composant.total = 2;
    fixture.detectChanges();

    const lignes = hote().querySelectorAll('[data-test=ligne-ppap]');
    expect(lignes.length).toBe(2);
    expect(lignes[0].textContent).toContain('Process flow diagram');
    expect(lignes[0].textContent).toContain('Process Design & Development');
    expect(lignes[1].textContent).toContain('MSA');
  });

  it('disparaît quand le cycle ne compte aucun livrable PPAP, au lieu d\'un cadre vide', () => {
    composant.phases = [phase(1, 'Planning', [livrable({ label: 'Project plan' })])];
    fixture.detectChanges();

    expect(hote().querySelector('[data-test=section-ppap]')).toBeNull();
  });

  it('affiche le compte venu du serveur, pas un compte recalculé', () => {
    // Le serveur dit 7 sur 12 ; la section n'en voit qu'un localement. C'est lui
    // qui fait foi : deux vues du même cycle doivent afficher le même chiffre.
    composant.phases = [phase(1, 'Validation', [livrable({ label: 'MSA', ppap: true })])];
    composant.done = 7;
    composant.total = 12;
    fixture.detectChanges();

    const compte = hote().querySelector('[data-test=compte-ppap]')!.textContent!;
    expect(compte).toContain('7');
    expect(compte).toContain('12');
    expect(composant.pourcentage).toBe(58);
  });

  it('ne divise pas par zéro quand le dossier est vide', () => {
    composant.done = 0;
    composant.total = 0;
    expect(composant.pourcentage).toBe(0);
  });

  it('remonte le livrable cliqué au parent, qui sait ouvrir le popup', () => {
    const etoile = livrable({ label: 'PPAP file and approval form', ppap: true });
    composant.phases = [phase(4, 'Validation', [etoile])];
    composant.total = 1;
    fixture.detectChanges();

    let recu: LignePpap | undefined;
    composant.ouvrir.subscribe(ligne => (recu = ligne));

    hote().querySelector<HTMLButtonElement>('[data-test=ligne-ppap] button')!.click();

    expect(recu?.deliverable).toBe(etoile);
    expect(recu?.phase.title).toBe('Validation');
  });

  it('distingue « acquis » de « prouvé » : la coche et le trombone sont deux lectures', () => {
    composant.phases = [phase(4, 'Validation', [
      livrable({ label: 'MSA', ppap: true, done: true, evidenceCount: 0 }),
      livrable({ label: 'FAIR', ppap: true, done: false, evidenceCount: 2 })
    ])];
    composant.done = 1;
    composant.total = 2;
    fixture.detectChanges();

    const lignes = hote().querySelectorAll('[data-test=ligne-ppap]');
    expect(lignes[0].classList).toContain('ppap__ligne--acquis');
    expect(lignes[0].querySelector('.ppap__preuve')).toBeNull();
    expect(lignes[1].classList).not.toContain('ppap__ligne--acquis');
    expect(lignes[1].querySelector('.ppap__preuve')).not.toBeNull();
  });
});

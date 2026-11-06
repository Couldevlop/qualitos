import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ImpactLevel, StakeholderRow } from '../../stakeholders.types';
import { StakeholderRegisterComponent } from './stakeholder-register.component';

/**
 * Le registre en tableau, et ses filtres.
 *
 * <p>Deux natures de filtre cohabitent, et c'est voulu : le côté et la
 * recherche REMONTENT au serveur (il les sait indexer), le niveau de risque
 * reste local (la criticité se calcule, elle ne se stocke pas).
 */
describe('StakeholderRegisterComponent', () => {

  function row(id: string, criticality: ImpactLevel): StakeholderRow {
    return {
      id, name: `Partie ${id}`, category: 'CUSTOMERS', side: 'EXTERNAL',
      qualityImpact: criticality, regulatoryImpact: 'LOW', criticality,
      relevance: 'RELEVANT', lastReviewedOn: null, nextReviewOn: null,
      reviewOverdue: false, requirementCount: 0
    };
  }

  const ROWS = [row('a', 'HIGH'), row('b', 'MEDIUM'), row('c', 'LOW'), row('d', 'HIGH')];

  let fixture: ComponentFixture<StakeholderRegisterComponent>;
  let component: StakeholderRegisterComponent;

  beforeEach(async () => {
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderRegisterComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(StakeholderRegisterComponent);
    component = fixture.componentInstance;
    component.rows = ROWS;
    fixture.detectChanges();
  });

  it('affiche toutes les lignes sans filtre de risque', () => {
    expect(component.visibleRows.length).toBe(4);
    expect(component.hasFilters).toBeFalse();
  });

  it('ne garde que le niveau de risque retenu', () => {
    component.onCriticality('HIGH');

    expect(component.visibleRows.map(r => r.id)).toEqual(['a', 'd']);
    expect(component.hasFilters).toBeTrue();
  });

  it('revient à tout quand on relâche le filtre', () => {
    component.onCriticality('LOW');
    expect(component.visibleRows.map(r => r.id)).toEqual(['c']);

    component.onCriticality('');

    expect(component.visibleRows.length).toBe(4);
    expect(component.hasFilters).toBeFalse();
  });

  it('le niveau de risque ne remonte PAS au serveur', () => {
    // Il porte sur la criticité, qui se calcule : le serveur ne saurait pas la
    // filtrer sans la stocker, et la stocker la laisserait périmer.
    const emis: unknown[] = [];
    component.filtersChange.subscribe(f => emis.push(f));

    component.onCriticality('HIGH');

    expect(emis).toEqual([]);
  });

  it('efface les trois filtres d un seul geste', () => {
    const emis: { side: string; search: string }[] = [];
    component.filtersChange.subscribe(f => emis.push(f));
    component.side = 'INTERNAL';
    component.search = 'metal';
    component.onCriticality('HIGH');

    component.clear();

    expect(component.criticality).toBe('');
    expect(emis).toEqual([{ side: '', search: '' }]);
  });

  it('le vide filtré se distingue du registre vide', () => {
    component.onCriticality('HIGH');
    component.rows = [row('c', 'LOW')];
    fixture.detectChanges();

    expect(component.visibleRows.length).toBe(0);
    expect(component.hasFilters).toBeTrue();
  });

  it('remonte le côté et la recherche au serveur', () => {
    const emis: { side: string; search: string }[] = [];
    component.filtersChange.subscribe(f => emis.push(f));

    component.onSide('INTERNAL');
    component.search = 'INTERNAL' as never;
    component.onSearch('metal');

    expect(emis[0]).toEqual({ side: 'INTERNAL', search: '' });
    expect(emis[1].search).toBe('metal');
  });

  it('suit les lignes par leur identifiant', () => {
    expect(component.trackById(0, ROWS[0])).toBe('a');
  });

  it('ouvre une ligne au clavier, sans laisser Espace défiler la page', () => {
    const ouvertes: string[] = [];
    component.opened.subscribe(id => ouvertes.push(id));
    const ligne = fixture.nativeElement.querySelector('[data-test="ligne"]') as HTMLElement;
    expect(ligne.getAttribute('tabindex')).withContext('la ligne est atteignable au clavier').toBe('0');

    ligne.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    const espace = new KeyboardEvent('keydown', { key: ' ', bubbles: true, cancelable: true });
    ligne.dispatchEvent(espace);

    expect(ouvertes).toEqual(['a', 'a']);
    // Sans cela, la fiche s'ouvre ET le registre saute d'un écran sous elle.
    expect(espace.defaultPrevented).toBeTrue();
  });
});

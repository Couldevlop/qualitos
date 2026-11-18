import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { FiveWhysSharedModule } from '../../../five-whys/five-whys-shared.module';
import { FiveWhysAnalysis, FiveWhysStep } from '../../../five-whys/five-whys.types';
import { IshikawaSharedModule } from '../../../ishikawa/ishikawa-shared.module';
import { NcCauseAnalysisComponent } from './nc-cause-analysis.component';

/**
 * La cause racine se conclut LA OU L ON LIT LA DESCENTE : dans le D4 du 8D et
 * sur la fiche de l'ecart, et plus seulement sur l'ecran dedie.
 *
 * <p>Les deux ecrans montraient la cascade sans aucun moyen d'en ecrire la
 * conclusion : il fallait quitter la page, retrouver la chaine ailleurs, et y
 * reecrire ce qu'on venait de lire.
 */
describe('NcCauseAnalysisComponent — conclure la cause racine', () => {

  let analyses: jasmine.SpyObj<FiveWhysService>;
  let fixture: ComponentFixture<NcCauseAnalysisComponent>;
  let component: NcCauseAnalysisComponent;

  function pourquoi(combien: number): FiveWhysStep[] {
    return Array.from({ length: combien }, (_, i) => ({
      id: 's' + (i + 1), position: i + 1, answer: 'Parce que ' + (i + 1),
      createdAt: '', updatedAt: ''
    })) as FiveWhysStep[];
  }

  function chaine(combien: number, rootCause?: string): FiveWhysAnalysis {
    return {
      id: 'fw-1', ncId: 'nc-1', ncReference: 'NC-2026-0007',
      problem: 'Fuite au presse-étoupe', rootCause: rootCause ?? null,
      steps: pourquoi(combien), createdAt: '', updatedAt: ''
    } as unknown as FiveWhysAnalysis;
  }

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function un(test: string): HTMLElement | null {
    return hote().querySelector(`[data-test="${test}"]`);
  }

  function monter(chaines: FiveWhysAnalysis[]): void {
    fixture = TestBed.createComponent(NcCauseAnalysisComponent);
    component = fixture.componentInstance;
    component.fiveWhys = chaines;
    component.ishikawa = [];
    fixture.detectChanges();
  }

  beforeEach(async () => {
    analyses = jasmine.createSpyObj<FiveWhysService>('FiveWhysService', ['setRootCause']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [NcCauseAnalysisComponent],
      imports: [SharedModule, UiModule, FiveWhysSharedModule, IshikawaSharedModule,
                NoopAnimationsModule],
      providers: [
        { provide: FiveWhysService, useValue: analyses },
        // Un vrai routeur, et non un espion : les blocs portent le lien vers
        // l'ecran dedie, et `routerLink` construit son adresse au montage.
        provideRouter([])
      ]
    }).compileComponents();
  });

  // ---------- le verrou des trois pourquoi ----------

  /**
   * Conclure au premier ou au deuxieme pourquoi, c'est nommer un SYMPTOME :
   * la methode ne tient que si la descente est allee assez loin. Meme regle
   * que l'ecran dedie et que le serveur -- une seule constante.
   */
  it('se tait avant le troisième pourquoi, et dit pourquoi', () => {
    monter([chaine(2)]);

    expect(component.peutConclure(chaine(2))).toBeFalse();
    expect(un('cause-racine-verrouillee')).not.toBeNull();
    expect(un('cause-racine-champ')).toBeNull();
  });

  it('ouvre la conclusion dès le troisième pourquoi', () => {
    monter([chaine(3)]);

    expect(component.peutConclure(chaine(3))).toBeTrue();
    expect(un('cause-racine-verrouillee')).toBeNull();
    expect(un('cause-racine-champ')).not.toBeNull();
  });

  // ---------- l'envoi ----------

  it('envoie la conclusion saisie et la pose sur la chaîne', () => {
    const c = chaine(3);
    analyses.setRootCause.and.returnValue(
      of({ ...c, rootCause: 'Pas de plan préventif' } as FiveWhysAnalysis));
    monter([c]);
    let emis: FiveWhysAnalysis | null = null;
    component.rootCauseConcluded.subscribe(a => (emis = a));

    component.saisir(c, '  Pas de plan préventif  ');
    component.conclure(c);

    // Le texte part SANS ses espaces de bord : c'est le libelle d'une cause.
    expect(analyses.setRootCause).toHaveBeenCalledWith('fw-1', 'Pas de plan préventif');
    expect(c.rootCause).toBe('Pas de plan préventif');
    expect(emis).toBeTruthy();
  });

  // Un champ vide n'est pas une conclusion : le bouton reste eteint, et
  // l'appel ne part pas meme si on force le clic.
  it('ne conclut rien sur un champ vide', () => {
    const c = chaine(3);
    monter([c]);

    expect(component.conclusionPrete(c)).toBeFalse();
    component.saisir(c, '   ');
    component.conclure(c);

    expect(analyses.setRootCause).not.toHaveBeenCalled();
  });

  // Et pas davantage avant trois pourquoi, meme en appelant la methode
  // directement : la regle est dans le composant, pas seulement dans le gabarit.
  it('ne conclut rien avant le troisième pourquoi', () => {
    const c = chaine(2);
    monter([c]);
    component.saisir(c, 'Pas de plan préventif');

    component.conclure(c);

    expect(analyses.setRootCause).not.toHaveBeenCalled();
  });

  /**
   * Le refus du serveur est AFFICHE. L'avaler laisserait croire que la
   * conclusion est enregistree, et on quitterait l'ecran en le croyant.
   */
  it('affiche le refus du serveur au lieu de laisser croire que c’est écrit', () => {
    const c = chaine(3);
    analyses.setRootCause.and.returnValue(throwError(() => new Error('503')));
    monter([c]);

    component.saisir(c, 'Pas de plan préventif');
    component.conclure(c);
    fixture.detectChanges();

    expect(un('cause-racine-echec')).not.toBeNull();
    expect(c.rootCause).toBeNull();
    // Et on peut reessayer : l'envoi ne reste pas bloque.
    expect(component.envoiEnCours).toBeNull();
  });

  // Deux chaines se concluent SEPAREMENT : un brouillon commun aurait recopie
  // la cause de l'une sous l'autre.
  it('garde un brouillon par chaîne', () => {
    const a = chaine(3);
    const b = { ...chaine(3), id: 'fw-2' } as FiveWhysAnalysis;
    monter([a, b]);

    component.saisir(a, 'Cause A');

    expect(component.brouillon(a)).toBe('Cause A');
    expect(component.brouillon(b)).toBe('');
  });
});

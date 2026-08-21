import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { CompetencyGrid } from '../../training.types';
import { CompetencyMatrixComponent } from './competency-matrix.component';

/**
 * La matrice de compétences.
 *
 * <p>Le point qui compte, et qu'aucun autre écran ne protège : une case jamais
 * évaluée doit rester VIDE. L'afficher en zéro affirmerait une incompétence que
 * personne n'a constatée — et fausserait la lecture de couverture qui fait tout
 * l'intérêt de la figure.
 */
describe('CompetencyMatrixComponent', () => {

  const grid = (over: Partial<CompetencyGrid> = {}): CompetencyGrid => ({
    people: [
      { userId: 'u-1', label: 'Anna Dubois' },
      { userId: 'u-2', label: 'Boris Lemaire' }
    ],
    groups: [
      {
        category: 'Gestion de projet',
        rows: [
          { skillId: 's-1', code: 'PLAN', name: 'Planification',
            levels: [4, 3], holders: 2, singlePointOfKnowledge: false },
          { skillId: 's-2', code: 'RISK', name: 'Gestion des risques',
            levels: [3, null], holders: 1, singlePointOfKnowledge: true }
        ]
      }
    ],
    ...over
  });

  let fixture: ComponentFixture<CompetencyMatrixComponent>;
  let component: CompetencyMatrixComponent;
  let service: jasmine.SpyObj<TrainingService>;
  let snack: { open: jasmine.Spy };

  beforeEach(async () => {
    service = jasmine.createSpyObj<TrainingService>('TrainingService', ['competencyMatrix']);
    snack = { open: jasmine.createSpy('open') };
    await TestBed.configureTestingModule({
      declarations: [CompetencyMatrixComponent],
      imports: [SharedModule, UiModule, FormsModule, MatSlideToggleModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CompetencyMatrixComponent);
    component = fixture.componentInstance;
  });

  it('dresse une colonne par personne évaluée et une ligne par compétence', fakeAsync(() => {
    service.competencyMatrix.and.returnValue(of(grid()));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const entetes = fixture.nativeElement.querySelectorAll('thead th');
    expect(entetes.length).toBe(3);
    expect(entetes[1].textContent).toContain('Anna Dubois');
    expect(fixture.nativeElement.textContent).toContain('Planification');
  }));

  it('laisse vide une case jamais évaluée, sans la confondre avec un zéro', fakeAsync(() => {
    service.competencyMatrix.and.returnValue(of(grid()));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.cellClass(null)).toBe('cell-unknown');
    expect(component.cellClass(0)).toBe('cell-none');
    expect(component.levelLabel(null)).toBe('');
    expect(fixture.nativeElement.querySelector('.cell-unknown')).toBeTruthy();
  }));

  it('signale par du TEXTE la compétence qui ne tient qu’à une personne', fakeAsync(() => {
    // Une couleur seule ne se lit ni au clavier, ni en daltonisme, ni à l'impression.
    service.competencyMatrix.and.returnValue(of(grid()));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Un seul détenteur');
    expect(component.atRiskCount()).toBe(1);
  }));

  it('filtre sur les compétences à risque sans relancer de requête', fakeAsync(() => {
    service.competencyMatrix.and.returnValue(of(grid()));
    fixture.detectChanges();
    tick();
    service.competencyMatrix.calls.reset();

    component.onlyAtRisk = true;
    fixture.detectChanges();

    expect(component.rowsOf(grid().groups[0].rows).length).toBe(2);
    expect(service.competencyMatrix).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Gestion des risques');
    expect(fixture.nativeElement.textContent).not.toContain('Planification');
  }));

  it('n’affiche pas un titre de famille dont toutes les lignes sont filtrées', () => {
    component.onlyAtRisk = true;
    const sansRisque = grid().groups[0].rows.filter(r => !r.singlePointOfKnowledge);

    expect(component.hasVisibleRows(sansRisque)).toBeFalse();
  });

  it('nomme les niveaux tels qu’ils sont stockés, de zéro à quatre', () => {
    expect(component.levelLabel(0)).toBe('Aucun');
    expect(component.levelLabel(4)).toBe('Expert');
  });

  it('explique l’absence de catalogue au lieu d’afficher un tableau vide', fakeAsync(() => {
    service.competencyMatrix.and.returnValue(of({ people: [], groups: [] }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  }));

  it('distingue « catalogue vide » de « personne évaluée »', fakeAsync(() => {
    service.competencyMatrix.and.returnValue(of(grid({ people: [] })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('personne n\'a encore été évalué');
  }));

  it('cesse de faire patienter quand le chargement échoue', fakeAsync(() => {
    service.competencyMatrix.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();
    tick();

    expect(component.loading).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Impossible de charger');
  }));
});

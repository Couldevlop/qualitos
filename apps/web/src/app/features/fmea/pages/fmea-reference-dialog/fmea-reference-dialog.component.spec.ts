import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  FMEA_DETECTION_SCALE,
  FMEA_EXAMPLE_ROWS,
  FMEA_OCCURRENCE_SCALE,
  FMEA_SEVERITY_SCALE
} from '../../fmea.reference';
import { FmeaReferenceDialogComponent } from './fmea-reference-dialog.component';

/**
 * Le référentiel n'a qu'un devoir : montrer les barèmes en entier et sans les
 * altérer. Un barème tronqué ou renuméroté vaut moins que pas de barème du
 * tout — l'évaluateur croirait coter sur l'échelle officielle.
 */
describe('FmeaReferenceDialogComponent', () => {
  let component: FmeaReferenceDialogComponent;
  let fixture: ComponentFixture<FmeaReferenceDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FmeaReferenceDialogComponent>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<FmeaReferenceDialogComponent>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [FmeaReferenceDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [{ provide: MatDialogRef, useValue: dialogRef }]
    }).compileComponents();

    fixture = TestBed.createComponent(FmeaReferenceDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('porte les trois barèmes complets, de 10 à 1', () => {
    expect(component.severity.length).toBe(10);
    expect(component.occurrence.length).toBe(10);
    expect(component.detection.length).toBe(10);

    // Décroissant : le plus grave en tête, comme dans le référentiel d'origine.
    expect(component.severity.map(r => r.score)).toEqual([10, 9, 8, 7, 6, 5, 4, 3, 2, 1]);
    expect(component.occurrence.map(r => r.score)).toEqual([10, 9, 8, 7, 6, 5, 4, 3, 2, 1]);
    expect(component.detection.map(r => r.score)).toEqual([10, 9, 8, 7, 6, 5, 4, 3, 2, 1]);
  });

  it('reproduit le texte du référentiel, sans le reformuler', () => {
    expect(FMEA_SEVERITY_SCALE[0].effect).toContain('Hazardous');
    expect(FMEA_DETECTION_SCALE[0].chance).toBe('Nearly impossible');
    expect(FMEA_OCCURRENCE_SCALE[0].timePeriod).toBe('More than once per day');
  });

  it('affiche le barème de sévérité dès l\'ouverture', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('May expose client to loss or harm without warning.');
  });

  it('embarque l\'exemple de PFMEA en entier', () => {
    expect(component.exampleRows.length).toBe(FMEA_EXAMPLE_ROWS.length);
    expect(component.exampleRows.length).toBe(15);
    expect(component.exampleTitle).toContain('Wiring Harness');

    // Le RPN est le produit des trois cotations : s'il ne l'était pas, la
    // référence enseignerait un calcul faux.
    for (const r of component.exampleRows) {
      expect(r.rpn).toBe(r.severity * r.occurrence * r.detection);
    }
  });

  it('colore les scores du plus grave au plus anodin', () => {
    expect(component.scoreClass(10)).toContain('critical');
    expect(component.scoreClass(9)).toContain('critical');
    expect(component.scoreClass(8)).toContain('high');
    expect(component.scoreClass(7)).toContain('high');
    expect(component.scoreClass(6)).toContain('medium');
    expect(component.scoreClass(4)).toContain('medium');
    expect(component.scoreClass(3)).toContain('low');
    expect(component.scoreClass(1)).toContain('low');
  });

  it('signale les RPN élevés, seul repère de hiérarchie de l\'exemple', () => {
    expect(component.rpnClass(216)).toContain('critical');
    expect(component.rpnClass(160)).toContain('high');
    expect(component.rpnClass(80)).toBe('rpn');
  });

  it('se ferme sans rien renvoyer : il ne modifie aucune donnée', () => {
    component.close();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

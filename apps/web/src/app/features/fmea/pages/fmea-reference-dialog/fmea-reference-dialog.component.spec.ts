import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FMEA_EXAMPLE_ROWS } from '../../fmea.reference';
import { FmeaService } from '../../fmea.service';
import { FmeaScaleReference, FmeaScaleRow, FmeaScaleView } from '../../fmea.types';
import { FmeaReferenceDialogComponent } from './fmea-reference-dialog.component';

/**
 * Le référentiel de cotation.
 *
 * <p>Deux promesses. La première : montrer les barèmes en entier et sans les
 * altérer — un barème tronqué vaut moins que pas de barème, l'évaluateur croirait
 * coter sur l'échelle officielle. La seconde : dire SUR QUEL barème
 * l'organisation cote, le sien ou celui de référence, parce que deux RPN issus
 * de barèmes différents ne se comparent pas.
 */
describe('FmeaReferenceDialogComponent', () => {
  let component: FmeaReferenceDialogComponent;
  let fixture: ComponentFixture<FmeaReferenceDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FmeaReferenceDialogComponent>>;
  let fmea: jasmine.SpyObj<FmeaService>;
  let auth: jasmine.SpyObj<AuthService>;
  let snack: { open: jasmine.Spy };

  /** Dix lignes, de 10 à 1 : un barème complet. */
  const rows = (prefix = 'Niveau'): FmeaScaleRow[] =>
    Array.from({ length: 10 }, (_, i) => ({
      score: 10 - i, label: `${prefix} ${10 - i}`, description: `Ce que vaut un ${10 - i}`
    }));

  const scale = (over: Partial<FmeaScaleView> = {}): FmeaScaleView => ({
    kind: 'SEVERITY', custom: false, rows: rows(), ...over
  });

  const reference = (over: Partial<FmeaScaleView> = {}): FmeaScaleReference => ({
    scales: [
      scale(over),
      { kind: 'OCCURRENCE', custom: false, rows: rows('Fréquence') },
      { kind: 'DETECTION', custom: false, rows: rows('Détection') }
    ]
  });

  async function setup(roles: string[] = []): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<FmeaReferenceDialogComponent>>(
      'MatDialogRef', ['close']);
    fmea = jasmine.createSpyObj<FmeaService>('FmeaService',
      ['ratingScales', 'replaceRatingScale', 'revertRatingScale']);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    snack = { open: jasmine.createSpy('open') };

    auth.hasAnyRole.and.callFake((wanted: string[]) =>
      wanted.some(role => roles.includes(role)));
    fmea.ratingScales.and.returnValue(of(reference()));

    await TestBed.configureTestingModule({
      declarations: [FmeaReferenceDialogComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: FmeaService, useValue: fmea },
        { provide: AuthService, useValue: auth },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FmeaReferenceDialogComponent);
    component = fixture.componentInstance;
  }

  // ---------- lecture ----------

  it('sert les trois barèmes du tenant, complets de 10 à 1', fakeAsync(async () => {
    await setup();
    fixture.detectChanges();
    tick();

    expect(fmea.ratingScales).toHaveBeenCalled();
    for (const kind of component.kinds) {
      expect(component.rows(kind).length).toBe(10);
      expect(component.rows(kind).map(r => r.score))
        .toEqual([10, 9, 8, 7, 6, 5, 4, 3, 2, 1]);
    }
  }));

  it('dit quand l\'organisation cote sur SON barème', fakeAsync(async () => {
    await setup();
    fmea.ratingScales.and.returnValue(of(reference({ custom: true })));
    fixture.detectChanges();
    tick();

    // Ce n'est pas cosmétique : deux RPN issus de barèmes différents ne se
    // comparent pas, et l'écran doit pouvoir le signaler.
    expect(component.isCustom('SEVERITY')).toBeTrue();
    expect(component.isCustom('DETECTION')).toBeFalse();
  }));

  it('embarque l\'exemple de PFMEA en entier, avec des RPN qui tombent juste',
    fakeAsync(async () => {
      await setup();
      fixture.detectChanges();
      tick();

      expect(component.exampleRows.length).toBe(FMEA_EXAMPLE_ROWS.length);
      expect(component.exampleRows.length).toBe(15);
      for (const r of component.exampleRows) {
        expect(r.rpn).toBe(r.severity * r.occurrence * r.detection);
      }
    }));

  it('dit que le référentiel est indisponible plutôt que d\'afficher un écran vide',
    fakeAsync(async () => {
      await setup();
      fmea.ratingScales.and.returnValue(throwError(() => ({ status: 503 })));
      fixture.detectChanges();
      tick();

      expect(component.error$.value).toBeTruthy();
    }));

  // ---------- qui peut modifier ----------

  it('ne propose pas de modifier le barème à un manager qualité', fakeAsync(async () => {
    await setup(['QUALITY_MANAGER']);
    fixture.detectChanges();
    tick();

    // Il cote ; il ne redéfinit pas l'échelle sur laquelle il cote.
    expect(component.canEdit).toBeFalse();
  }));

  it('le propose à la direction qualité', fakeAsync(async () => {
    await setup(['DIRECTOR_QUALITY']);
    fixture.detectChanges();
    tick();

    expect(component.canEdit).toBeTrue();
  }));

  // ---------- modification ----------

  it('travaille sur une COPIE : annuler ne laisse aucune trace', fakeAsync(async () => {
    await setup(['DIRECTOR_QUALITY']);
    fixture.detectChanges();
    tick();

    component.startEditing('SEVERITY');
    component.draft[0].label = 'Saisie abandonnée';
    component.cancelEditing();

    expect(component.rows('SEVERITY')[0].label).toBe('Niveau 10');
    expect(component.editing).toBeNull();
  }));

  it('envoie les dix lignes d\'un bloc', fakeAsync(async () => {
    await setup(['DIRECTOR_QUALITY']);
    fixture.detectChanges();
    tick();
    fmea.replaceRatingScale.and.returnValue(of(scale({ custom: true })));

    component.startEditing('SEVERITY');
    component.draft[0].label = '  Arrêt de ligne client  ';
    component.save();
    tick();

    const [kind, sent] = fmea.replaceRatingScale.calls.mostRecent().args;
    expect(kind).toBe('SEVERITY');
    expect(sent.length).toBe(10);
    // Les blancs de saisie ne font pas partie du barème.
    expect(sent[0].label).toBe('Arrêt de ligne client');
    expect(component.isCustom('SEVERITY')).toBeTrue();
    expect(component.editing).toBeNull();
  }));

  it('refuse d\'enregistrer tant qu\'un score n\'a pas d\'intitulé', fakeAsync(async () => {
    await setup(['DIRECTOR_QUALITY']);
    fixture.detectChanges();
    tick();

    component.startEditing('SEVERITY');
    component.draft[2].label = '   ';

    // Un score sans définition fait coter au jugé, exactement là où le barème
    // existe pour l'éviter.
    expect(component.incompleteScores).toEqual([8]);
    expect(component.canSave).toBeFalse();

    component.save();
    expect(fmea.replaceRatingScale).not.toHaveBeenCalled();
  }));

  it('garde la saisie à l\'écran quand le serveur refuse le barème', fakeAsync(async () => {
    await setup(['DIRECTOR_QUALITY']);
    fixture.detectChanges();
    tick();
    fmea.replaceRatingScale.and.returnValue(throwError(() => ({ status: 400 })));

    component.startEditing('SEVERITY');
    component.draft[0].label = 'Refusé par le serveur';
    component.save();
    tick();

    // Perdre dix lignes de saisie sur un refus obligerait à tout retaper.
    expect(component.editing).toBe('SEVERITY');
    expect(component.draft[0].label).toBe('Refusé par le serveur');
    expect(snack.open).toHaveBeenCalled();
  }));

  it('rétablit le barème de référence', fakeAsync(async () => {
    await setup(['DIRECTOR_QUALITY']);
    fmea.ratingScales.and.returnValue(of(reference({ custom: true })));
    fixture.detectChanges();
    tick();
    fmea.revertRatingScale.and.returnValue(of(scale({ custom: false })));

    component.revert('SEVERITY');
    tick();

    expect(fmea.revertRatingScale).toHaveBeenCalledWith('SEVERITY');
    expect(component.isCustom('SEVERITY')).toBeFalse();
  }));

  // ---------- présentation ----------

  it('colore les scores du plus grave au plus anodin', fakeAsync(async () => {
    await setup();
    expect(component.scoreClass(10)).toContain('critical');
    expect(component.scoreClass(9)).toContain('critical');
    expect(component.scoreClass(8)).toContain('high');
    expect(component.scoreClass(7)).toContain('high');
    expect(component.scoreClass(6)).toContain('medium');
    expect(component.scoreClass(4)).toContain('medium');
    expect(component.scoreClass(3)).toContain('low');
    expect(component.scoreClass(1)).toContain('low');
  }));

  it('signale les RPN élevés, seul repère de hiérarchie de l\'exemple',
    fakeAsync(async () => {
      await setup();
      expect(component.rpnClass(216)).toContain('critical');
      expect(component.rpnClass(160)).toContain('high');
      expect(component.rpnClass(80)).toBe('rpn');
    }));

  it('se ferme sans rien renvoyer', fakeAsync(async () => {
    await setup();
    component.close();
    expect(dialogRef.close).toHaveBeenCalledWith();
  }));
});

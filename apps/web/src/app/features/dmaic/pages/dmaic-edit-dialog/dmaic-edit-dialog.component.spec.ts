import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DmaicService } from '../../dmaic.service';
import { DmaicProjectResponse } from '../../dmaic.types';
import { DmaicEditDialogComponent } from './dmaic-edit-dialog.component';

/**
 * Édition d'un projet DMAIC (§3.4).
 *
 * Ce dialogue est presque entièrement fait de VALEURS PAR DÉFAUT : dix champs
 * facultatifs, chacun avec un repli à l'ouverture (`?? ''`, `?? null`) et une
 * omission à l'envoi (`|| undefined`, `?? undefined`). C'est là que se logent les
 * erreurs silencieuses — un champ vidé par l'utilisateur qui repart tel quel, ou
 * une limite de spécification à 0 confondue avec « non renseignée ». Les deux
 * côtés de chaque repli sont donc exercés.
 */
describe('DmaicEditDialogComponent', () => {
  let fixture: ComponentFixture<DmaicEditDialogComponent>;
  let component: DmaicEditDialogComponent;
  let svc: jasmine.SpyObj<DmaicService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DmaicEditDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const updated = { id: 'p-1', title: 'Projet' } as DmaicProjectResponse;

  /** Projet minimal : tous les champs facultatifs absents. */
  const bare = (): DmaicProjectResponse => ({
    id: 'p-1', tenantId: 't1', title: 'Réduction du taux de rebut',
    phase: 'DEFINE', status: 'ACTIVE', blackBeltId: 'bb-1',
    measureCount: 0, pokaYokeCount: 0,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  });

  /** Projet complet : tous les champs facultatifs renseignés. */
  const full = (): DmaicProjectResponse => ({
    ...bare(),
    problemStatement: 'Taux de rebut à 4,2 % sur la ligne 3.',
    goalStatement: 'Descendre sous 1,5 % en six mois.',
    championId: 'champion-1',
    targetCompletionDate: '2026-12-31',
    specLowerLimit: 9.8, specUpperLimit: 10.2, specTarget: 10,
    specUnit: 'mm',
    estimatedSavingsEur: 120000
  });

  function build(project: DmaicProjectResponse): void {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<DmaicService>('DmaicService', ['updateProject']);
    svc.updateProject.and.returnValue(of(updated));
    dialogRef = jasmine.createSpyObj<MatDialogRef<DmaicEditDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      declarations: [DmaicEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: DmaicService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { project } }
      ]
    });

    fixture = TestBed.createComponent(DmaicEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // ---- Ouverture : repli des champs absents -----------------------------------

  it('pré-remplit le formulaire avec le projet complet', () => {
    build(full());

    const v = component.form.getRawValue();
    expect(v.title).toBe('Réduction du taux de rebut');
    expect(v.problemStatement).toContain('4,2 %');
    expect(v.goalStatement).toContain('1,5 %');
    expect(v.championId).toBe('champion-1');
    expect(v.targetCompletionDate).toBe('2026-12-31');
    expect(v.specLowerLimit).toBe(9.8);
    expect(v.specUpperLimit).toBe(10.2);
    expect(v.specTarget).toBe(10);
    expect(v.specUnit).toBe('mm');
    expect(v.estimatedSavingsEur).toBe(120000);
  });

  it('ouvre les champs texte à vide et les champs numériques à null quand ils sont absents', () => {
    build(bare());

    const v = component.form.getRawValue();
    // Texte à '' : un `undefined` dans un champ de saisie afficherait la chaîne
    // « undefined » à l'écran.
    expect(v.problemStatement).toBe('');
    expect(v.goalStatement).toBe('');
    expect(v.championId).toBe('');
    expect(v.targetCompletionDate).toBe('');
    expect(v.specUnit).toBe('');
    // Numériques à null : 0 est une valeur légitime de spécification, le repli
    // ne doit donc pas être 0.
    expect(v.specLowerLimit).toBeNull();
    expect(v.specUpperLimit).toBeNull();
    expect(v.specTarget).toBeNull();
    expect(v.estimatedSavingsEur).toBeNull();
  });

  // ---- Validation ---------------------------------------------------------------

  it('refuse un intitulé vidé', () => {
    build(full());
    component.form.patchValue({ title: '' });

    component.submit();

    expect(svc.updateProject).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('refuse un intitulé ou une unité au-delà des limites du serveur', () => {
    build(full());

    component.form.patchValue({ title: 'x'.repeat(256) });
    component.submit();
    expect(svc.updateProject).not.toHaveBeenCalled();

    component.form.patchValue({ title: 'Projet', specUnit: 'u'.repeat(51) });
    component.submit();
    expect(svc.updateProject).not.toHaveBeenCalled();
  });

  it('n\'envoie pas deux fois pendant un envoi en cours', () => {
    build(full());
    component.submitting = true;

    component.submit();

    expect(svc.updateProject).not.toHaveBeenCalled();
  });

  // ---- Envoi : omission des champs vidés -----------------------------------------

  it('envoie tous les champs renseignés, nettoyés', () => {
    build(full());
    component.form.patchValue({ title: '  Titre révisé  ', specUnit: '  mm  ' });

    component.submit();

    expect(svc.updateProject).toHaveBeenCalledWith('p-1', {
      title: 'Titre révisé',
      problemStatement: 'Taux de rebut à 4,2 % sur la ligne 3.',
      goalStatement: 'Descendre sous 1,5 % en six mois.',
      championId: 'champion-1',
      targetCompletionDate: '2026-12-31',
      specLowerLimit: 9.8,
      specUpperLimit: 10.2,
      specTarget: 10,
      specUnit: 'mm',
      estimatedSavingsEur: 120000
    });
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('omet les champs vidés par l\'utilisateur plutôt que d\'envoyer du vide', () => {
    build(bare());
    component.form.patchValue({ title: 'Projet' });

    component.submit();

    expect(svc.updateProject).toHaveBeenCalledWith('p-1', {
      title: 'Projet',
      problemStatement: undefined,
      goalStatement: undefined,
      championId: undefined,
      targetCompletionDate: undefined,
      specLowerLimit: undefined,
      specUpperLimit: undefined,
      specTarget: undefined,
      specUnit: undefined,
      estimatedSavingsEur: undefined
    });
  });

  it('conserve une limite de spécification à zéro, qui est une valeur légitime', () => {
    build(bare());
    component.form.patchValue({
      title: 'Projet', specLowerLimit: 0, specTarget: 0, estimatedSavingsEur: 0
    });

    component.submit();

    // `?? undefined` et non `|| undefined` : un seuil bas à 0 mm ou une économie
    // estimée nulle sont des saisies valides, qui disparaîtraient avec `||`.
    expect(svc.updateProject).toHaveBeenCalledWith('p-1', jasmine.objectContaining({
      specLowerLimit: 0, specTarget: 0, estimatedSavingsEur: 0
    }));
  });

  it('omet un texte réduit à des espaces', () => {
    build(full());
    component.form.patchValue({ problemStatement: '   ', championId: '  ' });

    component.submit();

    expect(svc.updateProject).toHaveBeenCalledWith('p-1', jasmine.objectContaining({
      problemStatement: undefined, championId: undefined
    }));
  });

  // ---- Cas dégradés ----------------------------------------------------------------

  it('laisse le dialogue ouvert et explique quand le serveur refuse', () => {
    build(full());
    svc.updateProject.and.returnValue(throwError(() => ({ status: 409 })));

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    // La saisie doit rester récupérable : refermer ferait perdre le travail.
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien enregistrer à l\'annulation', () => {
    build(full());

    component.cancel();

    expect(svc.updateProject).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

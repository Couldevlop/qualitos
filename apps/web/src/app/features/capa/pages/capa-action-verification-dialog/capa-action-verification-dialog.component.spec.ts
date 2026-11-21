import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaActionResponse } from '../../capa.types';
import {
  CapaActionVerificationDialogComponent, CapaActionVerificationDialogData
} from './capa-action-verification-dialog.component';

/**
 * JUGER L'EFFICACITÉ D'UNE ACTION (ADR 0132).
 *
 * <p>Une action « faite » dit que le geste a eu lieu, pas qu'il a produit son
 * effet. Ce que ces bancs tiennent : on ne juge pas sans critère annoncé, le
 * verdict repose sur un relevé, et le critère ne se réécrit pas ici — il a été
 * posé à l'écriture de l'action (ADR 0108, 0131).
 */
describe('CapaActionVerificationDialogComponent', () => {

  let fixture: ComponentFixture<CapaActionVerificationDialogComponent>;
  let composant: CapaActionVerificationDialogComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaActionVerificationDialogComponent, boolean>>;

  const action = (p: Partial<CapaActionResponse> = {}): CapaActionResponse => ({
    id: 'a1',
    capaId: 'c1',
    title: 'Recalibrer la presse',
    status: 'DONE',
    actionType: 'CORRECTIVE',
    verificationRequired: true,
    effectivenessCriterion: 'Aucun rebut pendant 3 mois',
    ...p
  } as CapaActionResponse);

  async function setup(data: Partial<CapaActionVerificationDialogData> = {}): Promise<void> {
    capa = jasmine.createSpyObj<CapaService>('CapaService', ['verifyActionEffectiveness']);
    capa.verifyActionEffectiveness.and.returnValue(of(action()));
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [CapaActionVerificationDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { caseId: 'c1', action: action(), ...data }
        },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaActionVerificationDialogComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  }

  const un = (test: string): HTMLElement | null =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-test="${test}"]`);

  it('montre le critère de l’action, en lecture', async () => {
    await setup();
    expect(composant.critere).toBe('Aucun rebut pendant 3 mois');
    expect(un('action-verif-critere')!.textContent).toContain('Aucun rebut pendant 3 mois');
    // Pas de champ : le rouvrir reviendrait à choisir la cible après avoir vu
    // le résultat (ADR 0108).
    expect(un('action-verif-critere')!.querySelector('input')).toBeNull();
  });

  /** Le cas des actions antérieures aux colonnes de la V177. */
  it('retombe sur le critère du dossier quand l’action n’a pas le sien', async () => {
    await setup({
      action: action({ effectivenessCriterion: undefined }),
      caseCriterion: 'Zéro retard sur 620 expéditions'
    });
    expect(composant.critere).toBe('Zéro retard sur 620 expéditions');
    expect(composant.sansCritere).toBeFalse();
  });

  it('annonce AVANT le clic qu’aucun critère n’a été posé, et bloque', async () => {
    await setup({ action: action({ effectivenessCriterion: undefined }) });

    expect(composant.sansCritere).toBeTrue();
    expect(un('action-verif-sans-critere')).not.toBeNull();
    expect(un('action-verif-critere')).toBeNull();
    composant.choose('EFFECTIVE');
    composant.form.patchValue({ observed: '0 rebut' });
    expect(composant.complete).toBeFalse();
  });

  it('exige un verdict ET un relevé : un verdict sans mesure est une opinion', async () => {
    await setup();

    expect(composant.complete).toBeFalse();

    composant.choose('EFFECTIVE');
    expect(composant.complete).withContext('sans relevé').toBeFalse();

    composant.form.patchValue({ observed: '0 rebut sur 3 mois' });
    expect(composant.complete).toBeTrue();
  });

  it('propose les trois verdicts, et dit ce que le choix va déclencher', async () => {
    await setup();
    expect(composant.verdicts).toEqual(['EFFECTIVE', 'PARTIAL', 'INEFFECTIVE']);
    expect(un('action-verif-verdicts')!.querySelectorAll('button').length).toBe(3);

    composant.choose('PARTIAL');
    fixture.detectChanges();
    expect(un('action-verif-suite')!.textContent).toContain('retenir la clôture');
  });

  it('envoie le verdict, le relevé et les bornes, puis ferme en annonçant l’écriture', async () => {
    await setup();
    composant.choose('PARTIAL');
    composant.form.patchValue({
      baseline: ' 4,2 % ', target: ' < 1 % ', observed: ' 2,1 % ', comment: ' Deux cycles '
    });

    composant.submit();

    expect(capa.verifyActionEffectiveness).toHaveBeenCalledWith('c1', 'a1', {
      verdict: 'PARTIAL',
      observed: '2,1 %',
      baseline: '4,2 %',
      target: '< 1 %',
      comment: 'Deux cycles'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('n’emporte pas de chaîne vide : une borne non saisie est une absence', async () => {
    await setup();
    composant.choose('EFFECTIVE');
    composant.form.patchValue({ observed: '0 rebut' });

    composant.submit();

    const corps = capa.verifyActionEffectiveness.calls.mostRecent().args[2];
    expect(corps.baseline).toBeUndefined();
    expect(corps.target).toBeUndefined();
    expect(corps.comment).toBeUndefined();
  });

  it('ne ferme pas sur un refus du serveur : la saisie reste à corriger', async () => {
    await setup();
    capa.verifyActionEffectiveness.and.returnValue(throwError(() => ({ status: 409 })));
    composant.choose('EFFECTIVE');
    composant.form.patchValue({ observed: '0 rebut' });

    composant.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(composant.submitting$.value).toBeFalse();
  });

  it('n’envoie rien deux fois : un double clic n’écrit pas deux jugements', async () => {
    await setup();
    composant.choose('EFFECTIVE');
    composant.form.patchValue({ observed: '0 rebut' });
    composant.submitting$.next(true);

    composant.submit();

    expect(capa.verifyActionEffectiveness).not.toHaveBeenCalled();
  });
});

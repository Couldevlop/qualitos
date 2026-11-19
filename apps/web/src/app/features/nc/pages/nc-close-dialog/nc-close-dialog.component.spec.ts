import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { MatRadioModule } from '@angular/material/radio';
import {
  NcCloseDialogComponent, NcCloseDialogData, NcCloseDialogResult
} from './nc-close-dialog.component';

/**
 * CLORE UN ÉCART, C'EST AUSSI DIRE CE QU'IL A ENSEIGNÉ.
 *
 * <p>Un écart traité apprend quelque chose sur l'analyse des risques du
 * produit, et l'IATF 16949 en fait une exigence — toute non-conformité se
 * répercute sur la PFMEA et le plan de surveillance. La clôture ne demandait
 * rien : l'écart se refermait sans que personne n'ait eu à se prononcer.
 */
describe('NcCloseDialogComponent', () => {

  let fixture: ComponentFixture<NcCloseDialogComponent>;
  let component: NcCloseDialogComponent;
  let ref: jasmine.SpyObj<MatDialogRef<NcCloseDialogComponent, NcCloseDialogResult>>;

  async function monter(data: NcCloseDialogData): Promise<void> {
    ref = jasmine.createSpyObj<MatDialogRef<NcCloseDialogComponent, NcCloseDialogResult>>(
      'MatDialogRef', ['close']);
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [NcCloseDialogComponent],
      imports: [SharedModule, ReactiveFormsModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NcCloseDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const ECART: NcCloseDialogData = { reference: 'Int-NC-2026-0044' };

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  /**
   * AUCUNE RÉPONSE N'EST PRÉSÉLECTIONNÉE.
   *
   * <p>Un « non » proposé d'avance serait coché par le seul effet du clic
   * suivant, et la fiche porterait une décision que personne n'a prise. Le
   * bouton reste donc éteint jusqu'à ce que les deux questions soient tranchées.
   */
  it('n’offre aucune réponse par défaut, et le bouton reste éteint', async () => {
    await monter(ECART);

    expect(component.form.controls.pfmeaImpacted.value).toBeNull();
    expect(component.form.controls.controlPlanImpacted.value).toBeNull();
    expect(component.form.invalid).toBeTrue();
    const bouton = hote().querySelector<HTMLButtonElement>('[data-test="nc-close-valider"]');
    expect(bouton?.disabled).toBeTrue();
  });

  /** Une seule des deux questions tranchée ne suffit pas : les deux sont dues. */
  it('exige les DEUX réponses', async () => {
    await monter(ECART);
    component.form.controls.pfmeaImpacted.setValue(false);
    fixture.detectChanges();

    expect(component.form.invalid).toBeTrue();
    component.form.controls.controlPlanImpacted.setValue(false);
    fixture.detectChanges();
    expect(component.form.valid).toBeTrue();
  });

  /**
   * « NON » EST UNE DÉCISION, et c'est elle qui est rendue — pas un vide. Elle
   * atteste qu'on a examiné l'analyse de risque et jugé qu'elle n'était pas en
   * cause ; c'est ce que l'auditeur vient lire quand le PFMEA n'a pas bougé.
   */
  it('rend « non, non » comme une décision', async () => {
    await monter(ECART);
    component.form.setValue({ pfmeaImpacted: false, controlPlanImpacted: false });

    component.valider();

    expect(ref.close).toHaveBeenCalledWith({
      pfmeaImpacted: false, controlPlanImpacted: false
    });
  });

  it('rend les deux « oui »', async () => {
    await monter(ECART);
    component.form.setValue({ pfmeaImpacted: true, controlPlanImpacted: true });

    component.valider();

    expect(ref.close).toHaveBeenCalledWith({
      pfmeaImpacted: true, controlPlanImpacted: true
    });
  });

  /** Un formulaire incomplet ne rend rien, et montre ce qui manque. */
  it('ne rend rien tant qu’une question n’est pas tranchée', async () => {
    await monter(ECART);
    component.form.controls.pfmeaImpacted.setValue(true);

    component.valider();

    expect(ref.close).not.toHaveBeenCalled();
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="nc-close-control-plan-requis"]')).not.toBeNull();
  });

  /**
   * CE QUE LE « OUI » ENGAGE SE DIT AVANT LE CLIC : découvrir au refus qu'une
   * révision est exigée fait passer une règle pour une panne.
   */
  it('annonce le verrou dès qu’un « oui » est coché', async () => {
    await monter(ECART);
    expect(hote().querySelector('[data-test="nc-close-consequence"]')).toBeNull();

    component.form.controls.pfmeaImpacted.setValue(true);
    fixture.detectChanges();

    expect(component.exigeUneRevision()).toBeTrue();
    expect(hote().querySelector('[data-test="nc-close-consequence"]')).not.toBeNull();
  });

  /** Le plan de surveillance seul annonce le même verrou. */
  it('annonce le verrou pour le plan de surveillance seul', async () => {
    await monter(ECART);
    component.form.controls.controlPlanImpacted.setValue(true);
    fixture.detectChanges();

    expect(hote().querySelector('[data-test="nc-close-consequence"]')).not.toBeNull();
  });

  /**
   * LES RÉPONSES DÉJÀ DONNÉES SONT REPRISES : une clôture retenue par le verrou
   * qu'elles ont posé n'a pas à les redemander — et on voit ce qu'on avait dit,
   * pour pouvoir le corriger.
   */
  it('relit les réponses déjà données', async () => {
    await monter({ reference: 'Int-NC-2026-0044', pfmeaImpacted: true, controlPlanImpacted: false });

    expect(component.form.controls.pfmeaImpacted.value).toBeTrue();
    expect(component.form.controls.controlPlanImpacted.value).toBeFalse();
    expect(component.form.valid).toBeTrue();
  });

  /** La référence de l'écart est montrée : on clôt celui-là, pas un autre. */
  it('nomme l’écart qu’on va clore', async () => {
    await monter(ECART);

    expect(hote().textContent).toContain('Int-NC-2026-0044');
  });

  it('renonce sans rien rendre', async () => {
    await monter(ECART);

    component.annuler();

    expect(ref.close).toHaveBeenCalledWith();
  });

  /**
   * Chaque question porte sa propre légende : les deux documents se révisent
   * séparément, et un intitulé unique laisserait chercher lequel est en cause.
   */
  it('pose DEUX questions distinctes', async () => {
    await monter(ECART);

    expect(hote().querySelector('[data-test="nc-close-pfmea"]')).not.toBeNull();
    expect(hote().querySelector('[data-test="nc-close-control-plan"]')).not.toBeNull();
    expect(hote().querySelectorAll('fieldset.question').length).toBe(2);
  });
});

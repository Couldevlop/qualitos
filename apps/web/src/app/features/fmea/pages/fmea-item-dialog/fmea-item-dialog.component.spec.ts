import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FmeaService } from '../../fmea.service';
import { FmeaItemResponse } from '../../fmea.types';
import { FmeaItemDialogComponent, FmeaItemDialogData } from './fmea-item-dialog.component';

/**
 * La saisie d'une ligne de PFMEA.
 *
 * <p>Ce qui compte ici tient au RPN : il se recalcule sous les yeux pendant
 * qu'on cote, et sa couleur dit tout de suite si la ligne passe le seuil
 * critique du projet. Une cotation qu'il faut enregistrer pour connaître son
 * résultat ne se corrige pas — elle se subit.
 *
 * <p>Second point : les trois notes ont une valeur par défaut. Le formulaire
 * ne peut donc jamais être bloqué par un champ VIDE, seulement par une note
 * sortie de l'échelle — et c'est ce que la barre d'actions doit dire, faute de
 * quoi on cherche un champ obligatoire qui n'existe pas.
 */
describe('FmeaItemDialogComponent', () => {

  const item = (over: Partial<FmeaItemResponse> = {}): FmeaItemResponse => ({
    id: 'it-1', tenantId: 't-1', projectId: 'f-1', sequenceNo: 1,
    failureMode: 'Diamètre hors tolérance',
    severity: 7, occurrence: 4, detection: 3, rpn: 84, critical: false,
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z', ...over
  });

  let fixture: ComponentFixture<FmeaItemDialogComponent>;
  let component: FmeaItemDialogComponent;
  let svc: jasmine.SpyObj<FmeaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FmeaItemDialogComponent, FmeaItemResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function setup(data: Partial<FmeaItemDialogData> = {}): Promise<void> {
    svc = jasmine.createSpyObj<FmeaService>('FmeaService', ['addItem', 'updateItem']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<FmeaItemDialogComponent, FmeaItemResponse>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [FmeaItemDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: FmeaService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { projectId: 'f-1', criticalRpnThreshold: 100, ...data }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FmeaItemDialogComponent);
    component = fixture.componentInstance;
  }

  it('recalcule le RPN sous les yeux, pendant la cotation', async () => {
    // Une cotation qu'il faut enregistrer pour connaître son résultat ne se
    // corrige pas : elle se subit.
    await setup();

    component.form.patchValue({ severity: 8, occurrence: 5, detection: 4 });
    expect(component.rpnLive).toBe(160);
  });

  it('classe le RPN sur le seuil critique DU PROJET, pas sur une constante', async () => {
    // Deux projets peuvent placer leur seuil ailleurs ; une couleur figée
    // rendrait la pastille fausse pour l'un des deux.
    await setup({ criticalRpnThreshold: 100 });

    expect(component.rpnClass(0)).toContain('rpn-na');
    expect(component.rpnClass(40)).toContain('rpn-ok');
    expect(component.rpnClass(70)).toContain('rpn-high');       // ≥ 60 % du seuil
    expect(component.rpnClass(120)).toContain('rpn-critical');
  });

  it('ne rend un RPN après action que si les TROIS notes sont posées', async () => {
    // Un produit partiel laisserait croire à une amélioration mesurée alors
    // qu'une des trois notes manque encore.
    await setup();
    expect(component.rpnAfterLive).toBeNull();

    component.form.patchValue({ resultingSeverity: 4, resultingOccurrence: 3 });
    expect(component.rpnAfterLive).toBeNull();

    component.form.patchValue({ resultingDetection: 2 });
    expect(component.rpnAfterLive).toBe(24);
  });

  it('explique le blocage par l’échelle, faute de champ obligatoire vide', async () => {
    // Les trois notes ont une valeur par défaut : le formulaire ne peut être
    // invalide que par une note hors 1-10.
    await setup();
    expect(component.blockedReason).toBeUndefined();

    component.form.controls.severity.setValue(11);
    expect(component.blockedReason).toContain('entre 1 et 10');
  });

  it('nomme l’action selon qu’on crée ou qu’on modifie', async () => {
    await setup();
    const creation = component.submitLabel;

    await setup({ item: item() });
    expect(component.submitLabel).not.toBe(creation);
    expect(component.dialogTitle).toContain('Modifier');
  });

  it('rend la ligne créée à l’appelant et ferme', async () => {
    await setup();
    svc.addItem.and.returnValue(of(item()));

    component.form.patchValue({ failureMode: 'Bavure' });
    component.submit();

    expect(svc.addItem).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(item());
  });

  it('garde le dialogue ouvert quand le serveur refuse', async () => {
    // Fermer sur erreur ferait disparaître une saisie de vingt champs.
    await setup({ item: item() });
    svc.updateItem.and.returnValue(throwError(() => ({ status: 400, error: {} })));

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('refuse d’envoyer une cotation hors échelle', async () => {
    await setup();
    component.form.controls.detection.setValue(0);

    component.submit();

    expect(svc.addItem).not.toHaveBeenCalled();
    expect(component.form.controls.detection.touched).toBeTrue();
  });
});

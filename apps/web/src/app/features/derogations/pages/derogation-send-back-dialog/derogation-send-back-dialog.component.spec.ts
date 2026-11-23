import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DerogationSendBackDialogComponent } from './derogation-send-back-dialog.component';

describe('DerogationSendBackDialogComponent', () => {

  let fermeture: jasmine.SpyObj<MatDialogRef<DerogationSendBackDialogComponent, string>>;
  let fixture: ComponentFixture<DerogationSendBackDialogComponent>;
  let composant: DerogationSendBackDialogComponent;

  beforeEach(async () => {
    fermeture = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [DerogationSendBackDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: fermeture },
        { provide: MAT_DIALOG_DATA, useValue: { reference: 'DER-2026-0001' } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DerogationSendBackDialogComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('le motif part élagué', () => {
    composant.formulaire.patchValue({ reason: '  La quantité ne colle pas au lot.  ' });

    composant.renvoyer();

    expect(fermeture.close)
      .toHaveBeenCalledWith('La quantité ne colle pas au lot.');
  });

  it('SANS MOTIF, rien ne part', () => {
    composant.renvoyer();

    // Renvoyer sans dire quoi corriger laisse l'initiateur deviner, et fait un
    // aller-retour de plus à chaque fois.
    expect(fermeture.close).not.toHaveBeenCalled();
    expect(composant.formulaire.controls.reason.touched).toBeTrue();
  });

  it('UN MOTIF FAIT D\'ESPACES est refusé comme vide', () => {
    composant.formulaire.patchValue({ reason: '      ' });

    composant.renvoyer();

    // Sinon le contrôle de présence se contourne par une touche d'espace, et
    // l'initiateur reçoit un renvoi muet.
    expect(fermeture.close).not.toHaveBeenCalled();
    expect(composant.formulaire.controls.reason.hasError('required')).toBeTrue();
  });

  it('LA FENÊTRE NE PARLE PAS DE REFUS : elle parle de correction', () => {
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    // Le manager qualité ne refuse pas la dérogation — refuser usurperait la
    // décision du client, seul à pouvoir l'accorder. Un libellé de refus ici
    // lui donnerait un pouvoir que le cycle lui retire.
    expect(texte).toMatch(/correction|corriger/i);
    expect(texte).not.toMatch(/refuser la dérogation|rejeter/i);
    expect(texte).toContain('DER-2026-0001');
  });

  it('annuler ferme SANS RIEN rendre', () => {
    composant.formulaire.patchValue({ reason: 'Un motif' });

    composant.annuler();

    expect(fermeture.close).toHaveBeenCalledWith();
  });
});

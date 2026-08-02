import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../shared.module';
import { UiModule } from '../ui.module';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';

/**
 * Dialogue de confirmation partagé.
 *
 * Composant minuscule mais posé devant CHAQUE suppression de la plateforme : ce
 * qui compte ici est qu'il ne puisse jamais rendre un consentement par défaut.
 * Annuler, comme fermer, doit rendre `false` — jamais `undefined`, que du code
 * appelant pourrait confondre avec « pas encore répondu ».
 */
describe('ConfirmDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let component: ConfirmDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ConfirmDialogComponent, boolean>>;
  let data: ConfirmDialogData;

  function build(over: Partial<ConfirmDialogData> = {}): void {
    data = {
      title: 'Supprimer cette non-conformité ?',
      message: 'Cette action est irréversible.',
      ...over
    };
    TestBed.resetTestingModule();
    dialogRef = jasmine.createSpyObj<MatDialogRef<ConfirmDialogComponent, boolean>>(
      'MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      declarations: [ConfirmDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    });

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function buttons(): HTMLButtonElement[] {
    return Array.from(el().querySelectorAll('button'));
  }

  // ---- Réponse rendue ----------------------------------------------------------

  it('rend true à la confirmation', () => {
    build();

    component.confirm();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('rend false explicitement à l\'annulation, jamais undefined', () => {
    build();

    component.cancel();

    // `undefined` serait ambigu pour l'appelant : il ne saurait pas distinguer
    // un refus d'une absence de réponse.
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });

  // ---- Rendu -------------------------------------------------------------------

  it('affiche le titre et le message fournis', () => {
    build();

    expect(el().textContent).toContain('Supprimer cette non-conformité ?');
    expect(el().querySelector('.message')?.textContent)
      .toContain('Cette action est irréversible.');
  });

  it('utilise les libellés par défaut quand aucun n\'est fourni', () => {
    build();

    const labels = buttons().map(b => b.textContent?.trim());
    expect(labels).toContain(component.defaultCancelLabel);
    expect(labels).toContain(component.defaultConfirmLabel);
  });

  it('préfère les libellés fournis par l\'appelant', () => {
    build({ cancelLabel: 'Garder', confirmLabel: 'Supprimer définitivement' });

    const labels = buttons().map(b => b.textContent?.trim());
    expect(labels).toContain('Garder');
    expect(labels).toContain('Supprimer définitivement');
  });

  it('marque le bouton de confirmation comme destructeur quand demandé', () => {
    build({ destructive: true });
    const confirmButton = buttons()[1];

    // La couleur d'avertissement est le seul signal visuel qui distingue une
    // suppression d'une validation anodine.
    expect(confirmButton.classList.toString()).toContain('warn');
  });

  it('reste une action neutre par défaut', () => {
    build();

    expect(buttons()[1].classList.toString()).not.toContain('warn');
  });

  // ---- Interaction ---------------------------------------------------------------

  it('relaie les clics des deux boutons', () => {
    build();

    buttons()[0].click();
    expect(dialogRef.close).toHaveBeenCalledWith(false);

    buttons()[1].click();
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('déclare des boutons de type button, pour ne pas soumettre un formulaire parent', () => {
    build();

    // Sans `type="button"`, un clic dans un dialogue ouvert au-dessus d'un
    // formulaire le soumettrait au passage.
    expect(buttons().every(b => b.getAttribute('type') === 'button')).toBeTrue();
  });
});

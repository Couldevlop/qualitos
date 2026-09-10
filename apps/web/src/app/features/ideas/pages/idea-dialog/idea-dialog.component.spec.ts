import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IdeaDialogComponent } from './idea-dialog.component';

/**
 * Le dialogue de dépôt d'une idée.
 *
 * <p>Ce qui compte ici tient en deux points : le titre est le seul champ
 * obligatoire — c'est tout ce que chaque colonne affiche —, et un champ
 * réduit à des espaces n'est pas une valeur, ni pour le titre ni pour la
 * description facultative qu'on omet plutôt que d'enregistrer une ligne
 * blanche.
 */
describe('IdeaDialogComponent', () => {

  let fixture: ComponentFixture<IdeaDialogComponent>;
  let component: IdeaDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IdeaDialogComponent>>;

  async function setup(): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<IdeaDialogComponent>>('MatDialogRef', ['close']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [IdeaDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IdeaDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('ouvre un formulaire vide, et dit ce qui bloque', async () => {
    await setup();

    expect(component.form.getRawValue()).toEqual({ title: '', description: '' });
    expect(component.blockedReason).toContain('titre');
  });

  it('rend le titre et la description rognés, la description vide omise', async () => {
    await setup();
    component.form.setValue({ title: '  Bac de tri  ', description: '   ' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({ title: 'Bac de tri', description: undefined });
  });

  it('conserve une description non vide, rognée', async () => {
    await setup();
    component.form.setValue({ title: 'Bac de tri', description: '  Près du poste 3  ' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({ title: 'Bac de tri', description: 'Près du poste 3' });
  });

  it('refuse de valider sans titre — espaces compris — et ne ferme rien', async () => {
    await setup();
    component.form.setValue({ title: '   ', description: '' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('ne rend rien quand on renonce', async () => {
    await setup();

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IdeaRejectDialogComponent, IdeaRejectDialogData } from './idea-reject-dialog.component';

/**
 * Le dialogue générique du refus et de l'impact.
 *
 * <p>Un seul champ libre obligatoire, sous la donnée d'entrée qui distingue
 * les deux usages : le titre affiché, la question posée, le libellé du
 * bouton. Ce banc vérifie qu'il les reprend sans les confondre, qu'un texte
 * réduit à des espaces ne valide pas, et qu'un renoncement ne rend rien.
 */
describe('IdeaRejectDialogComponent', () => {

  let fixture: ComponentFixture<IdeaRejectDialogComponent>;
  let component: IdeaRejectDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IdeaRejectDialogComponent>>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  async function setup(data: IdeaRejectDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<IdeaRejectDialogComponent>>(
      'MatDialogRef', ['close']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [IdeaRejectDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IdeaRejectDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('affiche la question posée et l’idée concernée', async () => {
    await setup({
      title: 'Bac de tri', prompt: 'Pourquoi cette idée est-elle écartée ?', submitLabel: 'Écarter'
    });

    expect(hote().textContent).toContain('Pourquoi cette idée est-elle écartée ?');
    expect(hote().textContent).toContain('Bac de tri');
    expect(component.blockedReason).toContain('requis');
  });

  it('rend le texte saisi, rogné', async () => {
    await setup({
      title: 'Bac de tri', prompt: 'Qu’a produit cette idée ?', submitLabel: 'Consigner'
    });
    component.form.setValue({ text: '  Gain de dix minutes par poste  ' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({ text: 'Gain de dix minutes par poste' });
  });

  it('refuse un texte réduit à des espaces, et ne ferme rien', async () => {
    await setup({
      title: 'Bac de tri', prompt: 'Pourquoi cette idée est-elle écartée ?', submitLabel: 'Écarter'
    });
    component.form.setValue({ text: '   ' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.text.touched).toBeTrue();
  });

  it('ne rend rien quand on renonce', async () => {
    await setup({
      title: 'Bac de tri', prompt: 'Pourquoi cette idée est-elle écartée ?', submitLabel: 'Écarter'
    });

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

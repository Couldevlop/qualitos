import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApqpPhase } from '../../apqp.types';
import { ApqpPhaseDialogComponent, ApqpPhaseDialogData } from './apqp-phase-dialog.component';

/**
 * Le dialogue qui crée ou renomme une phase.
 *
 * <p>Ce qui compte ici tient en trois points : l'intitulé est le seul champ
 * obligatoire — c'est tout ce que le V affiche —, un champ réduit à des espaces
 * n'est pas une valeur, et un renoncement ne rend rien.
 */
describe('ApqpPhaseDialogComponent', () => {

  let fixture: ComponentFixture<ApqpPhaseDialogComponent>;
  let component: ApqpPhaseDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ApqpPhaseDialogComponent>>;

  const phase: ApqpPhase = {
    id: 'p2', position: 2, level: 2,
    title: 'Conception du produit',
    purpose: 'Fige les exigences produit.',
    question: 'Que doit faire le produit ?',
    deliverables: []
  };

  async function setup(data: ApqpPhaseDialogData = {}): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ApqpPhaseDialogComponent>>(
      'MatDialogRef', ['close']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpPhaseDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpPhaseDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('ouvre un formulaire vide en création, et dit ce qui bloque', async () => {
    await setup();

    expect(component.editing).toBeFalse();
    expect(component.form.getRawValue().title).toBe('');
    // Le bouton désactivé sans explication laisse chercher ; on nomme la raison.
    expect(component.blockedReason).toContain('intitulé');
  });

  it('reprend la phase en modification, champs vides compris', async () => {
    // `purpose`/`question` sont nuls côté serveur ; les laisser tels quels
    // rendrait le champ « null » à l'écran.
    await setup({ phase: { ...phase, purpose: null, question: null } });

    expect(component.editing).toBeTrue();
    expect(component.form.getRawValue()).toEqual({
      title: 'Conception du produit', purpose: '', question: ''
    });
  });

  it('rend l’intitulé et les textes rognés, les vides omis', async () => {
    // Un champ facultatif réduit à des espaces n'est pas une valeur : on
    // l'omet plutôt que d'enregistrer une ligne blanche.
    await setup({ phase });
    component.form.setValue({ title: '  Conception  ', purpose: '   ', question: 'Pourquoi ?' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      title: 'Conception', purpose: undefined, question: 'Pourquoi ?'
    });
  });

  it('refuse de valider sans intitulé — espaces compris — et ne ferme rien', async () => {
    // '   ' passait `required` et donnait une phase sans nom, qu'on ne pouvait
    // plus désigner dans le schéma.
    await setup();
    component.form.setValue({ title: '   ', purpose: 'Un objet', question: '' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('ne rend rien quand on renonce', async () => {
    // `close()` sans valeur : c'est ce qui distingue un renoncement d'un envoi
    // vide côté appelant.
    await setup({ phase });

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

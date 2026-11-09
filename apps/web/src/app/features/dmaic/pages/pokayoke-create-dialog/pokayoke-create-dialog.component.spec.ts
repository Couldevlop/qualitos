import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  PokaYokeCreateDialogComponent, PokaYokeCreateDialogData
} from './pokayoke-create-dialog.component';

/**
 * « Une action DMAIC devient un détrompeur inscrit à la bibliothèque,
 * réutilisable sur une autre ligne. »
 *
 * <p>Ce que ces bancs tiennent : le nom est proposé depuis l'action, parce que
 * celui qui l'a écrite a déjà nommé la solution ; l'origine est construite et
 * montrée en lecture, parce qu'une origine libre finirait vide ou fausse et
 * qu'un dispositif dont on ne sait plus d'où il vient ne se réutilise pas ; et
 * le projet voyage avec la demande, pour que le dispositif soit posé et pas
 * seulement inscrit.
 */
describe('PokaYokeCreateDialogComponent', () => {

  let fixture: ComponentFixture<PokaYokeCreateDialogComponent>;
  let component: PokaYokeCreateDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<PokaYokeCreateDialogComponent>>;

  const donnees = (action: string): PokaYokeCreateDialogData => ({
    projectId: 'proj-1',
    projectTitle: 'Réduire les défauts de soudure',
    action,
    position: 2
  });

  async function monter(action = 'Poser un gabarit asymétrique. Le capot ne rentre que dans le bon sens.'): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<PokaYokeCreateDialogComponent>>('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [PokaYokeCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: donnees(action) }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(PokaYokeCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => monter());

  // ---------- ce que l'action propose ----------

  it('propose le nom à partir de la première phrase de l’action', () => {
    expect(component.form.getRawValue().name).toBe('Poser un gabarit asymétrique');
  });

  it('garde le texte complet de l’action en description', () => {
    expect(component.form.getRawValue().description)
      .toBe('Poser un gabarit asymétrique. Le capot ne rentre que dans le bon sens.');
  });

  it('ramène un nom trop long à la limite du serveur', () => {
    const nom = PokaYokeCreateDialogComponent.nameFrom('a'.repeat(400));
    expect(nom.length).toBe(255);
    expect(nom.endsWith('…')).toBeTrue();
  });

  it('ne propose rien quand l’action est vide', () => {
    expect(PokaYokeCreateDialogComponent.nameFrom('   ')).toBe('');
    expect(PokaYokeCreateDialogComponent.nameFrom(null)).toBe('');
  });

  // ---------- l'origine, construite et montrée ----------

  it('cite le projet et le rang de l’action, en lecture', () => {
    const origine = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="origine"]')!.textContent ?? '';
    expect(origine).toContain('Réduire les défauts de soudure');
    expect(origine).toContain('2');
    // L'origine n'est pas un champ : rien ne doit inviter à la réécrire.
    expect(Object.keys(component.form.controls).sort()).toEqual(
      ['applicableIndustries', 'description', 'examples', 'mechanism', 'name', 'type']);
  });

  // ---------- ce qui part ----------

  it('n’inscrit rien sans nom', () => {
    component.form.patchValue({ name: '' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('rend la demande, avec le projet pour que le dispositif soit posé', () => {
    component.form.patchValue({ type: 'DETECTION', mechanism: 'SENSOR' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      name: 'Poser un gabarit asymétrique',
      description: 'Poser un gabarit asymétrique. Le capot ne rentre que dans le bon sens.',
      type: 'DETECTION',
      mechanism: 'SENSOR',
      applicableIndustries: undefined,
      examples: undefined,
      sourceReference: component.origin,
      projectId: 'proj-1'
    });
  });

  it('laisse les champs vides à vide, sans chaîne blanche', () => {
    // Une chaîne vide se lirait comme un champ renseigné alors que personne
    // n'a rien écrit.
    component.form.patchValue({ applicableIndustries: '   ', examples: '  ', description: '  ' });

    component.submit();

    const envoye = dialogRef.close.calls.mostRecent().args[0]!;
    expect(envoye.applicableIndustries).toBeUndefined();
    expect(envoye.examples).toBeUndefined();
    expect(envoye.description).toBeUndefined();
  });

  it('montre des mots, jamais les codes du serveur', () => {
    expect(component.typeLabel('PREVENTION')).toBe('Prévention');
    expect(component.mechanismLabel('PHYSICAL_SHAPE')).toBe('Forme physique');
    // Un code inconnu reste lisible plutôt que de laisser une case vide.
    expect(component.mechanismLabel('NOUVEAU_CODE')).toBe('NOUVEAU_CODE');
  });

  it('ferme sans rien rendre quand on renonce', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

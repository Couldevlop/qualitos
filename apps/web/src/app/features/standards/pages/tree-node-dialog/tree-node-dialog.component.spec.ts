import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ClauseDetail, RequirementDetail, RequirementRequest, SectionRequest } from '../../standards.types';
import { TreeNodeDialogComponent, TreeNodeDialogData } from './tree-node-dialog.component';

/**
 * Saisie d'un nœud du référentiel (§8) — une seule boîte pour les trois niveaux.
 *
 * Ce qui doit tenir : le formulaire s'adapte au niveau demandé, les champs
 * facultatifs vidés partent en `undefined` (le serveur les efface réellement),
 * et rien ne sort tant que la saisie est incomplète.
 */
describe('TreeNodeDialogComponent', () => {
  let fixture: ComponentFixture<TreeNodeDialogComponent>;
  let component: TreeNodeDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<TreeNodeDialogComponent>>;

  async function open(data: TreeNodeDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<TreeNodeDialogComponent>>('MatDialogRef', ['close']);
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [TreeNodeDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(TreeNodeDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('exige un code et un titre pour une section', async () => {
    await open({ level: 'SECTION' });

    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();

    component.form.patchValue({ code: '1', title: 'Programmation' });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      code: '1', title: 'Programmation', description: undefined
    } as SectionRequest);
  });

  it('borne le code à la longueur de sa colonne, différente selon le niveau', async () => {
    await open({ level: 'SECTION' });
    expect(component.codeMaxLength).toBe(20);
    component.form.patchValue({ code: '1'.repeat(21), title: 'X' });
    expect(component.form.controls.code.hasError('maxlength')).toBeTrue();

    await open({ level: 'CLAUSE' });
    expect(component.codeMaxLength).toBe(30);
  });

  it('exige un texte, et non un titre, pour une exigence', async () => {
    await open({ level: 'REQUIREMENT' });

    component.form.patchValue({ code: '1.1.1' });
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();

    component.form.patchValue({ text: 'Le programme est revu chaque année' });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      code: '1.1.1',
      text: 'Le programme est revu chaque année',
      obligation: 'MUST',
      evidenceTypes: undefined,
      measurableCriteria: undefined,
      riskIfMissing: undefined
    } as RequirementRequest);
  });

  it('reprend la clause à modifier', async () => {
    const clause = {
      id: 'c1', code: '1.1', title: 'Fréquence', description: 'annuelle',
      orderIndex: 0, requirements: []
    } as ClauseDetail;

    await open({ level: 'CLAUSE', node: clause });

    expect(component.editing).toBeTrue();
    expect(component.form.getRawValue().code).toBe('1.1');
    expect(component.form.getRawValue().title).toBe('Fréquence');
    expect(component.form.getRawValue().description).toBe('annuelle');
  });

  it('reprend l\'exigence à modifier, champs facultatifs compris', async () => {
    const requirement = {
      id: 'r1', code: '1.1.1', text: 'Texte', obligation: 'SHOULD',
      evidenceTypes: 'compte-rendu', measurableCriteria: 'signé',
      riskIfMissing: 'HIGH', orderIndex: 0
    } as RequirementDetail;

    await open({ level: 'REQUIREMENT', node: requirement });

    const v = component.form.getRawValue();
    expect(v.obligation).toBe('SHOULD');
    expect(v.evidenceTypes).toBe('compte-rendu');
    expect(v.measurableCriteria).toBe('signé');
    expect(v.riskIfMissing).toBe('HIGH');
  });

  it('vide réellement un champ facultatif effacé', async () => {
    const requirement = {
      id: 'r1', code: '1.1.1', text: 'Texte', obligation: 'MUST',
      evidenceTypes: 'compte-rendu', riskIfMissing: 'HIGH', orderIndex: 0
    } as RequirementDetail;
    await open({ level: 'REQUIREMENT', node: requirement });

    component.form.patchValue({ evidenceTypes: '   ', riskIfMissing: '' });
    component.submit();

    const sent = dialogRef.close.calls.mostRecent().args[0] as RequirementRequest;
    // `undefined` et non chaîne vide : le serveur doit EFFACER la preuve
    // attendue, faute de quoi l'écran continuerait de l'afficher.
    expect(sent.evidenceTypes).toBeUndefined();
    expect(sent.riskIfMissing).toBeUndefined();
  });

  it('nomme le niveau et le geste dans son titre', async () => {
    await open({ level: 'SECTION' });
    expect(component.title).toContain('Nouvelle section');

    await open({ level: 'CLAUSE', node: { id: 'c', code: '1.1', title: 'T', orderIndex: 0, requirements: [] } as ClauseDetail });
    expect(component.title).toContain('Modifier la clause');

    await open({ level: 'REQUIREMENT' });
    expect(component.title).toContain('Nouvelle exigence');

    await open({ level: 'REQUIREMENT', node: { id: 'r', code: '1', text: 'T', obligation: 'MUST', orderIndex: 0 } as RequirementDetail });
    expect(component.title).toContain('Modifier l\'exigence');
  });

  it('referme sans rien rendre à l\'annulation', async () => {
    await open({ level: 'SECTION' });

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

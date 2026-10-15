import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IshikawaService } from '../../ishikawa.service';
import { IshikawaCauseResponse } from '../../ishikawa.types';
import { IshikawaCauseDialogComponent, IshikawaCauseDialogData } from './ishikawa-cause-dialog.component';

/**
 * Ajouter une cause : les catégories suivent le mode du diagramme (6M, 7M,
 * 8M), et une sous-cause (5 Pourquoi) hérite de la catégorie de sa parente.
 */
describe('IshikawaCauseDialogComponent', () => {
  let fixture: ComponentFixture<IshikawaCauseDialogComponent>;
  let component: IshikawaCauseDialogComponent;
  let svc: jasmine.SpyObj<IshikawaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IshikawaCauseDialogComponent, IshikawaCauseResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(data: IshikawaCauseDialogData): Promise<void> {
    svc = jasmine.createSpyObj<IshikawaService>('IshikawaService', ['addCause']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [IshikawaCauseDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IshikawaService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(IshikawaCauseDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('propose les six M, sept ou huit selon le mode du diagramme', async () => {
    await build({ diagramId: 'd-1', mode: 'SIX_M' });
    expect(component.categories.length).toBe(6);

    TestBed.resetTestingModule();
    await build({ diagramId: 'd-1', mode: 'SEVEN_M' });
    expect(component.categories.map(c => c.value)).toContain('MANAGEMENT');
    expect(component.categories.length).toBe(7);

    TestBed.resetTestingModule();
    await build({ diagramId: 'd-1', mode: 'EIGHT_M' });
    expect(component.categories.map(c => c.value)).toContain('MONEY');
  });

  it('ajoute la cause, libellé rogné et score nul omis', async () => {
    await build({ diagramId: 'd-1', mode: 'SIX_M' });
    const cause = { id: 'c-1' } as IshikawaCauseResponse;
    svc.addCause.and.returnValue(of(cause));

    component.form.patchValue({ category: 'MACHINES', label: '  Usure buse  ', description: '  ', rootCauseScore: 0 });
    component.submit();

    expect(svc.addCause).toHaveBeenCalledWith('d-1', {
      category: 'MACHINES', label: 'Usure buse', description: undefined, rootCauseScore: undefined,
      parentId: undefined
    });
    expect(dialogRef.close).toHaveBeenCalledWith(cause);
  });

  it('une sous-cause hérite de la catégorie de sa parente, verrouillée', async () => {
    await build({ diagramId: 'd-1', mode: 'SIX_M', parent: { id: 'c-0', label: 'Usure', category: 'MATERIALS' } });
    svc.addCause.and.returnValue(of({ id: 'c-2' } as IshikawaCauseResponse));

    expect(component.form.controls.category.disabled).toBeTrue();
    component.form.patchValue({ label: 'Pourquoi ?', rootCauseScore: 0.8 });
    component.submit();

    expect(svc.addCause).toHaveBeenCalledWith('d-1', jasmine.objectContaining({
      category: 'MATERIALS', parentId: 'c-0', rootCauseScore: 0.8
    }));
  });

  it('n\'envoie rien sans libellé, et garde la fenêtre ouverte sur un refus', async () => {
    await build({ diagramId: 'd-1', mode: 'SIX_M' });
    component.submit();
    expect(svc.addCause).not.toHaveBeenCalled();

    svc.addCause.and.returnValue(throwError(() => ({ status: 400 })));
    component.form.patchValue({ label: 'Cause' });
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('se ferme sans rien rendre à l\'annulation', async () => {
    await build({ diagramId: 'd-1', mode: 'SIX_M' });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

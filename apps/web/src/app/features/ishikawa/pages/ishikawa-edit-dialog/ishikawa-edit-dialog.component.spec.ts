import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IshikawaService } from '../../ishikawa.service';
import { IshikawaDiagramResponse } from '../../ishikawa.types';
import { IshikawaEditDialogComponent } from './ishikawa-edit-dialog.component';

/** Modifier un diagramme : énoncé, mode et statut repartent tels qu'affichés. */
describe('IshikawaEditDialogComponent', () => {
  const DIAGRAM = {
    id: 'd-1', problemStatement: 'Rebuts ligne 2', description: null, mode: 'SIX_M', status: 'DRAFT'
  } as unknown as IshikawaDiagramResponse;

  let fixture: ComponentFixture<IshikawaEditDialogComponent>;
  let component: IshikawaEditDialogComponent;
  let svc: jasmine.SpyObj<IshikawaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IshikawaEditDialogComponent, IshikawaDiagramResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<IshikawaService>('IshikawaService', ['updateDiagram']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [IshikawaEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IshikawaService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { diagram: DIAGRAM } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(IshikawaEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('reprend le diagramme ouvert', () => {
    expect(component.form.getRawValue()).toEqual({
      problemStatement: 'Rebuts ligne 2', description: '', mode: 'SIX_M', status: 'DRAFT'
    });
  });

  it('enregistre les champs rognés et rend le diagramme au parent', () => {
    const updated = { ...DIAGRAM, mode: 'EIGHT_M' } as IshikawaDiagramResponse;
    svc.updateDiagram.and.returnValue(of(updated));

    component.form.patchValue({ problemStatement: '  Rebuts  ', description: '  ', mode: 'EIGHT_M', status: 'IN_REVIEW' });
    component.submit();

    expect(svc.updateDiagram).toHaveBeenCalledWith('d-1', {
      problemStatement: 'Rebuts', description: undefined, mode: 'EIGHT_M', status: 'IN_REVIEW'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
  });

  it('n\'envoie rien sans énoncé, et garde la fenêtre ouverte sur un refus', () => {
    component.form.patchValue({ problemStatement: '' });
    component.submit();
    expect(svc.updateDiagram).not.toHaveBeenCalled();

    svc.updateDiagram.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ problemStatement: 'Rebuts' });
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('se ferme sans rien rendre à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});

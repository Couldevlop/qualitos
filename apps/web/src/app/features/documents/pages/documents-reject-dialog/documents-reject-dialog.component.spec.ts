import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentsService } from '../../documents.service';
import { DocumentVersionResponse } from '../../documents.types';
import { DocumentsRejectDialogComponent } from './documents-reject-dialog.component';

describe('DocumentsRejectDialogComponent', () => {

  let fixture: ComponentFixture<DocumentsRejectDialogComponent>;
  let component: DocumentsRejectDialogComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let ref: jasmine.SpyObj<MatDialogRef<DocumentsRejectDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const VERSION = {
    id: 'v1', documentId: 'd1', versionNumber: 3, status: 'IN_REVIEW', authorId: 'a', createdAt: '', updatedAt: ''
  } as DocumentVersionResponse;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['reject']);
    ref = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [DocumentsRejectDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: DocumentsService, useValue: svc },
        { provide: MatDialogRef, useValue: ref },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { version: VERSION } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsRejectDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('nomme la version refusée', () => {
    expect(component.title).toContain('v3');
  });

  it('une raison vide ou faite d’espaces ne part pas', () => {
    component.submit();
    component.form.controls.reason.setValue('    ');
    component.submit();

    expect(svc.reject).not.toHaveBeenCalled();
    expect(component.form.controls.reason.touched).toBeTrue();
  });

  it('envoie la raison rognée et rend la version refusée', () => {
    const refusee = { ...VERSION, status: 'DRAFT' } as DocumentVersionResponse;
    svc.reject.and.returnValue(of(refusee));
    component.form.controls.reason.setValue('  Section 4 incomplète  ');

    component.submit();

    expect(svc.reject).toHaveBeenCalledWith('d1', 'v1', 'Section 4 incomplète');
    expect(ref.close).toHaveBeenCalledWith(refusee);
    expect(component.submitting).toBeFalse();
  });

  it('un refus du serveur se dit, et le dialogue reste ouvert', () => {
    svc.reject.and.returnValue(throwError(() => ({ status: 403 })));
    component.form.controls.reason.setValue('non');

    component.submit();

    expect(snack.open).toHaveBeenCalled();
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('annuler ferme sans rien rendre', () => {
    component.cancel();
    expect(ref.close).toHaveBeenCalledWith();
  });
});

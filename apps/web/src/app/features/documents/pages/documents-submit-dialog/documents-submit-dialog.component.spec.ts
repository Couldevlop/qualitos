import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { DocumentsService } from '../../documents.service';
import { DocumentVersionResponse } from '../../documents.types';
import { DocumentsSubmitDialogComponent } from './documents-submit-dialog.component';

describe('DocumentsSubmitDialogComponent', () => {

  let fixture: ComponentFixture<DocumentsSubmitDialogComponent>;
  let component: DocumentsSubmitDialogComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let ref: jasmine.SpyObj<MatDialogRef<DocumentsSubmitDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let moi: string | undefined = 'auteur';

  const VERSION = {
    id: 'v1', documentId: 'd1', versionNumber: 2, status: 'DRAFT', authorId: 'auteur', createdAt: '', updatedAt: ''
  } as DocumentVersionResponse;
  const MARIE: DirectoryUser = { id: 'a1', accountId: 'u1', displayName: 'Marie Curie', email: 'marie@acme.test', active: true };
  const PAUL: DirectoryUser = { id: 'a2', accountId: 'u2', displayName: ' ', email: 'paul@acme.test', active: true };

  function choisir(slot: 's1' | 's2' | 's3', user: DirectoryUser): void {
    component.form.controls[slot].setValue(user.accountId);
    component.onSelected(slot, user);
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['submit']);
    ref = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({ content: [MARIE, PAUL], page: 0, size: 20, totalElements: 2, totalPages: 1 }));
    directory.getByAccount.and.callFake(id => of(id === 'u2' ? PAUL : MARIE));
    await TestBed.configureTestingModule({
      declarations: [DocumentsSubmitDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        { provide: DocumentsService, useValue: svc },
        { provide: DirectoryService, useValue: directory },
        { provide: AuthService, useValue: { snapshot: () => (moi ? { userId: moi } : null) } },
        { provide: MatDialogRef, useValue: ref },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { version: VERSION } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsSubmitDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('nomme la version et offre trois places, Signataire 1 à 3', () => {
    expect(component.title).toContain('v2');
    expect([0, 1, 2].map(i => component.label(i))).toEqual(['Signataire 1', 'Signataire 2', 'Signataire 3']);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('qos-user-picker').length).toBe(3);
  });

  it('sans signataire 1, rien ne part', () => {
    component.submit();
    expect(svc.submit).not.toHaveBeenCalled();
    expect(component.form.controls.s1.touched).toBeTrue();
  });

  it('écarte l’auteur et les personnes déjà placées', () => {
    choisir('s1', MARIE);
    expect(component.excluded('s2')).toEqual(['auteur', 'u1']);
    expect(component.excluded('s1')).toEqual(['auteur']);
  });

  it('écarte aussi la personne qui soumet, si ce n’est pas l’auteur', () => {
    moi = 'manager';
    expect(component.excluded('s1')).toEqual(['auteur', 'manager']);
    moi = undefined;
    expect(component.excluded('s1')).toEqual(['auteur']);
    moi = 'auteur';
  });

  it('refuse une même personne à deux places', () => {
    choisir('s1', MARIE);
    choisir('s3', MARIE);
    fixture.detectChanges();

    component.submit();

    expect(component.form.hasError('duplicate')).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-test="doublon"]')).not.toBeNull();
    expect(svc.submit).not.toHaveBeenCalled();
  });

  it('envoie les signataires dans l’ordre, places vides retirées, avec le nom de l’annuaire', () => {
    const soumise = { ...VERSION, status: 'IN_REVIEW' } as DocumentVersionResponse;
    svc.submit.and.returnValue(of(soumise));
    choisir('s1', MARIE);
    choisir('s3', PAUL);

    component.submit();

    expect(svc.submit).toHaveBeenCalledOnceWith('d1', 'v1',
      [{ userId: 'u1', name: 'Marie Curie' }, { userId: 'u2', name: 'paul@acme.test' }]);
    expect(ref.close).toHaveBeenCalledWith(soumise);
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('une personne retirée de sa place ne part pas', () => {
    choisir('s1', MARIE);
    choisir('s2', PAUL);
    component.form.controls.s2.setValue(null);
    component.onSelected('s2', null);

    expect(component.signatories()).toEqual([{ userId: 'u1', name: 'Marie Curie' }]);
  });

  it('un refus du serveur reste dans le dialogue, avec son message', () => {
    svc.submit.and.returnValue(throwError(() => ({ status: 409, error: { detail: 'Attach the document file' } })));
    choisir('s1', MARIE);

    component.submit();

    expect(ref.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ne soumet pas deux fois pendant l’envoi', () => {
    choisir('s1', MARIE);
    component.submitting = true;
    component.submit();
    expect(svc.submit).not.toHaveBeenCalled();
  });

  it('annuler ferme sans rien rendre', () => {
    component.cancel();
    expect(ref.close).toHaveBeenCalledWith();
  });
});

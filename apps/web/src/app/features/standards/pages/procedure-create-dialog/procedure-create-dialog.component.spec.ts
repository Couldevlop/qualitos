import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentsService } from '../../../documents/documents.service';
import { DocumentPage, DocumentResponse } from '../../../documents/documents.types';
import { StandardsService } from '../../standards.service';
import { ProcedureCreateDialogComponent } from './procedure-create-dialog.component';

/**
 * Choix de la procédure source d'un référentiel d'audit (§8).
 *
 * Deux exigences structurent cet écran. Ne proposer QUE des procédures
 * approuvées — le serveur refuse les autres, et les afficher promettrait un
 * geste qui échoue. Et traduire chaque refus par ce qu'il appelle comme suite :
 * rejoindre le référentiel existant, publier une version, demander les droits.
 */
describe('ProcedureCreateDialogComponent', () => {
  let fixture: ComponentFixture<ProcedureCreateDialogComponent>;
  let component: ProcedureCreateDialogComponent;
  let docs: jasmine.SpyObj<DocumentsService>;
  let standards: jasmine.SpyObj<StandardsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ProcedureCreateDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let el: HTMLElement;

  function doc(over: Partial<DocumentResponse>): DocumentResponse {
    return {
      id: 'd1', tenantId: 't', code: 'PRO-002', title: 'Audit interne',
      type: 'PROCEDURE', status: 'ACTIVE', ownerId: 'u1', currentVersionId: 'v3',
      mandatoryRead: false, createdAt: '', updatedAt: '', versions: [],
      ...over
    } as DocumentResponse;
  }

  function page(content: DocumentResponse[]): DocumentPage {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: content.length };
  }

  beforeEach(async () => {
    docs = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['list']);
    docs.list.and.returnValue(of(page([doc({})])));
    standards = jasmine.createSpyObj<StandardsService>('StandardsService',
      ['createProcedureReferential']);
    standards.createProcedureReferential.and.returnValue(of(undefined));
    dialogRef = jasmine.createSpyObj<MatDialogRef<ProcedureCreateDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ProcedureCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: DocumentsService, useValue: docs },
        { provide: StandardsService, useValue: standards },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProcedureCreateDialogComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  it('ne retient que les procédures APPROUVÉES', done => {
    docs.list.and.returnValue(of(page([
      doc({ id: 'ok', code: 'PRO-002' }),
      // Un enregistrement : il n'énonce aucune exigence.
      doc({ id: 'record', type: 'RECORD' }),
      // Une procédure restée à l'état de brouillon : aucune version publiée.
      doc({ id: 'draft', currentVersionId: undefined })
    ])));
    component.ngOnInit();

    component.procedures$.subscribe(list => {
      expect(list.map(d => d.id)).toEqual(['ok']);
      done();
    });
  });

  it('dit quoi faire quand aucune procédure n\'est approuvée', () => {
    docs.list.and.returnValue(of(page([])));
    component.ngOnInit();
    fixture.detectChanges();

    const empty = el.querySelector('.empty-state');
    expect(empty).not.toBeNull();
    expect(empty!.textContent).toContain('Publiez d\'abord une version');
  });

  it('n\'envoie rien tant qu\'aucune procédure n\'est choisie', () => {
    component.submit();

    expect(standards.createProcedureReferential).not.toHaveBeenCalled();
    expect(component.form.controls.documentId.touched).toBeTrue();
  });

  it('crée le référentiel et referme sur un succès', () => {
    component.form.setValue({ documentId: 'd1' });

    component.submit();

    expect(standards.createProcedureReferential).toHaveBeenCalledWith('d1');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
    expect(component.submitting).toBeFalse();
  });

  it('n\'envoie pas deux fois pendant que la première demande court', () => {
    component.form.setValue({ documentId: 'd1' });
    component.submitting = true;

    component.submit();

    expect(standards.createProcedureReferential).not.toHaveBeenCalled();
  });

  [
    { status: 409, expected: 'existe déjà' },
    { status: 422, expected: 'approuvée' },
    { status: 403, expected: 'droits' },
    { status: 500, expected: 'impossible' }
  ].forEach(({ status, expected }) => {
    it(`traduit le refus ${status} par la suite à donner`, () => {
      standards.createProcedureReferential.and.returnValue(throwError(() => ({ status })));
      component.form.setValue({ documentId: 'd1' });

      component.submit();

      expect(snack.open).toHaveBeenCalled();
      expect(snack.open.calls.mostRecent().args[0]).toContain(expected);
      // La boîte reste ouverte : l'utilisateur doit pouvoir corriger son choix.
      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(component.submitting).toBeFalse();
    });
  });

  it('referme sans rien créer à l\'annulation', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(standards.createProcedureReferential).not.toHaveBeenCalled();
  });
});

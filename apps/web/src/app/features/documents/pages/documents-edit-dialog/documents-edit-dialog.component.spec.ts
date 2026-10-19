import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentResponse } from '../../documents.types';
import { DocumentsEditDialogComponent, DocumentsEditDialogData } from './documents-edit-dialog.component';

/**
 * §4.1 — l'édition ne porte que sur les métadonnées. Le code documentaire est
 * immuable (il sert de référence dans les preuves d'audit déjà signées) et
 * n'est donc même pas présent dans le formulaire.
 */
describe('DocumentsEditDialogComponent', () => {
  let component: DocumentsEditDialogComponent;
  let fixture: ComponentFixture<DocumentsEditDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DocumentsEditDialogComponent, DocumentResponse>>;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/documents`;

  const existing: DocumentResponse = {
    id: 'd1', tenantId: 't1', code: 'POL-QUAL-001', title: 'Politique Qualité 2026',
    description: 'Engagements direction.', type: 'POLICY', status: 'ACTIVE',
    ownerId: 'u1', mandatoryRead: true, currentVersionId: 'v1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z', versions: []
  };

  async function build(data: DocumentsEditDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<DocumentsEditDialogComponent, DocumentResponse>>(
      'MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [DocumentsEditDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideDirectoryStub(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentsEditDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('reprend les métadonnées existantes, sans le code (immuable)', async () => {
    await build({ document: existing });
    expect(component.form.controls.title.value).toBe('Politique Qualité 2026');
    expect(component.form.controls.description.value).toBe('Engagements direction.');
    expect(component.form.controls.type.value).toBe('POLICY');
    expect(component.form.controls.mandatoryRead.value).toBeTrue();
    expect(Object.keys(component.form.controls)).not.toContain('code');
  });

  it('expose les huit types documentaires', async () => {
    await build({ document: existing });
    expect(component.types.map(t => t.value)).toEqual([
      'POLICY', 'PROCEDURE', 'WORK_INSTRUCTION', 'RECORD', 'FORM', 'MANUAL', 'EXTERNAL', 'OTHER'
    ]);
  });

  it('remplace une description absente par du vide', async () => {
    await build({ document: { ...existing, description: undefined } });
    expect(component.form.controls.description.value).toBe('');
  });

  it('n\'envoie rien quand le titre a été vidé', async () => {
    await build({ document: existing });
    component.form.controls.title.setValue('');
    component.submit();
    http.expectNone(`${base}/d1`);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('envoie les métadonnées normalisées sur l\'identifiant du document', async () => {
    await build({ document: existing });
    component.form.patchValue({
      title: '  Politique Qualité 2027  ',
      description: '   ',
      type: 'MANUAL',
      mandatoryRead: false
    });
    component.submit();

    const req = http.expectOne(`${base}/d1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.title).toBe('Politique Qualité 2027');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.type).toBe('MANUAL');
    expect(req.request.body.mandatoryRead).toBeFalse();

    const updated = { ...existing, title: 'Politique Qualité 2027' };
    req.flush(updated);
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('ne ferme pas le dialogue quand le document est archivé côté serveur (409)', async () => {
    await build({ document: existing });
    component.submit();
    http.expectOne(`${base}/d1`).flush({ title: 'archived' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build({ document: existing });
    component.submit();
    const req = http.expectOne(`${base}/d1`);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(`${base}/d1`);
    req.flush(existing);
  });

  it('reprend dossier, processus, propriétaire et listes ; envoie le propriétaire', async () => {
    const folders = [{ folder: { id: 'f1', parentId: null, name: 'Achats', position: 0 }, depth: 0, hasChildren: false }];
    const processes = [{ id: 'p1', name: 'Approvisionnement', position: 0 }];
    await build({ document: { ...existing, folderId: 'f1', processId: 'p1' }, folders, processes });
    expect(component.folders).toBe(folders);
    expect(component.processes).toBe(processes);
    expect(component.form.controls.folderId.value).toBe('f1');
    expect(component.form.controls.processId.value).toBe('p1');
    expect(component.form.controls.ownerId.value).toBe('u1');

    component.form.patchValue({ ownerId: 'u2' });
    component.submit();
    const req = http.expectOne(`${base}/d1`);
    expect(req.request.body.ownerId).toBe('u2');
    expect(req.request.body.folderId).toBe('f1');
    expect(req.request.body.processId).toBe('p1');
    expect(req.request.body.clearFolder).toBeUndefined();
    expect(req.request.body.clearProcess).toBeUndefined();
    req.flush(existing);
  });

  it('sans listes fournies, le dialogue part de listes vides', async () => {
    await build({ document: existing });
    expect(component.folders).toEqual([]);
    expect(component.processes).toEqual([]);
  });

  it('exige un propriétaire', async () => {
    await build({ document: existing });
    component.form.patchValue({ ownerId: null });
    component.submit();
    http.expectNone(`${base}/d1`);
  });

  it('vider le dossier ou le processus se dit explicitement', async () => {
    await build({ document: { ...existing, folderId: 'f1', processId: 'p1' } });
    component.form.patchValue({ folderId: null, processId: null });
    component.submit();
    const req = http.expectOne(`${base}/d1`);
    expect(req.request.body.clearFolder).toBeTrue();
    expect(req.request.body.clearProcess).toBeTrue();
    expect(req.request.body.folderId).toBeUndefined();
    expect(req.request.body.processId).toBeUndefined();
    req.flush(existing);
  });

  it('un dossier ou un processus jamais posé n’est pas « vidé »', async () => {
    await build({ document: { ...existing, folderId: null, processId: undefined } });
    component.submit();
    const req = http.expectOne(`${base}/d1`);
    expect(req.request.body.clearFolder).toBeUndefined();
    expect(req.request.body.clearProcess).toBeUndefined();
    req.flush(existing);
  });

  it('déplacer dans un autre dossier ne vide rien', async () => {
    await build({ document: { ...existing, folderId: 'f1' } });
    component.form.patchValue({ folderId: 'f2' });
    component.submit();
    const req = http.expectOne(`${base}/d1`);
    expect(req.request.body.folderId).toBe('f2');
    expect(req.request.body.clearFolder).toBeUndefined();
    req.flush(existing);
  });

  it('un brouillon n’a pas d’échéance de revue réglable', async () => {
    await build({ document: { ...existing, lifecycle: 'DRAFT', nextReviewDate: null } });
    expect(component.reviewEditable).toBeFalse();
    expect(component.form.controls.nextReviewDate.value).toBe('');
    component.form.patchValue({ nextReviewDate: '2027-01-01' });
    component.submit();
    const req = http.expectOne(`${base}/d1`);
    expect('nextReviewDate' in req.request.body).toBeFalse();
    req.flush(existing);
  });

  it('sans cycle de vie connu, le document est traité en brouillon', async () => {
    await build({ document: { ...existing, lifecycle: undefined } });
    expect(component.reviewEditable).toBeFalse();
  });

  it('un document en vigueur envoie l’échéance de revue seulement si elle change', async () => {
    await build({ document: { ...existing, lifecycle: 'IN_FORCE', nextReviewDate: '2027-06-30' } });
    expect(component.reviewEditable).toBeTrue();
    expect(component.form.controls.nextReviewDate.value).toBe('2027-06-30');

    component.submit();
    const same = http.expectOne(`${base}/d1`);
    expect('nextReviewDate' in same.request.body).toBeFalse();
    same.flush(existing);

    component.form.patchValue({ nextReviewDate: '2027-09-30' });
    component.submit();
    const changed = http.expectOne(`${base}/d1`);
    expect(changed.request.body.nextReviewDate).toBe('2027-09-30');
    changed.flush(existing);

    component.form.patchValue({ nextReviewDate: '' });
    component.submit();
    const emptied = http.expectOne(`${base}/d1`);
    expect('nextReviewDate' in emptied.request.body).toBeFalse();
    emptied.flush(existing);
  });

  it('ferme sans rien modifier à l\'annulation', async () => {
    await build({ document: existing });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(`${base}/d1`);
  });
});

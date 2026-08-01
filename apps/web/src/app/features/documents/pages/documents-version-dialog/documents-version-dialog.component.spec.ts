import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentVersionResponse } from '../../documents.types';
import { DocumentsVersionDialogComponent } from './documents-version-dialog.component';

/**
 * §4.1 / ISO 9001 §7.5.3 — une nouvelle version n'existe pas sans motif de
 * changement : la note est obligatoire, et l'auteur vient du JWT (§18.2 #2).
 */
describe('DocumentsVersionDialogComponent', () => {
  let component: DocumentsVersionDialogComponent;
  let fixture: ComponentFixture<DocumentsVersionDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DocumentsVersionDialogComponent, DocumentVersionResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/documents/d1/versions`;
  const AUTHOR = '11111111-1111-1111-1111-111111111111';

  const created: DocumentVersionResponse = {
    id: 'v2', documentId: 'd1', versionNumber: 2, status: 'DRAFT', authorId: AUTHOR,
    changeNote: 'Révision annuelle',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: AUTHOR, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<DocumentsVersionDialogComponent, DocumentVersionResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [DocumentsVersionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        { provide: MAT_DIALOG_DATA, useValue: { documentId: 'd1', nextVersionNumber: 2 } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentsVersionDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('annonce le numéro de la version à venir', () => {
    expect(component.data.nextVersionNumber).toBe(2);
    expect(component.data.documentId).toBe('d1');
  });

  it('exige une note de version — une version sans motif n\'est pas auditable', () => {
    expect(component.form.controls.changeNote.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.changeNote.touched).toBeTrue();
  });

  it('borne la note à 255 caractères et l\'URI de contenu à 1024', () => {
    component.form.controls.changeNote.setValue('n'.repeat(256));
    expect(component.form.controls.changeNote.hasError('maxlength')).toBeTrue();

    component.form.controls.changeNote.setValue('Révision annuelle');
    component.form.controls.contentUri.setValue('s3://' + 'u'.repeat(1020));
    expect(component.form.controls.contentUri.hasError('maxlength')).toBeTrue();

    component.submit();
    http.expectNone(url);
  });

  it('bloque la création quand la session a expiré plutôt que d\'anonymiser l\'auteur', () => {
    currentUser = null;
    component.form.controls.changeNote.setValue('Révision annuelle');
    component.submit();
    http.expectNone(url);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('crée la version en DRAFT avec l\'auteur issu du JWT', () => {
    component.form.patchValue({
      changeNote: '  Révision annuelle  ', content: '   ', contentUri: '   '
    });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.changeNote).toBe('Révision annuelle');
    expect(req.request.body.content).toBeUndefined();
    expect(req.request.body.contentUri).toBeUndefined();
    expect(req.request.body.authorId).toBe(AUTHOR);

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet le corps ou l\'URI du document quand ils sont fournis', () => {
    component.form.patchValue({
      changeNote: 'Révision', content: '  Corps révisé  ', contentUri: '  s3://bucket/doc.pdf  '
    });
    component.submit();
    const req = http.expectOne(url);
    expect(req.request.body.content).toBe('Corps révisé');
    expect(req.request.body.contentUri).toBe('s3://bucket/doc.pdf');
    req.flush(created);
  });

  it('ne ferme pas le dialogue quand le serveur refuse la version', () => {
    component.form.controls.changeNote.setValue('Révision');
    component.submit();
    http.expectOne(url).flush({ title: 'draft exists' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.changeNote.setValue('Révision');
    component.submit();
    const req = http.expectOne(url);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(url);
    req.flush(created);
  });

  it('ferme sans rien créer à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});

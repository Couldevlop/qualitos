import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentResponse } from '../../documents.types';
import { DocumentsCreateDialogComponent } from './documents-create-dialog.component';

/**
 * Le code documentaire sert de clé métier dans tout le référentiel (et de nom de
 * fichier à l'export) : le format est rejoué côté client pour refuser la saisie
 * avant qu'un 400 ne remonte, et le propriétaire vient du JWT (§18.2 #2).
 */
describe('DocumentsCreateDialogComponent', () => {
  let component: DocumentsCreateDialogComponent;
  let fixture: ComponentFixture<DocumentsCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DocumentsCreateDialogComponent, DocumentResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/documents`;
  const OWNER = '11111111-1111-1111-1111-111111111111';

  const created: DocumentResponse = {
    id: 'd1', tenantId: 't1', code: 'POL-QUAL-001', title: 'Politique Qualité',
    type: 'POLICY', status: 'ACTIVE', ownerId: OWNER, mandatoryRead: true,
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', versions: []
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: OWNER, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<DocumentsCreateDialogComponent, DocumentResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [DocumentsCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentsCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('expose les sept types documentaires du référentiel avec un libellé', () => {
    expect(component.types.map(t => t.value)).toEqual([
      'POLICY', 'PROCEDURE', 'WORK_INSTRUCTION', 'RECORD', 'FORM', 'MANUAL', 'OTHER'
    ]);
    expect(component.types.every(t => !!t.label)).toBeTrue();
    expect(component.form.controls.type.value).toBe('PROCEDURE');
  });

  it('pré-remplit la note de la première version pour ne jamais publier une version sans motif', () => {
    expect(component.form.controls.initialChangeNote.value).toBe('Création initiale');
    expect(component.form.controls.mandatoryRead.value).toBeFalse();
  });

  it('refuse un code hors format (minuscules, espaces, caractères exotiques)', () => {
    component.form.controls.code.setValue('pol qual 001');
    expect(component.form.controls.code.hasError('pattern')).toBeTrue();

    component.form.controls.code.setValue('POL/QUAL');
    expect(component.form.controls.code.hasError('pattern')).toBeTrue();

    component.form.controls.code.setValue('POL-QUAL_001.V2');
    expect(component.form.controls.code.valid).toBeTrue();
  });

  it('exige un code et un titre avant tout envoi', () => {
    expect(component.form.controls.code.hasError('required')).toBeTrue();
    expect(component.form.controls.title.hasError('required')).toBeTrue();

    component.submit();
    http.expectNone(base);
    expect(component.form.controls.code.touched).toBeTrue();
    expect(component.submitting).toBeFalse();
  });

  it('bloque la création quand la session a expiré', () => {
    currentUser = null;
    component.form.patchValue({ code: 'POL-001', title: 'Politique' });
    component.submit();
    http.expectNone(base);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('rejette un code entouré d\'espaces au lieu de le normaliser silencieusement', () => {
    component.form.controls.code.setValue('  POL-QUAL-001  ');
    // Le motif s'applique à la valeur brute : l'espace est refusé à la saisie,
    // le `trim()` du submit n'est donc jamais atteint pour ce champ.
    expect(component.form.controls.code.hasError('pattern')).toBeTrue();
    component.form.controls.title.setValue('Politique');
    component.submit();
    http.expectNone(base);
  });

  it('normalise les champs et omet le contenu initial vide (enveloppe seule)', () => {
    component.form.patchValue({
      code: 'POL-QUAL-001',
      title: '  Politique Qualité  ',
      description: '   ',
      type: 'POLICY',
      mandatoryRead: true,
      initialContent: '   ',
      initialChangeNote: '  Création initiale  '
    });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.code).toBe('POL-QUAL-001');
    expect(req.request.body.title).toBe('Politique Qualité');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.initialContent).toBeUndefined();
    expect(req.request.body.initialChangeNote).toBe('Création initiale');
    expect(req.request.body.mandatoryRead).toBeTrue();
    expect(req.request.body.ownerId).toBe(OWNER);

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet le contenu de la première version quand il est saisi', () => {
    component.form.patchValue({
      code: 'PROC-001', title: 'Procédure', initialContent: '  Objet et domaine d\'application  '
    });
    component.submit();
    const req = http.expectOne(base);
    expect(req.request.body.initialContent).toBe('Objet et domaine d\'application');
    req.flush(created);
  });

  it('ne ferme pas le dialogue quand le code est déjà pris (409)', () => {
    component.form.patchValue({ code: 'POL-QUAL-001', title: 'Politique' });
    component.submit();
    http.expectOne(base).flush({ title: 'duplicate code' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    expect(component.form.controls.code.value).toBe('POL-QUAL-001');
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.patchValue({ code: 'POL-001', title: 'Politique' });
    component.submit();
    const req = http.expectOne(base);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(base);

    req.flush(created);
  });

  it('ferme le dialogue sans rien créer à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(base);
  });
});

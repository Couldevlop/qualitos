import { HttpEventType, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { DocumentResponse, UploadPolicy } from '../../documents.types';
import { CreateDialogData, DocumentsCreateDialogComponent } from './documents-create-dialog.component';

/**
 * Le code documentaire sert de clé métier dans tout le référentiel (et de nom de
 * fichier à l'export) : le format est rejoué côté client pour refuser la saisie
 * avant qu'un 400 ne remonte. Le propriétaire est par défaut celui qui crée ;
 * un fichier joint part dans la première version (ADR 0094).
 */
describe('DocumentsCreateDialogComponent', () => {
  let component: DocumentsCreateDialogComponent;
  let fixture: ComponentFixture<DocumentsCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DocumentsCreateDialogComponent, DocumentResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/documents`;
  const filesBase = `${environment.apiBaseUrl}/api/v1/document-files`;
  const OWNER = '11111111-1111-1111-1111-111111111111';
  const POLICY: UploadPolicy = { maxFileMb: 1, allowedTypes: ['application/pdf'], allowedExtensions: ['pdf', 'docx'] };

  const created: DocumentResponse = {
    id: 'd1', tenantId: 't1', code: 'POL-QUAL-001', title: 'Politique Qualité',
    type: 'POLICY', status: 'ACTIVE', ownerId: OWNER, mandatoryRead: true,
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    versions: [
      { id: 'v1', documentId: 'd1', versionNumber: 1, status: 'OBSOLETE', authorId: OWNER, createdAt: '', updatedAt: '' },
      { id: 'v2', documentId: 'd1', versionNumber: 2, status: 'DRAFT', authorId: OWNER, createdAt: '', updatedAt: '' }
    ]
  };

  const pickInput = (file: File): HTMLInputElement => {
    const dt = new DataTransfer();
    dt.items.add(file);
    const input = document.createElement('input');
    input.type = 'file';
    input.files = dt.files;
    return input;
  };
  const pdf = (name = 'politique.pdf', content = '%PDF-1.7'): File =>
    new File([content], name, { type: 'application/pdf' });

  /** Construit le dialogue ; la politique d'envoi est servie sauf `policy: undefined`. */
  async function build(opts: { data?: CreateDialogData | null; policy?: UploadPolicy | null } = { policy: POLICY }): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<DocumentsCreateDialogComponent, DocumentResponse>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const providers: unknown[] = [
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideDirectoryStub(),
      { provide: MatDialogRef, useValue: dialogRef },
      { provide: MatSnackBar, useValue: snack },
      { provide: AuthService, useValue: { snapshot: () => currentUser } }
    ];
    if (opts.data !== undefined) providers.push({ provide: MAT_DIALOG_DATA, useValue: opts.data });

    await TestBed.configureTestingModule({
      declarations: [DocumentsCreateDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: providers as never[]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentsCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    if (opts.policy !== undefined) {
      const req = http.expectOne(`${base}/upload-policy`);
      if (opts.policy) req.flush(opts.policy); else req.flush({}, { status: 500, statusText: 'Error' });
    }
  }

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: OWNER, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  describe('formulaire', () => {
    beforeEach(async () => { await build(); });

    it('expose les huit types documentaires du référentiel avec un libellé', () => {
      expect(component.types.map(t => t.value)).toEqual([
        'POLICY', 'PROCEDURE', 'WORK_INSTRUCTION', 'RECORD', 'FORM', 'MANUAL', 'EXTERNAL', 'OTHER'
      ]);
      expect(component.types.every(t => !!t.label)).toBeTrue();
      expect(component.form.controls.type.value).toBe('PROCEDURE');
    });

    it('pré-remplit la note de la première version pour ne jamais publier une version sans motif', () => {
      expect(component.form.controls.initialChangeNote.value).toBe('Création initiale');
      expect(component.form.controls.mandatoryRead.value).toBeFalse();
    });

    it('le propriétaire est par défaut l’utilisateur courant', () => {
      expect(component.form.controls.ownerId.value).toBe(OWNER);
    });

    it('sans données de dialogue : ni dossier, ni listes', () => {
      expect(component.folders).toEqual([]);
      expect(component.processes).toEqual([]);
      expect(component.form.controls.folderId.value).toBeNull();
      expect(component.accept).toBe('.pdf,.docx');
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

    it('exige un propriétaire', () => {
      component.form.patchValue({ code: 'POL-001', title: 'Politique', ownerId: null });
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
      expect(req.request.body.folderId).toBeNull();
      expect(req.request.body.processId).toBeNull();

      req.flush(created);
      expect(dialogRef.close).toHaveBeenCalledWith(created);
      expect(snack.open.calls.mostRecent().args[0]).toBe('Document créé.');
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
      expect(snack.open.calls.mostRecent().args[0]).toBe('État incompatible — rechargez la page.');
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

    describe('fichier joint', () => {
      it('refuse un fichier vide', () => {
        component.onPick(pickInput(new File([], 'vide.pdf')));
        expect(component.fileError).toBe('Ce fichier est vide.');
      });

      it('refuse un fichier trop volumineux', () => {
        component.onPick(pickInput(new File([new Uint8Array(1024 * 1024 + 1)], 'gros.pdf')));
        expect(component.fileError).toBe('Fichier trop volumineux (maximum 1 Mo).');
      });

      it('refuse une extension non acceptée ; retirer le fichier efface l’erreur', () => {
        const input = pickInput(pdf('macro.exe'));
        component.onPick(input);
        expect(input.value).toBe('');
        expect(component.file?.name).toBe('macro.exe');
        expect(component.fileError).toBe('Format non accepté.');

        component.form.patchValue({ code: 'POL-001', title: 'Politique' });
        component.submit();
        http.expectNone(base);

        component.clearFile();
        expect(component.file).toBeNull();
        expect(component.fileError).toBeNull();
      });

      it('un choix vide ne change rien', () => {
        const input = document.createElement('input');
        input.type = 'file';
        component.onPick(input);
        expect(component.file).toBeNull();
      });

      it('crée puis envoie le fichier dans la dernière version, avec la progression', () => {
        const file = pdf();
        component.onPick(pickInput(file));
        expect(component.fileError).toBeNull();
        component.form.patchValue({ code: 'POL-QUAL-001', title: 'Politique Qualité' });
        component.submit();

        http.expectOne(base).flush(created);
        expect(component.progress).toBe(0);
        const upload = http.expectOne(`${filesBase}/d1/versions/v2`);
        expect(upload.request.method).toBe('POST');
        expect(((upload.request.body as FormData).get('file') as File).name).toBe('politique.pdf');

        upload.event({ type: HttpEventType.UploadProgress, loaded: 3, total: 4 });
        expect(component.progress).toBe(75);
        expect(dialogRef.close).not.toHaveBeenCalled();

        upload.flush(created.versions[1]);
        expect(dialogRef.close).toHaveBeenCalledWith(created);
        expect(snack.open.calls.mostRecent().args[0]).toBe('Document créé.');
        expect(component.progress).toBeNull();
        expect(component.submitting).toBeFalse();
      });

      it('un fichier refusé ferme quand même sur le document créé, et le dit', () => {
        component.onPick(pickInput(pdf()));
        component.form.patchValue({ code: 'POL-QUAL-001', title: 'Politique Qualité' });
        component.submit();

        http.expectOne(base).flush(created);
        http.expectOne(`${filesBase}/d1/versions/v2`).error(new ProgressEvent('error'), { status: 0 });

        expect(dialogRef.close).toHaveBeenCalledWith(created);
        expect(snack.open.calls.mostRecent().args[0]).toBe('Service inaccessible — vérifiez votre connexion.');
        expect(snack.open.calls.mostRecent().args[2]).toEqual({ duration: 6000 });
        expect(component.submitting).toBeFalse();
      });

      it('un fichier refusé sans statut donne le message de repli', () => {
        component.onPick(pickInput(pdf()));
        component.form.patchValue({ code: 'POL-QUAL-001', title: 'Politique Qualité' });
        component.submit();

        http.expectOne(base).flush(created);
        http.expectOne(`${filesBase}/d1/versions/v2`).flush({}, { status: 404, statusText: 'Not Found' });

        expect(dialogRef.close).toHaveBeenCalledWith(created);
        expect(snack.open.calls.mostRecent().args[0]).toBe('Document créé, mais le fichier n\'a pas été envoyé.');
      });
    });
  });

  it('reprend le dossier courant et passe dossier et processus à la création', async () => {
    const folders = [{ folder: { id: 'f1', parentId: null, name: 'Achats', position: 0 }, depth: 0, hasChildren: false }];
    const processes = [{ id: 'p1', name: 'Approvisionnement', position: 0 }];
    await build({ data: { folderId: 'f1', folders, processes }, policy: POLICY });
    expect(component.folders).toBe(folders);
    expect(component.processes).toBe(processes);
    expect(component.form.controls.folderId.value).toBe('f1');

    component.form.patchValue({ code: 'PROC-001', title: 'Procédure', processId: 'p1' });
    component.submit();
    const req = http.expectOne(base);
    expect(req.request.body.folderId).toBe('f1');
    expect(req.request.body.processId).toBe('p1');
    req.flush(created);
  });

  it('accepte des données de dialogue nulles', async () => {
    await build({ data: null, policy: POLICY });
    expect(component.folders).toEqual([]);
    expect(component.form.controls.folderId.value).toBeNull();
  });

  it('sans session, le propriétaire reste à choisir et rien ne part', async () => {
    currentUser = null;
    await build();
    expect(component.form.controls.ownerId.value).toBeNull();
    component.form.patchValue({ code: 'POL-001', title: 'Politique' });
    component.submit();
    http.expectNone(base);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('un fichier choisi avant la politique est revérifié à son arrivée', async () => {
    await build({ policy: undefined });
    expect(component.accept).toBe('');
    component.onPick(pickInput(pdf('macro.exe')));
    expect(component.fileError).toBeNull();

    http.expectOne(`${base}/upload-policy`).flush(POLICY);
    expect(component.fileError).toBe('Format non accepté.');
  });

  it('une politique indisponible ne bloque que les fichiers vides', async () => {
    await build({ policy: null });
    component.onPick(pickInput(pdf('macro.exe')));
    expect(component.fileError).toBeNull();
    expect(component.size(1024)).toBe('1 Ko');
  });
});

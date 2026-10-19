import { HttpEvent, HttpEventType, HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject, of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { DocumentsService } from '../../documents.service';
import {
  CreateDocumentRequest, DocumentResponse, DocumentVersionResponse, UploadPolicy
} from '../../documents.types';
import {
  DocumentsImportDialogComponent, ImportDialogData, ImportItem, codeFromFileName, extensionOf, guessType,
  problemOf, titleFromFileName, uniqueCode
} from './documents-import-dialog.component';

const pdf = (name: string, content = '%PDF-1.7'): File => new File([content], name, { type: 'application/pdf' });

describe('import GED — aides pures', () => {
  const POLICY: UploadPolicy = { maxFileMb: 1, allowedTypes: ['application/pdf'], allowedExtensions: ['pdf', 'docx'] };

  const item = (over: Partial<ImportItem> = {}): ImportItem => ({
    file: pdf('procedure.pdf'), code: 'PROC', title: 'Procédure', type: 'PROCEDURE',
    state: 'ready', progress: 0, error: null, created: null, ...over
  });

  describe('problemOf', () => {
    it('accepte une ligne correcte, avec ou sans politique connue', () => {
      expect(problemOf(item(), POLICY)).toBeNull();
      expect(problemOf(item({ file: pdf('a.exe') }), null)).toBeNull();
    });

    it('refuse un fichier vide', () => {
      expect(problemOf(item({ file: new File([], 'vide.pdf') }), null)).toBe('Ce fichier est vide.');
    });

    it('refuse un fichier trop volumineux en citant la limite', () => {
      const big = new File([new Uint8Array(1024 * 1024 + 1)], 'gros.pdf');
      expect(problemOf(item({ file: big }), POLICY)).toBe('Fichier trop volumineux (maximum 1 Mo).');
    });

    it('refuse une extension non acceptée ou absente', () => {
      expect(problemOf(item({ file: pdf('script.exe') }), POLICY)).toBe('Format non accepté.');
      expect(problemOf(item({ file: pdf('sans-extension') }), POLICY)).toBe('Format non accepté.');
      expect(problemOf(item({ file: pdf('MAJUSCULES.DOCX') }), POLICY)).toBeNull();
    });

    it('refuse un code vide, hors format ou trop long', () => {
      const bad = 'Code invalide : lettres majuscules, chiffres, point, tiret, souligné.';
      expect(problemOf(item({ code: '' }), POLICY)).toBe(bad);
      expect(problemOf(item({ code: 'proc 1' }), POLICY)).toBe(bad);
      expect(problemOf(item({ code: 'A'.repeat(101) }), POLICY)).toBe(bad);
      expect(problemOf(item({ code: 'A'.repeat(100) }), POLICY)).toBeNull();
    });

    it('refuse un titre vide ou trop long', () => {
      expect(problemOf(item({ title: '   ' }), POLICY)).toBe('Le titre est obligatoire.');
      expect(problemOf(item({ title: 'T'.repeat(256) }), POLICY)).toBe('Le titre est obligatoire.');
    });

    it('un document déjà créé n’est plus jugé sur son code ni son titre', () => {
      expect(problemOf(item({ code: '', title: '', created: {} as DocumentResponse }), POLICY)).toBeNull();
      expect(problemOf(item({ code: '', created: {} as DocumentResponse, file: pdf('x.exe') }), POLICY))
        .toBe('Format non accepté.');
    });
  });

  it('extensionOf : la dernière extension, en minuscules, ou null', () => {
    expect(extensionOf('a.PDF')).toBe('pdf');
    expect(extensionOf('archive.tar.gz')).toBe('gz');
    expect(extensionOf('.bashrc')).toBeNull();
    expect(extensionOf('fichier.')).toBeNull();
    expect(extensionOf('sans')).toBeNull();
  });

  it('codeFromFileName : majuscules sans accents, séparateurs normalisés', () => {
    expect(codeFromFileName('Procédure achats v2.pdf')).toBe('PROCEDURE-ACHATS-V2');
    expect(codeFromFileName('Été — déjà   vu!.docx')).toBe('ETE-DEJA-VU');
    expect(codeFromFileName('mode_op.v3.pdf')).toBe('MODE_OP.V3');
    expect(codeFromFileName('--Fiche--.pdf')).toBe('FICHE');
    expect(codeFromFileName('sans-extension')).toBe('SANS-EXTENSION');
  });

  it('codeFromFileName : un nom sans rien d’utilisable donne DOC', () => {
    expect(codeFromFileName('   .pdf')).toBe('DOC');
    expect(codeFromFileName('###.pdf')).toBe('DOC');
    expect(codeFromFileName('')).toBe('DOC');
  });

  it('codeFromFileName : laisse la place d’un suffixe d’unicité', () => {
    expect(codeFromFileName('x'.repeat(300) + '.pdf').length).toBe(96);
  });

  it('titleFromFileName : le nom sans extension, soulignés en espaces', () => {
    expect(titleFromFileName('Procédure_achats__v2.pdf')).toBe('Procédure achats v2');
    expect(titleFromFileName('  Plan   de   contrôle .xlsx')).toBe('Plan de contrôle');
    expect(titleFromFileName('___.pdf')).toBe('___.pdf');
    expect(titleFromFileName('t'.repeat(300)).length).toBe(255);
  });

  it('uniqueCode : suffixe le premier numéro libre', () => {
    expect(uniqueCode('PROC', new Set())).toBe('PROC');
    expect(uniqueCode('PROC', new Set(['PROC']))).toBe('PROC-2');
    expect(uniqueCode('PROC', new Set(['PROC', 'PROC-2', 'PROC-3']))).toBe('PROC-4');
  });

  it('guessType : devine le type du nom, Autre à défaut', () => {
    expect(guessType('Politique qualité.pdf')).toBe('POLICY');
    expect(guessType('POL-001.pdf')).toBe('POLICY');
    expect(guessType('MQ v3.docx')).toBe('MANUAL');
    expect(guessType('Manuel qualité.pdf')).toBe('MANUAL');
    expect(guessType('Mode opératoire tour.pdf')).toBe('WORK_INSTRUCTION');
    expect(guessType('IT-12 montage.pdf')).toBe('WORK_INSTRUCTION');
    expect(guessType('Formulaire réclamation.xlsx')).toBe('FORM');
    expect(guessType('ENR-04.pdf')).toBe('RECORD');
    expect(guessType('Procédure achats.pdf')).toBe('PROCEDURE');
    expect(guessType('PR-01.pdf')).toBe('PROCEDURE');
    expect(guessType('audit interne.pdf')).toBe('OTHER');
    expect(guessType('rapport.pdf')).toBe('OTHER');
  });
});

/**
 * L'import de fichiers dans la GED (ADR 0094) : chaque fichier devient un
 * document en brouillon dont la première version porte le fichier ; les
 * fichiers partent un par un, et une reprise ne recrée pas un document déjà créé.
 */
describe('DocumentsImportDialogComponent', () => {
  let fixture: ComponentFixture<DocumentsImportDialogComponent>;
  let component: DocumentsImportDialogComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let ref: jasmine.SpyObj<MatDialogRef<DocumentsImportDialogComponent, DocumentResponse[]>>;
  let user: AuthUser | null;
  let policy$: Subject<UploadPolicy>;
  let data: ImportDialogData;

  const POLICY: UploadPolicy = { maxFileMb: 25, allowedTypes: [], allowedExtensions: ['pdf', 'docx'] };

  const version = (id: string, versionNumber: number): DocumentVersionResponse => ({
    id, documentId: 'd', versionNumber, status: 'DRAFT', authorId: 'me', createdAt: '', updatedAt: ''
  });

  const created = (id: string): DocumentResponse => ({
    id, tenantId: 't', code: id.toUpperCase(), title: id, type: 'OTHER', status: 'ACTIVE', ownerId: 'me',
    mandatoryRead: false, createdAt: '', updatedAt: '',
    versions: [version(id + '-v1', 1), version(id + '-v2', 2)]
  });

  const progress = (loaded: number, total: number): HttpEvent<DocumentVersionResponse> =>
    ({ type: HttpEventType.UploadProgress, loaded, total });
  const response = (): HttpEvent<DocumentVersionResponse> =>
    new HttpResponse<DocumentVersionResponse>({ body: version('v', 1), status: 200 });

  async function build(files: File[], folderId: string | null = 'f1'): Promise<void> {
    data = {
      files, folderId,
      folders: [{ folder: { id: 'f1', parentId: null, name: 'Achats', position: 0 }, depth: 0, hasChildren: false }],
      processes: [{ id: 'p1', name: 'Approvisionnement', position: 0 }]
    };
    await TestBed.configureTestingModule({
      declarations: [DocumentsImportDialogComponent],
      imports: [SharedModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: DocumentsService, useValue: svc },
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: AuthService, useValue: { snapshot: () => user } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsImportDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    user = { userId: 'me', tenantId: 't', displayName: 'Moi', roles: [] };
    policy$ = new Subject<UploadPolicy>();
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['uploadPolicy', 'create', 'uploadFile']);
    svc.uploadPolicy.and.returnValue(policy$);
    ref = jasmine.createSpyObj<MatDialogRef<DocumentsImportDialogComponent, DocumentResponse[]>>('MatDialogRef', ['close']);
  });

  it('reprend le dossier courant et transforme chaque fichier en ligne éditable', async () => {
    await build([pdf('Procédure achats.pdf'), pdf('Manuel qualité.pdf')]);
    expect(component.common.getRawValue()).toEqual({ folderId: 'f1', processId: null, mandatoryRead: false });
    expect(component.items.map(i => [i.code, i.title, i.type, i.state])).toEqual([
      ['PROCEDURE-ACHATS', 'Procédure achats', 'PROCEDURE', 'ready'],
      ['MANUEL-QUALITE', 'Manuel qualité', 'MANUAL', 'ready']
    ]);
    expect(component.pending.length).toBe(2);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('li.item').length).toBe(2);
  });

  it('sans dossier courant ni fichier, part vide', async () => {
    await build([], null);
    expect(component.common.controls.folderId.value).toBeNull();
    expect(component.items).toEqual([]);
    expect(component.finished).toBeFalse();
    expect(component.accept).toBe('');
  });

  it('dédoublonne les codes, y compris pour des fichiers ajoutés ensuite', async () => {
    await build([pdf('fiche.pdf'), pdf('fiche.docx')]);
    component.addFiles([pdf('Fiche.pdf')]);
    expect(component.items.map(i => i.code)).toEqual(['FICHE', 'FICHE-2', 'FICHE-3']);
  });

  it('la politique arrivée après les fichiers les revérifie', async () => {
    await build([pdf('a.pdf'), pdf('b.exe')]);
    expect(component.items.map(i => i.state)).toEqual(['ready', 'ready']);

    policy$.next(POLICY);
    expect(component.items.map(i => i.state)).toEqual(['ready', 'invalid']);
    expect(component.items[1].error).toBe('Format non accepté.');
    expect(component.hasInvalid).toBeTrue();
    expect(component.accept).toBe('.pdf,.docx');
  });

  it('une politique indisponible n’empêche pas l’import', async () => {
    svc.uploadPolicy.and.returnValue(throwError(() => ({ status: 500 })));
    await build([pdf('b.exe')]);
    expect(component.policy).toBeNull();
    expect(component.items[0].state).toBe('ready');
  });

  it('retire une ligne, sauf pendant l’envoi ou une fois importée', async () => {
    await build([pdf('a.pdf'), pdf('b.pdf'), pdf('c.pdf')]);
    const [a, b, c] = component.items;
    component.remove(a);
    expect(component.items).toEqual([b, c]);

    b.state = 'done';
    component.remove(b);
    component.running = true;
    component.remove(c);
    expect(component.items).toEqual([b, c]);
  });

  it('une ligne corrigée est mise en majuscules et revérifiée', async () => {
    await build([pdf('a.pdf')]);
    const item = component.items[0];
    item.code = 'pr-01';
    component.edited(item);
    expect(item.code).toBe('PR-01');
    expect(item.state).toBe('ready');

    item.code = 'PR 01';
    component.edited(item);
    expect(item.state).toBe('invalid');

    item.state = 'uploading';
    item.code = 'minuscule';
    component.edited(item);
    expect(item.code).toBe('minuscule');
    item.state = 'done';
    component.edited(item);
    expect(item.code).toBe('minuscule');
  });

  it('n’ajoute rien pendant l’envoi', async () => {
    await build([pdf('a.pdf')]);
    component.running = true;
    component.addFiles([pdf('b.pdf')]);
    expect(component.items.length).toBe(1);
  });

  it('crée puis envoie un fichier après l’autre, avec la progression, et ferme une fois tout importé', async () => {
    await build([pdf(' a .pdf'), pdf('b.pdf')]);
    component.common.patchValue({ processId: 'p1', mandatoryRead: true });
    const uploads = [new Subject<HttpEvent<DocumentVersionResponse>>(), new Subject<HttpEvent<DocumentVersionResponse>>()];
    svc.create.and.callFake((req: CreateDocumentRequest) => of(created(req.code.toLowerCase())));
    let n = 0;
    svc.uploadFile.and.callFake(() => uploads[n++]);

    component.start();
    expect(component.running).toBeTrue();
    expect(svc.create).toHaveBeenCalledTimes(1);
    expect(svc.create.calls.argsFor(0)[0]).toEqual({
      code: 'A', title: 'a', type: 'OTHER', ownerId: 'me', mandatoryRead: true, folderId: 'f1', processId: 'p1'
    });
    // La dernière version reçoit le fichier.
    expect(svc.uploadFile).toHaveBeenCalledOnceWith('a', 'a-v2', component.items[0].file);
    const [first, second] = component.items;
    expect(first.state).toBe('uploading');
    expect(second.state).toBe('ready');

    uploads[0].next(progress(50, 200));
    expect(first.progress).toBe(25);
    uploads[0].next({ type: HttpEventType.UploadProgress, loaded: 10 } as HttpEvent<DocumentVersionResponse>);
    expect(first.progress).toBe(25);
    uploads[0].next(response());
    uploads[0].complete();
    expect(first.state).toBe('done');
    expect(first.progress).toBe(100);
    expect(first.created?.id).toBe('a');

    expect(svc.create).toHaveBeenCalledTimes(2);
    expect(second.state).toBe('uploading');
    expect(ref.close).not.toHaveBeenCalled();
    uploads[1].next(response());
    uploads[1].complete();

    expect(component.running).toBeFalse();
    expect(component.finished).toBeTrue();
    expect(component.doneCount).toBe(2);
    expect(ref.close).toHaveBeenCalledWith([first.created!, second.created!]);
  });

  it('un fichier refusé garde le document créé ; la reprise ne le recrée pas', async () => {
    await build([pdf('a.pdf')]);
    svc.create.and.returnValue(of(created('a')));
    svc.uploadFile.and.returnValue(throwError(() => new Error('réseau')));

    component.start();
    const item = component.items[0];
    expect(item.state).toBe('failed');
    expect(item.created?.id).toBe('a');
    expect(item.error).toBe('Document créé, mais le fichier n\'a pas été envoyé.');
    expect(component.running).toBeFalse();
    expect(ref.close).not.toHaveBeenCalled();

    // Revérifiée, la ligne reste à reprendre, même si son code n'est plus valide.
    item.code = '';
    component.edited(item);
    expect(item.state).toBe('failed');

    svc.uploadFile.and.returnValue(of(response()));
    component.start();
    expect(svc.create).toHaveBeenCalledTimes(1);
    expect(svc.uploadFile).toHaveBeenCalledTimes(2);
    expect(item.state).toBe('done');
    expect(ref.close).toHaveBeenCalledWith([item.created!]);
  });

  it('une création refusée n’arrête pas les fichiers suivants', async () => {
    await build([pdf('a.pdf'), pdf('b.pdf')]);
    svc.create.and.callFake((req: CreateDocumentRequest) =>
      req.code === 'A' ? throwError(() => ({ status: 409 })) : of(created('b')));
    svc.uploadFile.and.returnValue(of(response()));

    component.start();
    const [a, b] = component.items;
    expect(a.state).toBe('failed');
    expect(a.created).toBeNull();
    expect(a.error).toBe('État incompatible — rechargez la page.');
    expect(b.state).toBe('done');
    expect(component.finished).toBeFalse();
    expect(ref.close).not.toHaveBeenCalled();

    // Fermer ne rend que les documents importés.
    component.close();
    expect(ref.close).toHaveBeenCalledWith([b.created!]);
  });

  it('une création en échec sans statut donne le message générique', async () => {
    await build([pdf('a.pdf')]);
    svc.create.and.returnValue(throwError(() => new Error('x')));
    component.start();
    expect(component.items[0].error).toBe('Erreur lors de la création.');
  });

  it('session expirée : rien ne part, chaque ligne le dit', async () => {
    await build([pdf('a.pdf'), pdf('b.pdf')]);
    user = null;
    component.start();
    expect(svc.create).not.toHaveBeenCalled();
    expect(component.running).toBeFalse();
    expect(component.items.every(i => i.state === 'failed')).toBeTrue();
    expect(component.items[0].error).toBe('Session expirée — veuillez vous reconnecter.');
  });

  it('ne démarre pas avec une ligne invalide, sans rien à envoyer ou déjà en cours', async () => {
    await build([pdf('a.pdf'), new File([], 'vide.pdf')]);
    component.start();
    expect(svc.create).not.toHaveBeenCalled();

    component.remove(component.items[1]);
    component.running = true;
    component.start();
    expect(svc.create).not.toHaveBeenCalled();

    component.running = false;
    component.items[0].state = 'done';
    component.start();
    expect(svc.create).not.toHaveBeenCalled();
  });

  it('fermer sans rien importer rend une liste vide', async () => {
    await build([pdf('a.pdf')]);
    component.close();
    expect(ref.close).toHaveBeenCalledWith([]);
  });

  it('ajoute les fichiers choisis ou déposés', async () => {
    await build([]);
    const dt = new DataTransfer();
    dt.items.add(pdf('choisi.pdf'));
    const input = document.createElement('input');
    input.type = 'file';
    input.files = dt.files;
    component.onPick(input);
    expect(component.items.map(i => i.file.name)).toEqual(['choisi.pdf']);
    expect(input.value).toBe('');

    const over = new DragEvent('dragover', { cancelable: true });
    component.onDragOver(over);
    expect(over.defaultPrevented).toBeTrue();
    expect(component.dragging).toBeTrue();
    component.onDragLeave(new DragEvent('dragleave'));
    expect(component.dragging).toBeFalse();

    const dropped = new DataTransfer();
    dropped.items.add(pdf('depose.pdf'));
    component.onDragOver(new DragEvent('dragover', { cancelable: true }));
    const drop = new DragEvent('drop', { dataTransfer: dropped, cancelable: true });
    component.onDrop(drop);
    expect(drop.defaultPrevented).toBeTrue();
    expect(component.dragging).toBeFalse();
    expect(component.items.map(i => i.file.name)).toEqual(['choisi.pdf', 'depose.pdf']);

    component.onDrop(new DragEvent('drop', { cancelable: true }));
    expect(component.items.length).toBe(2);
  });

  it('aides d’affichage', async () => {
    await build([pdf('a.pdf')]);
    expect(component.size(2048)).toBe('2 Ko');
    expect(component.trackByItem(0, component.items[0])).toBe(component.items[0].file);
  });
});

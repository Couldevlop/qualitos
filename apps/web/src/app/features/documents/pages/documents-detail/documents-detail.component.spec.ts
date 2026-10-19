import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { HttpEvent, HttpEventType, HttpResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentCircuitComponent } from '../../components/document-circuit/document-circuit.component';
import { DocumentsService } from '../../documents.service';
import { DocumentFolder, DocumentResponse, DocumentVersionResponse, FileLink } from '../../documents.types';
import { DocumentsEditDialogComponent } from '../documents-edit-dialog/documents-edit-dialog.component';
import { DocumentsRejectDialogComponent } from '../documents-reject-dialog/documents-reject-dialog.component';
import { DocumentsDetailComponent } from './documents-detail.component';
import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { PersonNameModule } from '../../../../shared/person-name/person-name.module';

describe('DocumentsDetailComponent — revue et circuit', () => {

  let fixture: ComponentFixture<DocumentsDetailComponent>;
  let component: DocumentsDetailComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const version = (o: Partial<DocumentVersionResponse>): DocumentVersionResponse => ({
    id: 'v1', documentId: 'doc-1', versionNumber: 1, status: 'PUBLISHED', authorId: 'autrice',
    createdAt: '', updatedAt: '', ...o
  });

  const DOC: DocumentResponse = {
    id: 'doc-1', tenantId: 't', code: 'PRO-001', title: 'Procédure', type: 'PROCEDURE', status: 'ACTIVE',
    ownerId: 'o', mandatoryRead: false, createdAt: '', updatedAt: '',
    versions: [
      version({ id: 'v1', versionNumber: 1, status: 'PUBLISHED' }),
      version({ id: 'v2', versionNumber: 2, status: 'IN_REVIEW' })
    ]
  } as DocumentResponse;

  const FOLDERS: DocumentFolder[] = [
    { id: 'f1', parentId: null, name: 'Procédures', position: 0 },
    { id: 'f2', parentId: 'f1', name: 'Réalisation', position: 0 }
  ];

  beforeEach(async () => {
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService',
      ['get', 'approve', 'folders', 'processes', 'uploadFile', 'fileLink']);
    svc.get.and.returnValue(of(DOC));
    svc.folders.and.returnValue(of(FOLDERS));
    svc.processes.and.returnValue(of([{ id: 'p1', name: 'Achats', position: 0 }]));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const circuits = jasmine.createSpyObj<CircuitService>('CircuitService', ['run']);
    circuits.run.and.returnValue(of(null));
    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['me', 'can']);
    authz.me.and.returnValue(of({ userId: 'chloe', roles: ['QUALITY_MANAGER'], permissions: ['*'] }));
    authz.can.and.returnValue(of(true));

    await TestBed.configureTestingModule({
      declarations: [DocumentsDetailComponent, DocumentCircuitComponent],
      imports: [PersonNameModule, SharedModule, UiModule, NoopAnimationsModule],
      providers: [provideDirectoryStub(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'doc-1' })) } },
        { provide: DocumentsService, useValue: svc },
        { provide: CircuitService, useValue: circuits },
        { provide: AuthzService, useValue: authz },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'chloe' }) } },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('met en tête la version en revue, avec son circuit', () => {
    expect(component.attention(DOC).map(v => v.id)).toEqual(['v2']);
    expect(hote().querySelectorAll('qos-document-circuit').length).toBe(1);
  });

  it('un brouillon revenu d’un refus demande aussi l’attention ; un brouillon neuf, non', () => {
    const d = { ...DOC, versions: [
      version({ id: 'a', status: 'DRAFT', rejectionReason: 'Incomplet' }),
      version({ id: 'b', status: 'DRAFT' })
    ] } as DocumentResponse;
    expect(component.attention(d).map(v => v.id)).toEqual(['a']);
    expect(component.trackById(0, d.versions[0])).toBe('a');
  });

  it('refuser ouvre le dialogue motivé et recharge après un refus', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(version({ status: 'DRAFT' })) } as never);
    svc.get.calls.reset();

    (hote().querySelector('[data-test="refuser"]') as HTMLButtonElement).click();

    expect(dialog.open).toHaveBeenCalledWith(DocumentsRejectDialogComponent, jasmine.objectContaining({
      data: { version: DOC.versions[1] }
    }));
    expect(svc.get).toHaveBeenCalled();
  });

  it('une approbation qui ne franchit qu’une étape le dit', () => {
    svc.approve.and.returnValue(of(version({ status: 'IN_REVIEW' })));
    component.approve(DOC.versions[1]);
    expect(snack.open.calls.mostRecent().args[0]).toContain('la validation continue');

    svc.approve.and.returnValue(of(version({ status: 'APPROVED' })));
    component.approve(DOC.versions[1]);
    expect(snack.open.calls.mostRecent().args[0]).toContain('Version approuvée');
  });

  it('charge l’arborescence et les processus pour nommer le classement', () => {
    expect(component.folders.map(r => [r.folder.id, r.depth])).toEqual([['f1', 0], ['f2', 1]]);
    expect(component.processes.length).toBe(1);
    expect(component.folderName('f2')).toBe('Réalisation');
    expect(component.folderName('inconnu')).toBeNull();
    expect(component.folderName(null)).toBeNull();
    expect(component.processName('p1')).toBe('Achats');
    expect(component.processName('p9')).toBeNull();
    expect(component.processName(undefined)).toBeNull();
  });

  it('une arborescence ou des processus indisponibles laissent des listes vides', () => {
    svc.folders.and.returnValue(throwError(() => ({ status: 500 })));
    svc.processes.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    expect(component.folders).toEqual([]);
    expect(component.processes).toEqual([]);
  });

  it('modifier passe l’arborescence et les processus au dialogue et recharge', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(DOC) } as never);
    svc.get.calls.reset();
    component.openEdit(DOC);
    expect(dialog.open).toHaveBeenCalledWith(DocumentsEditDialogComponent, jasmine.objectContaining({
      data: { document: DOC, folders: component.folders, processes: component.processes }
    }));
    expect(svc.get).toHaveBeenCalled();
  });

  describe('fichier d’une version', () => {
    const DRAFT = version({ id: 'v3', versionNumber: 3, status: 'DRAFT' });

    function input(withFile: boolean): HTMLInputElement {
      const el = document.createElement('input');
      el.type = 'file';
      if (withFile) {
        const dt = new DataTransfer();
        dt.items.add(new File(['%PDF'], 'procedure.pdf', { type: 'application/pdf' }));
        el.files = dt.files;
      }
      return el;
    }

    it('joint un fichier avec sa progression, puis recharge', () => {
      const events = new Subject<HttpEvent<DocumentVersionResponse>>();
      svc.uploadFile.and.returnValue(events);
      svc.get.calls.reset();
      const el = input(true);

      component.uploadFile(DRAFT, el);
      expect(el.value).toBe('');
      expect(svc.uploadFile).toHaveBeenCalledWith('doc-1', 'v3', jasmine.any(File));
      expect(component.uploading).toEqual({ versionId: 'v3', progress: 0 });

      events.next({ type: HttpEventType.UploadProgress, loaded: 1, total: 4 });
      expect(component.uploading?.progress).toBe(25);
      events.next({ type: HttpEventType.Sent });
      expect(component.uploading?.progress).toBe(25);

      // Un second envoi attend la fin du premier.
      component.uploadFile(DRAFT, input(true));
      expect(svc.uploadFile).toHaveBeenCalledTimes(1);

      events.next(new HttpResponse({ body: DRAFT, status: 200 }));
      expect(component.uploading).toBeNull();
      expect(snack.open.calls.mostRecent().args[0]).toBe('Fichier joint.');
      expect(svc.get).toHaveBeenCalled();
    });

    it('sans fichier choisi, rien ne part', () => {
      component.uploadFile(DRAFT, input(false));
      expect(svc.uploadFile).not.toHaveBeenCalled();
      expect(component.uploading).toBeNull();
    });

    it('un envoi refusé le dit et rend la main', () => {
      svc.uploadFile.and.returnValue(throwError(() => new Error('réseau')));
      component.uploadFile(DRAFT, input(true));
      expect(component.uploading).toBeNull();
      expect(snack.open.calls.mostRecent().args[0]).toBe('Le fichier n\'a pas pu être envoyé.');

      svc.uploadFile.and.returnValue(throwError(() => ({ status: 413 })));
      component.uploadFile(DRAFT, input(true));
      expect(snack.open.calls.mostRecent().args[0]).toBe('Requête refusée par le serveur.');
    });

    it('télécharge par un lien présigné', () => {
      const clicked: string[] = [];
      spyOn(HTMLAnchorElement.prototype, 'click').and.callFake(function (this: HTMLAnchorElement) {
        clicked.push(this.href);
      });
      const link: FileLink = {
        url: 'https://s3.test/telecharger', fileName: 'p.pdf', contentType: 'application/pdf',
        sizeBytes: 4, expiresInSeconds: 300
      };
      svc.fileLink.and.returnValue(of(link));
      component.download(DRAFT);
      expect(svc.fileLink).toHaveBeenCalledWith('doc-1', 'v3', true);
      expect(clicked).toEqual(['https://s3.test/telecharger']);
    });

    it('un téléchargement en échec le dit', () => {
      svc.fileLink.and.returnValue(throwError(() => ({ status: 404 })));
      component.download(DRAFT);
      expect(snack.open.calls.mostRecent().args[0]).toBe('Le fichier n\'a pas pu être téléchargé. Réessayez.');
    });
  });

  it('nomme types, versions et statuts', () => {
    expect(component.typeLabel('EXTERNAL')).toBe('Document externe');
    expect(component.typeLabel('PROCEDURE')).toBe('Procédure');
    expect(component.versionLabel('PUBLISHED')).toBe('Actuelle');
    expect(component.versionLabel('IN_REVIEW')).toBe('En revue');
    expect(component.versionLabel('INCONNU' as never)).toBe('INCONNU');
    expect(component.versionBadge('IN_REVIEW')).toBe('vbadge vbadge-in_review');
    expect(component.displayStatus({ ...DOC, displayStatus: 'REVIEW_OVERDUE' })).toBe('REVIEW_OVERDUE');
    expect(component.displayStatus({ ...DOC, status: 'ARCHIVED', displayStatus: undefined })).toBe('ARCHIVED');
    expect(component.displayStatus({ ...DOC, displayStatus: undefined })).toBe('DRAFT');
    expect(component.size(0)).toBe('0 o');
  });
});

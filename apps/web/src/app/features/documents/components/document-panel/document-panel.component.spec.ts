import { SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { PersonNameModule } from '../../../../shared/person-name/person-name.module';
import { SharedModule } from '../../../../shared/shared.module';
import { buildFolderRows } from '../../documents.folders';
import { DocumentsService } from '../../documents.service';
import {
  DocumentFileInfo, DocumentResponse, DocumentVersionResponse, FileLink, VersionStatus
} from '../../documents.types';
import { DocumentPanelComponent, MAX_PREVIEW_BYTES, previewKindOf, stepsOf } from './document-panel.component';

describe('previewKindOf', () => {
  it('reconnaît le PDF, les images et le texte, rien d’autre', () => {
    expect(previewKindOf('application/pdf')).toBe('pdf');
    expect(previewKindOf('image/png')).toBe('image');
    expect(previewKindOf('image/svg+xml')).toBe('image');
    expect(previewKindOf('text/plain')).toBe('text');
    expect(previewKindOf('text/csv')).toBe('text');
    expect(previewKindOf('application/vnd.openxmlformats-officedocument.wordprocessingml.document')).toBeNull();
    expect(previewKindOf('')).toBeNull();
    expect(previewKindOf(null)).toBeNull();
    expect(previewKindOf(undefined)).toBeNull();
  });
});

describe('stepsOf', () => {
  const states = (s: VersionStatus | null) => stepsOf(s).map(x => x.state);

  it('nomme les quatre temps d’une version', () => {
    expect(stepsOf(null).map(s => s.label)).toEqual(['Rédaction', 'Vérification', 'Approbation', 'Diffusion']);
  });

  it('place chaque statut de version sur le circuit', () => {
    expect(states(null)).toEqual(['todo', 'todo', 'todo', 'todo']);
    expect(states('DRAFT')).toEqual(['current', 'todo', 'todo', 'todo']);
    expect(states('IN_REVIEW')).toEqual(['done', 'current', 'todo', 'todo']);
    expect(states('APPROVED')).toEqual(['done', 'done', 'current', 'todo']);
    expect(states('PUBLISHED')).toEqual(['done', 'done', 'done', 'done']);
    expect(states('OBSOLETE')).toEqual(['done', 'done', 'done', 'done']);
  });
});

/**
 * La fiche d'un document dans la GED : aperçu par lien présigné, circuit de la
 * dernière version, métadonnées, historique, prises de connaissance.
 */
describe('DocumentPanelComponent', () => {
  let fixture: ComponentFixture<DocumentPanelComponent>;
  let component: DocumentPanelComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let createUrl: jasmine.Spy;
  let revokeUrl: jasmine.Spy;
  let urlCount: number;

  const file = (over: Partial<DocumentFileInfo> = {}): DocumentFileInfo => ({
    fileName: 'procedure.pdf', contentType: 'application/pdf', sizeBytes: 1024,
    previewable: true, purged: false, ...over
  });

  const version = (over: Partial<DocumentVersionResponse> = {}): DocumentVersionResponse => ({
    id: 'v1', documentId: 'd1', versionNumber: 1, status: 'PUBLISHED', authorId: 'u',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const doc = (over: Partial<DocumentResponse> = {}): DocumentResponse => ({
    id: 'd1', tenantId: 't', code: 'PR-001', title: 'Procédure achats', type: 'PROCEDURE', status: 'ACTIVE',
    ownerId: 'o', mandatoryRead: false, currentVersionId: 'v1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z',
    versions: [version({ file: file() })], ...over
  });

  const link = (url = 'https://s3.test/obj?sig=1'): FileLink =>
    ({ url, fileName: 'procedure.pdf', contentType: 'application/pdf', sizeBytes: 1024, expiresInSeconds: 300 });

  /** Pose le document comme le ferait le parent, sans rendre l'aperçu dans la page. */
  function show(d: DocumentResponse): void {
    const previous = component.document;
    component.document = d;
    component.ngOnChanges({ document: new SimpleChange(previous, d, previous === undefined) });
  }

  async function until(cond: () => boolean): Promise<void> {
    for (let i = 0; i < 50 && !cond(); i++) {
      await new Promise(r => setTimeout(r, 5));
    }
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['fileLink', 'fetchFile', 'countAcknowledgments']);
    svc.fileLink.and.returnValue(of(link()));
    svc.fetchFile.and.returnValue(of(new Blob(['%PDF-1.7'])));
    svc.countAcknowledgments.and.returnValue(of({ count: 7 }));
    urlCount = 0;
    createUrl = spyOn(URL, 'createObjectURL').and.callFake(() => `blob:fake-${++urlCount}`);
    revokeUrl = spyOn(URL, 'revokeObjectURL');

    await TestBed.configureTestingModule({
      declarations: [DocumentPanelComponent],
      imports: [SharedModule, PersonNameModule, NoopAnimationsModule],
      providers: [provideRouter([]), provideDirectoryStub(), { provide: DocumentsService, useValue: svc }]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentPanelComponent);
    component = fixture.componentInstance;
  });

  it('trie l’historique de la plus récente à la plus ancienne et présente la dernière', () => {
    show(doc({ versions: [
      version({ id: 'v1', versionNumber: 1, status: 'OBSOLETE' }),
      version({ id: 'v3', versionNumber: 3, status: 'IN_REVIEW' }),
      version({ id: 'v2', versionNumber: 2, status: 'PUBLISHED' })
    ] }));
    expect(component.history.map(v => v.id)).toEqual(['v3', 'v2', 'v1']);
    expect(component.version?.id).toBe('v3');
    expect(component.steps.map(s => s.state)).toEqual(['done', 'current', 'todo', 'todo']);
  });

  it('un document sans version n’a ni circuit entamé ni aperçu', () => {
    show(doc({ versions: [], currentVersionId: undefined }));
    expect(component.version).toBeNull();
    expect(component.history).toEqual([]);
    expect(component.steps.every(s => s.state === 'todo')).toBeTrue();
    expect(component.preview).toBe('none');
    expect(svc.fileLink).not.toHaveBeenCalled();
  });

  it('ignore un changement qui ne porte pas sur le document', () => {
    component.ngOnChanges({ folders: new SimpleChange([], [], true) });
    expect(component.version).toBeNull();
    expect(svc.fileLink).not.toHaveBeenCalled();
  });

  describe('aperçu', () => {
    it('sans fichier : rien à montrer', () => {
      show(doc({ versions: [version()] }));
      expect(component.preview).toBe('none');
      expect(svc.fileLink).not.toHaveBeenCalled();
    });

    it('fichier purgé : on le dit, sans le demander', () => {
      show(doc({ versions: [version({ file: file({ purged: true }) })] }));
      expect(component.preview).toBe('purged');
      expect(svc.fileLink).not.toHaveBeenCalled();
    });

    it('format sans aperçu : rien à montrer', () => {
      show(doc({ versions: [version({ file: file({ previewable: false }) })] }));
      expect(component.preview).toBe('none');
      show(doc({ id: 'd2', versions: [version({ file: file({ fileName: 'a.docx', contentType: 'application/msword' }) })] }));
      expect(component.preview).toBe('none');
      expect(component.previewKind).toBeNull();
      expect(svc.fileLink).not.toHaveBeenCalled();
    });

    it('trop volumineux : on renvoie au téléchargement', () => {
      show(doc({ versions: [version({ file: file({ sizeBytes: MAX_PREVIEW_BYTES + 1 }) })] }));
      expect(component.preview).toBe('too-large');
      expect(svc.fileLink).not.toHaveBeenCalled();
    });

    it('PDF : lu par lien d’aperçu puis montré depuis la mémoire, typé par le type déclaré', () => {
      const pending = new Subject<FileLink>();
      svc.fileLink.and.returnValue(pending);
      show(doc());
      expect(component.preview).toBe('loading');
      expect(component.previewKind).toBe('pdf');
      expect(svc.fileLink).toHaveBeenCalledWith('d1', 'v1', false);

      pending.next(link('https://s3.test/apercu'));
      expect(svc.fetchFile).toHaveBeenCalledWith('https://s3.test/apercu');
      expect(component.preview).toBe('ready');
      const blob = createUrl.calls.mostRecent().args[0] as Blob;
      expect(blob.type).toBe('application/pdf');
      expect(component.previewUrl).toBeTruthy();
      expect(component.imageUrl).toBeNull();
    });

    it('image : montrée par une URL mémoire', () => {
      show(doc({ versions: [version({ file: file({ fileName: 'plan.png', contentType: 'image/png' }) })] }));
      expect(component.preview).toBe('ready');
      expect(component.previewKind).toBe('image');
      expect(component.imageUrl).toBe('blob:fake-1');
      expect(component.previewUrl).toBeNull();
    });

    it('texte : lu et tronqué, sans URL mémoire', async () => {
      svc.fetchFile.and.returnValue(of(new Blob(['x'.repeat(20010)])));
      show(doc({ versions: [version({ file: file({ fileName: 'notes.txt', contentType: 'text/plain' }) })] }));
      expect(component.previewKind).toBe('text');
      await until(() => component.preview === 'ready');
      expect(component.preview).toBe('ready');
      expect(component.previewText?.length).toBe(20000);
      expect(createUrl).not.toHaveBeenCalled();
    });

    it('un lien refusé ou une lecture en échec : aperçu en échec, puis une reprise aboutit', () => {
      svc.fileLink.and.returnValue(throwError(() => ({ status: 403 })));
      show(doc());
      expect(component.preview).toBe('failed');

      svc.fileLink.and.returnValue(of(link()));
      svc.fetchFile.and.returnValue(throwError(() => ({ status: 0 })));
      component.retryPreview();
      expect(component.preview).toBe('failed');

      svc.fetchFile.and.returnValue(of(new Blob(['%PDF'])));
      component.retryPreview();
      expect(component.preview).toBe('ready');
    });

    it('le même fichier du même document n’est pas relu', () => {
      const d = doc();
      show(d);
      expect(svc.fileLink).toHaveBeenCalledTimes(1);
      show({ ...d, title: 'Renommé', versions: d.versions.map(v => ({ ...v })) });
      expect(svc.fileLink).toHaveBeenCalledTimes(1);
      expect(component.preview).toBe('ready');
      expect(revokeUrl).not.toHaveBeenCalled();
    });

    it('une nouvelle version sans fichier efface l’aperçu de la précédente', () => {
      const d = doc();
      show(d);
      expect(component.preview).toBe('ready');
      show({ ...d, versions: [version({ id: 'v2', versionNumber: 2, status: 'DRAFT' }), ...d.versions] });
      expect(component.version?.id).toBe('v2');
      expect(component.preview).toBe('none');
      expect(revokeUrl).toHaveBeenCalledWith('blob:fake-1');
    });

    it('un fichier retiré pendant que la fiche est ouverte passe à « retiré »', () => {
      const d = doc();
      show(d);
      show({ ...d, versions: [version({ file: file({ purged: true }) })] });
      expect(component.preview).toBe('purged');
    });

    it('un autre fichier libère l’URL mémoire précédente et relit', () => {
      const d = doc();
      show(d);
      show({ ...d, versions: [version({ file: file({ fileName: 'procedure-v2.pdf' }) })] });
      expect(svc.fileLink).toHaveBeenCalledTimes(2);
      expect(revokeUrl).toHaveBeenCalledWith('blob:fake-1');
      expect(createUrl).toHaveBeenCalledTimes(2);
    });

    it('un autre document relit son fichier', () => {
      show(doc());
      show(doc({ id: 'd2' }));
      expect(svc.fileLink).toHaveBeenCalledTimes(2);
      expect(svc.fileLink.calls.mostRecent().args[0]).toBe('d2');
    });

    it('la destruction libère l’URL mémoire et abandonne une lecture en cours', () => {
      show(doc());
      component.ngOnDestroy();
      expect(revokeUrl).toHaveBeenCalledWith('blob:fake-1');

      const pending = new Subject<FileLink>();
      svc.fileLink.and.returnValue(pending);
      revokeUrl.calls.reset();
      show(doc({ id: 'd3' }));
      component.ngOnDestroy();
      expect(pending.observed).toBeFalse();
      expect(revokeUrl).not.toHaveBeenCalled();
    });
  });

  describe('téléchargement', () => {
    let clicked: string[];

    beforeEach(() => {
      clicked = [];
      spyOn(HTMLAnchorElement.prototype, 'click').and.callFake(function (this: HTMLAnchorElement) {
        clicked.push(this.href);
      });
    });

    it('télécharge le fichier de la version présentée par un lien de téléchargement', () => {
      show(doc({ versions: [version({ file: file({ previewable: false }) })] }));
      svc.fileLink.and.returnValue(of(link('https://s3.test/telecharger')));
      component.download();
      expect(svc.fileLink).toHaveBeenCalledWith('d1', 'v1', true);
      expect(clicked).toEqual(['https://s3.test/telecharger']);
      expect(component.downloading).toBeFalse();
      expect(component.linkError).toBeNull();
    });

    it('sans fichier, ou pendant un téléchargement, ne fait rien', () => {
      show(doc({ versions: [version()] }));
      component.download();
      expect(svc.fileLink).not.toHaveBeenCalled();

      show(doc({ id: 'd2', versions: [version({ file: file({ previewable: false }) })] }));
      const pending = new Subject<FileLink>();
      svc.fileLink.and.returnValue(pending);
      component.download();
      expect(component.downloading).toBeTrue();
      component.download();
      expect(svc.fileLink).toHaveBeenCalledTimes(1);
    });

    it('un échec le dit et rend la main', () => {
      show(doc({ versions: [version({ file: file({ previewable: false }) })] }));
      svc.fileLink.and.returnValue(throwError(() => ({ status: 500 })));
      component.download();
      expect(component.downloading).toBeFalse();
      expect(component.linkError).toBe('Le fichier n\'a pas pu être téléchargé. Réessayez.');
      expect(clicked).toEqual([]);

      // Un nouveau document efface le message.
      show(doc({ id: 'd2', versions: [version()] }));
      expect(component.linkError).toBeNull();
    });
  });

  describe('prises de connaissance', () => {
    it('comptées pour une lecture obligatoire avec une version en vigueur', () => {
      show(doc({ mandatoryRead: true, currentVersionId: 'v1', versions: [version()] }));
      expect(svc.countAcknowledgments).toHaveBeenCalledWith('d1', 'v1');
      expect(component.acknowledgments).toBe(7);
    });

    it('non demandées sans lecture obligatoire ou sans version en vigueur', () => {
      show(doc({ mandatoryRead: false, versions: [version()] }));
      show(doc({ id: 'd2', mandatoryRead: true, currentVersionId: undefined, versions: [version()] }));
      expect(svc.countAcknowledgments).not.toHaveBeenCalled();
      expect(component.acknowledgments).toBeNull();
    });

    it('un échec du compteur ne montre rien', () => {
      svc.countAcknowledgments.and.returnValue(throwError(() => ({ status: 500 })));
      show(doc({ mandatoryRead: true, versions: [version()] }));
      expect(component.acknowledgments).toBeNull();
    });
  });

  it('nomme dossier, processus, statut et taille', () => {
    component.folders = buildFolderRows([{ id: 'f1', parentId: null, name: 'Achats', position: 0 }]);
    component.processes = [{ id: 'p1', name: 'Approvisionnement', position: 0 }];
    show(doc({ folderId: 'f1', processId: 'p1', displayStatus: 'REVIEW_OVERDUE', versions: [version()] }));
    expect(component.folderName).toBe('Achats');
    expect(component.processName).toBe('Approvisionnement');
    expect(component.displayStatus).toBe('REVIEW_OVERDUE');
    expect(component.size(1536)).toBe('1,5 Ko');
    expect(component.trackByVersion(0, version({ id: 'vx' }))).toBe('vx');

    show(doc({ id: 'd2', folderId: 'absent', processId: null, status: 'ARCHIVED', versions: [version()] }));
    expect(component.folderName).toBeNull();
    expect(component.processName).toBeNull();
    expect(component.displayStatus).toBe('ARCHIVED');
  });

  it('s’affiche et se ferme', () => {
    fixture.componentRef.setInput('document', doc({
      description: 'Achats groupe', versions: [version({ status: 'DRAFT', rejectionReason: 'Incomplet' })]
    }));
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('#doc-panel-title')?.textContent).toContain('Procédure achats');
    expect(host.textContent).toContain('Aucun fichier joint à cette version.');
    expect(host.textContent).toContain('Refusée : Incomplet');
    expect(host.querySelectorAll('.stepper li').length).toBe(4);

    let closed = false;
    component.closed.subscribe(() => closed = true);
    (host.querySelector('button.close') as HTMLButtonElement).click();
    expect(closed).toBeTrue();
  });
});

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, flush, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { defer, of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { PersonNameModule } from '../../../../shared/person-name/person-name.module';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { FolderRow } from '../../documents.folders';
import { VIEW_LABELS } from '../../documents.labels';
import { TenantModulesService } from '../../../admin/tenant-modules.service';
import { DocumentsService } from '../../documents.service';
import {
  DocumentFolder, DocumentListFilter, DocumentPage, DocumentResponse, DocumentVersionResponse
} from '../../documents.types';
import { DocumentsCreateDialogComponent } from '../documents-create-dialog/documents-create-dialog.component';
import { DocumentsImportDialogComponent } from '../documents-import-dialog/documents-import-dialog.component';
import { DocumentsListComponent } from './documents-list.component';

/**
 * La GED en trois volets (ADR 0094) : vues et arborescence à gauche, liste
 * filtrée et paginée au centre, fiche du document choisi à droite ; un dépôt
 * de fichiers sur la liste ouvre l'import, pour qui peut créer un document.
 */
describe('DocumentsListComponent', () => {
  let fixture: ComponentFixture<DocumentsListComponent>;
  let component: DocumentsListComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let canEdit: boolean;
  /** Les modules actifs du tenant, lus au premier rendu ; null = échec de chargement. */
  let modules: string[] | null;
  /** La fiche se tient à côté de la liste sur un écran large, par-dessus sinon. */
  let wide: boolean;

  const version = (id: string, versionNumber: number): DocumentVersionResponse => ({
    id, documentId: 'd', versionNumber, status: 'PUBLISHED', authorId: 'u', createdAt: '', updatedAt: ''
  });

  const doc = (id: string, over: Partial<DocumentResponse> = {}): DocumentResponse => ({
    id, tenantId: 't', code: 'PR-' + id, title: 'Document ' + id, type: 'PROCEDURE', status: 'ACTIVE',
    ownerId: 'o', mandatoryRead: false, createdAt: '', updatedAt: '', versions: [version('v-' + id, 1)],
    currentVersionId: 'v-' + id, ...over
  });

  const page = (content: DocumentResponse[], totalElements = content.length): DocumentPage =>
    ({ content, totalElements, totalPages: 1, number: 0, size: 20 });

  const FOLDERS: DocumentFolder[] = [
    { id: 'root', parentId: null, name: 'Procédures', position: 0 },
    { id: 'child', parentId: 'root', name: 'Réalisation', position: 0 },
    { id: 'leaf', parentId: 'child', name: 'Atelier', position: 0 },
    { id: 'other', parentId: null, name: 'Manuel', position: 1 }
  ];

  /** Le nombre de dialogues ouverts, propriété simulée par `createSpyObj`. */
  const openDialogs = (): jasmine.Spy =>
    Object.getOwnPropertyDescriptor(dialog, 'openDialogs')!.get as jasmine.Spy;
  const lastFilter = (): DocumentListFilter => svc.search.calls.mostRecent().args[0];
  const lastPage = (): number | undefined => svc.search.calls.mostRecent().args[1];
  const row = (id: string): FolderRow => component.folderRows.find(r => r.folder.id === id)!;

  function dialogReturning(result: unknown): void {
    dialog.open.and.returnValue({ afterClosed: () => of(result) } as MatDialogRef<unknown>);
  }

  async function configure(): Promise<void> {
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['search', 'views', 'folders', 'processes']);
    svc.search.and.returnValue(of(page([doc('a'), doc('b')])));
    svc.views.and.returnValue(of({ all: 2, mine: 1, toValidate: 1, reviewDue: 0, archived: 3 }));
    svc.folders.and.returnValue(of(FOLDERS));
    svc.processes.and.returnValue(of([{ id: 'p1', name: 'Achats', position: 0 }]));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open'], { openDialogs: [] });
    const authz = { can: jasmine.createSpy('can').and.callFake(() => of(canEdit)) };

    await TestBed.configureTestingModule({
      declarations: [DocumentsListComponent],
      imports: [SharedModule, UiModule, UserPickerModule, PersonNameModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideDirectoryStub(),
        { provide: DocumentsService, useValue: svc },
        { provide: AuthzService, useValue: authz },
        { provide: MatDialog, useValue: dialog },
        { provide: TenantModulesService, useValue: {
          enabledCodes: () => defer(() => modules ? of(modules) : throwError(() => new Error('down')))
        } }
      ],
      // La fiche du document a son propre banc.
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsListComponent);
    component = fixture.componentInstance;
  }

  /** Premier rendu : le filtre initial passe par l'anti-rebond. */
  function init(): void {
    fixture.detectChanges();
    tick(250);
    fixture.detectChanges();
  }

  function finish(): void {
    flush(200);
    discardPeriodicTasks();
    fixture.destroy();
  }

  beforeEach(async () => {
    canEdit = true;
    modules = ['documents', 'supplier'];
    wide = true;
    // Seule la requête de la fiche est simulée ; les autres (CDK) gardent le vrai navigateur.
    const realMatchMedia = window.matchMedia.bind(window);
    spyOn(window, 'matchMedia').and.callFake((q: string) => q === '(min-width: 1201px)'
      ? { matches: wide, media: q, onchange: null,
          addListener: () => undefined, removeListener: () => undefined,
          addEventListener: () => undefined, removeEventListener: () => undefined,
          dispatchEvent: () => false } as MediaQueryList
      : realMatchMedia(q));
    await configure();
  });

  it('cherche d’emblée dans la vue « Tous », page 0, et charge l’arborescence et les processus', fakeAsync(() => {
    init();
    expect(svc.search).toHaveBeenCalledTimes(1);
    expect(svc.search).toHaveBeenCalledWith({
      view: 'ALL', folderId: null, q: '', displayStatus: null, type: null, processId: null, ownerId: null
    }, 0, 20);
    expect(component.documents.map(d => d.id)).toEqual(['a', 'b']);
    expect(component.selected?.id).toBe('a');
    expect(component.folderRows.map(r => r.folder.id)).toEqual(['root', 'child', 'leaf', 'other']);
    expect(component.processes.length).toBe(1);
    expect(svc.views).toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr[tabindex]').length).toBe(2);
    finish();
  }));

  it('l’arborescence et les processus indisponibles laissent des listes vides', fakeAsync(() => {
    svc.folders.and.returnValue(throwError(() => ({ status: 500 })));
    svc.processes.and.returnValue(throwError(() => ({ status: 500 })));
    svc.views.and.returnValue(throwError(() => ({ status: 500 })));
    init();
    expect(component.folderRows).toEqual([]);
    expect(component.processes).toEqual([]);
    finish();
  }));

  it('choisir une vue revient en page 0 et quitte le dossier', fakeAsync(() => {
    init();
    component.folderId = 'root';
    component.goToPage(2);
    expect(lastPage()).toBe(2);

    component.selectView('MINE');
    expect(component.view).toBe('MINE');
    expect(component.folderId).toBeNull();
    expect(component.pageIndex).toBe(0);
    expect(lastFilter().view).toBe('MINE');
    expect(lastFilter().folderId).toBeNull();
    expect(lastPage()).toBe(0);
    finish();
  }));

  it('choisir un dossier lit les documents actifs de ce dossier ; le rechoisir le quitte', fakeAsync(() => {
    init();
    component.selectView('ARCHIVED');
    component.goToPage(1);

    component.selectFolder(row('child'));
    expect(component.view).toBe('ALL');
    expect(component.folderId).toBe('child');
    expect(component.pageIndex).toBe(0);
    expect(lastFilter()).toEqual(jasmine.objectContaining({ view: 'ALL', folderId: 'child' }));
    expect(lastPage()).toBe(0);

    component.selectFolder(row('child'));
    expect(component.folderId).toBeNull();
    expect(lastFilter().folderId).toBeNull();
    finish();
  }));

  it('les filtres passent par un anti-rebond et ramènent à la première page', fakeAsync(() => {
    init();
    component.goToPage(3);
    const before = svc.search.calls.count();

    component.filters.patchValue({ q: 'achats', type: 'POLICY', displayStatus: 'IN_FORCE', processId: 'p1' });
    tick(100);
    expect(svc.search.calls.count()).toBe(before);

    tick(150);
    expect(component.pageIndex).toBe(0);
    expect(lastPage()).toBe(0);
    expect(lastFilter()).toEqual(jasmine.objectContaining({
      q: 'achats', type: 'POLICY', displayStatus: 'IN_FORCE', processId: 'p1'
    }));
    finish();
  }));

  it('une valeur de filtre inchangée ne relance pas la recherche', fakeAsync(() => {
    init();
    const before = svc.search.calls.count();
    component.filters.patchValue({ q: '' });
    tick(250);
    expect(svc.search.calls.count()).toBe(before);
    finish();
  }));

  it('calcule la plage, le nombre de pages et borne la page demandée', fakeAsync(() => {
    svc.search.and.returnValue(of(page([doc('a')], 45)));
    init();
    expect(component.totalElements).toBe(45);
    expect(component.pageCount).toBe(3);
    expect(component.rangeStart).toBe(1);
    expect(component.rangeEnd).toBe(20);

    component.goToPage(1);
    expect(component.rangeStart).toBe(21);
    expect(component.rangeEnd).toBe(40);

    component.goToPage(2);
    expect(component.rangeStart).toBe(41);
    expect(component.rangeEnd).toBe(45);

    component.goToPage(-4);
    expect(component.pageIndex).toBe(0);
    expect(lastPage()).toBe(0);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.pager')).not.toBeNull();
    finish();
  }));

  it('une liste vide compte une page et une plage nulle', fakeAsync(() => {
    svc.search.and.returnValue(of(page([])));
    init();
    expect(component.pageCount).toBe(1);
    expect(component.rangeStart).toBe(0);
    expect(component.rangeEnd).toBe(0);
    expect(component.selected).toBeNull();
    finish();
  }));

  it('la fiche garde le document choisi s’il figure encore dans la liste, sinon prend le premier', fakeAsync(() => {
    init();
    component.select(component.documents[1]);
    expect(component.selected?.id).toBe('b');

    const fresh = doc('b', { title: 'Relu' });
    svc.search.and.returnValue(of(page([doc('a'), fresh])));
    component.onPanelChanged();
    expect(component.selected).toBe(fresh);

    svc.search.and.returnValue(of(page([doc('c')])));
    component.onPanelChanged();
    expect(component.selected?.id).toBe('c');
    finish();
  }));

  it('un échec de chargement vide la liste et dit une erreur sûre', fakeAsync(() => {
    const errors: (string | null)[] = [];
    component.error$.subscribe(e => errors.push(e));
    init();
    expect(component.documents.length).toBe(2);

    svc.search.and.returnValue(throwError(() => ({ status: 500, error: { detail: 'org.hibernate.Exception' } })));
    component.onPanelChanged();
    tick();
    expect(component.documents).toEqual([]);
    expect(component.totalElements).toBe(0);
    expect(component.selected).toBeNull();
    expect(errors[errors.length - 1]).toBe('Erreur serveur — réessayez dans un instant.');
    expect(errors.join()).not.toContain('hibernate');
    finish();
  }));

  it('un dossier n’est visible que si aucun ancêtre n’est replié', fakeAsync(() => {
    init();
    expect(component.isVisible(row('leaf'))).toBeTrue();

    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);
    component.toggleFolder(row('root'), event);
    expect(event.stopPropagation).toHaveBeenCalled();
    expect(component.collapsed.has('root')).toBeTrue();
    expect(component.isVisible(row('root'))).toBeTrue();
    expect(component.isVisible(row('child'))).toBeFalse();
    expect(component.isVisible(row('leaf'))).toBeFalse();
    expect(component.isVisible(row('other'))).toBeTrue();

    component.toggleFolder(row('root'), event);
    expect(component.collapsed.has('root')).toBeFalse();
    component.toggleFolder(row('child'), event);
    expect(component.isVisible(row('child'))).toBeTrue();
    expect(component.isVisible(row('leaf'))).toBeFalse();
    finish();
  }));

  it('dit si un filtre est actif et les efface tous, dossier compris', fakeAsync(() => {
    init();
    expect(component.hasFilters).toBeFalse();
    component.filters.patchValue({ ownerId: 'u1' });
    expect(component.hasFilters).toBeTrue();
    component.filters.patchValue({ ownerId: null });
    component.selectFolder(row('root'));
    expect(component.hasFilters).toBeTrue();

    component.filters.patchValue({ q: 'x', type: 'FORM' });
    const before = svc.search.calls.count();
    component.clearFilters();
    expect(component.folderId).toBeNull();
    expect(svc.search.calls.count()).toBe(before + 1);
    expect(component.filters.getRawValue()).toEqual({
      q: '', displayStatus: null, type: null, processId: null, ownerId: null
    });
    expect(component.hasFilters).toBeFalse();

    // Sans dossier choisi, effacer ne relance que par l'anti-rebond.
    component.filters.patchValue({ q: 'y' });
    tick(250);
    const after = svc.search.calls.count();
    component.clearFilters();
    expect(svc.search.calls.count()).toBe(after);
    tick(250);
    expect(lastFilter().q).toBe('');
    finish();
  }));

  it('titre la liste selon la vue ou le dossier', fakeAsync(() => {
    init();
    expect(component.heading).toBe('Documentation qualité');
    component.selectView('TO_VALIDATE');
    expect(component.heading).toBe(VIEW_LABELS.TO_VALIDATE);
    component.selectFolder(row('child'));
    expect(component.heading).toBe('Réalisation');
    component.folderId = 'inconnu';
    expect(component.heading).toBe('');
    finish();
  }));

  it('donne le numéro de version qui fait foi, à défaut le plus haut', fakeAsync(() => {
    init();
    const versions = [version('v1', 1), version('v3', 3), version('v2', 2)];
    expect(component.versionNumber(doc('x', { versions, currentVersionId: 'v2' }))).toBe(2);
    expect(component.versionNumber(doc('x', { versions, currentVersionId: 'absent' }))).toBe(3);
    expect(component.versionNumber(doc('x', { versions, currentVersionId: undefined }))).toBe(3);
    expect(component.versionNumber(doc('x', { versions: [], currentVersionId: undefined }))).toBeNull();
    finish();
  }));

  it('nomme un processus ou un dossier, ou rien', fakeAsync(() => {
    init();
    expect(component.processName('p1')).toBe('Achats');
    expect(component.processName('p9')).toBeNull();
    expect(component.processName(null)).toBeNull();
    expect(component.processName(undefined)).toBeNull();
    expect(component.folderName('leaf')).toBe('Atelier');
    expect(component.folderName('f9')).toBeNull();
    expect(component.folderName(null)).toBeNull();
    expect(component.trackById(0, { id: 'z' })).toBe('z');
    expect(component.trackByFolder(0, row('root'))).toBe('root');
    expect(component.displayStatus(doc('x', { status: 'ARCHIVED' }))).toBe('ARCHIVED');
    finish();
  }));

  it('la création ouvre le dialogue sur le dossier courant et recharge en montrant le nouveau document', fakeAsync(() => {
    init();
    component.selectFolder(row('root'));
    const created = doc('new');
    dialogReturning(created);
    svc.search.and.returnValue(of(page([doc('a'), created])));
    const searches = svc.search.calls.count();
    const views = svc.views.calls.count();

    component.openCreate();

    expect(dialog.open).toHaveBeenCalledWith(DocumentsCreateDialogComponent, jasmine.objectContaining({
      data: { folderId: 'root', folders: component.folderRows, processes: component.processes }
    }));
    expect(svc.search.calls.count()).toBe(searches + 1);
    expect(svc.views.calls.count()).toBe(views + 1);
    expect(component.selected).toBe(created);
    finish();
  }));

  it('une création annulée ne recharge rien', fakeAsync(() => {
    init();
    dialogReturning(undefined);
    const searches = svc.search.calls.count();
    component.openCreate();
    expect(svc.search.calls.count()).toBe(searches);
    finish();
  }));

  it('l’import reçoit les fichiers et montre le premier document importé', fakeAsync(() => {
    init();
    const file = new File(['x'], 'a.pdf');
    const first = doc('imp1');
    dialogReturning([first, doc('imp2')]);
    svc.search.and.returnValue(of(page([doc('a'), first])));
    const searches = svc.search.calls.count();

    component.openImport([file]);

    expect(dialog.open).toHaveBeenCalledWith(DocumentsImportDialogComponent, jasmine.objectContaining({
      data: jasmine.objectContaining({ files: [file], folderId: null })
    }));
    expect(svc.search.calls.count()).toBe(searches + 1);
    expect(component.selected).toBe(first);
    finish();
  }));

  it('un import sans document importé ne recharge rien', fakeAsync(() => {
    init();
    const searches = svc.search.calls.count();
    dialogReturning([]);
    component.openImport();
    expect((dialog.open.calls.mostRecent().args[1]?.data as { files: File[] }).files).toEqual([]);
    dialogReturning(undefined);
    component.openImport();
    expect(svc.search.calls.count()).toBe(searches);
    finish();
  }));

  describe('glisser-déposer', () => {
    function dragEvent(type: string, withFile: boolean): DragEvent {
      const dt = new DataTransfer();
      if (withFile) {
        dt.items.add(new File(['%PDF'], 'proc.pdf', { type: 'application/pdf' }));
      } else {
        dt.setData('text/plain', 'du texte');
      }
      return new DragEvent(type, { dataTransfer: dt, bubbles: true, cancelable: true });
    }

    it('un survol de fichiers montre la zone de dépôt, jusqu’à la dernière sortie', fakeAsync(() => {
      init();
      const enter = dragEvent('dragenter', true);
      (fixture.nativeElement as HTMLElement).dispatchEvent(enter);
      expect(enter.defaultPrevented).toBeTrue();
      expect(component.dragging).toBeTrue();
      fixture.detectChanges();
      expect((fixture.nativeElement as HTMLElement).querySelector('.drop-overlay')).not.toBeNull();

      component.onDragEnter(dragEvent('dragenter', true));
      component.onDragLeave(dragEvent('dragleave', true));
      expect(component.dragging).toBeTrue();
      component.onDragLeave(dragEvent('dragleave', true));
      expect(component.dragging).toBeFalse();
      component.onDragLeave(dragEvent('dragleave', true));
      expect(component.dragging).toBeFalse();
      finish();
    }));

    it('le survol accepte une copie', fakeAsync(() => {
      init();
      // Hors d'un vrai glisser, le navigateur ignore l'écriture de dropEffect : on la lit sur un double.
      const over = {
        dataTransfer: { types: ['Files'], dropEffect: 'none' },
        preventDefault: jasmine.createSpy('preventDefault')
      };
      component.onDragOver(over as unknown as DragEvent);
      expect(over.preventDefault).toHaveBeenCalled();
      expect(over.dataTransfer.dropEffect).toBe('copy');
      finish();
    }));

    it('un dépôt de fichiers ouvre l’import avec ces fichiers', fakeAsync(() => {
      init();
      const open = spyOn(component, 'openImport');
      component.onDragEnter(dragEvent('dragenter', true));
      const drop = dragEvent('drop', true);
      component.onDrop(drop);
      expect(drop.defaultPrevented).toBeTrue();
      expect(component.dragging).toBeFalse();
      expect(open).toHaveBeenCalledTimes(1);
      const files = open.calls.mostRecent().args[0]!;
      expect(files.map(f => f.name)).toEqual(['proc.pdf']);
      finish();
    }));

    it('un glisser sans fichier est ignoré', fakeAsync(() => {
      init();
      const open = spyOn(component, 'openImport');
      const enter = dragEvent('dragenter', false);
      component.onDragEnter(enter);
      const over = dragEvent('dragover', false);
      component.onDragOver(over);
      component.onDrop(dragEvent('drop', false));
      component.onDragLeave(dragEvent('dragleave', false));
      expect(enter.defaultPrevented).toBeFalse();
      expect(over.defaultPrevented).toBeFalse();
      expect(component.dragging).toBeFalse();
      expect(open).not.toHaveBeenCalled();
      finish();
    }));

    it('un événement sans DataTransfer est ignoré', fakeAsync(() => {
      init();
      const bare = new DragEvent('dragenter', { cancelable: true });
      component.onDragEnter(bare);
      expect(component.dragging).toBeFalse();
      finish();
    }));
  });

  describe('documents fournisseurs', () => {
    const link = () => fixture.nativeElement.querySelector('[data-test="documents-fournisseurs"]');

    it('ouvre la vue des pièces fournisseurs quand le module est actif', fakeAsync(() => {
      init();
      expect(link()).not.toBeNull();
      expect(link().getAttribute('href')).toContain('fournisseurs');
      finish();
    }));

    it('ne la propose pas quand le module fournisseurs est éteint', fakeAsync(() => {
      modules = ['documents'];
      init();
      expect(link()).toBeNull();
      finish();
    }));

    it('la propose quand les modules ne se chargent pas : le serveur reste l’autorité', fakeAsync(() => {
      modules = null;
      init();
      expect(link()).not.toBeNull();
      finish();
    }));
  });

  describe('sans le droit de modifier', () => {
    beforeEach(() => { canEdit = false; });

    it('ignore tout glisser-déposer de fichiers', fakeAsync(() => {
      init();
      const open = spyOn(component, 'openImport');
      const dt = new DataTransfer();
      dt.items.add(new File(['x'], 'a.pdf'));
      const enter = new DragEvent('dragenter', { dataTransfer: dt, cancelable: true });
      component.onDragEnter(enter);
      component.onDragOver(new DragEvent('dragover', { dataTransfer: dt, cancelable: true }));
      component.onDragLeave(new DragEvent('dragleave', { dataTransfer: dt, cancelable: true }));
      component.onDrop(new DragEvent('drop', { dataTransfer: dt, cancelable: true }));
      expect(enter.defaultPrevented).toBeFalse();
      expect(component.dragging).toBeFalse();
      expect(open).not.toHaveBeenCalled();
      finish();
    }));
  });

  describe('écran étroit : la fiche recouvre la liste', () => {
    beforeEach(() => { wide = false; });

    it('aucun document n’est ouvert d’office', fakeAsync(() => {
      init();
      expect(component.documents.length).toBe(2);
      expect(component.selected).toBeNull();
      finish();
    }));

    it('le document ouvert reste ouvert s’il figure encore dans la liste', fakeAsync(() => {
      init();
      component.select(component.documents[1]);
      component.onPanelChanged();
      expect(component.selected?.id).toBe('b');
      svc.search.and.returnValue(of(page([doc('c')])));
      component.onPanelChanged();
      expect(component.selected).toBeNull();
      finish();
    }));

    it('Échap referme la fiche, sauf si un dialogue est ouvert', fakeAsync(() => {
      init();
      component.select(component.documents[0]);
      openDialogs().and.returnValue([{} as never]);
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      expect(component.selected?.id).toBe('a');

      openDialogs().and.returnValue([]);
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      expect(component.selected).toBeNull();
      finish();
    }));
  });

  it('sur écran large, Échap laisse la fiche ouverte', fakeAsync(() => {
    init();
    component.onEscape();
    expect(component.selected?.id).toBe('a');
    finish();
  }));

  describe('navigation au clavier', () => {
    const rows = (): HTMLElement[] =>
      Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('tbody tr[tabindex]'));

    function key(target: HTMLElement, k: string): KeyboardEvent {
      const e = new KeyboardEvent('keydown', { key: k, bubbles: true, cancelable: true });
      target.dispatchEvent(e);
      return e;
    }

    it('les flèches parcourent la liste et la fiche suit', fakeAsync(() => {
      svc.search.and.returnValue(of(page([doc('a'), doc('b'), doc('c')])));
      init();
      expect(rows().length).toBe(3);

      const down = key(rows()[0], 'ArrowDown');
      expect(down.defaultPrevented).toBeTrue();
      expect(component.selected?.id).toBe('b');

      const up = key(rows()[2], 'ArrowUp');
      expect(up.defaultPrevented).toBeTrue();
      expect(component.selected?.id).toBe('b');
      finish();
    }));

    it('ne sort pas de la liste et ignore les autres touches', fakeAsync(() => {
      svc.search.and.returnValue(of(page([doc('a'), doc('b')])));
      init();
      expect(key(rows()[0], 'ArrowUp').defaultPrevented).toBeFalse();
      expect(key(rows()[1], 'ArrowDown').defaultPrevented).toBeFalse();
      expect(key(rows()[0], 'Tab').defaultPrevented).toBeFalse();
      expect(component.selected?.id).toBe('a');
      finish();
    }));

    it('Entrée choisit la ligne', fakeAsync(() => {
      init();
      key(rows()[1], 'Enter');
      expect(component.selected?.id).toBe('b');
      finish();
    }));
  });
});

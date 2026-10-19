import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FolderRow } from '../../../documents/documents.folders';
import { DocumentsService } from '../../../documents/documents.service';
import {
  DocumentFolder, DocumentProcess, DocumentSettings, StorageUsage
} from '../../../documents/documents.types';
import { DocumentSettingsComponent, MAX_FOLDER_DEPTH } from './document-settings.component';

/**
 * « Documentation » côté administrateur (ADR 0094) : revues périodiques,
 * limites et conservation des fichiers, arborescence et processus, stockage.
 */
describe('DocumentSettingsComponent', () => {
  let fixture: ComponentFixture<DocumentSettingsComponent>;
  let component: DocumentSettingsComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const host = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const SETTINGS: DocumentSettings = {
    reviewPeriodMonths: 24, reviewWarningDays: 30, maxFileMb: 25, platformMaxFileMb: 50,
    quotaMb: null, obsoleteRetentionYears: 10
  };
  const USAGE: StorageUsage = {
    files: 12, logicalBytes: 1000, originalBytes: 800, storedBytes: 400, quotaBytes: 1600
  };
  const FOLDERS: DocumentFolder[] = [
    { id: 'a', parentId: null, name: 'Procédures', position: 0 },
    { id: 'b', parentId: null, name: 'Manuel', position: 1 },
    { id: 'c', parentId: 'a', name: 'Réalisation', position: 0 }
  ];
  const PROCESSES: DocumentProcess[] = [
    { id: 'p1', name: 'Achats', position: 0 },
    { id: 'p2', name: 'Production', position: 3 }
  ];

  const row = (id: string): FolderRow => component.folderRows.find(r => r.folder.id === id)!;
  const confirmWith = (answer: boolean | undefined) =>
    dialog.open.and.returnValue({ afterClosed: () => of(answer) } as never);

  beforeEach(async () => {
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService', [
      'settings', 'saveSettings', 'storage', 'folders', 'processes',
      'createFolder', 'updateFolder', 'deleteFolder', 'createProcess', 'updateProcess', 'deleteProcess'
    ]);
    svc.settings.and.returnValue(of(SETTINGS));
    svc.storage.and.returnValue(of(USAGE));
    svc.folders.and.returnValue(of(FOLDERS));
    svc.processes.and.returnValue(of(PROCESSES));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [DocumentSettingsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: DocumentsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('chargement', () => {
    it('charge réglages, stockage, arborescence et processus, sans rien à enregistrer', () => {
      expect(component.loading).toBeFalse();
      expect(component.failed).toBeFalse();
      expect(component.settings).toEqual(SETTINGS);
      expect(component.form).toEqual({
        reviewPeriodMonths: 24, reviewWarningDays: 30, maxFileMb: 25, quotaMb: null, obsoleteRetentionYears: 10
      });
      expect(component.usage).toEqual(USAGE);
      expect(component.folderRows.map(r => r.folder.id)).toEqual(['a', 'c', 'b']);
      expect(component.processes).toEqual(PROCESSES);
      expect(component.dirty).toBeFalse();
      expect(component.problem).toBeNull();
      fixture.detectChanges();
      expect(host().querySelector('.barre')).toBeNull();
      expect(host().textContent).toContain('Réalisation');
      expect(host().textContent).toContain('Production');
    });

    it('un échec le dit, et réessayer recharge', () => {
      svc.storage.and.returnValue(throwError(() => ({ status: 500 })));
      component.load();
      expect(component.failed).toBeTrue();
      expect(component.loading).toBeFalse();
      fixture.detectChanges();
      expect(host().querySelector('.echec')).not.toBeNull();

      svc.storage.and.returnValue(of(USAGE));
      (host().querySelector('.echec button') as HTMLButtonElement).click();
      expect(component.failed).toBeFalse();
      expect(component.usage).toEqual(USAGE);
    });
  });

  describe('réglages', () => {
    it('vérifie la périodicité des revues', () => {
      for (const v of [0, 121, 1.5, null]) {
        component.form.reviewPeriodMonths = v as number;
        expect(component.problem).toBe('La périodicité va de 1 à 120 mois.');
      }
      component.form.reviewPeriodMonths = 120;
      expect(component.problem).toBeNull();
    });

    it('vérifie le préavis', () => {
      for (const v of [-1, 366]) {
        component.form.reviewWarningDays = v;
        expect(component.problem).toBe('Le préavis va de 0 à 365 jours.');
      }
      component.form.reviewWarningDays = 0;
      expect(component.problem).toBeNull();
    });

    it('borne la taille des fichiers par la limite de la plateforme', () => {
      component.form.maxFileMb = 0;
      expect(component.problem).toBe('La taille maximale va de 1 à 50 Mo.');
      component.form.maxFileMb = 51;
      expect(component.problem).toBe('La taille maximale va de 1 à 50 Mo.');
      component.form.maxFileMb = 50;
      expect(component.problem).toBeNull();

      component.settings = { ...SETTINGS, platformMaxFileMb: 100 };
      component.form.maxFileMb = 80;
      expect(component.problem).toBeNull();

      component.settings = null;
      expect(component.problem).toBe('La taille maximale va de 1 à 50 Mo.');
    });

    it('vérifie le quota, vide pour aucun', () => {
      component.form.quotaMb = 0;
      expect(component.problem).toBe('Le quota est un nombre entier de Mo, ou vide pour aucun.');
      component.form.quotaMb = 10_000_001;
      expect(component.problem).toContain('quota');
      component.form.quotaMb = 500;
      expect(component.problem).toBeNull();
      component.form.quotaMb = null;
      expect(component.problem).toBeNull();
    });

    it('vérifie la conservation, vide pour tout garder', () => {
      component.form.obsoleteRetentionYears = 0;
      expect(component.problem).toBe('La conservation va de 1 à 100 ans, ou vide pour tout garder.');
      component.form.obsoleteRetentionYears = 101;
      expect(component.problem).toContain('conservation');
      component.form.obsoleteRetentionYears = null;
      expect(component.problem).toBeNull();
    });

    it('un champ facultatif vidé vaut « aucun »', () => {
      expect(component.optional(null)).toBeNull();
      expect(component.optional('')).toBeNull();
      expect(component.optional('12')).toBe(12);
      expect(component.optional(7)).toBe(7);
    });

    it('enregistre ce qui a changé, puis relit le stockage', () => {
      component.form.reviewPeriodMonths = 12;
      expect(component.dirty).toBeTrue();
      fixture.detectChanges();
      expect(host().querySelector('.barre')).not.toBeNull();

      const saved = { ...SETTINGS, reviewPeriodMonths: 12 };
      svc.saveSettings.and.returnValue(of(saved));
      svc.storage.calls.reset();
      component.save();

      expect(svc.saveSettings).toHaveBeenCalledOnceWith({
        reviewPeriodMonths: 12, reviewWarningDays: 30, maxFileMb: 25, quotaMb: null, obsoleteRetentionYears: 10
      });
      expect(component.settings).toEqual(saved);
      expect(component.dirty).toBeFalse();
      expect(component.saving).toBeFalse();
      expect(snack.open.calls.mostRecent().args[0]).toBe('Paramètres enregistrés.');
      expect(svc.storage).toHaveBeenCalledTimes(1);
    });

    it('un stockage illisible après l’enregistrement ne dit rien', () => {
      component.form.reviewWarningDays = 10;
      svc.saveSettings.and.returnValue(of({ ...SETTINGS, reviewWarningDays: 10 }));
      svc.storage.and.returnValue(throwError(() => ({ status: 500 })));
      component.save();
      expect(snack.open).toHaveBeenCalledTimes(1);
      expect(component.usage).toEqual(USAGE);
    });

    it('n’enregistre ni sans changement, ni avec un problème, ni deux fois', () => {
      component.save();
      component.form.reviewPeriodMonths = 0;
      component.save();
      component.form.reviewPeriodMonths = 12;
      component.saving = true;
      component.save();
      expect(svc.saveSettings).not.toHaveBeenCalled();
    });

    it('un refus du serveur le dit et garde la saisie', () => {
      component.form.maxFileMb = 10;
      svc.saveSettings.and.returnValue(throwError(() => ({ status: 409 })));
      component.save();
      expect(snack.open.calls.mostRecent().args[0]).toBe('État incompatible — rechargez la page.');
      expect(component.saving).toBeFalse();
      expect(component.dirty).toBeTrue();
      expect(component.form.maxFileMb).toBe(10);
    });

    it('annuler revient à l’enregistré', () => {
      component.form.quotaMb = 2000;
      component.reset();
      expect(component.form.quotaMb).toBeNull();
      expect(component.dirty).toBeFalse();

      component.settings = null;
      component.form.quotaMb = 3000;
      component.reset();
      expect(component.form.quotaMb).toBe(3000);
    });
  });

  describe('stockage', () => {
    it('donne la part du quota, plafonnée, ou rien sans quota', () => {
      expect(component.quotaPercent).toBe(25);
      component.usage = { ...USAGE, storedBytes: 5000 };
      expect(component.quotaPercent).toBe(100);
      component.usage = { ...USAGE, quotaBytes: null };
      expect(component.quotaPercent).toBeNull();
      component.usage = null;
      expect(component.quotaPercent).toBeNull();
    });

    it('donne l’économie due aux doublons et à la compression', () => {
      expect(component.savedPercent).toBe(60);
      component.usage = { ...USAGE, storedBytes: 1200 };
      expect(component.savedPercent).toBe(0);
      component.usage = { ...USAGE, logicalBytes: 0 };
      expect(component.savedPercent).toBe(0);
      component.usage = null;
      expect(component.savedPercent).toBe(0);
    });

    it('affiche les tailles lisiblement', () => {
      expect(component.size(2048)).toBe('2 Ko');
      expect(component.size(null)).toBe('—');
    });
  });

  describe('arborescence et processus', () => {
    it('ouvre une saisie pour un dossier racine, un sous-dossier ou un renommage', () => {
      component.startFolder(null);
      expect(component.draft).toEqual({ kind: 'folder', id: null, parentId: null, name: '' });
      expect(component.isDraft('folder', null, null)).toBeTrue();
      expect(component.isDraft('folder', null, 'a')).toBeFalse();
      expect(component.isDraft('process', null)).toBeFalse();

      component.startFolder(row('a'));
      expect(component.isDraft('folder', null, 'a')).toBeTrue();
      expect(component.isDraft('folder', null, null)).toBeFalse();

      component.renameFolder(row('c'));
      expect(component.draft).toEqual({ kind: 'folder', id: 'c', parentId: 'a', name: 'Réalisation' });
      expect(component.isDraft('folder', 'c')).toBeTrue();
      expect(component.isDraft('folder', 'a')).toBeFalse();

      component.startProcess();
      expect(component.isDraft('process', null)).toBeTrue();
      component.renameProcess(PROCESSES[1]);
      expect(component.draft).toEqual({ kind: 'process', id: 'p2', parentId: null, name: 'Production' });
      expect(component.isDraft('process', 'p2')).toBeTrue();

      component.cancelDraft();
      expect(component.draft).toBeNull();
      expect(component.isDraft('process', 'p2')).toBeFalse();
    });

    it('rend la saisie dans la page', () => {
      component.startFolder(row('a'));
      fixture.detectChanges();
      expect(host().querySelectorAll('form.edition').length).toBe(1);
    });

    it('crée un dossier racine en dernière position, puis relit les listes', () => {
      svc.createFolder.and.returnValue(of({ id: 'n', parentId: null, name: 'Nouveau', position: 2 }));
      svc.folders.calls.reset();
      svc.processes.calls.reset();
      component.startFolder(null);
      component.draft!.name = '  Nouveau  ';
      component.commitDraft();
      expect(svc.createFolder).toHaveBeenCalledOnceWith({ name: 'Nouveau', parentId: null, position: 2 });
      expect(component.draft).toBeNull();
      expect(component.busy).toBeFalse();
      expect(svc.folders).toHaveBeenCalledTimes(1);
      expect(svc.processes).toHaveBeenCalledTimes(1);
    });

    it('crée un sous-dossier après ses frères', () => {
      svc.createFolder.and.returnValue(of({ id: 'n', parentId: 'a', name: 'Support', position: 1 }));
      component.startFolder(row('a'));
      component.draft!.name = 'Support';
      component.commitDraft();
      expect(svc.createFolder).toHaveBeenCalledOnceWith({ name: 'Support', parentId: 'a', position: 1 });
    });

    it('renomme un dossier en gardant sa place', () => {
      svc.updateFolder.and.returnValue(of({ id: 'b', parentId: null, name: 'Manuel qualité', position: 1 }));
      component.renameFolder(row('b'));
      component.draft!.name = 'Manuel qualité';
      component.commitDraft();
      expect(svc.updateFolder).toHaveBeenCalledOnceWith('b', { name: 'Manuel qualité', parentId: null, position: 1 });
      expect(svc.createFolder).not.toHaveBeenCalled();
    });

    it('renomme un dossier disparu sans position', () => {
      svc.updateFolder.and.returnValue(of({ id: 'zz', parentId: null, name: 'X', position: 0 }));
      component.draft = { kind: 'folder', id: 'zz', parentId: null, name: 'X' };
      component.commitDraft();
      expect(svc.updateFolder).toHaveBeenCalledOnceWith('zz', { name: 'X', parentId: null, position: null });
    });

    it('crée un processus en dernière position et en renomme un en gardant sa place', () => {
      svc.createProcess.and.returnValue(of({ id: 'p3', name: 'Ventes', position: 2 }));
      component.startProcess();
      component.draft!.name = 'Ventes';
      component.commitDraft();
      expect(svc.createProcess).toHaveBeenCalledOnceWith({ name: 'Ventes', position: 2 });

      svc.updateProcess.and.returnValue(of({ id: 'p2', name: 'Fabrication', position: 3 }));
      component.renameProcess(PROCESSES[1]);
      component.draft!.name = 'Fabrication';
      component.commitDraft();
      expect(svc.updateProcess).toHaveBeenCalledOnceWith('p2', { name: 'Fabrication', position: 3 });

      component.draft = { kind: 'process', id: 'disparu', parentId: null, name: 'Y' };
      component.commitDraft();
      expect(svc.updateProcess.calls.mostRecent().args).toEqual(['disparu', { name: 'Y', position: null }]);
    });

    it('n’envoie rien sans saisie, avec un nom vide ou trop long, ou pendant un envoi', () => {
      component.commitDraft();
      component.startFolder(null);
      component.draft!.name = '   ';
      component.commitDraft();
      component.draft!.name = 'x'.repeat(121);
      component.commitDraft();
      component.draft!.name = 'Correct';
      component.busy = true;
      component.commitDraft();
      expect(svc.createFolder).not.toHaveBeenCalled();
      expect(component.draft).not.toBeNull();
    });

    it('un refus du serveur le dit et garde la saisie', () => {
      svc.createFolder.and.returnValue(throwError(() => new Error('x')));
      component.startFolder(null);
      component.draft!.name = 'Doublon';
      component.commitDraft();
      expect(snack.open.calls.mostRecent().args[0]).toBe('Erreur lors de la création.');
      expect(component.draft?.name).toBe('Doublon');
      expect(component.busy).toBeFalse();
    });

    it('une relecture des listes en échec le dit', () => {
      svc.createProcess.and.returnValue(of({ id: 'p3', name: 'Ventes', position: 2 }));
      svc.folders.and.returnValue(throwError(() => ({ status: 503 })));
      component.startProcess();
      component.draft!.name = 'Ventes';
      component.commitDraft();
      expect(snack.open.calls.mostRecent().args[0]).toBe('Erreur serveur — réessayez dans un instant.');
    });

    it('supprime un dossier après confirmation, puis relit', () => {
      confirmWith(true);
      svc.deleteFolder.and.returnValue(of(undefined));
      svc.folders.calls.reset();
      component.deleteFolder(row('b'));
      expect(dialog.open).toHaveBeenCalledWith(ConfirmDialogComponent, jasmine.objectContaining({
        data: jasmine.objectContaining({ title: 'Supprimer le dossier « Manuel » ?', danger: true })
      }));
      expect(svc.deleteFolder).toHaveBeenCalledOnceWith('b');
      expect(svc.folders).toHaveBeenCalledTimes(1);
      expect(component.busy).toBeFalse();
    });

    it('ne supprime rien quand la confirmation est refusée ou fermée', () => {
      confirmWith(false);
      component.deleteFolder(row('b'));
      confirmWith(undefined);
      component.deleteProcess(PROCESSES[0]);
      expect(svc.deleteFolder).not.toHaveBeenCalled();
      expect(svc.deleteProcess).not.toHaveBeenCalled();
    });

    it('supprime un processus après confirmation ; un refus du serveur le dit', () => {
      confirmWith(true);
      svc.deleteProcess.and.returnValue(of(undefined));
      component.deleteProcess(PROCESSES[0]);
      expect(dialog.open.calls.mostRecent().args[1]?.data).toEqual(jasmine.objectContaining({
        title: 'Supprimer le processus « Achats » ?'
      }));
      expect(svc.deleteProcess).toHaveBeenCalledOnceWith('p1');

      svc.deleteProcess.and.returnValue(throwError(() => ({ status: 409 })));
      component.deleteProcess(PROCESSES[1]);
      expect(snack.open.calls.mostRecent().args[0]).toBe('État incompatible — rechargez la page.');
      expect(component.busy).toBeFalse();

      svc.deleteFolder.and.returnValue(throwError(() => new Error('x')));
      component.deleteFolder(row('a'));
      expect(snack.open.calls.mostRecent().args[0]).toBe('Erreur lors de la suppression.');
    });

    it('n’autorise un sous-dossier que sous la profondeur maximale', () => {
      expect(MAX_FOLDER_DEPTH).toBe(4);
      const at = (depth: number): FolderRow => ({ ...row('a'), depth });
      expect(component.canNest(at(0))).toBeTrue();
      expect(component.canNest(at(2))).toBeTrue();
      expect(component.canNest(at(3))).toBeFalse();
    });

    it('suit les lignes par identifiant', () => {
      expect(component.trackByFolder(0, row('a'))).toBe('a');
      expect(component.trackByProcess(0, PROCESSES[1])).toBe('p2');
    });
  });
});

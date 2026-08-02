import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabGroup } from '@angular/material/tabs';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { ConnectorsService } from '../../connectors.service';
import {
  CommConnection, ConnectorPage, EhrConnection, ErpConnection
} from '../../connectors.types';
import { CommConnectionDialogComponent } from '../comm-connection-dialog/comm-connection-dialog.component';
import { EhrConnectionDialogComponent } from '../ehr-connection-dialog/ehr-connection-dialog.component';
import { ErpConnectionDialogComponent } from '../erp-connection-dialog/erp-connection-dialog.component';
import { ConnectorsHomeComponent } from './connectors-home.component';

describe('ConnectorsHomeComponent', () => {
  let component: ConnectorsHomeComponent;
  let fixture: ComponentFixture<ConnectorsHomeComponent>;
  let svc: jasmine.SpyObj<ConnectorsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const erp = (over: Partial<ErpConnection> = {}): ErpConnection => ({
    id: 'e-1', tenantId: 't-1', name: 'SAP prod', provider: 'SAP',
    baseUrl: 'https://erp.example/odata', username: 'svc', externalScope: 'Usine A',
    status: 'ACTIVE', consecutiveFailures: 0, lastSyncAt: '2026-07-01T08:00:00Z',
    lastSuccessAt: '2026-07-01T08:00:00Z', createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  const ehr = (over: Partial<EhrConnection> = {}): EhrConnection => ({
    id: 'h-1', tenantId: 't-1', name: 'CHU Sud', provider: 'FHIR_R5',
    fhirBaseUrl: 'https://fhir.example/R5', authMode: 'BEARER', username: null,
    resourceCategory: 'AdverseEvent', status: 'ACTIVE', consecutiveFailures: 0,
    lastSyncAt: null, lastSuccessAt: null, createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  const comm = (over: Partial<CommConnection> = {}): CommConnection => ({
    id: 'c-1', tenantId: 't-1', name: 'Alertes HSE', provider: 'TEAMS', channel: '#hse',
    status: 'ACTIVE', consecutiveFailures: 0, lastNotifiedAt: null, lastSuccessAt: null,
    createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  const pageOf = <T>(content: T[], total = content.length): ConnectorPage<T> => ({
    content, totalElements: total, totalPages: 1, number: 0, size: 20
  });

  /**
   * Active un onglet et rend son texte.
   *
   * Material détache le corps des onglets inactifs : sans sélection explicite, une
   * assertion DOM ne verrait jamais que le premier onglet. À n'appeler que dans un
   * `fakeAsync` (l'attachement du portail passe par la file de tâches).
   */
  function tabText(index: number): string {
    const group = fixture.debugElement.query(By.directive(MatTabGroup)).componentInstance as MatTabGroup;
    group.selectedIndex = index;
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  /** Fait répondre le prochain `dialog.open(...)` avec la valeur fermée voulue. */
  function dialogCloses(value: unknown): void {
    dialog.open.and.returnValue({
      afterClosed: () => of(value) as Observable<unknown>
    } as unknown as MatDialogRef<unknown>);
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<ConnectorsService>('ConnectorsService', [
      'listErp', 'createErp', 'updateErp', 'deleteErp', 'syncErp',
      'listEhr', 'createEhr', 'updateEhr', 'deleteEhr', 'syncEhr',
      'listComm', 'createComm', 'updateComm', 'deleteComm', 'testComm'
    ]);
    svc.listErp.and.returnValue(of(pageOf([erp()])));
    svc.listEhr.and.returnValue(of(pageOf([ehr()])));
    svc.listComm.and.returnValue(of(pageOf([comm()])));
    svc.updateErp.and.returnValue(of(erp({ status: 'DISABLED' })));
    svc.updateEhr.and.returnValue(of(ehr({ status: 'DISABLED' })));
    svc.updateComm.and.returnValue(of(comm({ status: 'DISABLED' })));
    svc.deleteErp.and.returnValue(of(undefined));
    svc.deleteEhr.and.returnValue(of(undefined));
    svc.deleteComm.and.returnValue(of(undefined));
    svc.syncErp.and.returnValue(of({
      connectionId: 'e-1', suppliersImported: 4, suppliersIgnored: 1,
      kpisImported: 12, kpisIgnored: 2, ranAt: '2026-07-02T08:00:00Z', errorMessage: null
    }));
    svc.syncEhr.and.returnValue(of({
      connectionId: 'h-1', totalFetched: 12, created: 3, skipped: 9, errors: 0,
      ranAt: '2026-07-02T08:00:00Z', errorMessage: null
    }));
    svc.testComm.and.returnValue(of({ connectionId: 'c-1', success: true, errorMessage: null }));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ConnectorsHomeComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: ConnectorsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConnectorsHomeComponent);
    component = fixture.componentInstance;
  });

  // ---- Chargement ------------------------------------------------------------

  it('charge les trois familles dès l\'ouverture : les compteurs d\'onglets doivent être justes avant d\'ouvrir', () => {
    fixture.detectChanges();
    expect(svc.listErp).toHaveBeenCalledWith(0, 20);
    expect(svc.listEhr).toHaveBeenCalledWith(0, 20);
    expect(svc.listComm).toHaveBeenCalledWith(0, 20);
  });

  it('rend les lignes des trois familles', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    expect(tabText(0)).toContain('SAP prod');
    expect(tabText(1)).toContain('CHU Sud');
    expect(tabText(2)).toContain('Alertes HSE');
  }));

  it('porte le compte de chaque famille sur son onglet, avant même de l\'ouvrir', fakeAsync(() => {
    svc.listEhr.and.returnValue(of(pageOf([ehr()], 42)));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const labels = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.tab-count')
    ).map(el => el.textContent?.trim());
    expect(labels).toEqual(['1', '42', '1']);
  }));

  it('affiche des compteurs seulement quand la page couvre tout le jeu', (done) => {
    svc.listErp.and.returnValue(of(pageOf([
      erp({ id: 'e-1', status: 'ACTIVE' }),
      erp({ id: 'e-2', status: 'DISABLED_ON_ERRORS', consecutiveFailures: 10 })
    ])));
    fixture.detectChanges();
    component.erp$.subscribe(view => {
      expect(view.complete).toBeTrue();
      expect(view.activeCount).toBe(1);
      expect(view.disabledCount).toBe(1);
      expect(view.failingCount).toBe(1);
      done();
    });
  });

  it('masque les compteurs quand la page ne couvre pas tout le jeu : ils mentiraient', (done) => {
    svc.listEhr.and.returnValue(of(pageOf([ehr()], 57)));
    fixture.detectChanges();
    component.ehr$.subscribe(view => {
      expect(view.complete).toBeFalse();
      expect(view.total).toBe(57);
      done();
    });
  });

  it('survit à une page serveur incomplète sans planter l\'écran', (done) => {
    svc.listComm.and.returnValue(of({} as ConnectorPage<CommConnection>));
    fixture.detectChanges();
    component.comm$.subscribe(view => {
      expect(view.rows).toEqual([]);
      expect(view.total).toBe(0);
      done();
    });
  });

  it('affiche un message sûr par famille quand son chargement échoue', fakeAsync(() => {
    svc.listErp.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(tabText(0)).toContain('Vous n\'avez pas les droits pour cette action.');
    // Les deux autres familles restent exploitables : une panne d'ERP n'aveugle pas l'écran.
    expect(tabText(1)).toContain('CHU Sud');
  }));

  it('propose un état vide actionnable quand une famille n\'a aucune connexion', fakeAsync(() => {
    svc.listComm.and.returnValue(of(pageOf([])));
    fixture.detectChanges();
    tick();

    expect(tabText(2)).toContain('Aucune destination.');
  }));

  // ---- Synchronisation ERP ---------------------------------------------------

  it('ne propose la synchronisation que sur une connexion active : le serveur refuse les autres', () => {
    expect(component.canSync(erp({ status: 'ACTIVE' }))).toBeTrue();
    expect(component.canSync(erp({ status: 'DISABLED' }))).toBeFalse();
    expect(component.canSync(erp({ status: 'DISABLED_ON_ERRORS' }))).toBeFalse();
  });

  it('synchronise l\'ERP, garde le compte rendu et recharge la famille', () => {
    fixture.detectChanges();
    const before = svc.listErp.calls.count();

    component.syncErp(erp());

    expect(svc.syncErp).toHaveBeenCalledWith('e-1');
    expect(component.erpOutcome?.connectionName).toBe('SAP prod');
    expect(component.erpOutcome?.report.suppliersImported).toBe(4);
    expect(svc.listErp.calls.count()).toBe(before + 1);
    expect(component.pendingId).toBeNull();
  });

  it('lit l\'échec dans le rapport, pas dans le code HTTP', () => {
    svc.syncErp.and.returnValue(of({
      connectionId: 'e-1', suppliersImported: 0, suppliersIgnored: 0,
      kpisImported: 0, kpisIgnored: 0, ranAt: '2026-07-02T08:00:00Z',
      errorMessage: 'Secret decryption failed'
    }));
    fixture.detectChanges();

    component.syncErp(erp());

    expect(component.erpOutcomeTone(component.erpOutcome!)).toBe('danger');
  });

  it('remonte un message sûr quand la synchronisation ERP est refusée', () => {
    svc.syncErp.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fixture.detectChanges();

    component.syncErp(erp());

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Erreur serveur — réessayez dans un instant.');
    expect(component.pendingId).toBeNull();
  });

  it('écarte le compte rendu ERP à la demande', () => {
    fixture.detectChanges();
    component.syncErp(erp());
    component.dismissErpOutcome();
    expect(component.erpOutcome).toBeNull();
  });

  // ---- Synchronisation FHIR --------------------------------------------------

  it('synchronise le FHIR et distingue un import partiellement en erreur', () => {
    svc.syncEhr.and.returnValue(of({
      connectionId: 'h-1', totalFetched: 12, created: 3, skipped: 7, errors: 2,
      ranAt: '2026-07-02T08:00:00Z', errorMessage: null
    }));
    fixture.detectChanges();

    component.syncEhr(ehr());

    expect(svc.syncEhr).toHaveBeenCalledWith('h-1');
    expect(component.ehrOutcomeTone(component.ehrOutcome!)).toBe('warn');
  });

  it('marque un import FHIR sain en succès', () => {
    fixture.detectChanges();
    component.syncEhr(ehr());
    expect(component.ehrOutcomeTone(component.ehrOutcome!)).toBe('success');
    component.dismissEhrOutcome();
    expect(component.ehrOutcome).toBeNull();
  });

  // ---- Test de destination ---------------------------------------------------

  it('envoie un message de test et retient le résultat', () => {
    fixture.detectChanges();
    const before = svc.listComm.calls.count();

    component.testComm(comm());

    expect(svc.testComm).toHaveBeenCalledWith('c-1');
    expect(component.commOutcomeTone(component.commOutcome!)).toBe('success');
    // Le test bouge le compteur d'échecs de la connexion : seul un rechargement dit vrai.
    expect(svc.listComm.calls.count()).toBe(before + 1);
  });

  it('signale un webhook révoqué sans transformer la réponse en erreur', () => {
    svc.testComm.and.returnValue(of({ connectionId: 'c-1', success: false, errorMessage: 'HTTP 404' }));
    fixture.detectChanges();

    component.testComm(comm());

    expect(component.commOutcomeTone(component.commOutcome!)).toBe('danger');
    component.dismissCommOutcome();
    expect(component.commOutcome).toBeNull();
  });

  // ---- Cycle de vie ----------------------------------------------------------

  it('désactive une connexion active et réactive une connexion à l\'arrêt', () => {
    fixture.detectChanges();

    component.toggleErpStatus(erp({ status: 'ACTIVE' }));
    expect(svc.updateErp).toHaveBeenCalledWith('e-1', { status: 'DISABLED' });

    svc.updateEhr.and.returnValue(of(ehr({ status: 'ACTIVE' })));
    component.toggleEhrStatus(ehr({ status: 'DISABLED_ON_ERRORS' }));
    expect(svc.updateEhr).toHaveBeenCalledWith('h-1', { status: 'ACTIVE' });

    component.toggleCommStatus(comm({ status: 'ACTIVE' }));
    expect(svc.updateComm).toHaveBeenCalledWith('c-1', { status: 'DISABLED' });
  });

  it('remonte un message sûr quand une transition échoue', () => {
    svc.updateErp.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    fixture.detectChanges();

    component.toggleErpStatus(erp());

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
  });

  it('ignore toute seconde action tant que la première est en vol', () => {
    fixture.detectChanges();
    component.pendingId = 'e-1';

    component.syncErp(erp());
    component.syncEhr(ehr());
    component.testComm(comm());
    component.toggleErpStatus(erp());
    component.removeErp(erp());
    component.editErp(erp());

    expect(svc.syncErp).not.toHaveBeenCalled();
    expect(svc.syncEhr).not.toHaveBeenCalled();
    expect(svc.testComm).not.toHaveBeenCalled();
    expect(svc.updateErp).not.toHaveBeenCalled();
    // Aucune confirmation ni formulaire ne s'ouvre non plus : le clic est inopérant.
    expect(dialog.open).not.toHaveBeenCalled();
  });

  // ---- Suppression -----------------------------------------------------------

  it('ne supprime rien sans confirmation', () => {
    dialogCloses(false);
    fixture.detectChanges();

    component.removeErp(erp());
    component.removeEhr(ehr());
    component.removeComm(comm());

    expect(dialog.open.calls.first().args[0]).toBe(ConfirmDialogComponent);
    expect(svc.deleteErp).not.toHaveBeenCalled();
    expect(svc.deleteEhr).not.toHaveBeenCalled();
    expect(svc.deleteComm).not.toHaveBeenCalled();
  });

  it('supprime après confirmation et efface le compte rendu devenu orphelin', () => {
    fixture.detectChanges();
    component.syncErp(erp());
    expect(component.erpOutcome).not.toBeNull();

    dialogCloses(true);
    component.removeErp(erp());

    expect(svc.deleteErp).toHaveBeenCalledWith('e-1');
    expect(component.erpOutcome).toBeNull();
    expect(snack.open.calls.mostRecent().args[0] as string).toContain('supprimée');
  });

  it('conserve un compte rendu qui concerne une autre connexion', () => {
    fixture.detectChanges();
    component.syncErp(erp());

    dialogCloses(true);
    component.removeErp(erp({ id: 'e-99' }));

    expect(component.erpOutcome).not.toBeNull();
  });

  it('remonte un message sûr quand la suppression échoue', () => {
    dialogCloses(true);
    svc.deleteComm.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    fixture.detectChanges();

    component.removeComm(comm());

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('Erreur lors de la suppression.');
  });

  // ---- Formulaires -----------------------------------------------------------

  it('ouvre le bon formulaire par famille et recharge après enregistrement', () => {
    dialogCloses(erp());
    fixture.detectChanges();
    const before = svc.listErp.calls.count();

    component.createErp();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(ErpConnectionDialogComponent);
    expect(svc.listErp.calls.count()).toBe(before + 1);
    expect(snack.open.calls.mostRecent().args[0] as string).toContain('enregistrée');
  });

  it('ouvre le formulaire FHIR en édition avec la connexion visée', () => {
    dialogCloses(ehr());
    fixture.detectChanges();

    component.editEhr(ehr());

    const args = dialog.open.calls.mostRecent().args;
    expect(args[0]).toBe(EhrConnectionDialogComponent);
    expect((args[1] as { data: { connection: EhrConnection | null } }).data.connection?.id).toBe('h-1');
    expect(snack.open.calls.mostRecent().args[0] as string).toContain('mise à jour');
  });

  it('ouvre le formulaire de destination et revient à la première page après création', () => {
    dialogCloses(comm());
    fixture.detectChanges();
    component.onCommPage({ pageIndex: 3, pageSize: 50, length: 200 } as PageEvent);

    component.createComm();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(CommConnectionDialogComponent);
    // Une connexion neuve n'est pas forcément sur la page courante : on revient au début.
    expect(svc.listComm.calls.mostRecent().args).toEqual([0, 50]);
  });

  it('ne recharge rien quand le formulaire est annulé', () => {
    dialogCloses(undefined);
    fixture.detectChanges();
    const before = svc.listEhr.calls.count();

    component.createEhr();

    expect(svc.listEhr.calls.count()).toBe(before);
    expect(snack.open).not.toHaveBeenCalled();
  });

  // ---- Pagination ------------------------------------------------------------

  it('propage la pagination de chaque famille indépendamment', () => {
    fixture.detectChanges();

    component.onErpPage({ pageIndex: 1, pageSize: 10, length: 40 } as PageEvent);
    component.onEhrPage({ pageIndex: 2, pageSize: 50, length: 300 } as PageEvent);
    component.onCommPage({ pageIndex: 0, pageSize: 100, length: 120 } as PageEvent);

    expect(svc.listErp.calls.mostRecent().args).toEqual([1, 10]);
    expect(svc.listEhr.calls.mostRecent().args).toEqual([2, 50]);
    expect(svc.listComm.calls.mostRecent().args).toEqual([0, 100]);
    expect(component.erpPageIndex).toBe(1);
    expect(component.ehrPageSize).toBe(50);
    expect(component.commPageIndex).toBe(0);
  });

  it('n\'offre aucune taille de page au-delà du plafond serveur', () => {
    expect(Math.max(...component.pageSizes)).toBe(100);
  });

  // ---- Présentation ----------------------------------------------------------

  it('nomme et colore les statuts de la même façon partout', () => {
    expect(component.statusLabel('ACTIVE')).toBe('Active');
    expect(component.statusLabel('DISABLED')).toBe('Désactivée');
    expect(component.statusLabel('DISABLED_ON_ERRORS')).toBe('Désactivée sur erreurs');
    expect(component.statusTone('ACTIVE')).toBe('success');
    expect(component.statusTone('DISABLED')).toBe('neutral');
    expect(component.statusTone('DISABLED_ON_ERRORS')).toBe('danger');
  });

  it('rend les noms commerciaux des fournisseurs', () => {
    expect(component.erpProvider(erp({ provider: 'ORACLE_FUSION' }))).toBe('Oracle Fusion Cloud');
    expect(component.ehrProvider(ehr({ provider: 'FHIR_R4' }))).toBe('HL7 FHIR R4');
    expect(component.commProvider(comm({ provider: 'MATTERMOST' }))).toBe('Mattermost');
  });

  it('explique le mode d\'authentification FHIR en clair', () => {
    expect(component.ehrAuth(ehr({ authMode: 'BEARER' }))).toContain('Bearer');
    expect(component.ehrAuth(ehr({ authMode: 'BASIC' }))).toContain('Basic');
  });

  it('distingue les connexions actives et celles qui accumulent des échecs', () => {
    expect(component.isActive(erp({ status: 'ACTIVE' }))).toBeTrue();
    expect(component.isActive(erp({ status: 'DISABLED' }))).toBeFalse();
    expect(component.isFailing(comm({ consecutiveFailures: 0 }))).toBeFalse();
    expect(component.isFailing(comm({ consecutiveFailures: 4 }))).toBeTrue();
  });

  it('identifie les lignes par leur identifiant pour ne pas recréer le DOM', () => {
    expect(component.trackById(0, erp())).toBe('e-1');
  });

  it('n\'affiche jamais l\'URL de webhook d\'une destination : le serveur ne la renvoie pas', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    const text = tabText(2);
    expect(text).toContain('Webhook masqué');
    expect(text).not.toContain('hooks.');
  }));

  // ---- Les trois familles se comportent-elles pareil ? -------------------------

  it('explique un refus de chargement, famille par famille, sans vider les autres', () => {
    svc.listEhr.and.returnValue(throwError(() => ({ status: 403 })));
    fixture.detectChanges();

    // Une famille en échec ne doit pas emporter les deux autres : chaque onglet
    // a son propre flux et son propre bandeau.
    expect(component.erpError$).toBeTruthy();
    let ehrError: string | null = null;
    component.ehrError$.subscribe(m => (ehrError = m));
    expect(ehrError).toBeTruthy();
  });

  it('explique un refus de chargement des destinations de notification', () => {
    svc.listComm.and.returnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();

    let commError: string | null = null;
    component.commError$.subscribe(m => (commError = m));
    expect(commError).toBeTruthy();
  });

  it('ouvre le formulaire FHIR en création et revient à la première page', () => {
    fixture.detectChanges();
    const before = svc.listEhr.calls.count();
    dialogCloses(ehr());

    component.createEhr();

    expect(svc.listEhr.calls.count()).toBe(before + 1);
  });

  it('ouvre le formulaire de destination en édition sur la connexion visée', () => {
    fixture.detectChanges();
    dialogCloses(comm());

    component.editComm(comm());

    expect(dialog.open).toHaveBeenCalled();
    const data = (dialog.open.calls.mostRecent().args[1] as { data: { connection: unknown } }).data;
    expect(data.connection).toEqual(comm());
  });

  it('n\'ouvre aucun formulaire d\'édition tant qu\'une action est en vol', () => {
    fixture.detectChanges();
    component.pendingId = 'e-1';
    dialog.open.calls.reset();

    component.editErp(erp());
    component.editEhr(ehr());
    component.editComm(comm());

    // Éditer pendant une synchronisation ou une suppression ferait travailler
    // l'utilisateur sur un état qui va changer sous ses yeux.
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('synchronise une connexion FHIR et recharge la famille', () => {
    svc.syncEhr.and.returnValue(of({ connectionId: 'h-1', imported: 3, updated: 1, failed: 0 } as never));
    fixture.detectChanges();
    const before = svc.listEhr.calls.count();

    component.syncEhr(ehr());

    expect(svc.syncEhr).toHaveBeenCalledWith('h-1');
    expect(component.ehrOutcome).not.toBeNull();
    expect(svc.listEhr.calls.count()).toBe(before + 1);
    expect(component.pendingId).toBeNull();
  });

  it('explique un échec de synchronisation FHIR sans figer l\'écran', () => {
    svc.syncEhr.and.returnValue(throwError(() => ({ status: 502 })));
    fixture.detectChanges();

    component.syncEhr(ehr());

    expect(component.ehrOutcome).toBeNull();
    // L'indicateur doit être libéré, sinon toute action ultérieure resterait
    // silencieusement ignorée.
    expect(component.pendingId).toBeNull();
  });

  it('ne lance pas deux synchronisations FHIR en parallèle', () => {
    fixture.detectChanges();
    component.pendingId = 'h-1';
    svc.syncEhr.calls.reset();

    component.syncEhr(ehr());

    expect(svc.syncEhr).not.toHaveBeenCalled();
  });

  it('bascule le statut d\'une connexion FHIR et d\'une destination', () => {
    svc.updateEhr.and.returnValue(of(ehr({ status: 'DISABLED' })));
    svc.updateComm.and.returnValue(of(comm({ status: 'DISABLED' })));
    fixture.detectChanges();

    component.toggleEhrStatus(ehr({ status: 'ACTIVE' }));
    expect(svc.updateEhr).toHaveBeenCalledWith('h-1', { status: 'DISABLED' });

    component.toggleCommStatus(comm({ status: 'DISABLED' }));
    expect(svc.updateComm).toHaveBeenCalledWith('c-1', { status: 'ACTIVE' });
  });

  it('supprime une connexion FHIR après confirmation et efface son compte rendu', () => {
    svc.syncEhr.and.returnValue(of({ connectionId: 'h-1', imported: 1, updated: 0, failed: 0 } as never));
    svc.deleteEhr.and.returnValue(of(void 0));
    fixture.detectChanges();
    component.syncEhr(ehr());
    expect(component.ehrOutcome).not.toBeNull();

    dialogCloses(true);
    component.removeEhr(ehr());

    expect(svc.deleteEhr).toHaveBeenCalledWith('h-1');
    // Laisser le compte rendu d'une connexion supprimée afficherait un résultat
    // rattaché à une ressource qui n'existe plus.
    expect(component.ehrOutcome).toBeNull();
  });

  it('supprime une destination après confirmation et efface son compte rendu', () => {
    svc.testComm.and.returnValue(of({ connectionId: 'c-1', success: true }));
    svc.deleteComm.and.returnValue(of(void 0));
    fixture.detectChanges();
    component.testComm(comm());
    expect(component.commOutcome).not.toBeNull();

    dialogCloses(true);
    component.removeComm(comm());

    expect(svc.deleteComm).toHaveBeenCalledWith('c-1');
    expect(component.commOutcome).toBeNull();
  });

  it('explique un échec de suppression sur les deux autres familles', () => {
    svc.deleteEhr.and.returnValue(throwError(() => ({ status: 409 })));
    svc.deleteComm.and.returnValue(throwError(() => ({ status: 409 })));
    fixture.detectChanges();

    dialogCloses(true);
    component.removeEhr(ehr());
    expect(component.pendingId).toBeNull();

    dialogCloses(true);
    component.removeComm(comm());
    expect(component.pendingId).toBeNull();
  });

  it('n\'envoie aucun message de test tant qu\'une action est en vol', () => {
    fixture.detectChanges();
    component.pendingId = 'c-1';
    svc.testComm.calls.reset();

    component.testComm(comm());

    expect(svc.testComm).not.toHaveBeenCalled();
  });

  it('explique un échec d\'envoi du message de test', () => {
    svc.testComm.and.returnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();

    component.testComm(comm());

    expect(component.commOutcome).toBeNull();
    expect(component.pendingId).toBeNull();
  });
});

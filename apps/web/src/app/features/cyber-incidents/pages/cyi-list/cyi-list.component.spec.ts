import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiView } from '../../cyi.types';
import { CyiDetectDialogComponent } from '../cyi-detect-dialog/cyi-detect-dialog.component';
import { CyiListComponent } from './cyi-list.component';

/**
 * L'écran combine un mode (tous / trois registres de retard NIS 2) et un filtre
 * de statut. Le croisement des deux n'est pas offert par le serveur : c'est le
 * composant qui restreint alors côté client — le point le plus facile à casser.
 */
describe('CyiListComponent', () => {
  let fixture: ComponentFixture<CyiListComponent>;
  let component: CyiListComponent;
  let svc: jasmine.SpyObj<CyberIncidentsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const view = (over: Partial<CyiView> = {}): CyiView => ({
    id: 'i-1', tenantId: 't-1', reference: 'NIS2-INC-2026-001',
    title: 'Tentative ransomware', description: null,
    detectedAt: '2026-07-01T08:00:00Z', occurredAt: null,
    earlyWarningDeadlineAt: null, initialAssessmentDeadlineAt: null, finalReportDeadlineAt: null,
    incidentType: 'RANSOMWARE', severity: 'CRITICAL', status: 'ASSESSING',
    estimatedAffectedUsers: 42, affectedAssets: [], affectedServices: [],
    linkedBreachId: null, containmentMeasures: null, impactDescription: null,
    earlyWarningSentAt: null, earlyWarningReference: null,
    initialAssessmentSentAt: null, initialAssessmentReference: null,
    finalReportSentAt: null, finalReportReference: null,
    closureNotes: null, rejectionReason: null,
    reportedByUserId: 'u-1', handledByUserId: null, closedAt: null,
    updatedAt: '2026-07-01T09:00:00Z',
    earlyWarningOverdue: false, initialAssessmentOverdue: false, finalReportOverdue: false,
    significant: true,
    ...over
  });

  const rows: CyiView[] = [
    view(),
    view({ id: 'i-2', reference: 'NIS2-INC-2026-002', title: 'DDoS', incidentType: 'DDOS',
           severity: 'HIGH', status: 'MITIGATED', earlyWarningOverdue: true }),
    view({ id: 'i-3', reference: 'NIS2-INC-2026-003', title: 'Phishing', incidentType: 'PHISHING',
           severity: 'MEDIUM', status: 'DETECTED', significant: false })
  ];

  function dialogCloses(value: unknown): void {
    dialog.open.and.returnValue({
      afterClosed: () => of(value) as Observable<unknown>
    } as unknown as MatDialogRef<unknown>);
  }

  /**
   * `debounceTime(120)` en amont, `deferredView` (asyncScheduler) en aval : on
   * laisse s'écouler du temps réel, la fenêtre d'anti-rebond n'étant pas
   * simulable sans figer aussi les macrotâches de `deferredView`.
   *
   * L'attente doit couvrir l'anti-rebond ET la détection de changements qui
   * suit. À 200 ms la marge n'était que de 80 ms : suffisante sur une machine au
   * repos, dépassée dès que la suite complète tourne. Le même réglage a produit
   * un échec intermittent sur la liste NIS 2, bâtie sur le même patron.
   */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve, 400));
    fixture.detectChanges();
  }

  function tableRows(): number {
    return (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length;
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<CyberIncidentsService>('CyberIncidentsService', [
      'list', 'earlyWarningOverdue', 'initialAssessmentOverdue', 'finalReportOverdue'
    ]);
    svc.list.and.returnValue(of(rows));
    svc.earlyWarningOverdue.and.returnValue(of([rows[1]]));
    svc.initialAssessmentOverdue.and.returnValue(of([]));
    svc.finalReportOverdue.and.returnValue(of([]));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CyiListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CyberIncidentsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CyiListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  // ---- Chargement -------------------------------------------------------------

  it('charge tous les incidents sans filtre au premier affichage', async () => {
    fixture.detectChanges();
    await settle();

    expect(svc.list).toHaveBeenCalledOnceWith(undefined);
    expect(tableRows()).toBe(3);
  });

  it('affiche le type d\'incident en clair, jamais la constante serveur', async () => {
    fixture.detectChanges();
    await settle();

    expect(text()).toContain('Rançongiciel');
    expect(text()).toContain('Phishing');
    expect(text()).not.toContain('RANSOMWARE');
  });

  it('n\'affiche d\'échéances NIS 2 que pour les incidents significatifs', async () => {
    fixture.detectChanges();
    await settle();

    expect(text()).toContain('non applicable');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.flag-overdue').length).toBe(1);
  });

  // ---- Filtres ----------------------------------------------------------------

  it('interroge le registre de retard correspondant au mode choisi', async () => {
    fixture.detectChanges();
    await settle();

    component.modeCtrl.setValue('EW_OVERDUE');
    await settle();
    expect(svc.earlyWarningOverdue).toHaveBeenCalled();

    component.modeCtrl.setValue('IA_OVERDUE');
    await settle();
    expect(svc.initialAssessmentOverdue).toHaveBeenCalled();

    component.modeCtrl.setValue('FR_OVERDUE');
    await settle();
    expect(svc.finalReportOverdue).toHaveBeenCalled();

    // Le filtre de statut ne doit jamais partir sur ces registres.
    expect(svc.list).toHaveBeenCalledTimes(1);
  });

  it('délègue le filtre de statut au serveur en mode « tous »', async () => {
    fixture.detectChanges();
    await settle();

    component.statusCtrl.setValue('DETECTED');
    await settle();

    expect(svc.list.calls.mostRecent().args[0]).toBe('DETECTED');
  });

  it('restreint côté client quand un statut est croisé avec un registre de retard', async () => {
    svc.earlyWarningOverdue.and.returnValue(of(rows));
    fixture.detectChanges();
    await settle();

    component.modeCtrl.setValue('EW_OVERDUE');
    component.statusCtrl.setValue('MITIGATED');
    await settle();

    expect(tableRows()).toBe(1);
    expect(text()).toContain('NIS2-INC-2026-002');
    expect(text()).not.toContain('NIS2-INC-2026-003');
  });

  it('regroupe les changements rapprochés en un seul appel serveur', async () => {
    fixture.detectChanges();
    await settle();
    const before = svc.list.calls.count();

    component.statusCtrl.setValue('DETECTED');
    component.statusCtrl.setValue('CLOSED');
    await settle();

    expect(svc.list.calls.count()).toBe(before + 1);
    expect(svc.list.calls.mostRecent().args[0]).toBe('CLOSED');
  });

  // ---- États dégradés ---------------------------------------------------------

  it('affiche un message sûr et vide le tableau quand le chargement échoue', async () => {
    svc.list.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    fixture.detectChanges();
    await settle();

    expect(text()).toContain('Vous n\'avez pas les droits pour cette action.');
    expect(tableRows()).toBe(0);
  });

  it('efface l\'erreur précédente quand un nouveau filtre réussit', async () => {
    svc.list.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fixture.detectChanges();
    await settle();
    expect(text()).toContain('Erreur serveur — réessayez dans un instant.');

    svc.list.and.returnValue(of(rows));
    component.statusCtrl.setValue('DETECTED');
    await settle();

    expect(text()).not.toContain('Erreur serveur');
    expect(tableRows()).toBe(3);
  });

  it('annonce un registre vide plutôt qu\'un tableau sans ligne', async () => {
    svc.list.and.returnValue(of([]));
    fixture.detectChanges();
    await settle();

    expect(text()).toContain('Aucun incident pour le filtre courant.');
    expect((fixture.nativeElement as HTMLElement).querySelector('table')).toBeNull();
  });

  // ---- Navigation et signalement ----------------------------------------------

  it('ouvre la fiche de l\'incident cliqué', async () => {
    const navigate = spyOn(router, 'navigate');
    fixture.detectChanges();
    await settle();

    component.open(rows[1]);

    expect(navigate).toHaveBeenCalledWith(['/cyber-incidents', 'i-2']);
  });

  it('recharge la liste après un signalement effectif', async () => {
    dialogCloses(view({ id: 'neuf' }));
    fixture.detectChanges();
    await settle();
    const before = svc.list.calls.count();

    component.detect();
    await settle();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(CyiDetectDialogComponent);
    expect(snack.open).toHaveBeenCalled();
    expect(svc.list.calls.count()).toBe(before + 1);
  });

  it('ne recharge rien quand le signalement est annulé', async () => {
    dialogCloses(undefined);
    fixture.detectChanges();
    await settle();
    const before = svc.list.calls.count();

    component.detect();
    await settle();

    expect(snack.open).not.toHaveBeenCalled();
    expect(svc.list.calls.count()).toBe(before);
  });

  // ---- Présentation -----------------------------------------------------------

  it('dérive les classes de badge du statut et de la sévérité', () => {
    expect(component.statusBadge('MITIGATED')).toBe('badge badge-mitigated');
    expect(component.severityBadge('CRITICAL')).toBe('sev sev-critical');
    expect(component.typeOf('SUPPLY_CHAIN')).toBe('Chaîne d\'approvisionnement');
  });
});

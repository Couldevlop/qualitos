import { HttpErrorResponse } from '@angular/common/http';
import { Type } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiView } from '../../cyi.types';
import { CyiLinkBreachDialogComponent } from '../cyi-link-breach-dialog/cyi-link-breach-dialog.component';
import { CyiMitigateDialogComponent } from '../cyi-mitigate-dialog/cyi-mitigate-dialog.component';
import { CyiNotificationDialogComponent } from '../cyi-notification-dialog/cyi-notification-dialog.component';
import { CyiSeverityDialogComponent } from '../cyi-severity-dialog/cyi-severity-dialog.component';
import { CyiTextDialogComponent } from '../cyi-text-dialog/cyi-text-dialog.component';
import { CyiDetailComponent } from './cyi-detail.component';

/**
 * La fiche n'offre que les transitions légales de l'incident : proposer
 * « Endiguer » sur un incident clôturé, ou « Alerte 24h » sur un incident non
 * significatif, ferait produire à l'utilisateur un 409 incompréhensible.
 */
describe('CyiDetailComponent', () => {
  let fixture: ComponentFixture<CyiDetailComponent>;
  let component: CyiDetailComponent;
  let svc: jasmine.SpyObj<CyberIncidentsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let currentUser: AuthUser | null;

  const USER = '11111111-1111-1111-1111-111111111111';
  const params$ = new BehaviorSubject(convertToParamMap({ id: 'cyi-seed-001' }));

  const view = (over: Partial<CyiView> = {}): CyiView => ({
    id: 'cyi-seed-001', tenantId: 't-1', reference: 'NIS2-INC-2026-001',
    title: 'Tentative ransomware sur partage RH',
    description: 'Chiffrement détecté par l\'EDR.',
    detectedAt: '2026-07-01T08:00:00Z', occurredAt: '2026-07-01T07:30:00Z',
    earlyWarningDeadlineAt: '2026-07-02T08:00:00Z',
    initialAssessmentDeadlineAt: '2026-07-04T08:00:00Z',
    finalReportDeadlineAt: '2026-07-31T08:00:00Z',
    incidentType: 'RANSOMWARE', severity: 'CRITICAL', status: 'DETECTED',
    estimatedAffectedUsers: 42, affectedAssets: ['LAPTOP-RH-042'], affectedServices: ['Partage RH'],
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

  function dialogCloses(value: unknown): void {
    dialog.open.and.returnValue({
      afterClosed: () => of(value) as Observable<unknown>
    } as unknown as MatDialogRef<unknown>);
  }

  function build(incident: CyiView | null, source?: Observable<CyiView>): void {
    svc.get.and.returnValue(source ?? (incident ? of(incident) : of(view())));
    fixture = TestBed.createComponent(CyiDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    tick(50);
    fixture.detectChanges();
  }

  function settle(): void {
    tick(50);
    fixture.detectChanges();
  }

  /** Le libellé d'un bouton Material est le dernier <span> non vide (le premier porte l'icône). */
  function toolbarLabels(): string[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.toolbar button'))
      .map(button => Array.from(button.querySelectorAll('span'))
        .map(span => (span.textContent ?? '').trim())
        .filter(t => t.length > 0)
        .pop() ?? '');
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  beforeEach(async () => {
    currentUser = { userId: USER, tenantId: 't-1', displayName: 'RSSI', roles: ['quality_manager'] };
    svc = jasmine.createSpyObj<CyberIncidentsService>('CyberIncidentsService', ['get', 'startAssessment']);
    svc.startAssessment.and.returnValue(of(view({ status: 'ASSESSING' })));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    params$.next(convertToParamMap({ id: 'cyi-seed-001' }));

    await TestBed.configureTestingModule({
      declarations: [CyiDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CyberIncidentsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        provideRouter([]),
        // Après `provideRouter` : le routeur fournit lui aussi `ActivatedRoute`,
        // et c'est le dernier fournisseur déclaré qui gagne.
        { provide: ActivatedRoute, useValue: { paramMap: params$.asObservable() } }
      ]
    }).compileComponents();
  });

  // ---- Chargement -------------------------------------------------------------

  it('résout la fiche depuis le paramètre de route et l\'affiche', fakeAsync(() => {
    build(view());

    expect(svc.get).toHaveBeenCalledWith('cyi-seed-001');
    expect(text()).toContain('NIS2-INC-2026-001');
    expect(text()).toContain('Tentative ransomware sur partage RH');
    expect(text()).toContain('Rançongiciel');
    expect(text()).toContain('LAPTOP-RH-042');
  }));

  it('accepte un identifiant UUID comme paramètre de route', fakeAsync(() => {
    params$.next(convertToParamMap({ id: '22222222-2222-4222-8222-222222222222' }));
    build(view());

    expect(svc.get).toHaveBeenCalledWith('22222222-2222-4222-8222-222222222222');
  }));

  it('refuse un identifiant hors format sans interroger le serveur', fakeAsync(() => {
    params$.next(convertToParamMap({ id: '../../admin' }));
    build(null);

    expect(svc.get).not.toHaveBeenCalled();
    expect(text()).toContain('Identifiant invalide.');
  }));

  it('affiche un message sûr quand la fiche est introuvable', fakeAsync(() => {
    build(null, throwError(() => new HttpErrorResponse({ status: 404 })));

    expect(text()).toContain('Erreur lors du chargement.');
  }));

  it('n\'affiche le bloc d\'échéances NIS 2 que pour un incident significatif', fakeAsync(() => {
    build(view());
    expect(text()).toContain('Incident significatif (NIS 2)');

    build(view({ significant: false, severity: 'MEDIUM' }));
    expect(text()).not.toContain('Incident significatif (NIS 2)');
  }));

  // ---- Transitions offertes ----------------------------------------------------

  it('propose évaluation, rejet et sévérité sur un incident détecté', fakeAsync(() => {
    build(view());
    const labels = toolbarLabels();

    expect(labels).toContain('Évaluer');
    expect(labels).toContain('Rejeter');
    expect(labels).toContain('Sévérité');
    expect(labels).not.toContain('Endiguer');
    expect(labels).not.toContain('Clôturer');
  }));

  it('propose l\'endiguement une fois l\'évaluation en cours', fakeAsync(() => {
    build(view({ status: 'ASSESSING' }));
    const labels = toolbarLabels();

    expect(labels).toContain('Endiguer');
    expect(labels).not.toContain('Évaluer');
  }));

  it('propose la clôture une fois l\'incident endigué', fakeAsync(() => {
    build(view({ status: 'MITIGATED' }));
    const labels = toolbarLabels();

    expect(labels).toContain('Clôturer');
    expect(labels).not.toContain('Rejeter');
  }));

  it('ne propose plus aucune transition sur un incident terminal', fakeAsync(() => {
    build(view({ status: 'CLOSED', closedAt: '2026-07-10T00:00:00Z', closureNotes: 'RETEX diffusé.' }));

    expect(toolbarLabels()).toEqual(['Lier violation']);
    expect(text()).toContain('RETEX diffusé.');
  }));

  it('n\'offre les notifications CSIRT qu\'aux incidents significatifs non terminaux', fakeAsync(() => {
    build(view());

    expect(component.canNotify(view(), 'EARLY_WARNING')).toBeTrue();
    expect(component.canNotify(view(), 'INITIAL_ASSESSMENT')).toBeTrue();
    expect(component.canNotify(view(), 'FINAL_REPORT')).toBeTrue();

    expect(component.canNotify(view({ significant: false }), 'EARLY_WARNING')).toBeFalse();
    expect(component.canNotify(view({ status: 'CLOSED' }), 'EARLY_WARNING')).toBeFalse();
    expect(component.canNotify(view({ status: 'REJECTED' }), 'FINAL_REPORT')).toBeFalse();
  }));

  it('retire chaque notification déjà envoyée', fakeAsync(() => {
    build(view());

    expect(component.canNotify(view({ earlyWarningSentAt: '2026-07-01T10:00:00Z' }), 'EARLY_WARNING')).toBeFalse();
    expect(component.canNotify(view({ initialAssessmentSentAt: '2026-07-02T10:00:00Z' }), 'INITIAL_ASSESSMENT')).toBeFalse();
    expect(component.canNotify(view({ finalReportSentAt: '2026-07-20T10:00:00Z' }), 'FINAL_REPORT')).toBeFalse();
  }));

  // ---- Démarrage d'évaluation --------------------------------------------------

  it('démarre l\'évaluation au nom de l\'utilisateur connecté puis recharge', fakeAsync(() => {
    build(view());
    const before = svc.get.calls.count();

    component.startAssessment(view());
    settle();

    expect(svc.startAssessment).toHaveBeenCalledWith('cyi-seed-001', { handledByUserId: USER });
    expect(svc.get.calls.count()).toBe(before + 1);
    expect(snack.open).toHaveBeenCalled();
  }));

  it('refuse de démarrer une évaluation sans session valide', fakeAsync(() => {
    build(view());
    currentUser = null;

    component.startAssessment(view());

    expect(svc.startAssessment).not.toHaveBeenCalled();
    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Session expirée — veuillez vous reconnecter.');
  }));

  it('remonte un message sûr quand le serveur refuse le démarrage', fakeAsync(() => {
    build(view());
    svc.startAssessment.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.startAssessment(view());

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
  }));

  // ---- Actions déléguées à un dialogue -----------------------------------------

  it('ouvre le bon dialogue pour chaque action et recharge au retour', fakeAsync(() => {
    build(view({ status: 'ASSESSING' }));
    dialogCloses(view({ status: 'MITIGATED' }));

    const actions: Array<[() => void, Type<unknown>]> = [
      [() => component.mitigate(view()), CyiMitigateDialogComponent],
      [() => component.notify(view(), 'EARLY_WARNING'), CyiNotificationDialogComponent],
      [() => component.close(view()), CyiTextDialogComponent],
      [() => component.reject(view()), CyiTextDialogComponent],
      [() => component.changeSeverity(view()), CyiSeverityDialogComponent],
      [() => component.linkBreach(view()), CyiLinkBreachDialogComponent]
    ];

    for (const [action, expected] of actions) {
      const before = svc.get.calls.count();
      action();
      settle();
      expect(dialog.open.calls.mostRecent().args[0]).toBe(expected);
      expect(svc.get.calls.count()).toBe(before + 1);
    }
  }));

  it('transmet au dialogue le mode et l\'incident visés', fakeAsync(() => {
    build(view());

    component.notify(view(), 'FINAL_REPORT');
    expect((dialog.open.calls.mostRecent().args[1] as { data: unknown }).data)
      .toEqual({ id: 'cyi-seed-001', mode: 'FINAL_REPORT' });

    component.reject(view());
    expect((dialog.open.calls.mostRecent().args[1] as { data: unknown }).data)
      .toEqual({ id: 'cyi-seed-001', mode: 'REJECT' });

    component.changeSeverity(view({ severity: 'HIGH' }));
    expect((dialog.open.calls.mostRecent().args[1] as { data: unknown }).data)
      .toEqual({ id: 'cyi-seed-001', current: 'HIGH' });
  }));

  it('ne recharge pas la fiche quand le dialogue est annulé', fakeAsync(() => {
    build(view());
    dialogCloses(undefined);
    const before = svc.get.calls.count();

    component.mitigate(view());
    component.close(view());
    component.linkBreach(view());
    settle();

    expect(svc.get.calls.count()).toBe(before);
  }));

  // ---- Présentation -------------------------------------------------------------

  it('dérive les classes de badge du statut et de la sévérité', fakeAsync(() => {
    build(view());

    expect(component.statusBadge('REJECTED')).toBe('badge badge-rejected');
    expect(component.severityBadge('LOW')).toBe('sev sev-low');
    expect(component.typeOf('UNAUTHORIZED_ACCESS')).toBe('Accès non autorisé');
  }));

  it('résume les gardes de transition du domaine', fakeAsync(() => {
    build(view());

    expect(component.canStart('DETECTED')).toBeTrue();
    expect(component.canStart('ASSESSING')).toBeFalse();
    expect(component.canMitigate('ASSESSING')).toBeTrue();
    expect(component.canClose('MITIGATED')).toBeTrue();
    expect(component.canReject('DETECTED')).toBeTrue();
    expect(component.canReject('MITIGATED')).toBeFalse();
    expect(component.canChangeSev('ASSESSING')).toBeTrue();
    expect(component.canChangeSev('CLOSED')).toBeFalse();
    expect(component.canChangeSev('REJECTED')).toBeFalse();
  }));
});

import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Nis2MeasuresService } from '../../nis2m.service';
import { Nis2MeasureView } from '../../nis2m.types';
import { Nis2mPlanDialogComponent } from '../nis2m-plan-dialog/nis2m-plan-dialog.component';
import { Nis2mListComponent } from './nis2m-list.component';

/**
 * Trois filtres se croisent (mode, statut, catégorie) alors que le serveur n'en
 * accepte qu'un à la fois : le composant choisit la route puis restreint le
 * reste côté client. C'est là que se logent les écarts entre ce que l'écran
 * annonce filtrer et ce qu'il montre réellement.
 */
describe('Nis2mListComponent', () => {
  let fixture: ComponentFixture<Nis2mListComponent>;
  let component: Nis2mListComponent;
  let svc: jasmine.SpyObj<Nis2MeasuresService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const view = (over: Partial<Nis2MeasureView> = {}): Nis2MeasureView => ({
    id: 'm-1', tenantId: 't-1', reference: 'NIS2-MFA-001',
    category: 'MFA_AND_COMMUNICATIONS', title: 'MFA obligatoire', description: null,
    status: 'VERIFIED', ownerUserId: null, maturityLevel: 4,
    residualRiskRating: 'LOW', criticalRiskJustification: null, reviewIntervalDays: 180,
    effectiveFrom: null, effectiveTo: null,
    lastReviewedAt: null, reviewedByUserId: null, nextReviewDueAt: '2027-01-01T00:00:00Z',
    evidenceUrls: [], linkedProcessingActivityIds: [], linkedProcessorAgreementIds: [],
    notes: null, createdByUserId: 'u-1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    reviewOverdue: false, criticalResidualRisk: false,
    ...over
  });

  const rows: Nis2MeasureView[] = [
    view(),
    view({ id: 'm-2', reference: 'NIS2-BCP-002', category: 'BUSINESS_CONTINUITY',
           title: 'Plan de continuité', status: 'IN_PROGRESS', residualRiskRating: 'MEDIUM',
           nextReviewDueAt: null }),
    view({ id: 'm-3', reference: 'NIS2-SUPCHAIN-003', category: 'SUPPLY_CHAIN_SECURITY',
           title: 'Audit sous-traitants', residualRiskRating: 'CRITICAL',
           criticalResidualRisk: true, reviewOverdue: true, nextReviewDueAt: '2026-06-01T00:00:00Z' })
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
   * repos, dépassée dès que la suite complète tourne — l'ancienne erreur restait
   * alors affichée et le test échouait par intermittence. 400 ms laissent une
   * marge de plus de trois fois la fenêtre d'anti-rebond.
   */
  const SETTLE_MS = 400;

  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve, SETTLE_MS));
    fixture.detectChanges();
  }

  function tableRows(): number {
    return (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length;
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<Nis2MeasuresService>('Nis2MeasuresService', [
      'list', 'listByCategory', 'reviewOverdue'
    ]);
    svc.list.and.returnValue(of(rows));
    svc.listByCategory.and.returnValue(of([rows[1]]));
    svc.reviewOverdue.and.returnValue(of([rows[2]]));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [Nis2mListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: Nis2MeasuresService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Nis2mListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  // ---- Chargement -------------------------------------------------------------

  it('charge toutes les mesures sans filtre au premier affichage', async () => {
    fixture.detectChanges();
    await settle();

    expect(svc.list).toHaveBeenCalledOnceWith(undefined);
    expect(svc.listByCategory).not.toHaveBeenCalled();
    expect(tableRows()).toBe(3);
  });

  it('affiche la catégorie de l\'article 21 en clair, jamais la constante serveur', async () => {
    fixture.detectChanges();
    await settle();

    expect(text()).toContain('(j) MFA & communications sécurisées');
    expect(text()).toContain('(c) Continuité d\'activité');
    expect(text()).not.toContain('BUSINESS_CONTINUITY');
  });

  it('signale visuellement la seule mesure dont la revue est en retard', async () => {
    fixture.detectChanges();
    await settle();

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.flag-overdue').length).toBe(1);
  });

  // ---- Filtres ----------------------------------------------------------------

  it('délègue le filtre de statut au serveur quand aucune catégorie n\'est choisie', async () => {
    fixture.detectChanges();
    await settle();

    component.statusCtrl.setValue('VERIFIED');
    await settle();

    expect(svc.list.calls.mostRecent().args[0]).toBe('VERIFIED');
    expect(svc.listByCategory).not.toHaveBeenCalled();
  });

  it('bascule sur la route par catégorie dès qu\'une catégorie est choisie', async () => {
    fixture.detectChanges();
    await settle();

    component.categoryCtrl.setValue('BUSINESS_CONTINUITY');
    await settle();

    expect(svc.listByCategory).toHaveBeenCalledWith('BUSINESS_CONTINUITY');
    expect(tableRows()).toBe(1);
  });

  it('restreint côté client quand catégorie et statut sont croisés', async () => {
    svc.listByCategory.and.returnValue(of(rows));
    fixture.detectChanges();
    await settle();

    component.categoryCtrl.setValue('BUSINESS_CONTINUITY');
    component.statusCtrl.setValue('IN_PROGRESS');
    await settle();

    expect(tableRows()).toBe(1);
    expect(text()).toContain('NIS2-BCP-002');
    expect(text()).not.toContain('NIS2-MFA-001');
  });

  it('interroge le registre des revues en retard dans le mode dédié', async () => {
    fixture.detectChanges();
    await settle();

    component.modeCtrl.setValue('OVERDUE');
    await settle();

    expect(svc.reviewOverdue).toHaveBeenCalled();
    expect(tableRows()).toBe(1);
    expect(text()).toContain('NIS2-SUPCHAIN-003');
  });

  it('restreint aussi le registre des retards par statut et par catégorie', async () => {
    svc.reviewOverdue.and.returnValue(of(rows));
    fixture.detectChanges();
    await settle();

    component.modeCtrl.setValue('OVERDUE');
    component.statusCtrl.setValue('VERIFIED');
    component.categoryCtrl.setValue('SUPPLY_CHAIN_SECURITY');
    await settle();

    expect(svc.reviewOverdue).toHaveBeenCalled();
    expect(svc.listByCategory).not.toHaveBeenCalled();
    expect(tableRows()).toBe(1);
    expect(text()).toContain('NIS2-SUPCHAIN-003');
  });

  it('regroupe les changements rapprochés en un seul appel serveur', async () => {
    fixture.detectChanges();
    await settle();
    const before = svc.list.calls.count();

    component.statusCtrl.setValue('PLANNED');
    component.statusCtrl.setValue('DEPRECATED');
    await settle();

    expect(svc.list.calls.count()).toBe(before + 1);
    expect(svc.list.calls.mostRecent().args[0]).toBe('DEPRECATED');
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
    component.statusCtrl.setValue('VERIFIED');
    await settle();

    expect(text()).not.toContain('Erreur serveur');
    expect(tableRows()).toBe(3);
  });

  it('annonce un registre vide plutôt qu\'un tableau sans ligne', async () => {
    svc.list.and.returnValue(of([]));
    fixture.detectChanges();
    await settle();

    expect(text()).toContain('Aucune mesure pour le filtre courant.');
    expect((fixture.nativeElement as HTMLElement).querySelector('table')).toBeNull();
  });

  // ---- Navigation et planification --------------------------------------------

  it('ouvre la fiche de la mesure cliquée', async () => {
    const navigate = spyOn(router, 'navigate');
    fixture.detectChanges();
    await settle();

    component.open(rows[2]);

    expect(navigate).toHaveBeenCalledWith(['/nis2-measures', 'm-3']);
  });

  it('recharge la liste après une planification effective', async () => {
    dialogCloses(view({ id: 'neuve' }));
    fixture.detectChanges();
    await settle();
    const before = svc.list.calls.count();

    component.plan();
    await settle();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(Nis2mPlanDialogComponent);
    expect(snack.open).toHaveBeenCalled();
    expect(svc.list.calls.count()).toBe(before + 1);
  });

  it('ne recharge rien quand la planification est annulée', async () => {
    dialogCloses(undefined);
    fixture.detectChanges();
    await settle();
    const before = svc.list.calls.count();

    component.plan();
    await settle();

    expect(snack.open).not.toHaveBeenCalled();
    expect(svc.list.calls.count()).toBe(before);
  });

  // ---- Présentation -----------------------------------------------------------

  it('dérive les classes de badge du statut et du risque résiduel', () => {
    expect(component.statusBadge('IN_PROGRESS')).toBe('badge badge-in_progress');
    expect(component.riskBadge('CRITICAL')).toBe('risk risk-critical');
    expect(component.catLabel('CRYPTOGRAPHY')).toBe('(h) Cryptographie & chiffrement');
  });

  it('propose les dix catégories de l\'article 21', () => {
    expect(component.categories.length).toBe(10);
    expect(component.statuses).toEqual(['PLANNED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED', 'DEPRECATED']);
  });
});

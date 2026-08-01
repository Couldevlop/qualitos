import { HttpErrorResponse } from '@angular/common/http';
import { Type } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Nis2MeasuresService } from '../../nis2m.service';
import { Nis2MeasureView } from '../../nis2m.types';
import { Nis2mEditDialogComponent } from '../nis2m-edit-dialog/nis2m-edit-dialog.component';
import { Nis2mReviewDialogComponent } from '../nis2m-review-dialog/nis2m-review-dialog.component';
import { Nis2mDetailComponent } from './nis2m-detail.component';

/**
 * La fiche n'offre que l'étape suivante du cycle de vie de la mesure, et exige
 * une confirmation pour les deux actions irréversibles (désactivation,
 * suppression) — une mesure supprimée par erreur emporte avec elle la preuve
 * de conformité à l'article 21.
 */
describe('Nis2mDetailComponent', () => {
  let fixture: ComponentFixture<Nis2mDetailComponent>;
  let component: Nis2mDetailComponent;
  let svc: jasmine.SpyObj<Nis2MeasuresService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const params$ = new BehaviorSubject(convertToParamMap({ id: 'nis2m-seed-001' }));

  const view = (over: Partial<Nis2MeasureView> = {}): Nis2MeasureView => ({
    id: 'nis2m-seed-001', tenantId: 't-1', reference: 'NIS2-MFA-001',
    category: 'MFA_AND_COMMUNICATIONS',
    title: 'MFA obligatoire pour les administrateurs',
    description: 'FIDO2/WebAuthn imposé sur Keycloak.',
    status: 'PLANNED', ownerUserId: null, maturityLevel: 4,
    residualRiskRating: 'LOW', criticalRiskJustification: null, reviewIntervalDays: 180,
    effectiveFrom: null, effectiveTo: null,
    lastReviewedAt: null, reviewedByUserId: null, nextReviewDueAt: null,
    evidenceUrls: [], linkedProcessingActivityIds: [], linkedProcessorAgreementIds: [],
    notes: null, createdByUserId: 'u-1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    reviewOverdue: false, criticalResidualRisk: false,
    ...over
  });

  function dialogCloses(value: unknown): void {
    dialog.open.and.returnValue({
      afterClosed: () => of(value) as Observable<unknown>
    } as unknown as MatDialogRef<unknown>);
  }

  function build(measure: Nis2MeasureView | null, source?: Observable<Nis2MeasureView>): void {
    svc.get.and.returnValue(source ?? (measure ? of(measure) : of(view())));
    fixture = TestBed.createComponent(Nis2mDetailComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
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
    svc = jasmine.createSpyObj<Nis2MeasuresService>('Nis2MeasuresService', [
      'get', 'startImplementation', 'markImplemented', 'deprecate', 'delete'
    ]);
    svc.startImplementation.and.returnValue(of(view({ status: 'IN_PROGRESS' })));
    svc.markImplemented.and.returnValue(of(view({ status: 'IMPLEMENTED' })));
    svc.deprecate.and.returnValue(of(view({ status: 'DEPRECATED' })));
    svc.delete.and.returnValue(of(undefined));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    params$.next(convertToParamMap({ id: 'nis2m-seed-001' }));

    await TestBed.configureTestingModule({
      declarations: [Nis2mDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: Nis2MeasuresService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        provideRouter([]),
        // Après `provideRouter` : le routeur fournit lui aussi `ActivatedRoute`,
        // et c'est le dernier fournisseur déclaré qui gagne.
        { provide: ActivatedRoute, useValue: { paramMap: params$.asObservable() } }
      ]
    }).compileComponents();
  });

  // ---- Chargement -------------------------------------------------------------

  it('résout la mesure depuis le paramètre de route et l\'affiche', fakeAsync(() => {
    build(view());

    expect(svc.get).toHaveBeenCalledWith('nis2m-seed-001');
    expect(text()).toContain('NIS2-MFA-001');
    expect(text()).toContain('MFA obligatoire pour les administrateurs');
    expect(text()).toContain('(j) MFA & communications sécurisées');
  }));

  it('accepte un identifiant UUID comme paramètre de route', fakeAsync(() => {
    params$.next(convertToParamMap({ id: '33333333-3333-4333-8333-333333333333' }));
    build(view());

    expect(svc.get).toHaveBeenCalledWith('33333333-3333-4333-8333-333333333333');
  }));

  it('refuse un identifiant hors format sans interroger le serveur', fakeAsync(() => {
    params$.next(convertToParamMap({ id: 'nis2-measures' }));
    build(null);

    expect(svc.get).not.toHaveBeenCalled();
    expect(text()).toContain('Identifiant invalide.');
  }));

  it('affiche un message sûr quand la mesure est introuvable', fakeAsync(() => {
    build(null, throwError(() => new HttpErrorResponse({ status: 404 })));

    expect(text()).toContain('Erreur lors du chargement.');
  }));

  it('met en avant un risque résiduel critique et sa justification', fakeAsync(() => {
    build(view({
      residualRiskRating: 'CRITICAL', criticalResidualRisk: true,
      criticalRiskJustification: 'Dépendance à un hébergeur unique.'
    }));

    expect(text()).toContain('Attention direction requise');
    expect(text()).toContain('Dépendance à un hébergeur unique.');
  }));

  it('signale une revue périodique en retard', fakeAsync(() => {
    build(view({ status: 'VERIFIED', reviewOverdue: true, nextReviewDueAt: '2026-06-01T00:00:00Z' }));

    expect(text()).toContain('Revue en retard');
  }));

  // ---- Transitions offertes ----------------------------------------------------

  it('propose le démarrage sur une mesure planifiée', fakeAsync(() => {
    build(view());
    const labels = toolbarLabels();

    expect(labels).toContain('Démarrer');
    expect(labels).toContain('Éditer');
    expect(labels).not.toContain('Implémentée');
    expect(labels).not.toContain('Vérifier');
  }));

  it('propose le marquage « implémentée » sur une mesure en cours', fakeAsync(() => {
    build(view({ status: 'IN_PROGRESS' }));

    expect(toolbarLabels()).toContain('Implémentée');
    expect(toolbarLabels()).not.toContain('Démarrer');
  }));

  it('propose la vérification puis la revue périodique', fakeAsync(() => {
    build(view({ status: 'IMPLEMENTED' }));
    expect(toolbarLabels()).toContain('Vérifier');

    build(view({ status: 'VERIFIED' }));
    expect(toolbarLabels()).toContain('Revue périodique');
    expect(toolbarLabels()).not.toContain('Vérifier');
  }));

  it('ne propose plus ni édition ni désactivation sur une mesure désactivée', fakeAsync(() => {
    build(view({ status: 'DEPRECATED', effectiveTo: '2026-07-15T00:00:00Z' }));

    expect(toolbarLabels()).toEqual(['Supprimer']);
  }));

  it('résume les gardes de transition du domaine', fakeAsync(() => {
    build(view());

    expect(component.canEdit('VERIFIED')).toBeTrue();
    expect(component.canEdit('DEPRECATED')).toBeFalse();
    expect(component.canStart('PLANNED')).toBeTrue();
    expect(component.canStart('IN_PROGRESS')).toBeFalse();
    expect(component.canImplement('IN_PROGRESS')).toBeTrue();
    expect(component.canVerify('IMPLEMENTED')).toBeTrue();
    expect(component.canReview('VERIFIED')).toBeTrue();
    expect(component.canReview('IMPLEMENTED')).toBeFalse();
    expect(component.canDeprecate('DEPRECATED')).toBeFalse();
  }));

  // ---- Transitions directes -----------------------------------------------------

  it('démarre l\'implémentation puis recharge la fiche', fakeAsync(() => {
    build(view());
    const before = svc.get.calls.count();

    component.start(view());
    settle();

    expect(svc.startImplementation).toHaveBeenCalledWith('nis2m-seed-001');
    expect(svc.get.calls.count()).toBe(before + 1);
    expect(snack.open).toHaveBeenCalled();
  }));

  it('marque la mesure implémentée puis recharge la fiche', fakeAsync(() => {
    build(view({ status: 'IN_PROGRESS' }));
    const before = svc.get.calls.count();

    component.markImplemented(view({ status: 'IN_PROGRESS' }));
    settle();

    expect(svc.markImplemented).toHaveBeenCalledWith('nis2m-seed-001');
    expect(svc.get.calls.count()).toBe(before + 1);
  }));

  it('remonte un message sûr quand le serveur refuse une transition', fakeAsync(() => {
    build(view());
    svc.startImplementation.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.start(view());

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
  }));

  // ---- Actions déléguées à un dialogue -------------------------------------------

  it('ouvre le bon dialogue pour l\'édition, la vérification et la revue', fakeAsync(() => {
    build(view({ status: 'VERIFIED' }));
    dialogCloses(view({ status: 'VERIFIED' }));

    const actions: Array<[() => void, Type<unknown>]> = [
      [() => component.edit(view()), Nis2mEditDialogComponent],
      [() => component.verify(view()), Nis2mReviewDialogComponent],
      [() => component.review(view()), Nis2mReviewDialogComponent]
    ];

    for (const [action, expected] of actions) {
      const before = svc.get.calls.count();
      action();
      settle();
      expect(dialog.open.calls.mostRecent().args[0]).toBe(expected);
      expect(svc.get.calls.count()).toBe(before + 1);
    }
  }));

  it('distingue vérification et revue périodique par le mode transmis', fakeAsync(() => {
    build(view());

    component.verify(view());
    expect((dialog.open.calls.mostRecent().args[1] as { data: unknown }).data)
      .toEqual({ id: 'nis2m-seed-001', mode: 'VERIFY' });

    component.review(view());
    expect((dialog.open.calls.mostRecent().args[1] as { data: unknown }).data)
      .toEqual({ id: 'nis2m-seed-001', mode: 'REVIEW' });
  }));

  it('ne recharge pas la fiche quand le dialogue est annulé', fakeAsync(() => {
    build(view());
    dialogCloses(undefined);
    const before = svc.get.calls.count();

    component.edit(view());
    component.verify(view());
    settle();

    expect(svc.get.calls.count()).toBe(before);
  }));

  // ---- Actions irréversibles ------------------------------------------------------

  it('exige une confirmation avant de désactiver une mesure', fakeAsync(() => {
    build(view());
    dialogCloses(false);

    component.deprecate(view());
    settle();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(ConfirmDialogComponent);
    expect(svc.deprecate).not.toHaveBeenCalled();
  }));

  it('désactive la mesure après confirmation puis recharge la fiche', fakeAsync(() => {
    build(view());
    dialogCloses(true);
    const before = svc.get.calls.count();

    component.deprecate(view());
    settle();

    expect(svc.deprecate).toHaveBeenCalledWith('nis2m-seed-001');
    expect(svc.get.calls.count()).toBe(before + 1);
    expect(snack.open).toHaveBeenCalled();
  }));

  it('remonte un message sûr quand la désactivation échoue', fakeAsync(() => {
    build(view());
    dialogCloses(true);
    svc.deprecate.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.deprecate(view());
    settle();

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
  }));

  it('exige une confirmation avant de supprimer une mesure', fakeAsync(() => {
    build(view());
    dialogCloses(false);
    const navigate = spyOn(router, 'navigate');

    component.remove(view());
    settle();

    expect(svc.delete).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  }));

  it('supprime après confirmation puis retourne au registre', fakeAsync(() => {
    build(view());
    dialogCloses(true);
    const navigate = spyOn(router, 'navigate');

    component.remove(view());
    settle();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(ConfirmDialogComponent);
    expect(svc.delete).toHaveBeenCalledWith('nis2m-seed-001');
    expect(navigate).toHaveBeenCalledWith(['/nis2-measures']);
  }));

  it('reste sur la fiche et explique l\'échec quand la suppression est refusée', fakeAsync(() => {
    build(view());
    dialogCloses(true);
    svc.delete.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const navigate = spyOn(router, 'navigate');

    component.remove(view());
    settle();

    expect(navigate).not.toHaveBeenCalled();
    expect(snack.open.calls.mostRecent().args[0] as string).toBe('Erreur serveur — réessayez dans un instant.');
  }));

  // ---- Présentation ----------------------------------------------------------------

  it('dérive les classes de badge du statut et du risque résiduel', fakeAsync(() => {
    build(view());

    expect(component.statusBadge('IMPLEMENTED')).toBe('badge badge-implemented');
    expect(component.riskBadge('HIGH')).toBe('risk risk-high');
    expect(component.catLabel('RISK_ANALYSIS')).toBe('(a) Analyse de risque & politiques SI');
  }));

  it('rend les preuves cliquables dans un onglet isolé', fakeAsync(() => {
    build(view({ evidenceUrls: ['https://wiki.qualitos.local/mfa-policy.pdf'] }));

    const link = (fixture.nativeElement as HTMLElement)
      .querySelector('a[href="https://wiki.qualitos.local/mfa-policy.pdf"]');
    expect(link?.getAttribute('target')).toBe('_blank');
    expect(link?.getAttribute('rel')).toBe('noopener noreferrer');
  }));
});

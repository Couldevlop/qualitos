import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Subscription, of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { EnrollmentResponse, PathResponse, SkillResponse } from '../../training.types';
import { TrainingHomeComponent } from './training-home.component';

describe('TrainingHomeComponent', () => {
  let fixture: ComponentFixture<TrainingHomeComponent>;
  let component: TrainingHomeComponent;
  let svc: jasmine.SpyObj<TrainingService>;
  /** Valeur renvoyée par le prochain dialogue ouvert. */
  let dialogResult: unknown;
  let session: AuthUser | null;
  /** Souscriptions manuelles aux onglets non rendus — recréées à chaque test. */
  let subs: Subscription;

  const path = (over: Partial<PathResponse> = {}): PathResponse => ({
    id: 'path-1', tenantId: 't1', code: 'yellow-belt', name: 'Yellow Belt',
    description: 'Bases qualité', targetRole: 'Opérateur', durationHours: 14,
    passingScore: 70, validityMonths: 36, status: 'ACTIVE', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const skill = (over: Partial<SkillResponse> = {}): SkillResponse => ({
    id: 'skill-1', tenantId: 't1', code: 'spc', name: 'Cartes SPC',
    description: 'X-R, EWMA', category: 'DMAIC',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const enrollment = (over: Partial<EnrollmentResponse> = {}): EnrollmentResponse => ({
    id: 'enr-1', tenantId: 't1', userId: 'u1', pathId: 'path-1',
    status: 'ENROLLED', progressPct: 0, enrolledOn: '2026-05-01',
    createdAt: '2026-05-01T00:00:00Z', updatedAt: '2026-05-01T00:00:00Z', ...over
  });

  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  function build(): void {
    fixture = TestBed.createComponent(TrainingHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** `deferredView` livre ses états via `asyncScheduler` : un tour de boucle suffit. */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  function bannerText(): string {
    return ((fixture.nativeElement as HTMLElement)
      .querySelector('.banner-error')?.textContent ?? '').trim();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<TrainingService>('TrainingService',
      ['listPaths', 'listSkills', 'listEnrollments', 'startEnrollment', 'cancelEnrollment']);
    svc.listPaths.and.returnValue(of(page([path()])));
    svc.listSkills.and.returnValue(of(page([skill()])));
    svc.listEnrollments.and.returnValue(of(page([enrollment()])));
    svc.startEnrollment.and.returnValue(of(enrollment({ status: 'IN_PROGRESS' })));
    svc.cancelEnrollment.and.returnValue(of(enrollment({ status: 'CANCELLED' })));
    dialogResult = { id: 'nouveau' };
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['quality_manager'] };
    subs = new Subscription();

    await TestBed.configureTestingModule({
      declarations: [TrainingHomeComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: AuthService, useValue: { snapshot: () => session } },
        provideRouter([])
      ]
    }).compileComponents();
  });

  afterEach(() => subs.unsubscribe());

  // ---- Chargement -----------------------------------------------------------

  it('charge les parcours actifs par défaut et alimente le tableau', () => {
    build();
    expect(svc.listPaths).toHaveBeenCalledWith(0, 20, 'ACTIVE');
    expect(component.pathTotal).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Yellow Belt');
  });

  it('ne charge un onglet qu’à son ouverture (lazy) — aucun appel inutile au rendu', () => {
    build();
    expect(svc.listSkills).not.toHaveBeenCalled();
    expect(svc.listEnrollments).not.toHaveBeenCalled();

    subs.add(component.skills$.subscribe());
    expect(svc.listSkills).toHaveBeenCalledWith(0, 20, undefined);
  });

  it('restreint les inscriptions à l’utilisateur de la session', () => {
    build();
    subs.add(component.enrollments$.subscribe());
    expect(svc.listEnrollments).toHaveBeenCalledWith(0, 20, { userId: 'u1' });
    expect(component.enrollTotal).toBe(1);
  });

  it('n’interroge pas le serveur sur les inscriptions sans session', () => {
    session = null;
    build();
    let rows: EnrollmentResponse[] | undefined;
    subs.add(component.enrollments$.subscribe(r => (rows = r)));
    expect(svc.listEnrollments).not.toHaveBeenCalled();
    expect(rows).toEqual([]);
  });

  // ---- Filtres et pagination ------------------------------------------------

  it('transmet le statut sélectionné et l’omet pour « Tous »', () => {
    build();
    component.pathStatusFilter.setValue('DRAFT');
    expect(svc.listPaths).toHaveBeenCalledWith(0, 20, 'DRAFT');

    component.pathStatusFilter.setValue('');
    expect(svc.listPaths).toHaveBeenCalledWith(0, 20, undefined);
  });

  it('nettoie la catégorie saisie et ignore une saisie vide', () => {
    build();
    subs.add(component.skills$.subscribe());
    component.skillCategoryFilter.setValue('  DMAIC  ');
    expect(svc.listSkills).toHaveBeenCalledWith(0, 20, 'DMAIC');

    component.skillCategoryFilter.setValue('   ');
    expect(svc.listSkills).toHaveBeenCalledWith(0, 20, undefined);
  });

  it('borne la pagination des trois onglets et relance la requête', () => {
    build();
    subs.add(component.skills$.subscribe());
    subs.add(component.enrollments$.subscribe());

    component.onPathPage({ pageIndex: -2, pageSize: 500 } as PageEvent);
    expect(component.pathIndex).toBe(0);
    expect(component.pathSize).toBe(100);
    expect(svc.listPaths).toHaveBeenCalledWith(0, 100, 'ACTIVE');

    component.onSkillPage({ pageIndex: 3, pageSize: 0 } as PageEvent);
    expect(component.skillSize).toBe(1);
    expect(svc.listSkills).toHaveBeenCalledWith(3, 1, undefined);

    component.onEnrollPage({ pageIndex: 1, pageSize: 50 } as PageEvent);
    expect(svc.listEnrollments).toHaveBeenCalledWith(1, 50, { userId: 'u1' });
  });

  // ---- Erreurs --------------------------------------------------------------

  it('affiche un bandeau et vide le tableau quand le chargement des parcours échoue', async () => {
    svc.listPaths.and.returnValue(throwError(() => ({ status: 500 })));
    build();
    await settle();
    expect(bannerText()).toContain('Erreur serveur');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(0);
  });

  it('affiche un bandeau quand le catalogue de compétences échoue', async () => {
    svc.listSkills.and.returnValue(throwError(() => ({ status: 403 })));
    build();
    let rows: SkillResponse[] | undefined;
    subs.add(component.skills$.subscribe(r => (rows = r)));
    await settle();
    expect(rows).toEqual([]);
    expect(bannerText()).toContain('droits');
  });

  it('affiche un bandeau quand les inscriptions échouent', async () => {
    svc.listEnrollments.and.returnValue(throwError(() => ({ status: 0 })));
    build();
    subs.add(component.enrollments$.subscribe());
    await settle();
    expect(bannerText()).toContain('Service inaccessible');
  });

  // ---- Actions sur les inscriptions -----------------------------------------

  it('démarre une inscription puis recharge la liste', () => {
    build();
    subs.add(component.enrollments$.subscribe());
    const before = svc.listEnrollments.calls.count();
    component.startEnrollment(enrollment());
    expect(svc.startEnrollment).toHaveBeenCalledWith('enr-1');
    expect(svc.listEnrollments.calls.count()).toBe(before + 1);
  });

  it('signale l’échec du démarrage sans recharger', async () => {
    build();
    subs.add(component.enrollments$.subscribe());
    svc.startEnrollment.and.returnValue(throwError(() => ({ status: 409 })));
    const before = svc.listEnrollments.calls.count();
    component.startEnrollment(enrollment());
    await settle();
    expect(bannerText()).toContain('Impossible de démarrer');
    expect(svc.listEnrollments.calls.count()).toBe(before);
  });

  it('annule une inscription puis recharge la liste', () => {
    build();
    subs.add(component.enrollments$.subscribe());
    const before = svc.listEnrollments.calls.count();
    component.cancelEnrollment(enrollment());
    expect(svc.cancelEnrollment).toHaveBeenCalledWith('enr-1');
    expect(svc.listEnrollments.calls.count()).toBe(before + 1);
  });

  it('signale l’échec de l’annulation', async () => {
    build();
    subs.add(component.enrollments$.subscribe());
    svc.cancelEnrollment.and.returnValue(throwError(() => ({ status: 409 })));
    component.cancelEnrollment(enrollment());
    await settle();
    expect(bannerText()).toContain('Impossible d\'annuler');
  });

  // ---- Dialogues ------------------------------------------------------------

  it('recharge chaque liste après une création confirmée', () => {
    build();
    subs.add(component.skills$.subscribe());
    subs.add(component.enrollments$.subscribe());
    const paths = svc.listPaths.calls.count();
    const skills = svc.listSkills.calls.count();
    const enrolls = svc.listEnrollments.calls.count();

    component.openPathCreate();
    component.openSkillCreate();
    component.openEnroll();

    expect(svc.listPaths.calls.count()).toBe(paths + 1);
    expect(svc.listSkills.calls.count()).toBe(skills + 1);
    expect(svc.listEnrollments.calls.count()).toBe(enrolls + 1);
  });

  it('ne recharge rien quand le dialogue est annulé', () => {
    build();
    dialogResult = undefined;
    const paths = svc.listPaths.calls.count();
    component.openPathCreate();
    expect(svc.listPaths.calls.count()).toBe(paths);
  });

  it('ouvre la fiche du parcours cliqué', () => {
    build();
    const nav = spyOn(TestBed.inject(Router), 'navigate');
    component.openPath(path({ id: 'path-9' }));
    expect(nav).toHaveBeenCalledWith(['/training/paths', 'path-9']);
  });

  // ---- Présentation ---------------------------------------------------------

  it('affiche le code du parcours dans les inscriptions, sinon un identifiant tronqué', () => {
    build();
    expect(component.pathName('path-1')).toBe('yellow-belt');
    expect(component.pathName('0123456789abcdef')).toBe('01234567');
  });

  it('mappe les statuts sur des classes de badge distinctes', () => {
    build();
    expect(component.pathStatusBadge('ACTIVE')).toBe('pbadge pbadge-active');
    expect(component.pathStatusBadge('ARCHIVED')).toBe('pbadge pbadge-archived');
    expect(component.enrollStatusBadge('IN_PROGRESS')).toBe('ebadge ebadge-in_progress');
  });

  it('bascule la couleur de progression au seuil de 80 %', () => {
    build();
    expect(component.progressColor(79)).toBe('accent');
    expect(component.progressColor(80)).toBe('primary');
  });
});

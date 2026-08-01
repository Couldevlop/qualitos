import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { LearnerProgressResponse, PathResponse } from '../../training.types';
import { LearningHomeComponent } from './learning-home.component';

describe('LearningHomeComponent', () => {
  let fixture: ComponentFixture<LearningHomeComponent>;
  let component: LearningHomeComponent;
  let svc: jasmine.SpyObj<TrainingService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const progress = (over: Partial<LearnerProgressResponse> = {}): LearnerProgressResponse => ({
    userId: 'u1', tenantId: 't1', points: 160, completedCount: 2, bestScore: 88,
    beltLevel: 'YELLOW', pointsToNextBelt: 140, badges: ['FIRST_STEPS', 'YELLOW_BELT'],
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-06-01T00:00:00Z', ...over
  });

  const path = (over: Partial<PathResponse> = {}): PathResponse => ({
    id: 'path-1', tenantId: 't1', code: 'yellow-belt-quality', name: 'Yellow Belt Qualité',
    description: 'Bases qualité', targetRole: 'Opérateur', durationHours: 14,
    passingScore: 70, validityMonths: 36, status: 'ACTIVE', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  function build(): void {
    fixture = TestBed.createComponent(LearningHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** `deferredView` livre ses états via `asyncScheduler` : un tour de boucle suffit. */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  function text(): string { return (fixture.nativeElement as HTMLElement).textContent ?? ''; }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<TrainingService>('TrainingService',
      ['myProgress', 'listPaths', 'completeLearning']);
    svc.myProgress.and.returnValue(of(progress()));
    svc.listPaths.and.returnValue(of(page([path()])));
    svc.completeLearning.and.returnValue(of(progress({ points: 260, completedCount: 3 })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [LearningHomeComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: svc },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
  });

  // ---- Chargement -----------------------------------------------------------

  it('affiche la ceinture, les points et les badges de l’apprenant', () => {
    build();
    expect(svc.myProgress).toHaveBeenCalled();
    expect(text()).toContain('YELLOW');
    expect(text()).toContain('160');
    expect(text()).toContain('FIRST_STEPS');
  });

  it('ne demande que les parcours actifs, ceux qu’un apprenant peut suivre', () => {
    build();
    expect(svc.listPaths).toHaveBeenCalledWith(0, 50, 'ACTIVE');
    expect(text()).toContain('Yellow Belt Qualité');
  });

  it('invite à démarrer quand aucun badge n’est encore obtenu', () => {
    svc.myProgress.and.returnValue(of(progress({ badges: [], completedCount: 0, points: 0 })));
    build();
    expect(text()).toContain('Aucun badge pour l\'instant');
  });

  it('annonce le palier maximal quand il ne reste aucun point à gagner', () => {
    svc.myProgress.and.returnValue(of(progress({ beltLevel: 'BLACK', pointsToNextBelt: 0 })));
    build();
    expect(text()).toContain('Palier maximal atteint');
  });

  it('affiche un tiret quand aucun score n’a encore été enregistré', () => {
    svc.myProgress.and.returnValue(of(progress({ bestScore: undefined })));
    build();
    expect(text()).toContain('—');
  });

  // ---- Dégradations ---------------------------------------------------------

  it('affiche un bandeau et une progression vierge si le service échoue', async () => {
    svc.myProgress.and.returnValue(throwError(() => ({ status: 500 })));
    build();
    await settle();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')?.textContent)
      .toContain('Erreur serveur');
    // La page reste utilisable : ceinture blanche, aucun point.
    expect(text()).toContain('WHITE');
  });

  it('affiche la page même si le catalogue des parcours est indisponible', () => {
    svc.listPaths.and.returnValue(throwError(() => ({ status: 403 })));
    build();
    expect(text()).toContain('YELLOW');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(0);
  });

  // ---- Complétion d'un parcours ---------------------------------------------

  it('enregistre la complétion du parcours et recharge la progression', () => {
    build();
    const before = svc.myProgress.calls.count();
    component.markComplete(path());
    expect(svc.completeLearning).toHaveBeenCalledWith({ itemCode: 'yellow-belt-quality', score: 100 });
    expect(snack.open).toHaveBeenCalled();
    expect(svc.myProgress.calls.count()).toBe(before + 1);
    expect(component.completing).toBeFalse();
  });

  it('ignore un second clic tant que la complétion est en vol (anti double-comptage)', () => {
    const pending = new Subject<LearnerProgressResponse>();
    svc.completeLearning.and.returnValue(pending.asObservable());
    build();
    component.markComplete(path());
    component.markComplete(path());
    expect(svc.completeLearning.calls.count()).toBe(1);
    expect(component.completing).toBeTrue();

    pending.next(progress());
    pending.complete();
    expect(component.completing).toBeFalse();
  });

  it('signale l’échec d’enregistrement et rend le bouton à nouveau cliquable', () => {
    svc.completeLearning.and.returnValue(throwError(() => ({ status: 409 })));
    build();
    const before = svc.myProgress.calls.count();
    component.markComplete(path());
    expect(snack.open)
      .toHaveBeenCalledWith('État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(component.completing).toBeFalse();
    expect(svc.myProgress.calls.count()).toBe(before);
  });

  // ---- Présentation ---------------------------------------------------------

  it('marque comme atteintes les ceintures inférieures ou égale à la ceinture courante', () => {
    build();
    expect(component.beltReached('GREEN', 'WHITE')).toBeTrue();
    expect(component.beltReached('GREEN', 'GREEN')).toBeTrue();
    expect(component.beltReached('GREEN', 'BLACK')).toBeFalse();
  });

  it('dérive les classes CSS de la ceinture et des badges', () => {
    build();
    expect(component.beltClass('BLACK')).toBe('belt belt-black');
    expect(component.badgeChipClass('QUALITY_CHAMPION')).toBe('badge-chip badge-quality-champion');
  });
});

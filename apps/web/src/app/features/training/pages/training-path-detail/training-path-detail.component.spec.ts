import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subscription, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { PathResponse, SkillRequirementResponse, SkillResponse } from '../../training.types';
import { TrainingPathDetailComponent } from './training-path-detail.component';

describe('TrainingPathDetailComponent', () => {
  let fixture: ComponentFixture<TrainingPathDetailComponent>;
  let component: TrainingPathDetailComponent;
  let svc: jasmine.SpyObj<TrainingService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  /** Valeur renvoyée par le prochain dialogue (confirmation ou formulaire). */
  let dialogResult: unknown;
  let routeId: string;
  /** Souscription manuelle à `path$` — recréée à chaque test. */
  let subs: Subscription;
  /** État du « serveur » : les détachements doivent y être reflétés. */
  let storedRequirements: SkillRequirementResponse[];

  const path = (over: Partial<PathResponse> = {}): PathResponse => ({
    id: 'path-1', tenantId: 't1', code: 'yellow-belt', name: 'Yellow Belt Qualité',
    description: 'Bases qualité', targetRole: 'Opérateur', durationHours: 14,
    passingScore: 70, validityMonths: 36, status: 'DRAFT', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const requirement = (over: Partial<SkillRequirementResponse> = {}): SkillRequirementResponse => ({
    id: 'req-1', pathId: 'path-1', skillId: 'skill-1', targetLevel: 2,
    createdAt: '2026-01-01T00:00:00Z', ...over
  });

  const skill = (over: Partial<SkillResponse> = {}): SkillResponse => ({
    id: 'skill-1', tenantId: 't1', code: 'ishikawa', name: 'Animation Ishikawa',
    description: 'Atelier 6M', category: 'Méthodes',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  function build(): void {
    fixture = TestBed.createComponent(TrainingPathDetailComponent);
    component = fixture.componentInstance;
    // La fiche n'est chargée que si quelqu'un souscrit : le template le fait via `async`.
    fixture.detectChanges();
    subs.add(component.path$.subscribe());
  }

  /** `deferredView` livre ses états via `asyncScheduler` : un tour de boucle suffit. */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<TrainingService>('TrainingService', [
      'getPath', 'listRequirements', 'listSkills', 'activatePath', 'reopenPath',
      'archivePath', 'deletePath', 'detachRequirement'
    ]);
    svc.getPath.and.returnValue(of(path()));
    // Le composant recharge la table après détachement : le double doit se
    // comporter comme un vrai serveur et ne plus renvoyer l'exigence retirée,
    // sinon il la ressuscite au rechargement.
    storedRequirements = [requirement()];
    svc.listRequirements.and.callFake(() => of(storedRequirements));
    svc.listSkills.and.returnValue(of(page([skill()])));
    svc.activatePath.and.returnValue(of(path({ status: 'ACTIVE' })));
    svc.reopenPath.and.returnValue(of(path({ status: 'DRAFT' })));
    svc.archivePath.and.returnValue(of(path({ status: 'ARCHIVED' })));
    svc.deletePath.and.returnValue(of(void 0));
    svc.detachRequirement.and.callFake((_pathId: string, skillId: string) => {
      storedRequirements = storedRequirements.filter(r => r.skillId !== skillId);
      return of(void 0);
    });
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialogResult = true;
    routeId = 'path-1';
    subs = new Subscription();

    await TestBed.configureTestingModule({
      declarations: [TrainingPathDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: MatSnackBar, useValue: snack },
        provideRouter([]),
        // Après `provideRouter` : celui-ci fournit aussi un ActivatedRoute racine
        // qui écraserait le double si l'ordre était inversé.
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ get id() { return routeId; } })) } }
      ]
    }).compileComponents();
  });

  afterEach(() => subs.unsubscribe());

  // ---- Garde sur l'identifiant (OWASP A03) ----------------------------------

  it('refuse un identifiant qui n’est ni un UUID ni un identifiant de démo', async () => {
    routeId = '../../etc/passwd';
    build();
    await settle();
    expect(svc.getPath).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')?.textContent)
      .toContain('Identifiant invalide');
  });

  it('accepte un UUID renvoyé par le backend', () => {
    routeId = '3f2504e0-4f89-11d3-9a0c-0305e82c3301';
    build();
    expect(svc.getPath).toHaveBeenCalledWith('3f2504e0-4f89-11d3-9a0c-0305e82c3301');
  });

  // ---- Chargement -----------------------------------------------------------

  it('charge le parcours, ses exigences et le catalogue de compétences', () => {
    build();
    expect(svc.getPath).toHaveBeenCalledWith('path-1');
    expect(svc.listRequirements).toHaveBeenCalledWith('path-1');
    expect(svc.listSkills).toHaveBeenCalledWith(0, 200);
    expect(component.requirements.length).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Yellow Belt Qualité');
  });

  it('affiche un bandeau d’erreur quand le parcours est introuvable', async () => {
    svc.getPath.and.returnValue(throwError(() => ({ status: 404 })));
    build();
    await settle();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')).toBeTruthy();
  });

  it('reste affichable si le catalogue de compétences est indisponible', () => {
    svc.listSkills.and.returnValue(throwError(() => ({ status: 500 })));
    svc.listRequirements.and.returnValue(throwError(() => ({ status: 500 })));
    build();
    expect(component.requirements).toEqual([]);
    expect(component.skillsById).toEqual({});
  });

  // ---- Transitions d'état ----------------------------------------------------

  it('active le parcours puis recharge la fiche', () => {
    build();
    const before = svc.getPath.calls.count();
    component.activate(path());
    expect(svc.activatePath).toHaveBeenCalledWith('path-1');
    expect(snack.open).toHaveBeenCalled();
    expect(svc.getPath.calls.count()).toBe(before + 1);
  });

  it('signale une activation refusée par le serveur sans recharger', () => {
    build();
    svc.activatePath.and.returnValue(throwError(() => ({ status: 409 })));
    const before = svc.getPath.calls.count();
    component.activate(path());
    expect(snack.open).toHaveBeenCalledWith('État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(svc.getPath.calls.count()).toBe(before);
  });

  it('rouvre un parcours actif', () => {
    build();
    component.reopen(path({ status: 'ACTIVE' }));
    expect(svc.reopenPath).toHaveBeenCalledWith('path-1');
  });

  it('signale une réouverture refusée', () => {
    build();
    svc.reopenPath.and.returnValue(throwError(() => ({ status: 403 })));
    component.reopen(path({ status: 'ACTIVE' }));
    expect(snack.open).toHaveBeenCalledWith('Vous n\'avez pas les droits pour cette action.', 'OK', { duration: 4000 });
  });

  it('archive après confirmation seulement', () => {
    build();
    dialogResult = false;
    component.archive(path());
    expect(svc.archivePath).not.toHaveBeenCalled();

    dialogResult = true;
    component.archive(path());
    expect(svc.archivePath).toHaveBeenCalledWith('path-1');
  });

  it('signale un archivage refusé', () => {
    build();
    svc.archivePath.and.returnValue(throwError(() => ({ status: 500 })));
    component.archive(path());
    expect(snack.open).toHaveBeenCalledWith('Erreur serveur — réessayez dans un instant.', 'OK', { duration: 4000 });
  });

  // ---- Suppression ------------------------------------------------------------

  it('supprime le parcours après confirmation puis revient à la liste', () => {
    build();
    const nav = spyOn(TestBed.inject(Router), 'navigate');
    component.remove(path());
    expect(svc.deletePath).toHaveBeenCalledWith('path-1');
    expect(nav).toHaveBeenCalledWith(['/training']);
  });

  it('ne supprime rien si la confirmation est refusée', () => {
    build();
    dialogResult = false;
    component.remove(path());
    expect(svc.deletePath).not.toHaveBeenCalled();
  });

  it('reste sur la fiche quand la suppression échoue', () => {
    build();
    svc.deletePath.and.returnValue(throwError(() => ({ status: 409 })));
    const nav = spyOn(TestBed.inject(Router), 'navigate');
    component.remove(path());
    expect(nav).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  // ---- Exigences de compétences -------------------------------------------------

  it('retire l’exigence de la table après confirmation', () => {
    build();
    component.detach(path(), requirement());
    expect(svc.detachRequirement).toHaveBeenCalledWith('path-1', 'skill-1');
    expect(component.requirements).toEqual([]);
  });

  it('conserve l’exigence si la confirmation est refusée', () => {
    build();
    dialogResult = false;
    component.detach(path(), requirement());
    expect(svc.detachRequirement).not.toHaveBeenCalled();
    expect(component.requirements.length).toBe(1);
  });

  it('conserve l’exigence quand le serveur refuse le retrait', () => {
    build();
    svc.detachRequirement.and.returnValue(throwError(() => ({ status: 409 })));
    component.detach(path(), requirement());
    expect(component.requirements.length).toBe(1);
    expect(snack.open).toHaveBeenCalled();
  });

  it('recharge après un rattachement ou une édition confirmés, pas après une annulation', () => {
    build();
    const before = svc.getPath.calls.count();
    component.openAttach(path());
    component.openEdit(path());
    expect(svc.getPath.calls.count()).toBe(before + 2);

    dialogResult = undefined;
    component.openAttach(path());
    expect(svc.getPath.calls.count()).toBe(before + 2);
  });

  // ---- Présentation --------------------------------------------------------------

  it('résout le libellé des compétences, avec repli quand le catalogue est incomplet', () => {
    build();
    expect(component.skillCode(requirement())).toBe('ishikawa');
    expect(component.skillName(requirement())).toBe('Animation Ishikawa');
    expect(component.skillCategory(requirement())).toBe('Méthodes');

    const orphan = requirement({ skillId: '0123456789abcdef' });
    expect(component.skillCode(orphan)).toBe('01234567');
    expect(component.skillName(orphan)).toBe('—');
    expect(component.skillCategory(orphan)).toBe('—');
  });

  it('nomme les niveaux de l’échelle Dreyfus et retombe sur le nombre hors bornes', () => {
    build();
    expect(component.levelLabel(0)).toBe('NONE');
    expect(component.levelLabel(4)).toBe('EXPERT');
    expect(component.levelLabel(9)).toBe('9');
  });

  it('mappe le statut du parcours sur une classe de badge', () => {
    build();
    expect(component.statusBadge('DRAFT')).toBe('pbadge pbadge-draft');
    expect(component.statusBadge('ARCHIVED')).toBe('pbadge pbadge-archived');
  });
});

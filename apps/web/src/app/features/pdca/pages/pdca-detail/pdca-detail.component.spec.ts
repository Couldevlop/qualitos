import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse, PdcaStatus, PdcaStepResponse } from '../../pdca.types';
import { PdcaDetailComponent } from './pdca-detail.component';

const ID = '11111111-1111-1111-1111-111111111111';

function buildCycle(overrides: Partial<PdcaCycleResponse> = {}): PdcaCycleResponse {
  return {
    id: ID, tenantId: 't1', title: 'Réduction des défauts de soudure',
    description: 'Objectif -30% NC en 90j.', status: 'DO', ownerId: 'u1',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z',
    steps: [], ...overrides
  };
}

const STEP: PdcaStepResponse = {
  id: 's1', cycleId: ID, phase: 'PLAN', title: 'Analyse Pareto', status: 'DONE',
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z'
};

/**
 * La fiche cycle porte les transitions de la roue de Deming : chaque garde
 * (identifiant, état terminal, action déjà en vol) évite une transition
 * illégitime que le serveur refuserait — ou pire, appliquerait deux fois.
 */
describe('PdcaDetailComponent', () => {
  let fixture: ComponentFixture<PdcaDetailComponent>;
  let component: PdcaDetailComponent;
  let pdca: jasmine.SpyObj<PdcaService>;
  let router: Router;
  let routeId: string;

  /** Monte le composant : le premier reload$ précède l'abonnement de la vue. */
  function setup(): void {
    fixture = TestBed.createComponent(PdcaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    routeId = ID;
    pdca = jasmine.createSpyObj<PdcaService>('PdcaService',
      ['getCycle', 'advanceCycle', 'cancelCycle']);
    pdca.getCycle.and.returnValue(of(buildCycle()));

    await TestBed.configureTestingModule({
      declarations: [PdcaDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: PdcaService, useValue: pdca },
        // paramMap lu à la volée : chaque test choisit l'identifiant de route
        // avant de monter le composant.
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => routeId } } } }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  // --- garde sur l'identifiant de route ---------------------------------------

  it('refuse un identifiant malformé et renvoie vers la liste sans appeler l\'API', () => {
    routeId = '../../admin';
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    setup();

    expect(pdca.getCycle).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/pdca']);
    expect(snackSpy).toHaveBeenCalled();
    expect(component.cycle$).toBeUndefined();
  });

  // --- chargement --------------------------------------------------------------

  it('charge le cycle et rend ses étapes', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    setup();

    expect(pdca.getCycle).toHaveBeenCalledWith(ID);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Réduction des défauts de soudure');
    expect(el.querySelectorAll('.steps-table tbody tr').length).toBe(1);
    expect(el.querySelector('.empty')).toBeNull();
  });

  it('invite à créer la première étape quand le cycle est vide', () => {
    setup();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.steps-table')).toBeNull();
    expect(el.querySelector('.empty')).toBeTruthy();
  });

  it('affiche un message sûr et aucune fiche quand le cycle est introuvable', fakeAsync(() => {
    pdca.getCycle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    setup();
    tick();                 // deferredView publie l'état en macrotâche
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.state-row.error')?.textContent).toContain('Cycle introuvable.');
    expect(el.querySelector('.info-card')).toBeNull();
  }));

  it('n\'expose pas le détail technique d\'une erreur serveur', fakeAsync(() => {
    pdca.getCycle.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500, error: { detail: 'org.hibernate.LazyInitializationException' }
    })));
    setup();
    tick();
    fixture.detectChanges();

    const banner = (fixture.nativeElement as HTMLElement).querySelector('.state-row.error');
    expect(banner?.textContent).toContain('Erreur serveur');
    expect(banner?.textContent).not.toContain('Hibernate');
  }));

  // --- avancement de phase -----------------------------------------------------

  it('avance la phase, confirme et recharge la fiche', () => {
    setup();
    pdca.getCycle.calls.reset();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.advanceCycle.and.returnValue(of(buildCycle({ status: 'CHECK' })));

    component.advance('DO');

    expect(pdca.advanceCycle).toHaveBeenCalledWith(ID);
    expect(snackSpy).toHaveBeenCalled();
    expect(pdca.getCycle).toHaveBeenCalledTimes(1);   // rechargement après transition
    expect(component.acting$.value).toBeFalse();
  });

  it('n\'avance pas un cycle déjà terminé (COMPLETED / CANCELLED)', () => {
    setup();
    component.advance('COMPLETED');
    component.advance('CANCELLED');
    expect(pdca.advanceCycle).not.toHaveBeenCalled();
  });

  it('ignore un second avancement tant que le premier est en vol', () => {
    setup();
    pdca.advanceCycle.and.returnValue(new Subject<PdcaCycleResponse>());

    component.advance('DO');
    component.advance('DO');

    expect(pdca.advanceCycle).toHaveBeenCalledTimes(1);
    expect(component.acting$.value).toBeTrue();
  });

  it('signale le refus serveur sans recharger la fiche', () => {
    setup();
    pdca.getCycle.calls.reset();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.advanceCycle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.advance('DO');

    expect(snackSpy).toHaveBeenCalledWith(
      'État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(pdca.getCycle).not.toHaveBeenCalled();
    expect(component.acting$.value).toBeFalse();
  });

  // --- annulation --------------------------------------------------------------

  it('annule le cycle et recharge la fiche', () => {
    setup();
    pdca.getCycle.calls.reset();
    pdca.cancelCycle.and.returnValue(of(buildCycle({ status: 'CANCELLED' })));

    component.cancel();

    expect(pdca.cancelCycle).toHaveBeenCalledWith(ID);
    expect(pdca.getCycle).toHaveBeenCalledTimes(1);
    expect(component.acting$.value).toBeFalse();
  });

  it('ignore une seconde annulation tant que la première est en vol', () => {
    setup();
    pdca.cancelCycle.and.returnValue(new Subject<PdcaCycleResponse>());
    component.cancel();
    component.cancel();
    expect(pdca.cancelCycle).toHaveBeenCalledTimes(1);
  });

  it('signale l\'échec d\'annulation avec un message sûr', () => {
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.cancelCycle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    component.cancel();

    expect(snackSpy).toHaveBeenCalledWith(
      'Vous n\'avez pas les droits pour cette action.', 'OK', { duration: 4000 });
    expect(component.acting$.value).toBeFalse();
  });

  // --- ajout d'étape -----------------------------------------------------------

  it('présélectionne la phase courante dans le dialogue d\'étape et recharge après ajout', () => {
    setup();
    pdca.getCycle.calls.reset();
    const dialog = TestBed.inject(MatDialog);
    const openSpy = spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(STEP) } as never);

    component.openAddStep('CHECK');

    expect(openSpy.calls.mostRecent().args[1]?.data).toEqual({ cycleId: ID, defaultPhase: 'CHECK' });
    expect(pdca.getCycle).toHaveBeenCalledTimes(1);
  });

  it('ne présélectionne aucune phase depuis un statut hors roue', () => {
    setup();
    pdca.getCycle.calls.reset();
    const dialog = TestBed.inject(MatDialog);
    const openSpy = spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(undefined) } as never);

    component.openAddStep('COMPLETED');

    expect(openSpy.calls.mostRecent().args[1]?.data)
      .toEqual({ cycleId: ID, defaultPhase: undefined });
    expect(pdca.getCycle).not.toHaveBeenCalled();   // dialogue fermé sans étape : pas de rechargement
  });

  // --- présentation ------------------------------------------------------------

  it('classe les états terminaux de la roue', () => {
    setup();
    const terminal: PdcaStatus[] = ['COMPLETED', 'CANCELLED'];
    const running: PdcaStatus[] = ['PLAN', 'DO', 'CHECK', 'ACT'];
    terminal.forEach(s => expect(component.isTerminal(s)).withContext(s).toBeTrue());
    running.forEach(s => expect(component.isTerminal(s)).withContext(s).toBeFalse());
  });

  it('dérive les classes de badge du statut, du statut d\'étape et de la phase', () => {
    setup();
    expect(component.statusBadge('COMPLETED')).toBe('badge badge-completed');
    expect(component.stepStatusBadge('IN_PROGRESS')).toBe('badge badge-in_progress');
    expect(component.phaseColor('ACT')).toBe('phase phase-act');
  });

  it('revient à la liste des cycles', () => {
    setup();
    component.goBack();
    expect(router.navigate).toHaveBeenCalledWith(['/pdca']);
  });
});

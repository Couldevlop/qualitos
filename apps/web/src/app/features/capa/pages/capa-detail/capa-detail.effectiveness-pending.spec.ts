import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { EMPTY, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaCaseResponse } from '../../capa.types';
import { CapaDetailComponent } from './capa-detail.component';
import {
  CapaRevisionImpactComponent
} from '../capa-revision-impact/capa-revision-impact.component';
import { ProductsService } from '../../../products/products.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { DirectoryService } from '../../../../core/directory/directory.service';

function routerDouble(): Router {
  const router = jasmine.createSpyObj<Router>('Router',
    ['navigate', 'createUrlTree', 'serializeUrl'], { events: EMPTY });
  router.createUrlTree.and.returnValue({} as never);
  router.serializeUrl.and.returnValue('/');
  return router;
}

/** Le tableau des actions a son propre banc : ici, seule sa présence compte. */
@Component({ selector: 'qos-capa-action-panel', template: '', standalone: false })
class PanneauStub {
  @Input() capaCaseId = '';
}

/**
 * LA VÉRIFICATION D'EFFICACITÉ EST REPORTÉE, PAS PERDUE.
 *
 * <p>Ajouter une action à un dossier résolu le ramène en traitement : c'est
 * juste, il reste du travail. Mais la question d'efficacité, visible l'instant
 * d'avant, disparaissait alors de l'écran sans un mot — et on croyait à une
 * fonction perdue. Ces bancs portent sur la phrase qui dit à quelle condition
 * elle revient.
 */
describe('CapaDetailComponent — la vérification d’efficacité reportée', () => {

  const CASE_ID = '88888888-8888-8888-8888-888888888888';

  let fixture: ComponentFixture<CapaDetailComponent>;
  let component: CapaDetailComponent;
  let capa: jasmine.SpyObj<CapaService>;

  const action = (over: Partial<CapaActionResponse> = {}): CapaActionResponse => ({
    id: 'a1', capaId: CASE_ID, title: 'Trier le lot', status: 'DONE',
    actionType: 'CORRECTIVE', ...over
  });

  const dossier = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: CASE_ID, tenantId: 't1', title: 'Étiquetage manquant', type: 'CORRECTIVE',
    criticity: 'HIGH', status: 'IN_PROGRESS', sourceType: 'INTERNAL', ownerId: 'u1',
    createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z',
    actions: [action()], effectivenessCriterion: 'Zéro récidive sur 3 mois', ...over
  });

  function setup(c: CapaCaseResponse): void {
    capa.getCase.and.returnValue(of(c));
    fixture = TestBed.createComponent(CapaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function encart(): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="efficacite-reportee"]');
  }

  beforeEach(async () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['getCase', 'listEvidences', 'uploadEvidence', 'deleteEvidence',
       'listActionEvidences', 'uploadActionEvidence', 'deleteActionEvidence',
       'updateAction', 'addAction', 'deleteCase',
       'verifyEffectiveness', 'rejectCase']);
    capa.listEvidences.and.returnValue(of([]));
    capa.listActionEvidences.and.returnValue(of([]));

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CapaDetailComponent, CapaRevisionImpactComponent, PanneauStub],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        {
          provide: DirectoryService,
          useValue: { getByAccount: () => throwError(() => ({ status: 404 })) }
        },
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: ProductsService, useValue: { revisionRequestsForTrigger: () => of([]) } },
        {
          provide: MatSnackBar,
          useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open'])
        },
        { provide: MatDialog, useValue: jasmine.createSpyObj<MatDialog>('MatDialog', ['open']) },
        { provide: Router, useValue: routerDouble() },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } }
        }
      ]
    }).compileComponents();
  });

  // --- l'invariant : le report se DIT --------------------------------------

  it('annonce le report quand le dossier travaille encore', () => {
    setup(dossier({ actions: [action({ status: 'IN_PROGRESS' })] }));

    expect(encart())
      .withContext('sans un mot, la disparition du bloc passe pour une fonction perdue')
      .not.toBeNull();
  });

  it('dit COMBIEN d’actions restent, puisque c’est la condition du retour', () => {
    setup(dossier({
      actions: [action({ id: 'a1', status: 'IN_PROGRESS' }),
                action({ id: 'a2', status: 'PENDING' }),
                action({ id: 'a3', status: 'DONE' })]
    }));

    expect(encart()?.textContent).toContain('2');
  });

  it('parle de la DERNIÈRE action quand il n’en reste qu’une', () => {
    setup(dossier({
      actions: [action({ id: 'a1', status: 'PENDING' }), action({ id: 'a2', status: 'DONE' })]
    }));

    expect(encart()?.textContent).toContain('dernière');
  });

  // --- ce qu'il ne faut PAS dire -------------------------------------------

  /** Le bloc est là : annoncer en plus qu'il viendra serait du bruit. */
  it('se tait quand la vérification est déjà possible', () => {
    setup(dossier({ status: 'RESOLVED' }));

    expect(encart()).toBeNull();
    expect(component.canVerifyEffectiveness('RESOLVED')).toBeTrue();
  });

  /** Annoncer une étape dont personne n'a décidé serait inventer une exigence. */
  it('se tait quand aucune vérification n’est attendue', () => {
    setup(dossier({
      effectivenessCriterion: undefined, verificationRequired: false,
      actions: [action({ status: 'IN_PROGRESS' })]
    }));

    expect(encart()).toBeNull();
  });

  /** La décision explicite de vérifier suffit, sans critère écrit. */
  it('annonce le report sur la seule décision de vérifier', () => {
    setup(dossier({
      effectivenessCriterion: undefined, verificationRequired: true,
      actions: [action({ status: 'IN_PROGRESS' })]
    }));

    expect(encart()).not.toBeNull();
  });

  /** Déjà jugée : la question ne revient pas, et ne doit pas s'annoncer. */
  it('se tait quand l’efficacité a déjà été jugée', () => {
    setup(dossier({
      effectivenessVerified: false, actions: [action({ status: 'IN_PROGRESS' })]
    }));

    expect(encart()).toBeNull();
  });

  /** Un dossier clos ou écarté ne reporte rien : il n'attend plus. */
  it('se tait sur un dossier clos', () => {
    setup(dossier({ status: 'CLOSED', actions: [action({ status: 'IN_PROGRESS' })] }));

    expect(encart()).toBeNull();
  });
});

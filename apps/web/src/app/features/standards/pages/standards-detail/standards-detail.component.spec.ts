import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, ClauseDetail, RequirementDetail, SectionDetail, StandardDetail
} from '../../standards.types';
import { StandardsDetailComponent } from './standards-detail.component';

/**
 * Fiche d'une adoption (§8) — vue de lecture pendant un audit, et, pour les
 * seuls référentiels du tenant, écran de saisie de leur arborescence.
 *
 * L'invariant que cette spec tient : les commandes d'édition n'existent QUE sur
 * un référentiel appartenant au tenant. Les proposer sur une norme livrée
 * promettrait un geste que le serveur refuse (403) — l'utilisateur en
 * conclurait que l'application est cassée, non que la norme est en lecture seule.
 */
describe('StandardsDetailComponent', () => {
  let fixture: ComponentFixture<StandardsDetailComponent>;
  let component: StandardsDetailComponent;
  let svc: jasmine.SpyObj<StandardsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let el: HTMLElement;

  const ADOPTION = { id: 'ad1', standardId: 's1', standardCode: 'PRO-002' } as AdoptionResponse;

  const REQUIREMENT = {
    id: 'r1', code: '1.1.1', text: 'Le programme est revu chaque année',
    obligation: 'MUST', orderIndex: 0
  } as RequirementDetail;

  const CLAUSE = {
    id: 'c1', code: '1.1', title: 'Fréquence', orderIndex: 0, requirements: [REQUIREMENT]
  } as ClauseDetail;

  const SECTION = {
    id: 'sec1', code: '1', title: 'Programmation', orderIndex: 0, clauses: [CLAUSE]
  } as SectionDetail;

  function detail(over: Partial<StandardDetail> = {}): StandardDetail {
    return {
      id: 's1', code: 'PRO-002', fullName: 'Procédure d\'audit interne',
      currentVersion: 'v3', certificationBodyRequired: false, status: 'PUBLISHED',
      sections: [SECTION], owned: true, ...over
    } as StandardDetail;
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<StandardsService>('StandardsService', [
      'getAdoption', 'getStandardDetail', 'listDocumentTemplates', 'listProcessTemplates',
      'listRevisions', 'getAlignment', 'getRoadmap', 'getAuditBlanc', 'listEvidence',
      'addSection', 'updateSection', 'deleteSection',
      'addClause', 'updateClause', 'deleteClause',
      'addRequirement', 'updateRequirement', 'deleteRequirement'
    ]);
    svc.getAdoption.and.returnValue(of(ADOPTION));
    svc.getStandardDetail.and.returnValue(of(detail()));
    svc.listDocumentTemplates.and.returnValue(of([]));
    svc.listProcessTemplates.and.returnValue(of([]));
    svc.listRevisions.and.returnValue(of([]));
    svc.getAlignment.and.returnValue(throwError(() => ({ status: 504 })));
    svc.getRoadmap.and.returnValue(throwError(() => ({ status: 504 })));
    svc.getAuditBlanc.and.returnValue(throwError(() => ({ status: 504 })));
    svc.listEvidence.and.returnValue(of([]));
    [svc.addSection, svc.updateSection, svc.deleteSection,
      svc.addClause, svc.updateClause, svc.deleteClause,
      svc.addRequirement, svc.updateRequirement, svc.deleteRequirement]
      .forEach(spy => spy.and.returnValue(of(undefined)));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [StandardsDetailComponent],
      // FormsModule : la fiche pilote son formulaire de liaison de preuve en
      // ngModel, comme le fait StandardsModule.
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1' }) } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'ad1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StandardsDetailComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement as HTMLElement;
  });

  /** Le dialogue rend `value` ; `undefined` simule une annulation. */
  function dialogReturns(value: unknown): void {
    dialog.open.and.returnValue(
      { afterClosed: () => of(value) } as MatDialogRef<unknown, unknown>);
  }

  it('n\'ouvre l\'édition que sur un référentiel appartenant au tenant', () => {
    // Une norme livrée se maintient par migration : proposer de la modifier
    // promettrait une action que le serveur refusera de toute façon (403).
    svc.getStandardDetail.and.returnValue(of(detail({ owned: false })));
    fixture.detectChanges();
    expect(el.querySelector('.add-section-btn')).toBeNull();
    expect(el.querySelector('.edit-requirement-btn')).toBeNull();

    svc.getStandardDetail.and.returnValue(of(detail({ owned: true })));
    component.ngOnInit();
    fixture.detectChanges();
    expect(el.querySelector('.add-section-btn')).not.toBeNull();
    expect(el.querySelector('.add-clause-btn')).not.toBeNull();
    expect(el.querySelector('.add-requirement-btn')).not.toBeNull();
  });

  it('dit quoi faire devant un référentiel vide', () => {
    // C'est l'état NORMAL à la création : l'écran doit inviter, pas donner
    // l'impression d'un chargement raté.
    svc.getStandardDetail.and.returnValue(of(detail({ sections: [] })));
    fixture.detectChanges();

    expect(el.querySelector('.tree-empty')!.textContent)
      .toContain('saisissez celles de votre procédure');
  });

  it('recharge la fiche après un ajout, sans la reconstruire à la main', () => {
    fixture.detectChanges();
    dialogReturns({ code: '2', title: 'Réalisation' });
    svc.getStandardDetail.calls.reset();

    el.querySelector<HTMLButtonElement>('.add-section-btn')!.click();

    expect(svc.addSection).toHaveBeenCalledWith('s1', { code: '2', title: 'Réalisation' });
    // Reconstruire l'arbre côté client ferait diverger l'écran de la base au
    // premier cas non prévu (ordre, code normalisé par le serveur).
    expect(svc.getStandardDetail).toHaveBeenCalledWith('s1');
  });

  it('n\'écrit rien quand la saisie est abandonnée', () => {
    fixture.detectChanges();
    dialogReturns(undefined);

    component.addSection();
    component.editSection(SECTION);
    component.addClause(SECTION);
    component.editClause(CLAUSE);
    component.addRequirement(CLAUSE);
    component.editRequirement(REQUIREMENT);

    expect(svc.addSection).not.toHaveBeenCalled();
    expect(svc.updateSection).not.toHaveBeenCalled();
    expect(svc.addClause).not.toHaveBeenCalled();
    expect(svc.updateClause).not.toHaveBeenCalled();
    expect(svc.addRequirement).not.toHaveBeenCalled();
    expect(svc.updateRequirement).not.toHaveBeenCalled();
  });

  it('adresse chaque écriture au bon niveau', () => {
    fixture.detectChanges();
    dialogReturns({ code: 'x', title: 'T', text: 'T', obligation: 'MUST' });

    component.editSection(SECTION);
    component.addClause(SECTION);
    component.editClause(CLAUSE);
    component.addRequirement(CLAUSE);
    component.editRequirement(REQUIREMENT);

    expect(svc.updateSection).toHaveBeenCalledWith('s1', 'sec1', jasmine.anything());
    expect(svc.addClause).toHaveBeenCalledWith('s1', 'sec1', jasmine.anything());
    expect(svc.updateClause).toHaveBeenCalledWith('s1', 'c1', jasmine.anything());
    expect(svc.addRequirement).toHaveBeenCalledWith('s1', 'c1', jasmine.anything());
    expect(svc.updateRequirement).toHaveBeenCalledWith('s1', 'r1', jasmine.anything());
  });

  it('dit ce que la suppression emporte avant de l\'exécuter', () => {
    fixture.detectChanges();
    const ask = spyOn(window, 'confirm').and.returnValue(true);

    component.deleteSection(SECTION);

    // La question NOMME ce qui part : « êtes-vous sûr ? » ne se lit plus.
    expect(ask.calls.mostRecent().args[0]).toContain('1');
    expect(svc.deleteSection).toHaveBeenCalledWith('s1', 'sec1');
  });

  it('ne supprime rien si la confirmation est refusée', () => {
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteSection(SECTION);
    component.deleteClause(CLAUSE);
    component.deleteRequirement(REQUIREMENT);

    expect(svc.deleteSection).not.toHaveBeenCalled();
    expect(svc.deleteClause).not.toHaveBeenCalled();
    expect(svc.deleteRequirement).not.toHaveBeenCalled();
  });

  it('supprime clause et exigence quand la confirmation est donnée', () => {
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(true);

    component.deleteClause(CLAUSE);
    component.deleteRequirement(REQUIREMENT);

    expect(svc.deleteClause).toHaveBeenCalledWith('s1', 'c1');
    expect(svc.deleteRequirement).toHaveBeenCalledWith('s1', 'r1');
  });

  [
    { status: 409, expected: 'déjà pris' },
    { status: 403, expected: 'plateforme' },
    { status: 500, expected: 'impossible' }
  ].forEach(({ status, expected }) => {
    it(`explique le refus ${status} par le geste qu'il appelle`, () => {
      fixture.detectChanges();
      dialogReturns({ code: '1', title: 'Doublon' });
      svc.addSection.and.returnValue(throwError(() => ({ status })));
      svc.getStandardDetail.calls.reset();

      component.addSection();

      expect(snack.open.calls.mostRecent().args[0]).toContain(expected);
      // Rien n'a changé côté serveur : inutile de relire la fiche.
      expect(svc.getStandardDetail).not.toHaveBeenCalled();
    });
  });
});

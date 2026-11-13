import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { routerEspion } from '../../../../shared/testing/router.stub';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaCaseResponse } from '../../capa.types';
import { CapaDetailComponent } from './capa-detail.component';
import {
  CapaRevisionImpactComponent
} from '../capa-revision-impact/capa-revision-impact.component';
import { ProductsService } from '../../../products/products.service';
import { AuthzService } from '../../../../core/authz/authz.service';

/**
 * Le tableau des actions, déclaré ICI alors que la fiche ne l'emploie plus.
 *
 * <p>C'est volontaire, et c'est ce qui donne sa valeur au banc « ne montre
 * aucun tableau d'actions » : le sélecteur est connu du module de test, donc
 * s'il réapparaissait dans le gabarit il serait rendu, et le banc tomberait.
 * Sans cette déclaration, l'absence constatée ne prouverait rien — un composant
 * inconnu ne se rend pas davantage.
 */
@Component({ selector: 'qos-capa-action-panel', template: '', standalone: false })
class PanneauStub {
  @Input() capaCaseId = '';
}

/**
 * La fiche CAPA : ce qu'elle porte, et ce qu'elle ne porte plus (ADR 0117).
 *
 * <p>Le statut du dossier se déduit de ses actions : les boutons « Démarrer »
 * et « Résoudre » ont disparu, parce qu'ils ne décidaient rien — le serveur
 * refusait déjà la résolution tant qu'une action restait à faire. Restent les
 * deux décisions qui, elles, ne se déduisent de rien : rejeter le dossier, et
 * juger son efficacité.
 *
 * <p>Et chaque fait n'est plus écrit qu'à un endroit : l'en-tête porte ce qui
 * appartient au dossier, le tableau ce qui varie d'une action à l'autre. Le
 * pavé vertical qui dépliait chaque action en répétant la nature, l'échéance
 * et le statut de l'en-tête a disparu avec le « Type » du dossier — depuis
 * l'ADR 0115, un dossier porte des correctives ET des préventives.
 */
describe('CapaDetailComponent — la fiche', () => {

  const CASE_ID = '55555555-5555-5555-5555-555555555555';
  const ACTION_ID = 'a1111111-1111-1111-1111-111111111111';

  let fixture: ComponentFixture<CapaDetailComponent>;
  let capa: jasmine.SpyObj<CapaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let annuaire: DirectoryUser[];
  let getByAccount: jasmine.Spy;

  const action = (over: Partial<CapaActionResponse> = {}): CapaActionResponse => ({
    id: ACTION_ID, capaId: CASE_ID, title: 'Réviser le plan de contrôle réception',
    status: 'PENDING', actionType: 'CORRECTIVE', ...over
  });

  const dossier = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: CASE_ID, tenantId: 't1', title: 'Étiquetage manquant', type: 'CORRECTIVE',
    criticity: 'HIGH', status: 'IN_PROGRESS', sourceType: 'NON_CONFORMITY',
    ownerId: '9c1f2b7e-0000-4000-8000-000000000000',
    createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z',
    actions: [action()], ...over
  });

  function setup(c: CapaCaseResponse = dossier()): void {
    capa.getCase.and.returnValue(of(c));
    fixture = TestBed.createComponent(CapaDetailComponent);
    fixture.detectChanges();
    (fixture.componentInstance as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(async () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['getCase', 'listEvidences', 'uploadEvidence', 'deleteEvidence',
       'listActionEvidences', 'uploadActionEvidence', 'deleteActionEvidence',
       'updateAction', 'addAction', 'rejectCase']);
    capa.listEvidences.and.returnValue(of([]));
    capa.listActionEvidences.and.returnValue(of([]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    annuaire = [];
    getByAccount = jasmine.createSpy('getByAccount').and.callFake((id: string) => {
      const u = annuaire.find(p => p.accountId === id);
      return u ? of(u) : throwError(() => ({ status: 404 }));
    });

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CapaDetailComponent, CapaRevisionImpactComponent, PanneauStub],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: DirectoryService, useValue: { getByAccount } },
        // Tous les droits : ces bancs testent la fiche, pas la matrice (ADR 0078).
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: ProductsService, useValue: { revisionRequestsForTrigger: () => of([]) } },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: routerEspion() },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } } }
      ]
    }).compileComponents();
  });

  // --- un seul workflow : celui des actions (ADR 0117) ----------------------

  it('ne propose pas de résoudre le dossier à la main : son statut se déduit', () => {
    setup(dossier({ status: 'OPEN' }));
    expect(hote().textContent ?? '').not.toContain('Résoudre');
  });

  it('garde les deux décisions qui ne se déduisent de rien', () => {
    // Renoncer au dossier se propose tant qu'il vit...
    setup(dossier({ status: 'IN_PROGRESS' }));
    expect(hote().textContent ?? '').toContain('Rejeter');

    // ...et juger son efficacité une fois toutes ses actions faites, c'est-à-dire
    // une fois qu'elles l'ont rendu RESOLU (ADR 0073, ADR 0108). Ni l'une ni
    // l'autre ne se lit dans l'avancement des actions.
    setup(dossier({ status: 'RESOLVED' }));
    expect(hote().querySelector('.effectiveness-actions')).not.toBeNull();
  });

  // --- une CAPA est une action : pas de liste d'actions dedans ---------------

  it('ne montre AUCUN tableau d’actions : une CAPA est elle-même une action', () => {
    setup(dossier({ actions: [action(), action({ id: 'a2', actionType: 'PREVENTIVE' })] }));
    // Ni le tableau, ni le pavé vertical qui l'avait précédé. L'un répétait
    // l'en-tête ligne par ligne, l'autre répétait l'en-tête en une ligne.
    expect(hote().querySelector('qos-capa-action-panel')).toBeNull();
    expect(hote().querySelector('[data-test="action-unique"]')).toBeNull();
    expect(hote().querySelector('[data-test="actions-card"]')).toBeNull();
  });

  it('fait avancer le travail du dossier, puisque son statut s’en déduit', () => {
    setup(dossier({ status: 'OPEN', actions: [action({ status: 'PENDING' })] }));
    capa.updateAction.and.returnValue(of(action({ status: 'IN_PROGRESS' })));

    const bouton: HTMLButtonElement =
      hote().querySelector('[data-test="avancement-avancer"]')!;
    expect(bouton.textContent).toContain('Démarrer');
    bouton.click();

    expect(capa.updateAction).toHaveBeenCalledWith(CASE_ID, ACTION_ID, { status: 'IN_PROGRESS' });
  });

  it('propose de terminer ce qui est en cours, et plus rien de ce qui est fait', () => {
    setup(dossier({ actions: [action({ status: 'IN_PROGRESS' })] }));
    expect(hote().querySelector('[data-test="avancement-avancer"]')!.textContent)
      .toContain('Terminer');

    setup(dossier({ status: 'RESOLVED', actions: [action({ status: 'DONE' })] }));
    expect(hote().querySelector('[data-test="avancement-avancer"]')).toBeNull();
  });

  it('ne fait plus rien avancer sur un dossier clos ou rejeté', () => {
    setup(dossier({ status: 'CLOSED', actions: [action({ status: 'PENDING' })] }));
    const bouton = hote().querySelector('[data-test="avancement-avancer"]') as HTMLButtonElement;
    expect(bouton.disabled).toBeTrue();
  });

  it('relit le dossier après avoir avancé : le statut affiché change avec lui', () => {
    setup(dossier({ status: 'OPEN', actions: [action({ status: 'PENDING' })] }));
    capa.updateAction.and.returnValue(of(action({ status: 'IN_PROGRESS' })));
    capa.getCase.calls.reset();
    capa.getCase.and.returnValue(of(dossier({ status: 'IN_PROGRESS' })));

    (hote().querySelector('[data-test="avancement-avancer"]') as HTMLButtonElement).click();

    // Sans cette relecture, l'en-tête dirait « ouvert » au-dessus d'un travail
    // qu'on vient de démarrer.
    expect(capa.getCase).toHaveBeenCalled();
  });

  it('dit qu’un dossier sans travail restera ouvert, au lieu de laisser chercher', () => {
    setup(dossier({ status: 'OPEN', actions: [] }));
    expect(hote().querySelector('[data-test="avancement-sans-action"]')).not.toBeNull();
    expect(hote().querySelector('[data-test="avancement-avancer"]')).toBeNull();
  });

  it('dit COMBIEN de travaux porte un dossier qui en porte plusieurs, sans les lister', () => {
    setup(dossier({
      sourceNonConformity: { id: 'nc-1', reference: 'NC-2026-0004', title: 'Étiquetage' },
      actions: [action(), action({ id: 'a2' }), action({ id: 'a3' })]
    }));

    const texte = hote().querySelector('[data-test="avancement-plusieurs"]')?.textContent ?? '';
    expect(texte).toContain('3');
    // Aucun intitulé d'action : les énumérer remettrait la liste qu'on retire.
    expect(hote().textContent).not.toContain('Réviser le plan de contrôle réception');
    // Et la porte vers l'endroit où elles se traitent.
    expect(hote().querySelector('[data-test="avancement-vers-ecart"]')).not.toBeNull();
    expect(hote().querySelector('[data-test="avancement-avancer"]')).toBeNull();
  });

  it('n’écrit plus le type du dossier, qui redisait la nature des actions', () => {
    setup(dossier({ type: 'PREVENTIVE' }));
    const info = hote().querySelector('.info-card')?.textContent ?? '';
    // Depuis l'ADR 0115 un dossier porte plusieurs natures : il ne peut plus
    // en afficher UNE sans mentir, et la ligne faisait doublon avec le tableau.
    expect(info).not.toContain('Préventive');
  });

  // --- le pilote, par son nom ------------------------------------------------

  it('nomme le pilote par le nom recopié à sa désignation, sans lire l’annuaire', () => {
    setup(dossier({ ownerName: 'Amina Dridi' }));
    expect(hote().querySelector('[data-test="capa-pilote"]')?.textContent?.trim()).toBe('Amina Dridi');
    expect(getByAccount).not.toHaveBeenCalled();
  });

  it('nomme le pilote d’un ancien dossier par l’annuaire, jamais par son identifiant', () => {
    annuaire = [{ id: 'i-k', accountId: '9c1f2b7e-0000-4000-8000-000000000000', displayName: 'Karim Benali',
                  email: 'karim@client.test', active: true }];
    setup(dossier());
    expect(getByAccount).toHaveBeenCalledWith('9c1f2b7e-0000-4000-8000-000000000000');
    expect(hote().querySelector('[data-test="capa-pilote"]')?.textContent?.trim()).toBe('Karim Benali');
  });

  it('affiche « — » plutôt qu’un identifiant quand le pilote est inconnu', () => {
    setup(dossier());
    expect(hote().querySelector('[data-test="capa-pilote"]')?.textContent?.trim()).toBe('—');
    expect(hote().textContent).not.toContain('9c1f2b7e');
  });

  // --- l'origine du dossier --------------------------------------------------

  it('dit la source en toutes lettres', () => {
    setup(dossier({ sourceType: 'RISK', sourceRef: 'R-014' }));
    const info = hote().querySelector('.info-card')?.textContent ?? '';
    expect(info).toContain('Risque');
    expect(info).toContain('R-014');
    expect(info).not.toContain('RISK');
  });

  it('garde l’écart d’origine visible, une fois, dans l’en-tête du dossier', () => {
    setup(dossier({
      sourceNonConformity: { id: 'nc-1', reference: 'NC-2026-0018', title: 'Étiquetage lot 4471 illisible' }
    }));
    expect(hote().querySelector('.info-card')?.textContent)
      .toContain('Étiquetage lot 4471 illisible');
  });
});

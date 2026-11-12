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

/** Le tableau des actions a son propre banc : ici, seule sa PRÉSENCE compte. */
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

  it('ne propose plus de démarrer ni de résoudre le dossier à la main', () => {
    setup(dossier({ status: 'OPEN' }));
    const texte = hote().textContent ?? '';
    expect(texte).not.toContain('Démarrer');
    expect(texte).not.toContain('Résoudre');
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

  it('confie les actions au tableau, et ne les déplie plus lui-même', () => {
    setup(dossier({ actions: [action(), action({ id: 'a2', actionType: 'PREVENTIVE' })] }));
    expect(hote().querySelector('qos-capa-action-panel')).not.toBeNull();
    // Le pavé vertical a disparu : il répétait l'en-tête ligne par ligne.
    expect(hote().querySelector('[data-test="action-unique"]')).toBeNull();
  });

  it('passe au tableau le dossier affiché, et le relit quand une action bouge', () => {
    setup();
    const panneau = fixture.debugElement.query(
      (n) => n.name === 'qos-capa-action-panel').componentInstance as PanneauStub;
    expect(panneau.capaCaseId).toBe(CASE_ID);

    capa.getCase.calls.reset();
    capa.getCase.and.returnValue(of(dossier({ status: 'RESOLVED' })));
    fixture.componentInstance.reloadCase();
    fixture.detectChanges();

    // Terminer la dernière action résout le dossier : sans cette relecture,
    // l'en-tête dirait « en cours » au-dessus d'un tableau tout fait.
    expect(capa.getCase).toHaveBeenCalled();
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

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { OpportunitySheet, RiskSheet } from '../../risk-register.types';
import { opportunite, risque } from '../../testing/risk-register.fixtures';
import { ActionDialogComponent, ActionDialogResult } from '../action-dialog/action-dialog.component';
import { ItemDetailComponent } from './item-detail.component';

/**
 * La fiche : ce qu'elle montre (cotation, CAPA liées, suivi rédigé dans la
 * langue de l'écran) et ce qu'elle permet (ouvrir une CAPA, gérer les ACT-n),
 * selon le rôle.
 */
describe('ItemDetailComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/risk-register`;
  let fixture: ComponentFixture<ItemDetailComponent>;
  let component: ItemDetailComponent;
  let http: HttpTestingController;
  let router: Router;
  let dialog: MatDialog;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const FICHE_RISQUE: RiskSheet = {
    risk: risque({ sourceId: 'nc-1', origin: 'NON_CONFORMITY', originRef: 'NC-2026-0042' }),
    capas: [{ id: 'c1', title: 'Carte SPC sur le cordon', dueDate: '2026-12-01', status: 'IN_PROGRESS',
      kind: 'CORRECTIVE', assignee: 'A. Diallo' }],
    events: [
      { id: 'e2', type: 'RATING_CHANGED', fromValue: '3x3', toValue: '4x3', detail: null, at: '2026-10-02T08:00:00Z' },
      { id: 'e1', type: 'CREATED', fromValue: null, toValue: 'NON_CONFORMITY', detail: 'NC-2026-0042', at: '2026-10-01T08:00:00Z' }
    ]
  };

  const FICHE_OPP: OpportunitySheet = {
    opportunity: opportunite(),
    actions: [{ id: 'a1', number: 3, title: 'Chiffrer le raccordement', dueDate: null, status: 'IN_PROGRESS' }],
    events: [{ id: 'e1', type: 'ACTION_OPENED', fromValue: null, toValue: 'ACT-3', detail: 'Chiffrer',
      at: '2026-10-02T08:00:00Z' }]
  };

  async function setup(kind: 'risk' | 'opportunity', roles = ['QUALITY_MANAGER']): Promise<void> {
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ItemDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { data: { kind }, paramMap: convertToParamMap({ id: kind === 'risk' ? 'r1' : 'o1' }) } } },
        // Les droits livrés : risk.manage et opportunity.manage vont au pilotage qualité.
        { provide: AuthzService, useValue: { can: () => of(roles.some(r => ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'QUALITY_DIRECTOR', 'ADMIN_TENANT', 'SUPER_ADMIN'].includes(r))) } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    dialog = TestBed.inject(MatDialog);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(ItemDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function repondre(result: ActionDialogResult | boolean | undefined): jasmine.Spy {
    return spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(result) } as MatDialogRef<unknown>);
  }

  /** La fenêtre CAPA propose les noms déjà employés : ils arrivent d'abord. */
  function suggestions(): void {
    http.expectOne(`${endpoint}/suggestions`).flush({ processes: [], sites: [], owners: ['M. Kone', 'A. Diallo'] });
  }

  afterEach(() => http.verify());

  it('montre la cotation brute et résiduelle, les CAPA liées et le suivi rédigé', async () => {
    await setup('risk');
    http.expectOne(`${endpoint}/risks/r1`).flush(FICHE_RISQUE);
    fixture.detectChanges();

    expect(hote().querySelector('[data-test="cotation-brute"]')!.textContent).toContain('4 × 3 = 12');
    expect(hote().querySelector('[data-test="cotation-residuelle"]')!.textContent).toContain('4 × 2 = 8');
    expect(hote().querySelector('[data-test="capas"]')!.textContent).toContain('Carte SPC sur le cordon');
    expect(hote().querySelector('[data-test="capas"]')!.textContent).toContain('En cours');
    expect(hote().querySelector('[data-test="capas"]')!.textContent).toContain('Corrective');
    expect(hote().querySelector('[data-test="capas"]')!.textContent).toContain('A. Diallo');
    const suivi = hote().querySelector('[data-test="suivi"]')!.textContent!;
    expect(suivi).toContain('Cotation brute passée de 3 × 3 à 4 × 3');
    expect(suivi).toContain('NC-2026-0042');
    expect(component.sourceLink).toEqual(['/nc', 'nc-1']);
    expect(component.breadcrumbs[2].label).toBe('R-014');
  });

  it('ouvre un dossier CAPA depuis la fiche et recharge', async () => {
    await setup('risk');
    http.expectOne(`${endpoint}/risks/r1`).flush(FICHE_RISQUE);
    fixture.detectChanges();
    const ouverture = repondre({ title: 'Carte SPC', description: null, dueDate: '2026-12-01', status: 'TO_START', kind: 'CORRECTIVE', assignee: 'A. Diallo' });

    (hote().querySelector('[data-test="creer-capa"]') as HTMLElement).click();
    suggestions();

    expect(ouverture.calls.mostRecent().args[0]).toBe(ActionDialogComponent);
    // Le propriétaire du risque est proposé comme responsable.
    expect(ouverture.calls.mostRecent().args[1]!.data).toEqual(jasmine.objectContaining({
      mode: 'capa', reference: 'R-014', assignee: 'M. Kone', assignees: ['M. Kone', 'A. Diallo']
    }));
    const req = http.expectOne(`${endpoint}/risks/r1/capa`);
    expect(req.request.body).toEqual({
      title: 'Carte SPC', description: null, kind: 'CORRECTIVE', assignee: 'A. Diallo', dueDate: '2026-12-01'
    });
    req.flush({ id: 'c2', title: 'Carte SPC', dueDate: '2026-12-01', status: 'OPEN', kind: 'CORRECTIVE', assignee: 'A. Diallo' });
    http.expectOne(`${endpoint}/risks/r1`).flush(FICHE_RISQUE);
  });

  it('fenêtre fermée sans saisie : rien ne part', async () => {
    await setup('risk');
    http.expectOne(`${endpoint}/risks/r1`).flush(FICHE_RISQUE);
    repondre(undefined);
    component.createAction();
    suggestions();
    http.expectNone(`${endpoint}/risks/r1/capa`);
    expect(component.busy).toBeFalse();
  });

  it('modifier mène au formulaire prérempli', async () => {
    await setup('risk');
    http.expectOne(`${endpoint}/risks/r1`).flush(FICHE_RISQUE);
    fixture.detectChanges();
    (hote().querySelector('[data-test="modifier"]') as HTMLElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'r1', 'modifier']);
  });

  it('sans rôle de pilotage, la fiche se lit sans bouton d’écriture', async () => {
    await setup('risk', ['USER']);
    http.expectOne(`${endpoint}/risks/r1`).flush(FICHE_RISQUE);
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="modifier"]')).toBeNull();
    expect(hote().querySelector('[data-test="creer-capa"]')).toBeNull();
    const spy = repondre({ title: 'Carte SPC', description: null, dueDate: '2026-12-01', status: 'TO_START', kind: 'CORRECTIVE', assignee: 'A. Diallo' });
    component.createAction();
    expect(spy).not.toHaveBeenCalled();
  });

  it('une fiche introuvable le dit', async () => {
    await setup('risk');
    http.expectOne(`${endpoint}/risks/r1`).flush('x', { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
  });

  it('fiche opportunité : score, actions ACT-n, ajout, modification et suppression confirmée', async () => {
    await setup('opportunity');
    http.expectOne(`${endpoint}/opportunities/o1`).flush(FICHE_OPP);
    fixture.detectChanges();

    expect(hote().querySelector('[data-test="score"]')!.textContent).toContain('4 × 4 = 16');
    expect(hote().querySelector('[data-test="actions"]')!.textContent).toContain('ACT-3');
    expect(hote().querySelector('[data-test="suivi"]')!.textContent).toContain('Action ACT-3 ouverte : Chiffrer');
    expect(component.gainWord(4)).toBe('Fort');
    expect(component.feasibilityWord(4)).toBe('Facile');

    const spy = repondre({ title: 'Piloter', description: null, dueDate: null, status: 'TO_START', kind: null, assignee: null });
    component.createAction();
    http.expectOne(`${endpoint}/opportunities/o1/actions`).flush({ id: 'a2', number: 4, title: 'Piloter', dueDate: null, status: 'TO_START' });
    http.expectOne(`${endpoint}/opportunities/o1`).flush(FICHE_OPP);

    spy.and.returnValue({ afterClosed: () => of({ title: 'Chiffrer', description: null, dueDate: null, status: 'DONE', kind: null, assignee: null }) } as MatDialogRef<unknown>);
    component.editAction(FICHE_OPP.actions[0]);
    const revue = http.expectOne(`${endpoint}/opportunities/o1/actions/a1`);
    expect(revue.request.method).toBe('PUT');
    expect(revue.request.body.status).toBe('DONE');
    revue.flush(FICHE_OPP.actions[0]);
    http.expectOne(`${endpoint}/opportunities/o1`).flush(FICHE_OPP);

    spy.and.returnValue({ afterClosed: () => of(true) } as MatDialogRef<unknown>);
    component.deleteAction(FICHE_OPP.actions[0]);
    const suppression = http.expectOne(`${endpoint}/opportunities/o1/actions/a1`);
    expect(suppression.request.method).toBe('DELETE');
    suppression.flush(null);
    http.expectOne(`${endpoint}/opportunities/o1`).flush(FICHE_OPP);

    spy.and.returnValue({ afterClosed: () => of(false) } as MatDialogRef<unknown>);
    component.deleteAction(FICHE_OPP.actions[0]);
    http.expectNone(`${endpoint}/opportunities/o1/actions/a1`);

    component.edit();
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'opportunites', 'o1', 'modifier']);
  });

  it('une erreur d’ouverture de CAPA est signalée, sans recharger', async () => {
    await setup('risk');
    http.expectOne(`${endpoint}/risks/r1`).flush(FICHE_RISQUE);
    repondre({ title: 'Carte SPC', description: null, dueDate: '2026-12-01', status: 'TO_START', kind: 'CORRECTIVE', assignee: 'A. Diallo' });
    component.createAction();
    suggestions();
    http.expectOne(`${endpoint}/risks/r1/capa`).flush({ field: 'status' }, { status: 422, statusText: 'KO' });
    expect(component.busy).toBeFalse();
  });

  it('le lien vers la source ne vaut que pour une NC ou un changement', async () => {
    await setup('risk');
    http.expectOne(`${endpoint}/risks/r1`).flush({ ...FICHE_RISQUE, risk: risque({ sourceId: 'ch-1', origin: 'CHANGE' }) });
    expect(component.sourceLink).toEqual(['/changes', 'ch-1']);
    component.risk = { ...FICHE_RISQUE, risk: risque({ sourceId: 'i-1', origin: 'FMEA' }) };
    expect(component.sourceLink).toBeNull();
    component.risk = { ...FICHE_RISQUE, risk: risque({ sourceId: null }) };
    expect(component.sourceLink).toBeNull();
    expect(component.bar(3)).toBe(60);
    expect(component.bar(null)).toBe(0);
  });
});

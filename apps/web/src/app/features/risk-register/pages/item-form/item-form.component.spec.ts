import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { RiskDraft } from '../../risk-register.types';
import { opportunite, risque } from '../../testing/risk-register.fixtures';
import { ItemFormComponent } from './item-form.component';

/**
 * Le formulaire : création (directe ou depuis un objet source), modification,
 * aperçu du niveau, garde de la résiduelle, 422 accroché au bon champ.
 */
describe('ItemFormComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/risk-register`;
  let fixture: ComponentFixture<ItemFormComponent>;
  let component: ItemFormComponent;
  let http: HttpTestingController;
  let router: Router;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  interface Options {
    kind?: 'risk' | 'opportunity';
    id?: string | null;
    query?: Record<string, string>;
    roles?: string[];
  }

  async function setup(o: Options = {}): Promise<void> {
    const roles = o.roles ?? ['QUALITY_MANAGER'];
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ItemFormComponent],
      imports: [SharedModule, UiModule, MatAutocompleteModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute, useValue: {
            snapshot: {
              data: { kind: o.kind ?? 'risk' },
              paramMap: convertToParamMap(o.id ? { id: o.id } : {}),
              queryParamMap: convertToParamMap(o.query ?? {})
            }
          }
        },
        { provide: AuthService, useValue: { hasAnyRole: (a: string[]) => a.some(r => roles.includes(r)) } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(ItemFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne(`${endpoint}/suggestions`).flush({ processes: ['Production', 'Achats'], sites: [], owners: [] });
  }

  function remplir(): void {
    component.form.patchValue({ title: '  Dérive soudure ', process: 'Production', owner: 'M. Kone' });
  }

  afterEach(() => http.verify());

  it('crée un risque : seuls les champs de création partent, sans tenant ni référence', async () => {
    await setup();
    fixture.detectChanges();
    remplir();
    component.form.patchValue({ grossSeverity: 4, grossProbability: 3, residualSeverity: 1, residualProbability: 1 });

    component.submit();

    const req = http.expectOne(`${endpoint}/risks`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Dérive soudure');
    expect(req.request.body.grossSeverity).toBe(4);
    expect(req.request.body.residualSeverity).toBeNull();
    expect(req.request.body.status).toBeUndefined();
    expect(req.request.body.requirements).toEqual(['ISO_9001_6_1']);
    expect(req.request.body.tenantId).toBeUndefined();
    expect(req.request.body.reference).toBeUndefined();
    req.flush(risque({ id: 'nouveau-id' }));
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'nouveau-id']);
  });

  it('montre le niveau avant l’enregistrement, mêmes seuils que le serveur', async () => {
    await setup();
    component.form.patchValue({ grossSeverity: 5, grossProbability: 3 });
    fixture.detectChanges();
    expect(component.grossScore).toBe(15);
    expect(component.grossLevel).toBe('CRITICAL');
    expect(hote().querySelector('[data-test="apercu-niveau"]')!.textContent).toContain('15');
  });

  it('un formulaire incomplet ne part pas', async () => {
    await setup();
    component.submit();
    expect(component.form.get('title')!.touched).toBeTrue();
    http.expectNone(`${endpoint}/risks`);
  });

  it('coche et décoche les exigences', async () => {
    await setup();
    component.toggleRequirement('IATF_16949_6_1_2', true);
    component.toggleRequirement('IATF_16949_6_1_2', true);
    expect(component.form.get('requirements')!.value).toEqual(['ISO_9001_6_1', 'IATF_16949_6_1_2']);
    component.toggleRequirement('ISO_9001_6_1', false);
    expect(component.hasRequirement('ISO_9001_6_1')).toBeFalse();
    expect(component.requirements.map(r => r.value)).toContain('IATF_16949_6_1_2');
  });

  it('propose les processus déjà employés, filtrés par la saisie', async () => {
    await setup();
    component.form.patchValue({ process: 'ach' });
    expect(component.filtered(component.suggestions.processes, 'process')).toEqual(['Achats']);
  });

  it('modifie un risque : la fiche prérempli, la résiduelle et le suivi partent', async () => {
    await setup({ id: 'r1' });
    http.expectOne(`${endpoint}/risks/r1`).flush({ risk: risque(), capas: [], events: [] });
    fixture.detectChanges();

    expect(component.form.value.title).toBe('Dérive du procédé de soudure');
    expect(component.reference).toBe('R-014');
    expect(hote().querySelector('[data-test="gravite-residuelle"]')).not.toBeNull();
    expect(component.breadcrumbs.map(b => b.label)).toContain('R-014');

    component.submit();
    const req = http.expectOne(`${endpoint}/risks/r1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.residualSeverity).toBe(4);
    expect(req.request.body.status).toBe('IN_TREATMENT');
    expect(req.request.body.nextReviewOn).toBe('2027-01-15');
    req.flush(risque());
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'r1']);
  });

  it('refuse une résiduelle au-dessus de la brute avant d’envoyer', async () => {
    await setup({ id: 'r1' });
    http.expectOne(`${endpoint}/risks/r1`).flush({ risk: risque(), capas: [], events: [] });
    component.form.patchValue({ grossSeverity: 2, grossProbability: 2, residualSeverity: 3, residualProbability: 3 });
    fixture.detectChanges();

    expect(component.residualTooHigh).toBeTrue();
    expect(hote().querySelector('[data-test="residuelle-trop-haute"]')).not.toBeNull();
    component.submit();
    http.expectNone(`${endpoint}/risks/r1`);
  });

  it('accroche un 422 au champ que le serveur désigne', async () => {
    await setup();
    remplir();
    component.submit();
    http.expectOne(`${endpoint}/risks`).flush(
      { field: 'title', detail: 'Ce champ dépasse 255 caractères.' },
      { status: 422, statusText: 'Unprocessable Entity' });
    expect(component.serverError('title')).toBe('Ce champ dépasse 255 caractères.');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('depuis une AMDEC : le brouillon prérempli, l’origine est figée et la source part avec le risque', async () => {
    await setup({ query: { origine: 'FMEA', source: 'item-9' } });
    const draft: RiskDraft = {
      origin: 'FMEA', sourceId: 'item-9', originRef: 'PFMEA-7 #3', title: 'Cordon poreux', cause: 'Buse usée',
      effect: 'Fuite', grossSeverity: 5, grossProbability: 3, process: null,
      existing: [{ id: 'r1', reference: 'R-014' }]
    } as RiskDraft;
    http.expectOne(`${endpoint}/sources/FMEA/item-9`).flush({ ...draft, eligible: true, reason: null });
    fixture.detectChanges();

    expect(component.form.getRawValue().title).toBe('Cordon poreux');
    expect(component.form.get('origin')!.disabled).toBeTrue();
    expect(hote().querySelector('[data-test="source"]')!.textContent).toContain('PFMEA-7 #3');
    expect(hote().querySelector('[data-test="source-existants"]')!.textContent).toContain('R-014');

    component.form.patchValue({ process: 'Soudage', owner: 'M. Kone' });
    component.submit();
    const req = http.expectOne(`${endpoint}/risks`);
    expect(req.request.body.sourceId).toBe('item-9');
    expect(req.request.body.origin).toBe('FMEA');
    expect(req.request.body.originRef).toBe('PFMEA-7 #3');
    req.flush(risque());
  });

  it('une ligne sous le seuil est annoncée, et l’enregistrement bloqué', async () => {
    await setup({ query: { origine: 'FMEA', source: 'item-9' } });
    http.expectOne(`${endpoint}/sources/FMEA/item-9`).flush({
      origin: 'FMEA', sourceId: 'item-9', originRef: 'PFMEA-7 #3', title: 'x', cause: null, effect: null,
      grossSeverity: 1, grossProbability: 1, process: null, eligible: false, reason: 'BELOW_THRESHOLD', existing: []
    });
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="source-non-eligible"]')!.textContent).toContain('seuil');
    expect((hote().querySelector('[data-test="enregistrer"]') as HTMLButtonElement).disabled).toBeTrue();
    remplir();
    component.submit();
    http.expectNone(`${endpoint}/risks`);
  });

  it('une origine inconnue dans l’adresse est ignorée', async () => {
    await setup({ query: { origine: 'PIRATE', source: 'x' } });
    expect(component.draft).toBeNull();
    expect(component.originLocked).toBeFalse();
  });

  it('une source introuvable laisse un message et pas de formulaire', async () => {
    await setup({ query: { origine: 'NON_CONFORMITY', source: 'nc-x' } });
    http.expectOne(`${endpoint}/sources/NON_CONFORMITY/nc-x`).flush('x', { status: 404, statusText: 'Not Found' });
    expect(component.failed).toBeTrue();
  });

  it('crée une opportunité avec son échéance visée et ses origines propres', async () => {
    await setup({ kind: 'opportunity' });
    fixture.detectChanges();
    expect(component.origins.map(o => o.value)).not.toContain('FMEA');
    expect(component.requirements.map(r => r.value)).toContain('ISO_9001_10_3');
    component.form.patchValue({ title: 'Automatiser', process: 'Production', owner: 'Mme Diallo',
      targetDate: '2027-03-31', gain: 5, feasibility: 3 });
    expect(component.grossLevel).toBe('PRIORITY');

    component.submit();
    const req = http.expectOne(`${endpoint}/opportunities`);
    expect(req.request.body.targetDate).toBe('2027-03-31');
    expect(req.request.body.gain).toBe(5);
    req.flush(opportunite({ id: 'o9' }));
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'opportunites', 'o9']);
  });

  it('modifie une opportunité', async () => {
    await setup({ kind: 'opportunity', id: 'o1' });
    http.expectOne(`${endpoint}/opportunities/o1`).flush({ opportunity: opportunite(), actions: [], events: [] });
    component.submit();
    const req = http.expectOne(`${endpoint}/opportunities/o1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.status).toBe('UNDER_STUDY');
    req.flush(opportunite());
  });

  it('en lecture seule, le dit et n’envoie rien', async () => {
    await setup({ roles: ['USER'] });
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="lecture-seule"]')).not.toBeNull();
    remplir();
    component.submit();
    http.expectNone(`${endpoint}/risks`);
  });

  it('annuler revient d’où l’on vient', async () => {
    await setup();
    component.cancel();
    expect(router.navigate).toHaveBeenCalledWith(['/risques']);

    await setup({ kind: 'opportunity', id: 'o1' });
    http.expectOne(`${endpoint}/opportunities/o1`).flush({ opportunity: opportunite(), actions: [], events: [] });
    component.cancel();
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'opportunites', 'o1']);
  });
});

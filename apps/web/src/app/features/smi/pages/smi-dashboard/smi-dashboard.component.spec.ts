import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SmiStandardsOpener } from '../../smi-standards-opener.service';
import { dueText, standardCodeLabel, standardShortName, upcomingLink } from '../../smi.labels';
import { SmiDashboard, StandardRef } from '../../smi.types';
import { SmiDashboardComponent } from './smi-dashboard.component';

const VIDE = [0, 1, 2, 3, 4].map(() => [0, 0, 0, 0, 0]);

export function tableau(o: Partial<SmiDashboard> = {}): SmiDashboard {
  const brute = VIDE.map(l => [...l]);
  brute[3][2] = 2;   // gravité 4 × probabilité 3
  brute[0][0] = 5;   // gravité 1 × probabilité 1
  const residuelle = VIDE.map(l => [...l]);
  residuelle[3][1] = 1;
  return {
    standards: [
      { adoptionId: 'a1', code: 'iso-9001', name: 'ISO 9001' },
      { adoptionId: 'a2', code: 'iso-45001', name: 'ISO 45001' }
    ],
    selected: null,
    compliance: { global: 86, perStandard: [
      { code: 'iso-9001', name: 'ISO 9001', score: 91 }, { code: 'iso-45001', name: 'ISO 45001', score: 78 }
    ] },
    overdueActions: { total: 12, critical: 3 },
    majorRisks: { total: 9, critical: 3, high: 6 },
    riskMatrix: { open: 7, gross: brute, residual: residuelle },
    nextAudit: { id: 'au1', reference: 'AUD-2026-004', title: 'Audit interne ISO 14001', type: 'INTERNAL',
      standard: 'ISO 14001', scheduledOn: '2026-11-03', daysUntil: 26 },
    thisWeek: [
      { kind: 'CHANGE', targetId: 'ch1', reference: 'MOC-7', title: 'Validation MOC', dueOn: '2026-10-07', daysLeft: -1 },
      { kind: 'CALIBRATION', targetId: 'eq1', reference: 'PC-118', title: 'Étalonnage pied à coulisse',
        dueOn: '2026-10-10', daysLeft: 2 }
    ],
    ...o
  };
}

describe('SmiDashboardComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/smi/dashboard`;
  let fixture: ComponentFixture<SmiDashboardComponent>;
  let component: SmiDashboardComponent;
  let http: HttpTestingController;
  let router: Router;
  let query$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let opener: jasmine.SpyObj<SmiStandardsOpener>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const texte = (sel: string): string => hote().querySelector(sel)?.textContent ?? '';

  async function setup(params: Record<string, string> = {}, admin = false): Promise<void> {
    query$ = new BehaviorSubject(convertToParamMap(params));
    opener = jasmine.createSpyObj<SmiStandardsOpener>('SmiStandardsOpener', ['canAdd', 'open']);
    opener.canAdd.and.returnValue(admin);
    await TestBed.configureTestingModule({
      declarations: [SmiDashboardComponent],
      imports: [SharedModule, UiModule, MatButtonToggleModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: query$ } },
        { provide: SmiStandardsOpener, useValue: opener }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(SmiDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // Certains tests ne montent pas le composant : rien à vérifier pour eux, et on
  // ne vérifie jamais le contrôleur HTTP laissé par le test précédent.
  afterEach(() => {
    http?.verify();
    http = undefined as unknown as HttpTestingController;
  });

  it('montre les quatre indicateurs, la conformité par norme et la semaine', async () => {
    await setup();
    http.expectOne(endpoint).flush(tableau());
    fixture.detectChanges();

    expect(texte('[data-test="kpi-conformite"]')).toContain('86');
    expect(texte('[data-test="kpi-actions"]')).toContain('12');
    expect(texte('[data-test="kpi-actions"]')).toContain('3');
    expect(texte('[data-test="kpi-risques"]')).toContain('9');
    expect(texte('[data-test="kpi-audit"]')).toContain('Audit interne ISO 14001');
    expect(texte('[data-test="conformite-normes"]')).toContain('ISO 45001');
    expect(texte('[data-test="matrice-risques"]')).toContain('7');
    expect(texte('[data-test="semaine"]')).toContain('Validation MOC');
    expect(texte('[data-test="semaine"]')).toContain('Échu');
    expect(hote().querySelector('[data-test="lien-exigences"]')).not.toBeNull();
  });

  it('chaque case porte son nombre de risques et mène à la liste de la case', async () => {
    await setup();
    http.expectOne(endpoint).flush(tableau());
    fixture.detectChanges();

    // Gravité 5 en haut, probabilité 1 à gauche, comme la maquette.
    expect(component.grid[0][0]).toEqual(jasmine.objectContaining({ severity: 5, probability: 1 }));
    expect(component.grid[1][2]).toEqual(jasmine.objectContaining({ severity: 4, probability: 3, count: 2, level: 'HIGH' }));
    expect(component.grid[4][0]).toEqual(jasmine.objectContaining({ count: 5, level: 'LOW' }));
    const case43 = hote().querySelector('[data-test="case-4-3"]') as HTMLAnchorElement;
    expect(case43.textContent!.trim()).toBe('2');
    expect(case43.getAttribute('aria-label')).toContain('Élevé');
    expect(case43.getAttribute('href')).toContain('/smi/risques?g=4&p=3&vue=gross');
    expect(component.placed).toBe(7);
  });

  it('la bascule résiduelle relit la grille et signale les risques non placés', async () => {
    await setup();
    http.expectOne(endpoint).flush(tableau());
    fixture.detectChanges();

    component.setView('residual');
    fixture.detectChanges();

    expect(component.grid[1][1].count).toBe(1);
    expect(component.placed).toBe(1);
    expect(texte('[data-test="hors-residuelle"]')).toContain('6');
    expect(component.cellQuery(component.grid[1][1])).toEqual({ g: 4, p: 2, vue: 'residual' });
  });

  it('le filtre de norme passe par l’adresse et se recharge', async () => {
    await setup({ norme: 'iso-45001' });
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('standard')).toBe('iso-45001');
    req.flush(tableau({ selected: 'iso-45001' }));
    fixture.detectChanges();

    expect(component.standard).toBe('iso-45001');
    expect(component.cellQuery(component.grid[0][0])['norme']).toBe('iso-45001');
    expect(hote().querySelector('[data-test="norme-iso-45001"]')!.getAttribute('aria-pressed')).toBe('true');

    (hote().querySelector('[data-test="norme-tous"]') as HTMLButtonElement).click();
    expect(router.navigate).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: { norme: null } }));

    query$.next(convertToParamMap({}));
    const tous = http.expectOne(r => r.url === endpoint);
    expect(tous.request.params.has('standard')).toBeFalse();
    tous.flush(tableau());
  });

  it('une erreur s’affiche, et réessayer recharge la même norme', async () => {
    await setup({ norme: 'iso-9001' });
    http.expectOne(r => r.url === endpoint).flush('x', { status: 503, statusText: 'KO' });
    fixture.detectChanges();
    expect(component.error).toBeTruthy();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();

    component.reload();
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('standard')).toBe('iso-9001');
    req.flush(tableau({ selected: 'iso-9001' }));
    expect(component.error).toBeNull();
  });

  it('sans norme ni audit, les indicateurs le disent au lieu d’afficher zéro', async () => {
    await setup();
    http.expectOne(endpoint).flush(tableau({ standards: [], compliance: null, nextAudit: null, thisWeek: [] }));
    fixture.detectChanges();

    expect(texte('[data-test="kpi-conformite"]')).toContain('Aucune norme adoptée');
    expect(texte('[data-test="kpi-audit"]')).toContain('Aucun audit planifié');
    expect(texte('[data-test="semaine"]')).toContain('sept jours');
    expect(hote().querySelector('[data-test="norme-tous"]')).toBeNull();
  });

  it('libellés et liens des échéances', () => {
    expect(dueText(-2)).toBe('Échu');
    expect(dueText(0)).toBe('Aujourd\'hui');
    expect(dueText(3)).toBe('J-3');
    expect(upcomingLink('CAPA_ACTION', 'x')).toEqual(['/capa', 'x']);
    expect(upcomingLink('CALIBRATION', 'x')).toEqual(['/calibration', 'x']);
    expect(upcomingLink('CHANGE', 'x')).toEqual(['/changes', 'x']);
    expect(upcomingLink('RISK_REVIEW', 'x')).toEqual(['/risques', 'x']);
    expect(upcomingLink('AUDIT', 'x')).toEqual(['/audits', 'x']);
  });

  it('borne la largeur des barres', async () => {
    await setup();
    http.expectOne(endpoint).flush(tableau());
    expect(component.barWidth(140)).toBe(100);
    expect(component.barWidth(-3)).toBe(0);
    expect(component.barWidth(null)).toBe(0);
  });

  // ---------- plusieurs normes, et l'ajout d'une norme ----------

  const SMI_9001_14001 = (): SmiDashboard => tableau({
    standards: [
      { adoptionId: 'a1', code: 'iso-9001', name: 'ISO 9001:2015 — Systèmes de management de la qualité — Exigences' },
      { adoptionId: 'a3', code: 'iso-14001', name: 'ISO 14001:2015 — Systèmes de management environnemental' }
    ],
    compliance: { global: 34, perStandard: [
      { code: 'iso-9001', name: 'ISO 9001:2015 — Systèmes de management de la qualité — Exigences', score: 57 },
      { code: 'iso-14001', name: 'ISO 14001:2015 — Systèmes de management environnemental', score: 10 }
    ] }
  });

  it('ISO 9001 et ISO 14001 se lisent côte à côte, sous leur nom court', async () => {
    await setup();
    http.expectOne(endpoint).flush(SMI_9001_14001());
    fixture.detectChanges();

    const puce = hote().querySelector('[data-test="norme-iso-14001"]') as HTMLButtonElement;
    expect(puce.textContent!.trim()).toBe('ISO 14001:2015');
    // Le titre complet reste lisible au survol.
    expect(puce.getAttribute('title')).toContain('environnemental');
    expect(texte('[data-test="conformite-normes"]')).toContain('ISO 9001:2015');
    expect(texte('[data-test="conformite-normes"]')).toContain('10');
    expect(texte('[data-test="kpi-conformite"]')).toContain('34');
  });

  it('sans le rôle d’administrateur, ni bouton ni fenêtre : le paramétrage revient à l’administrateur', async () => {
    await setup({}, false);
    http.expectOne(endpoint).flush(tableau({ standards: [], compliance: null }));
    fixture.detectChanges();

    expect(component.canAddStandard).toBeFalse();
    expect(hote().querySelector('[data-test="ajouter-norme"]')).toBeNull();
    expect(hote().querySelector('[data-test="kpi-ajouter-norme"]')).toBeNull();
    expect(texte('[data-test="kpi-conformite"]')).toContain('administrateur');
  });

  it('l’administrateur ajoute une norme, et le tableau se recharge sur toutes les normes', async () => {
    await setup({}, true);
    http.expectOne(endpoint).flush(tableau());
    fixture.detectChanges();
    const ajoutee: StandardRef = { adoptionId: 'a3', code: 'iso-14001', name: 'ISO 14001:2015 — SME' };
    opener.open.and.returnValue(of(ajoutee));

    (hote().querySelector('[data-test="ajouter-norme"]') as HTMLButtonElement).click();

    expect(opener.open).toHaveBeenCalled();
    http.expectOne(r => r.url === endpoint && !r.params.has('standard')).flush(SMI_9001_14001());
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="norme-iso-14001"]')).not.toBeNull();
  });

  it('ajouter depuis une vue filtrée revient à « Tous » ; fermer sans ajouter ne recharge rien', async () => {
    await setup({ norme: 'iso-9001' }, true);
    http.expectOne(r => r.url === endpoint).flush(tableau({ selected: 'iso-9001' }));
    fixture.detectChanges();

    opener.open.and.returnValue(of(undefined));
    component.addStandard();
    expect(router.navigate).not.toHaveBeenCalled();

    opener.open.and.returnValue(of({ adoptionId: 'a3', code: 'iso-14001', name: 'ISO 14001' }));
    component.addStandard();
    expect(router.navigate).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: { norme: null } }));
  });

  it('sans norme, l’administrateur ajoute la première depuis l’indicateur de conformité', async () => {
    await setup({}, true);
    http.expectOne(endpoint).flush(tableau({ standards: [], compliance: null }));
    fixture.detectChanges();
    opener.open.and.returnValue(of(undefined));

    (hote().querySelector('[data-test="kpi-ajouter-norme"]') as HTMLButtonElement).click();

    expect(opener.open).toHaveBeenCalled();
    // Pas encore de norme : pas de filtre, mais le bouton d'ajout reste là.
    expect(hote().querySelector('[data-test="norme-tous"]')).toBeNull();
    expect(hote().querySelector('[data-test="ajouter-norme"]')).not.toBeNull();
  });

  it('nom court d’une norme : le titre avant le tiret long, sinon le code', () => {
    expect(standardShortName('ISO 45001:2018 — Santé et sécurité au travail', 'iso-45001')).toBe('ISO 45001:2018');
    expect(standardShortName('ISO 9001', 'iso-9001')).toBe('ISO 9001');
    expect(standardShortName('', 'iso-22301')).toBe('ISO 22301');
    expect(standardShortName(null, 'iso-20000-1')).toBe('ISO 20000-1');
    expect(standardCodeLabel('iatf-16949')).toBe('IATF 16949');
  });
});

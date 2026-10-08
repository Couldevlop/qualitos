import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { dueText, upcomingLink } from '../../smi.labels';
import { SmiDashboard } from '../../smi.types';
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

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const texte = (sel: string): string => hote().querySelector(sel)?.textContent ?? '';

  async function setup(params: Record<string, string> = {}): Promise<void> {
    query$ = new BehaviorSubject(convertToParamMap(params));
    await TestBed.configureTestingModule({
      declarations: [SmiDashboardComponent],
      imports: [SharedModule, UiModule, MatButtonToggleModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: query$ } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(SmiDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

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
});

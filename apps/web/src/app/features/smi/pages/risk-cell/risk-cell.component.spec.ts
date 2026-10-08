import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { RiskCapaOpener } from '../../../risk-register/risk-capa-opener.service';
import { risque } from '../../../risk-register/testing/risk-register.fixtures';
import { RiskSheet, RiskView } from '../../../risk-register/risk-register.types';
import { RiskCellComponent } from './risk-cell.component';

describe('RiskCellComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/risk-register`;
  let fixture: ComponentFixture<RiskCellComponent>;
  let component: RiskCellComponent;
  let http: HttpTestingController;
  let router: Router;
  let opener: jasmine.SpyObj<RiskCapaOpener>;
  let query$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  // Deux risques en 4 × 3 (brut), un en 4 × 2 (résiduel de R-014), un clos, un d'une autre norme.
  const REGISTRE: RiskView[] = [
    risque({ id: 'r1', reference: 'R-014', process: 'Production', site: 'Usine A' }),
    risque({ id: 'r2', reference: 'R-031', title: 'Fuite de liquide', type: 'ENVIRONMENT', process: 'Maintenance',
      site: 'Usine B', residualSeverity: null, residualProbability: null, residualScore: null, residualLevel: null,
      requirements: ['ISO_14001_6_1'] }),
    risque({ id: 'r3', reference: 'R-040', status: 'CLOSED' }),
    risque({ id: 'r4', reference: 'R-050', grossSeverity: 1, grossProbability: 1, grossScore: 1 })
  ];

  const fiche = (r: RiskView, capas: RiskSheet['capas'] = []): RiskSheet => ({ risk: r, capas, events: [] });

  async function setup(params: Record<string, string>, roles = ['QUALITY_MANAGER']): Promise<void> {
    query$ = new BehaviorSubject(convertToParamMap(params));
    opener = jasmine.createSpyObj<RiskCapaOpener>('RiskCapaOpener', ['open']);
    await TestBed.configureTestingModule({
      declarations: [RiskCellComponent],
      imports: [SharedModule, UiModule, FormsModule, MatButtonToggleModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: query$ } },
        { provide: RiskCapaOpener, useValue: opener },
        { provide: AuthService, useValue: { hasAnyRole: (a: string[]) => a.some(r => roles.includes(r)) } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(RiskCellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /** Charge le registre, puis répond aux lectures de fiches (compte des CAPA, détail). */
  function charger(capasR1: RiskSheet['capas'] = []): void {
    http.expectOne(`${endpoint}/risks`).flush(REGISTRE);
    for (const req of http.match(r => /\/risks\/r\d$/.test(r.url))) {
      const id = req.request.url.split('/').pop()!;
      const r = REGISTRE.find(x => x.id === id)!;
      req.flush(fiche(r, id === 'r1' ? capasR1 : []));
    }
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('liste les risques ouverts de la case, avec leurs actions en cours, et détaille le premier', async () => {
    await setup({ g: '4', p: '3' });
    charger([{ id: 'c1', title: 'Carte SPC', dueDate: null, status: 'IN_PROGRESS', kind: 'PREVENTIVE', assignee: 'A' },
      { id: 'c2', title: 'Ancienne', dueDate: null, status: 'CLOSED', kind: 'PREVENTIVE', assignee: null }]);

    expect(component.rows.map(r => r.reference)).toEqual(['R-014', 'R-031']);
    expect(component.cellTitle).toContain('2');
    expect(component.cellTitle).toContain('(12)');
    expect(component.capaCounts['r1']).toBe(1);
    expect(hote().querySelectorAll('[data-test="ligne-risque"]').length).toBe(2);
    const detail = hote().querySelector('[data-test="detail"]')!.textContent!;
    expect(detail).toContain('R-014');
    expect(detail).toContain('4 × 3 = 12');
    expect(detail).toContain('4 × 2 = 8');
    expect(detail).toContain('Carte SPC');
    expect(component.activeCapas).toBe(1);
    expect(component.breadcrumbs[2].label).toContain('4');
  });

  it('filtre par type, processus et site, sans quitter la case', async () => {
    await setup({ g: '4', p: '3' });
    charger();
    expect(component.processes).toEqual(['Maintenance', 'Production']);
    expect(component.sites).toEqual(['Usine A', 'Usine B']);

    component.typeFilter = 'ENVIRONMENT';
    component.onFilterChange();
    http.match(r => /\/risks\/r\d$/.test(r.url)).forEach(r => r.flush(fiche(REGISTRE[1])));
    expect(component.rows.map(r => r.reference)).toEqual(['R-031']);
    expect(component.selected!.risk.id).toBe('r2');

    component.typeFilter = '';
    component.processFilter = 'Production';
    component.siteFilter = 'Usine B';
    component.onFilterChange();
    fixture.detectChanges();
    expect(component.rows).toEqual([]);
    expect(component.selected).toBeNull();
    expect(hote().textContent).toContain('Aucune ligne ne correspond aux filtres');
  });

  it('en résiduel, la case se lit sur la cotation visée ; la norme filtre par exigence', async () => {
    await setup({ g: '4', p: '2', vue: 'residual' });
    charger();
    // R-031 n'a pas de résiduelle visée : il n'est dans aucune case résiduelle.
    expect(component.rows.map(r => r.reference)).toEqual(['R-014', 'R-050']);

    query$.next(convertToParamMap({ g: '4', p: '3', norme: 'iso-14001' }));
    http.match(r => /\/risks\/r\d$/.test(r.url)).forEach(r => r.flush(fiche(REGISTRE[1])));
    expect(component.view).toBe('gross');
    expect(component.rows.map(r => r.reference)).toEqual(['R-031']);

    component.setView('residual');
    expect(router.navigate).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: { vue: 'residual' } }));
  });

  it('une case vide le dit ; des notes hors bornes retombent à 1', async () => {
    await setup({ g: '9', p: 'x' });
    http.expectOne(`${endpoint}/risks`).flush(REGISTRE);
    http.match(r => /\/risks\/r\d$/.test(r.url)).forEach(r => r.flush(fiche(REGISTRE[3])));
    fixture.detectChanges();
    expect(component.severity).toBe(1);
    expect(component.probability).toBe(1);
    expect(component.rows.map(r => r.reference)).toEqual(['R-050']);

    query$.next(convertToParamMap({ g: '5', p: '5' }));
    fixture.detectChanges();
    expect(hote().textContent).toContain('Aucun risque ouvert dans cette case');
    expect(component.selected).toBeNull();
  });

  it('crée une action CAPA depuis le détail puis relit la fiche', async () => {
    await setup({ g: '4', p: '3' });
    charger();
    opener.open.and.returnValue(of({ id: 'c9', title: 'x', dueDate: '2026-12-01', status: 'OPEN', kind: 'CORRECTIVE', assignee: 'A' }));

    (hote().querySelector('[data-test="creer-capa"]') as HTMLButtonElement).click();

    expect(opener.open).toHaveBeenCalledWith({ id: 'r1', reference: 'R-014', owner: 'M. Kone' });
    http.expectOne(`${endpoint}/risks/r1`).flush(fiche(REGISTRE[0]));
    expect(component.busy).toBeFalse();

    opener.open.and.returnValue(of(null));
    component.createCapa();
    http.expectNone(`${endpoint}/risks/r1`);

    opener.open.and.returnValue(throwError(() => new Error('422')));
    component.createCapa();
    expect(component.busy).toBeFalse();
  });

  it('ouvrir la fiche risque ; sans rôle de pilotage, pas de bouton CAPA', async () => {
    await setup({ g: '4', p: '3' }, ['USER']);
    charger();
    expect(hote().querySelector('[data-test="creer-capa"]')).toBeNull();
    component.createCapa();
    expect(opener.open).not.toHaveBeenCalled();

    (hote().querySelector('[data-test="ouvrir-fiche"]') as HTMLButtonElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'r1']);
  });

  it('un registre indisponible s’annonce et se recharge', async () => {
    await setup({ g: '4', p: '3' });
    http.expectOne(`${endpoint}/risks`).flush('x', { status: 503, statusText: 'KO' });
    fixture.detectChanges();
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();

    component.charger();
    charger();
    expect(component.failed).toBeFalse();
  });

  it('une fiche illisible n’empêche pas de compter les autres', async () => {
    await setup({ g: '4', p: '3' });
    http.expectOne(`${endpoint}/risks`).flush(REGISTRE);
    for (const req of http.match(r => /\/risks\/r\d$/.test(r.url))) {
      req.flush('x', { status: 500, statusText: 'KO' });
    }
    expect(component.capaCounts['r1']).toBeUndefined();
    expect(component.detailLoading).toBeFalse();
  });
});

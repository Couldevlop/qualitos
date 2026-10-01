import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { OpportunityView, RiskView } from '../../risk-register.types';
import { opportunite, risque } from '../../testing/risk-register.fixtures';
import { RegisterComponent } from './register.component';

/**
 * Le registre : deux onglets reliés, recherche, filtres, tri par cotation,
 * export CSV sans injection de formule, bouton de création selon le rôle.
 */
describe('RegisterComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/risk-register`;
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let http: HttpTestingController;
  let router: Router;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const RISQUES: RiskView[] = [
    risque(),
    risque({ id: 'r2', reference: 'R-007', title: 'Instrument hors étalonnage', process: 'Métrologie',
      grossSeverity: 3, grossProbability: 5, grossScore: 15, grossLevel: 'CRITICAL', residualScore: 6,
      residualLevel: 'MEDIUM', status: 'TO_TREAT' }),
    risque({ id: 'r3', reference: 'R-060', title: 'Fatigue visuelle', type: 'HEALTH_SAFETY', process: 'Qualité',
      grossSeverity: 2, grossProbability: 2, grossScore: 4, grossLevel: 'LOW', residualSeverity: null,
      residualProbability: null, residualScore: null, residualLevel: null, status: 'ACCEPTED' })
  ];

  async function setup(tab: 'risks' | 'opportunities' = 'risks', roles = ['QUALITY_MANAGER']): Promise<void> {
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [RegisterComponent],
      imports: [SharedModule, UiModule, FormsModule, MatButtonToggleModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { data: { tab } } } },
        { provide: AuthService, useValue: { hasAnyRole: (a: string[]) => a.some(r => roles.includes(r)) } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function servir(risks: RiskView[] = RISQUES, opps: OpportunityView[] = [opportunite()]): void {
    http.expectOne(`${endpoint}/risks`).flush(risks);
    http.expectOne(`${endpoint}/opportunities`).flush(opps);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('compte les deux registres dans les onglets et range les risques du plus lourd au plus léger', async () => {
    await setup();
    servir();

    expect(hote().querySelector('[data-test="onglet-risques"]')!.textContent).toContain('3');
    expect(hote().querySelector('[data-test="onglet-opportunites"]')!.textContent).toContain('1');
    const refs = Array.from(hote().querySelectorAll('[data-test="ligne-risque"] .ref')).map(e => e.textContent!.trim());
    expect(refs).toEqual(['R-007', 'R-014', 'R-060']);
    expect(hote().querySelector('[data-test="legende-risques"]')!.textContent).toContain('3');
  });

  it('écrit le niveau en toutes lettres à côté du calcul', async () => {
    await setup();
    servir();
    const premiere = hote().querySelector('[data-test="ligne-risque"]')!;
    expect(premiere.textContent).toContain('3×5 = 15');
    expect(premiere.querySelector('.cotation--CRITICAL')).not.toBeNull();
  });

  it('trie sur la résiduelle quand on la choisit, la brute faute de résiduelle', async () => {
    await setup();
    servir();
    component.ratingView = 'residual';
    expect(component.filteredRisks.map(r => r.reference)).toEqual(['R-014', 'R-007', 'R-060']);
  });

  it('filtre par recherche, type, processus et statut', async () => {
    await setup();
    servir();

    component.search = 'étalonnage';
    expect(component.filteredRisks.map(r => r.reference)).toEqual(['R-007']);
    component.search = 'kone';
    expect(component.filteredRisks.length).toBe(3);
    component.search = '';
    component.typeFilter = 'HEALTH_SAFETY';
    expect(component.filteredRisks.map(r => r.reference)).toEqual(['R-060']);
    component.typeFilter = '';
    component.processFilter = 'Métrologie';
    expect(component.filteredRisks.map(r => r.reference)).toEqual(['R-007']);
    component.processFilter = '';
    component.statusFilter = 'ACCEPTED';
    expect(component.filteredRisks.map(r => r.reference)).toEqual(['R-060']);
    expect(component.processes).toEqual(['Métrologie', 'Production', 'Qualité']);

    component.resetFilters();
    expect(component.filteredRisks.length).toBe(3);
  });

  it('dit quand aucun filtre ne correspond, et quand le registre est vide', async () => {
    await setup();
    servir();
    component.search = 'introuvable';
    fixture.detectChanges();
    expect(hote().textContent).toContain('Aucune ligne ne correspond');

    await setup();
    servir([], []);
    expect(hote().textContent).toContain('Aucun risque dans le registre');
  });

  it('ouvre la fiche au clic et au clavier', async () => {
    await setup();
    servir();
    const ligne = hote().querySelector('[data-test="ligne-risque"]') as HTMLElement;
    ligne.click();
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'r2']);
    ligne.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(router.navigate).toHaveBeenCalledTimes(2);
  });

  it('l’onglet opportunités se lit sur son adresse, filtre les prioritaires et crée une opportunité', async () => {
    await setup('opportunities');
    servir(RISQUES, [opportunite(), opportunite({ id: 'o2', reference: 'O-008', score: 9, level: 'MEDIUM',
      gain: 3, feasibility: 3, status: 'PLANNED' })]);

    expect(hote().querySelectorAll('[data-test="ligne-opportunite"]').length).toBe(2);
    expect(hote().querySelector('[data-test="ligne-opportunite"]')!.textContent).toContain('Prioritaire');
    component.priorityOnly = true;
    expect(component.filteredOpportunities.map(o => o.reference)).toEqual(['O-003']);
    component.statusFilter = 'PLANNED';
    component.priorityOnly = false;
    expect(component.filteredOpportunities.map(o => o.reference)).toEqual(['O-008']);

    (hote().querySelector('[data-test="nouveau"]') as HTMLElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'opportunites', 'nouvelle']);
    component.openOpportunity(opportunite());
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'opportunites', 'o1']);
  });

  it('changer d’onglet change d’adresse et remet le statut à zéro', async () => {
    await setup();
    servir();
    component.statusFilter = 'TO_TREAT';
    component.setTab('opportunities');
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'opportunites']);
    expect(component.statusFilter).toBe('');
    component.setTab('opportunities');
    expect(router.navigate).toHaveBeenCalledTimes(1);
    component.setTab('risks');
    expect(router.navigate).toHaveBeenCalledWith(['/risques']);
  });

  it('sans rôle de pilotage, pas de bouton de création', async () => {
    await setup('risks', ['USER']);
    servir();
    expect(hote().querySelector('[data-test="nouveau"]')).toBeNull();
    expect(component.editable).toBeFalse();
  });

  it('nouveau risque mène au formulaire', async () => {
    await setup();
    servir();
    component.create();
    expect(router.navigate).toHaveBeenCalledWith(['/risques', 'nouveau']);
  });

  it('signale un échec de chargement et propose de réessayer', async () => {
    await setup();
    http.expectOne(`${endpoint}/risks`).flush('x', { status: 500, statusText: 'KO' });
    // forkJoin annule la seconde lecture dès que la première échoue : elle part,
    // mais il n'y a plus personne pour lire sa réponse.
    expect(http.expectOne(`${endpoint}/opportunities`).cancelled).toBeTrue();
    fixture.detectChanges();
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
  });

  it('exporte les lignes affichées, en neutralisant les formules', async () => {
    await setup();
    servir([risque({ title: '=HYPERLINK("http://x")' })]);
    let blob: Blob | undefined;
    spyOn(URL, 'createObjectURL').and.callFake((b: Blob | MediaSource) => { blob = b as Blob; return 'blob:x'; });
    spyOn(URL, 'revokeObjectURL');
    spyOn(HTMLAnchorElement.prototype, 'click');

    component.exporter();

    const texte = await blob!.text();
    expect(texte).toContain('"\'=HYPERLINK(""http://x"")"');
    expect(texte).toContain('R-014');
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();

    component.tab = 'opportunities';
    component.exporter();
    expect(await blob!.text()).toContain('O-003');
  });
});

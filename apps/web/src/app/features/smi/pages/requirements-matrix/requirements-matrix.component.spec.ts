import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AlignmentReport } from '../../../standards/standards.types';
import { chapterLabel, coverageLabel, moduleLabel } from '../../smi.labels';
import { RequirementsMatrix } from '../../smi.types';
import { RequirementsMatrixComponent } from './requirements-matrix.component';

describe('RequirementsMatrixComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/smi/requirements-matrix`;
  let fixture: ComponentFixture<RequirementsMatrixComponent>;
  let component: RequirementsMatrixComponent;
  let http: HttpTestingController;
  let prevMock: boolean;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const MATRICE: RequirementsMatrix = {
    standards: [
      { adoptionId: 'a9001', code: 'iso-9001', name: 'ISO 9001' },
      { adoptionId: 'a45001', code: 'iso-45001', name: 'ISO 45001' }
    ],
    rows: [
      { chapter: '4', modules: ['PROCESS_MAP', 'RISK_REGISTER'], cells: [
        { standardCode: 'iso-9001', status: 'COVERED', covered: 3, total: 3, sectionTitle: 'Contexte' },
        { standardCode: 'iso-45001', status: 'PARTIAL', covered: 1, total: 3, sectionTitle: 'Contexte' }
      ] },
      { chapter: '6', modules: ['RISK_REGISTER', 'OBJECTIVES'], cells: [
        { standardCode: 'iso-9001', status: 'COVERED', covered: 4, total: 4, sectionTitle: 'Planification' },
        { standardCode: 'iso-45001', status: 'GAP', covered: 0, total: 5, sectionTitle: 'Planification' }
      ] },
      { chapter: '10', modules: ['NC', 'CAPA'], cells: [
        { standardCode: 'iso-9001', status: 'NOT_APPLICABLE', covered: 0, total: 0, sectionTitle: null },
        { standardCode: 'iso-45001', status: 'COVERED', covered: 2, total: 2, sectionTitle: 'Amélioration' }
      ] }
    ]
  };

  const ALIGNEMENT_45001: AlignmentReport = {
    tenantStandardId: 'a45001', standardId: 's', standardCode: 'iso-45001', overallScore: 40,
    totalRequirements: 10, coveredRequirements: 4, totalMustRequirements: 8, coveredMustRequirements: 3,
    sections: [
      { sectionId: 's6', sectionCode: '6', sectionTitle: 'Planification', score: 0, totalRequirements: 5,
        coveredRequirements: 0, clauses: [
          { clauseId: 'c61', clauseCode: '6.1', clauseTitle: 'Actions face aux risques', score: 0, totalRequirements: 3, coveredRequirements: 0 },
          { clauseId: 'c62', clauseCode: '6.2', clauseTitle: 'Objectifs SST', score: 100, totalRequirements: 2, coveredRequirements: 2 }
        ] },
      { sectionId: 's4', sectionCode: '4', sectionTitle: 'Contexte', score: 33, totalRequirements: 3,
        coveredRequirements: 1, clauses: [] }
    ]
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    await TestBed.configureTestingModule({
      declarations: [RequirementsMatrixComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RequirementsMatrixComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    environment.useMockApi = prevMock;
  });

  function alignement(): void {
    http.expectOne(`${environment.apiBaseUrl}/api/v1/standards/adoptions/a45001/alignment`).flush(ALIGNEMENT_45001);
    fixture.detectChanges();
  }

  it('croise chapitres et normes, nomme les modules, et ouvre d’emblée le premier écart', () => {
    http.expectOne(endpoint).flush(MATRICE);
    fixture.detectChanges();
    alignement();

    const tableau = hote().querySelector('[data-test="matrice-exigences"]')!.textContent!;
    expect(tableau).toContain('4 · Contexte de l\'organisme');
    expect(tableau).toContain('Cartographie · Risques');
    expect(tableau).toContain('Écart');
    expect(hote().querySelector('[data-test="case-10-iso-9001"]')).toBeNull();

    expect(component.selected!.row.chapter).toBe('6');
    expect(component.selected!.standard.code).toBe('iso-45001');
    // Seules les clauses pas entièrement prouvées restent à faire.
    expect(component.gaps.map(c => c.clauseCode)).toEqual(['6.1']);
    expect(hote().querySelector('[data-test="clauses"]')!.textContent).toContain('Actions face aux risques');
    expect(hote().querySelector('[data-test="completer"]')!.getAttribute('href'))
      .toBe('/standards/adoptions/a45001');
  });

  it('« Créer une action CAPA » mène à la création CAPA préremplie', () => {
    http.expectOne(endpoint).flush(MATRICE);
    fixture.detectChanges();
    alignement();

    const q = component.capaQuery;
    expect(q['nouveau']).toBe('1');
    expect(q['titre']).toContain('ISO 45001');
    expect(q['titre']).toContain('6');
    expect(q['ref']).toBe('iso-45001 §6');
    expect(q['description']).toContain('0');
    const lien = hote().querySelector('[data-test="creer-capa"]')!.getAttribute('href')!;
    expect(lien).toContain('/capa?nouveau=1');
  });

  it('choisir une case couverte n’interroge pas l’alignement ; une case sans objet ne s’ouvre pas', () => {
    http.expectOne(endpoint).flush(MATRICE);
    fixture.detectChanges();
    alignement();

    (hote().querySelector('[data-test="case-4-iso-9001"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(component.selected!.cell.status).toBe('COVERED');
    expect(component.gaps).toEqual([]);
    expect(hote().querySelector('[data-test="creer-capa"]')).toBeNull();
    expect(component.isSelected(MATRICE.rows[0], 0)).toBeTrue();

    component.select(MATRICE.rows[2], MATRICE.rows[2].cells[0], 0);
    expect(component.selected!.row.chapter).toBe('4');
    expect(component.capaQuery['ref']).toBe('iso-9001 §4');
  });

  it('un alignement illisible le dit dans le détail', () => {
    http.expectOne(endpoint).flush(MATRICE);
    fixture.detectChanges();
    http.expectOne(`${environment.apiBaseUrl}/api/v1/standards/adoptions/a45001/alignment`)
      .flush('x', { status: 500, statusText: 'KO' });
    fixture.detectChanges();
    expect(component.gapsFailed).toBeTrue();
    expect(hote().querySelector('[data-test="detail"] [role="alert"]')).not.toBeNull();
  });

  it('sans norme adoptée, invite à en adopter une', () => {
    http.expectOne(endpoint).flush({ standards: [], rows: [] });
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="aucune-norme"]')).not.toBeNull();
    expect(component.selected).toBeNull();
    expect(component.capaQuery).toEqual({});
  });

  it('une matrice indisponible s’annonce et se recharge', () => {
    http.expectOne(endpoint).flush('x', { status: 503, statusText: 'KO' });
    fixture.detectChanges();
    expect(component.error).toBeTruthy();
    component.charger();
    http.expectOne(endpoint).flush({ standards: [], rows: [] });
    expect(component.error).toBeNull();
  });

  it('libellés : chapitre, module et état inconnus restent lisibles', () => {
    http.expectOne(endpoint).flush({ standards: [], rows: [] });
    expect(chapterLabel('11')).toBe('11');
    expect(moduleLabel('INCONNU')).toBe('INCONNU');
    expect(coverageLabel('PARTIAL')).toBe('Partiel');
    expect(component.breadcrumbs[0].route).toBe('/smi');
  });
});

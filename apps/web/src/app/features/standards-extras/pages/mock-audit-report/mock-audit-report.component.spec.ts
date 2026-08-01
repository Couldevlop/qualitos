import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsExtrasService } from '../../standards-extras.service';
import { MockAuditReport } from '../../standards-extras.types';
import { MockAuditReportComponent } from './mock-audit-report.component';

describe('MockAuditReportComponent', () => {
  let svc: jasmine.SpyObj<StandardsExtrasService>;
  let harness: RouterTestingHarness;
  let component: MockAuditReportComponent;

  const report: MockAuditReport = {
    id: 'r1', adoptionId: 'a1', standardId: 's1', standardCode: 'iso-9001',
    standardName: 'Management de la qualité', readiness: 64,
    majorCount: 1, minorCount: 1, observationCount: 1, questionCount: 3,
    questions: [
      { clauseCode: '7.5', question: 'Qui approuve les procédures ?', rationale: 'Clause à risque.' },
      { clauseCode: '9.2', question: 'Quel est le programme d\'audit ?', rationale: 'Preuve absente.' },
      { clauseCode: '4.1', question: 'Où est l\'analyse de contexte ?', rationale: 'Revue annuelle.' }
    ],
    gaps: [
      {
        clauseCode: '7.5', title: 'Maîtrise des informations documentées',
        criticality: 'MAJOR', coverageRatio: 0.25, totalRequirements: 4, coveredRequirements: 1,
        finding: 'Aucune procédure de gestion documentaire démontrée.',
        questions: [
          { clauseCode: '7.5', question: 'Qui approuve les procédures ?', rationale: 'Clause à risque.' }
        ]
      },
      {
        clauseCode: '9.2', title: 'Audit interne', criticality: 'MINOR',
        coverageRatio: 0.5, totalRequirements: 2, coveredRequirements: 1,
        finding: 'Programme d\'audit partiel.', questions: []
      },
      {
        clauseCode: '4.1', title: 'Compréhension du contexte', criticality: 'OBSERVATION',
        coverageRatio: 1, totalRequirements: 2, coveredRequirements: 2,
        finding: 'Analyse de contexte à jour.', questions: []
      }
    ],
    remediationPlan: [
      { clauseCode: '7.5', criticality: 'MAJOR', priority: 'high',
        targetModule: 'DOCUMENT_CONTROL', action: 'Rédiger la procédure de maîtrise documentaire.' },
      { clauseCode: '9.2', criticality: 'MINOR', priority: 'medium',
        targetModule: 'AUDIT', action: 'Compléter le programme d\'audit interne.' }
    ],
    aiProvider: 'mistral', createdByUserId: 'u1', createdAt: '2026-07-01T10:00:00Z'
  };

  /**
   * Vraies routes plutôt qu'un `ActivatedRoute` factice : le lien de retour est
   * RELATIF, seule une résolution réelle prouve qu'il retombe sur l'écran d'origine.
   */
  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      declarations: [MockAuditReportComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsExtrasService, useValue: svc },
        provideRouter([
          { path: 'ims/audit-blanc-ia/:adoptionId/:runId', component: MockAuditReportComponent }
        ])
      ]
    }).compileComponents();

    harness = await RouterTestingHarness.create();
    component = await harness.navigateByUrl('/ims/audit-blanc-ia/a1/r1', MockAuditReportComponent);
    harness.detectChanges();
  };

  const html = (): HTMLElement => harness.routeNativeElement as HTMLElement;

  beforeEach(() => {
    svc = jasmine.createSpyObj<StandardsExtrasService>('StandardsExtrasService',
      ['mockAuditReport']);
    svc.mockAuditReport.and.returnValue(of(report));
  });

  it('charge le rapport désigné par l\'URL (adoption + exécution)', async () => {
    await open();
    expect(svc.mockAuditReport).toHaveBeenCalledWith('a1', 'r1');
    expect(component.report?.standardCode).toBe('iso-9001');
  });

  it('ramène au hub par un lien relatif, sans coder le chemin de montage', async () => {
    await open();
    const back = html().querySelector('a[href]');
    expect(back?.getAttribute('href')).toBe('/ims');
  });

  it('rend un bloc par écart, du majeur à l\'observation', async () => {
    await open();
    expect(html().querySelectorAll('.gap').length).toBe(3);
    expect(html().querySelector('.gap')?.getAttribute('data-criticality')).toBe('MAJOR');
  });

  it('filtre les écarts par criticité', async () => {
    await open();
    component.setFilter('MAJOR');
    harness.detectChanges();
    expect(component.visibleGaps.length).toBe(1);
    expect(html().querySelectorAll('.gap').length).toBe(1);
    expect(component.countFor('OBSERVATION')).toBe(1);
  });

  it('ne déplie les questions ciblées que sur demande', async () => {
    await open();
    expect(html().querySelectorAll('.gap .questions').length).toBe(0);

    component.toggleGap(report.gaps[0]);
    harness.detectChanges();
    expect(component.isExpanded(report.gaps[0])).toBeTrue();
    expect(html().querySelectorAll('.gap .questions li').length).toBe(1);

    component.toggleGap(report.gaps[0]);
    harness.detectChanges();
    expect(html().querySelectorAll('.gap .questions').length).toBe(0);
  });

  it('convertit le ratio de couverture du serveur en pourcentage', async () => {
    await open();
    expect(component.coveragePercent(report.gaps[0])).toBe(25);
    expect(component.coveragePercent(report.gaps[2])).toBe(100);
  });

  it('rend une ligne de plan par écart actionnable et traduit la priorité', async () => {
    await open();
    const rows = html().querySelectorAll('.data tbody tr');
    expect(rows.length).toBe(2);
    expect(component.priorityLabel('high')).toBe('Haute');
    expect(component.priorityLabel('inconnue')).toBe('inconnue');
  });

  it('liste l\'intégralité des questions posées', async () => {
    await open();
    expect(html().querySelectorAll('.questions--full li').length).toBe(3);
  });

  it('propose de réessayer quand le rapport est introuvable', async () => {
    svc.mockAuditReport.and.returnValue(throwError(() => ({ status: 404 })));
    await open();
    const banner = html().querySelector('.banner-error');
    expect(banner?.textContent).toContain('introuvable');
    expect(component.report).toBeNull();
  });
});

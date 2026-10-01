import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { csvCell } from './risk-register.csv';
import {
  OPPORTUNITY_ORIGINS, actionStatusLabel, capaStatusLabel, eventText, levelOf, opportunityLevelLabel,
  originLabel, ratingText, requirementLabel, riskLevelLabel, typeLabel
} from './risk-register.labels';
import { RiskRegisterService } from './risk-register.service';
import { RegisterEvent } from './risk-register.types';

describe('risk-register — libellés, CSV et service', () => {

  const ev = (o: Partial<RegisterEvent>): RegisterEvent =>
    ({ id: 'e', type: 'CREATED', fromValue: null, toValue: null, detail: null, at: '2026-10-01', ...o });

  it('les niveaux suivent les seuils du serveur', () => {
    expect([1, 4, 5, 9, 10, 14, 15, 25].map(levelOf))
      .toEqual(['LOW', 'LOW', 'MEDIUM', 'MEDIUM', 'HIGH', 'HIGH', 'CRITICAL', 'CRITICAL']);
    expect(riskLevelLabel('CRITICAL')).toBe('Critique');
    expect(opportunityLevelLabel('PRIORITY')).toBe('Prioritaire');
    expect(riskLevelLabel(null)).toBe('—');
    expect(opportunityLevelLabel(undefined)).toBe('—');
  });

  it('une valeur inconnue s’affiche telle quelle, une valeur absente par un tiret', () => {
    expect(typeLabel('QUALITY')).toBe('Qualité');
    expect(typeLabel(null)).toBe('—');
    expect(originLabel('INCONNUE' as never)).toBe('INCONNUE');
    expect(capaStatusLabel('RESOLVED')).toBe('Résolue');
    expect(capaStatusLabel('NOUVEAU')).toBe('NOUVEAU');
    expect(capaStatusLabel(null)).toBe('—');
    expect(actionStatusLabel('DONE')).toBe('Terminée');
    expect(requirementLabel('IATF_16949_6_1_2')).toBe('IATF 16949 · 6.1.2');
    expect(requirementLabel('X' as never)).toBe('X');
  });

  it('les opportunités ne proposent pas les origines propres aux risques', () => {
    expect(OPPORTUNITY_ORIGINS.map(o => o.value)).toEqual(
      ['DIRECT', 'MANAGEMENT_REVIEW', 'AUDIT', 'CUSTOMER_FEEDBACK', 'MONITORING']);
  });

  it('compose chaque ligne du suivi dans la langue de l’écran', () => {
    expect(ratingText('4x3')).toBe('4 × 3');
    expect(ratingText(null)).toBe('—');
    expect(eventText(ev({ toValue: 'FMEA', detail: 'PFMEA-7 #3' }), 'risk')).toContain('AMDEC PFMEA-7 #3');
    expect(eventText(ev({ toValue: 'AUDIT' }), 'opportunity')).toContain('Opportunité créée');
    expect(eventText(ev({ type: 'RATING_CHANGED', fromValue: '3x3', toValue: '4x3' }), 'opportunity'))
      .toBe('Évaluation passée de 3 × 3 à 4 × 3');
    expect(eventText(ev({ type: 'RESIDUAL_CHANGED', fromValue: null, toValue: '4x2' }), 'risk')).toContain('4 × 2');
    expect(eventText(ev({ type: 'STATUS_CHANGED', fromValue: 'TO_TREAT', toValue: 'CLOSED' }), 'risk'))
      .toBe('Statut : À traiter → Clos');
    expect(eventText(ev({ type: 'STATUS_CHANGED', fromValue: 'UNDER_STUDY', toValue: 'DONE' }), 'opportunity'))
      .toContain('Réalisée');
    expect(eventText(ev({ type: 'DECISION_CHANGED', fromValue: 'UNDECIDED', toValue: 'REDUCE' }), 'risk'))
      .toBe('Décision : À décider → Réduire');
    expect(eventText(ev({ type: 'DECISION_CHANGED', fromValue: 'UNDECIDED', toValue: 'PLAN' }), 'opportunity'))
      .toContain('Planifier');
    expect(eventText(ev({ type: 'ACTION_OPENED', detail: 'Carte SPC' }), 'risk')).toBe('Action CAPA ouverte : Carte SPC');
    expect(eventText(ev({ type: 'INCONNU' as never }), 'risk')).toBe('INCONNU');
  });

  it('une cellule CSV ne peut pas devenir une formule', () => {
    expect(csvCell('=1+1')).toBe('"\'=1+1"');
    expect(csvCell('+33 6')).toBe('"\'+33 6"');
    expect(csvCell('-2')).toBe('"\'-2"');
    expect(csvCell('@SUM(A1)')).toBe('"\'@SUM(A1)"');
    expect(csvCell('a "b"; c')).toBe('"a ""b""; c"');
    expect(csvCell(null)).toBe('""');
    expect(csvCell(12)).toBe('"12"');
  });

  it('le service appelle les bonnes adresses, identifiants encodés', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    const service = TestBed.inject(RiskRegisterService);
    const http = TestBed.inject(HttpTestingController);
    const base = `${environment.apiBaseUrl}/api/v1/risk-register`;

    service.risks().subscribe();
    service.risk('a/b').subscribe();
    service.createRisk({} as never).subscribe();
    service.reviseRisk('r1', {} as never).subscribe();
    service.openCapa('r1', { title: 't' }).subscribe();
    service.draft('FMEA', 'i1').subscribe();
    service.opportunities().subscribe();
    service.opportunity('o1').subscribe();
    service.createOpportunity({} as never).subscribe();
    service.reviseOpportunity('o1', {} as never).subscribe();
    service.addAction('o1', { title: 't' }).subscribe();
    service.reviseAction('o1', 'a1', { title: 't' }).subscribe();
    service.deleteAction('o1', 'a1').subscribe();
    service.suggestions().subscribe();

    http.expectOne({ method: 'GET', url: `${base}/risks` }).flush([]);
    http.expectOne({ method: 'GET', url: `${base}/risks/a%2Fb` }).flush({});
    http.expectOne({ method: 'POST', url: `${base}/risks` }).flush({});
    http.expectOne({ method: 'PUT', url: `${base}/risks/r1` }).flush({});
    http.expectOne({ method: 'POST', url: `${base}/risks/r1/capa` }).flush({});
    http.expectOne({ method: 'GET', url: `${base}/sources/FMEA/i1` }).flush({});
    http.expectOne({ method: 'GET', url: `${base}/opportunities` }).flush([]);
    http.expectOne({ method: 'GET', url: `${base}/opportunities/o1` }).flush({});
    http.expectOne({ method: 'POST', url: `${base}/opportunities` }).flush({});
    http.expectOne({ method: 'PUT', url: `${base}/opportunities/o1` }).flush({});
    http.expectOne({ method: 'POST', url: `${base}/opportunities/o1/actions` }).flush({});
    http.expectOne({ method: 'PUT', url: `${base}/opportunities/o1/actions/a1` }).flush({});
    http.expectOne({ method: 'DELETE', url: `${base}/opportunities/o1/actions/a1` }).flush(null);
    http.expectOne({ method: 'GET', url: `${base}/suggestions` }).flush({});
    http.verify();
  });
});

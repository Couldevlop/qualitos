import { HttpErrorResponse } from '@angular/common/http';

import {
  mocClassificationLabel,
  mocDecisionLabel,
  mocDomainLabel,
  mocErrorMessage,
  mocHistoryLabel,
  mocLevelLabel,
  mocStatusLabel,
  mocTypeLabel
} from './moc.labels';
import { IMPACT_DOMAINS, IMPACT_LEVELS } from './moc.types';

describe('moc.labels', () => {

  it('donne un mot à chaque code', () => {
    expect(mocStatusLabel('IN_VALIDATION')).toBe('En validation');
    expect(mocClassificationLabel('CRITICAL')).toBe('Critique');
    expect(mocClassificationLabel('NOT_ASSESSED')).toBe('Non évalué');
    expect(mocTypeLabel('TEMPORARY')).toBe('Temporaire');
    expect(mocDecisionLabel('PENDING')).toBe('En attente');
    expect(mocHistoryLabel('ANALYSIS_COMPLETED')).toBe("Analyse d'impact terminée");
    expect(IMPACT_DOMAINS.map(mocDomainLabel)).toEqual(['Sécurité', 'Santé et environnement', 'Qualité',
      'Production', 'Maintenance', 'Coûts', 'Réglementaire', 'Formation']);
    expect(IMPACT_LEVELS.map(mocLevelLabel)).toEqual(['Aucun', 'Faible', 'Moyen', 'Élevé']);
  });

  it('traduit un refus du serveur par son code, avec le rôle nommé', () => {
    const err = (code: string, role?: string) =>
      new HttpErrorResponse({ status: 409, error: { code, role } });
    expect(mocErrorMessage(err('moc.role-without-holder', 'HSE'), 'x')).toContain('« HSE »');
    expect(mocErrorMessage(err('moc.analysis-incomplete'), 'x')).toContain('huit domaines');
    expect(mocErrorMessage(err('moc.not-holder', 'Directeur de site'), 'x')).toContain('Directeur de site');
    expect(mocErrorMessage(err('moc.self-validation'), 'x')).toContain('demandeur');
    expect(mocErrorMessage(err('moc.comment-required'), 'x')).toContain('commentaire');
    expect(mocErrorMessage(err('moc.settings.role-in-use', 'HSE'), 'x')).toContain('ne peut pas être retiré');
    for (const code of ['moc.not-owner', 'moc.role-only-requester', 'moc.already-decided', 'moc.delete-not-draft',
      'moc.settings.role-in-use-holders', 'moc.settings.role-duplicate', 'moc.settings.required-empty',
      'moc.settings.thresholds', 'moc.settings.weights-zero']) {
      expect(mocErrorMessage(err(code, 'R'), 'repli')).not.toBe('repli');
    }
  });

  it('retombe sur le message sûr sans code connu, sans jamais montrer le texte du serveur', () => {
    const e = new HttpErrorResponse({ status: 403, error: { code: 'moc.inconnu', detail: 'détail interne' } });
    const m = mocErrorMessage(e, 'repli');
    expect(m).not.toContain('détail interne');
    expect(mocErrorMessage(new HttpErrorResponse({ status: 404 }), 'Introuvable')).toBe('Introuvable');
    expect(mocErrorMessage(undefined, 'repli')).toBe('repli');
  });
});

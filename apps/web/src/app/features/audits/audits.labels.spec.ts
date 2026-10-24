import { auditTypeLabel, findingTypeLabel } from './audits.labels';

describe('audits.labels', () => {
  it('traduit le type d\'audit au lieu d\'afficher le code de l\'API', () => {
    expect(auditTypeLabel('INTERNAL')).toBe('Interne');
    expect(auditTypeLabel('EXTERNAL')).toBe('Externe');
    expect(auditTypeLabel('LPA')).toBe('LPA');
  });

  it('traduit le type de constat', () => {
    expect(findingTypeLabel('MAJOR_NC')).toBe('Non-conformité majeure');
    expect(findingTypeLabel('OPPORTUNITY')).toBe('Opportunité');
  });

  it('rend la valeur brute si inconnue, un tiret si absente', () => {
    expect(auditTypeLabel('ODD')).toBe('ODD');
    expect(findingTypeLabel(undefined)).toBe('—');
  });
});

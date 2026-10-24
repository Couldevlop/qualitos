import { NcCategoryLabelPipe, SeverityLabelPipe } from './severity-label.pipe';

describe('SeverityLabelPipe / NcCategoryLabelPipe', () => {
  const severity = new SeverityLabelPipe();
  const category = new NcCategoryLabelPipe();

  it('traduit les deux échelles de gravité', () => {
    expect(severity.transform('LOW')).toBe('Faible');
    expect(severity.transform('MEDIUM')).toBe('Moyenne');
    expect(severity.transform('HIGH')).toBe('Élevée');
    expect(severity.transform('CRITICAL')).toBe('Critique');
    expect(severity.transform('MINOR')).toBe('Mineure');
    expect(severity.transform('MAJOR')).toBe('Majeure');
  });

  it('traduit les catégories de NC', () => {
    expect(category.transform('DOCUMENTATION')).toBe('Documentation');
    expect(category.transform('SAFETY')).toBe('Sécurité');
  });

  it('rend le code inconnu tel quel, un tiret sans code', () => {
    expect(severity.transform('EXTREME')).toBe('EXTREME');
    expect(severity.transform(null)).toBe('—');
    expect(category.transform(undefined)).toBe('—');
  });
});

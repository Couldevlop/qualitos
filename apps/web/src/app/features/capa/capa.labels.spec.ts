import { capaCriticityLabel, capaSourceLabel } from './capa.labels';

describe('capa.labels', () => {
  it('traduit la criticité au lieu d\'afficher le code de l\'API', () => {
    expect(capaCriticityLabel('LOW')).toBe('Faible');
    expect(capaCriticityLabel('MEDIUM')).toBe('Moyenne');
    expect(capaCriticityLabel('HIGH')).toBe('Élevée');
    expect(capaCriticityLabel('CRITICAL')).toBe('Critique');
  });

  it('rend la valeur brute si la criticité est inconnue, un tiret si elle manque', () => {
    expect(capaCriticityLabel('EXTREME')).toBe('EXTREME');
    expect(capaCriticityLabel(null)).toBe('—');
  });

  it('traduit la source, y compris celles des automates', () => {
    expect(capaSourceLabel('NON_CONFORMITY')).toBe('Non-conformité');
    expect(capaSourceLabel('RISK')).toBe('Risque');
  });
});

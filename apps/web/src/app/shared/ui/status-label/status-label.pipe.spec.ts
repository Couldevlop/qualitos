import { StatusLabelPipe } from './status-label.pipe';
import { KNOWN_STATUS_CODES, statusLabel } from './status-labels';

describe('StatusLabelPipe', () => {
  const pipe = new StatusLabelPipe();

  it('traduit un code connu', () => {
    expect(pipe.transform('IN_PROGRESS')).toBe('En cours');
    expect(pipe.transform('OPEN')).toBe('Ouvert');
    expect(pipe.transform('NOTIFIED_REGULATOR')).toBe("Notifié à l'autorité");
  });

  it('accorde au féminin sur demande (« NC ouverte »)', () => {
    expect(pipe.transform('OPEN', 'f')).toBe('Ouverte');
    expect(pipe.transform('CLOSED', 'f')).toBe('Clôturée');
    expect(pipe.transform('CANCELLED', 'f')).toBe('Annulée');
    expect(pipe.transform('REJECTED', 'f')).toBe('Rejetée');
    expect(pipe.transform('RESOLVED', 'f')).toBe('Résolue');
  });

  it('garde le libellé commun quand le féminin ne change rien', () => {
    expect(pipe.transform('UNDER_ANALYSIS', 'f')).toBe("En cours d'analyse");
    expect(pipe.transform('ACTION_DEFINED', 'f')).toBe('Action définie');
    expect(pipe.transform('OPEN', 'm')).toBe('Ouvert');
  });

  it('ne laisse jamais un code inconnu en capitales', () => {
    expect(statusLabel('WAITING_FOR_PARTS')).toBe('Waiting for parts');
  });

  it('rend un tiret sans code', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform(undefined)).toBe('—');
    expect(pipe.transform('')).toBe('—');
  });

  it('a un libellé pour chaque code de la table, jamais le code lui-même', () => {
    expect(KNOWN_STATUS_CODES.length).toBeGreaterThan(100);
    for (const code of KNOWN_STATUS_CODES) {
      expect(statusLabel(code)).not.toBe(code);
    }
  });
});

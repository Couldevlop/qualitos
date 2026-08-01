import {
  requirementLabel, riskBasis, riskLabel, riskTone, roleLabel, statusLabel, statusTone
} from './ai-systems.labels';
import { AiRiskClassification, AiSystemRole, AiSystemStatus } from './ai-systems.types';

/**
 * Le registre affiche des libellés métier, jamais les constantes serveur : un
 * responsable qualité lit « Haut risque », pas « HIGH ».
 */
describe('ai-systems.labels', () => {

  const risks: AiRiskClassification[] = ['UNACCEPTABLE', 'HIGH', 'LIMITED', 'MINIMAL_OR_NO'];
  const statuses: AiSystemStatus[] =
    ['DRAFT', 'REGISTERED', 'IN_USE', 'DECOMMISSIONED', 'WITHDRAWN'];
  const roles: AiSystemRole[] = ['PROVIDER', 'DEPLOYER', 'IMPORTER', 'DISTRIBUTOR'];

  it('traduit chaque classification sans jamais laisser fuiter le code serveur', () => {
    const labels = risks.map(riskLabel);
    expect(new Set(labels).size).toBe(risks.length);
    expect(labels.some(l => l.includes('_'))).toBeFalse();
    expect(riskLabel('HIGH')).toBe('Haut risque');
  });

  it('rappelle la base légale qui justifie les obligations', () => {
    expect(riskBasis('UNACCEPTABLE')).toContain('Art. 5');
    expect(riskBasis('HIGH')).toContain('Annexe III');
    expect(riskBasis('LIMITED')).toContain('Art. 50');
    expect(riskBasis('MINIMAL_OR_NO')).toContain('aucune obligation');
  });

  it('colore la classification par sévérité décroissante', () => {
    expect(riskTone('UNACCEPTABLE')).toBe('danger');
    expect(riskTone('HIGH')).toBe('warn');
    expect(riskTone('LIMITED')).toBe('info');
    expect(riskTone('MINIMAL_OR_NO')).toBe('success');
  });

  it('traduit chaque statut du cycle de vie', () => {
    const labels = statuses.map(statusLabel);
    expect(new Set(labels).size).toBe(statuses.length);
    expect(statusLabel('IN_USE')).toBe('En service');
  });

  it('ne met en vert que ce qui est réellement exploité', () => {
    expect(statusTone('IN_USE')).toBe('success');
    expect(statusTone('REGISTERED')).toBe('info');
    expect(statusTone('DRAFT')).toBe('neutral');
    expect(statusTone('DECOMMISSIONED')).toBe('warn');
    expect(statusTone('WITHDRAWN')).toBe('warn');
  });

  it('traduit les quatre rôles de l\'article 3', () => {
    const labels = roles.map(roleLabel);
    expect(new Set(labels).size).toBe(roles.length);
    expect(roleLabel('DEPLOYER')).toBe('Déployeur');
  });

  it('cite l\'article de chaque obligation, pour rendre la check-list opposable', () => {
    expect(requirementLabel('conformity-evidence')).toContain('Art. 43');
    expect(requirementLabel('human-oversight')).toContain('Art. 14');
    expect(requirementLabel('transparency')).toContain('Art. 13');
  });
});

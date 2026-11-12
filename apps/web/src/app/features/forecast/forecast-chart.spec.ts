import { buildForecastChart } from './forecast-chart';
import { ForecastResponse } from './forecast.types';

/**
 * Le graphe de prévision est partagé entre la page d'essai et la fiche de
 * l'indicateur : la même série ne doit pas se dessiner de deux façons.
 */
describe('buildForecastChart', () => {

  const RES: ForecastResponse = {
    n: 4, slope: 1, intercept: 10, residualSigma: 0.5, r2: 0.9,
    horizon: 2, target: 20, direction: 'at_least', probability: 0.6,
    confidence: 'medium', model: 'holt_linear', seasonalPeriod: 0,
    points: [
      { step: 1, value: 14, low: 13, high: 15 },
      { step: 2, value: 15, low: 13, high: 17 }
    ]
  };

  /** Les séries du graphe, par nom et dans l'ordre déclaré. */
  function series(option: ReturnType<typeof buildForecastChart>): any[] {
    return (option as any).series as any[];
  }

  it('couvre l’historique puis l’horizon sur un seul axe de périodes', () => {
    const opt = buildForecastChart([10, 11, 12, 13], RES);
    expect((opt as any).xAxis.data).toEqual(['1', '2', '3', '4', '5', '6']);
  });

  // L'historique s'arrête où la prévision commence : prolonger la ligne bleue
  // sur la zone projetée ferait passer une projection pour une mesure.
  it('arrête l’historique à la dernière mesure', () => {
    const s = series(buildForecastChart([10, 11, 12, 13], RES));
    expect(s[0].data).toEqual([10, 11, 12, 13, null, null]);
  });

  // La prévision est raccordée au dernier point réel : sans ce raccord, la
  // courbe pointillée flotte, détachée de la série qu'elle prolonge.
  it('raccorde la prévision à la dernière mesure', () => {
    const s = series(buildForecastChart([10, 11, 12, 13], RES));
    const prevision = s.find(x => x.name === 'Prévision');
    expect(prevision.data).toEqual([null, null, null, 13, 14, 15]);
  });

  it('empile la bande d’incertitude sur sa borne basse', () => {
    const s = series(buildForecastChart([10, 11, 12, 13], RES));
    const bandes = s.filter(x => x.name === 'IP 95 %');
    expect(bandes.length).toBe(2);
    expect(bandes[0].data).toEqual([null, null, null, null, 13, 13]);
    // Épaisseur = high − low, et rien là où l'intervalle n'existe pas.
    expect(bandes[1].data).toEqual([null, null, null, null, 2, 4]);
  });

  it('trace la cible en ligne de repère', () => {
    const s = series(buildForecastChart([10, 11, 12, 13], RES));
    const prevision = s.find(x => x.name === 'Prévision');
    expect(prevision.markLine.data[0].yAxis).toBe(20);
  });

  // Un contrat partiel (points absents) ne doit pas casser le rendu : la fiche
  // de l'indicateur afficherait une page blanche au lieu d'un graphe vide.
  it('survit à une réponse sans points projetés', () => {
    const s = series(buildForecastChart([10, 11, 12, 13], { ...RES, points: undefined as never }));
    expect(s[0].data).toEqual([10, 11, 12, 13, null, null]);
    expect(s.find(x => x.name === 'Prévision').data).toEqual([null, null, null, 13]);
  });

  it('survit à un historique vide', () => {
    const s = series(buildForecastChart([], RES));
    expect(s.find(x => x.name === 'Prévision').data).toEqual([14, 15]);
  });
});

import { QosPaginatorIntl } from './paginator-intl';

describe('QosPaginatorIntl', () => {
  const intl = new QosPaginatorIntl();

  it('traduit les libellés du paginateur', () => {
    expect(intl.itemsPerPageLabel).toBe('Éléments par page :');
    expect(intl.nextPageLabel).toBe('Page suivante');
    expect(intl.previousPageLabel).toBe('Page précédente');
    expect(intl.firstPageLabel).toBe('Première page');
    expect(intl.lastPageLabel).toBe('Dernière page');
  });

  it('annonce la plage affichée sur le total', () => {
    expect(intl.getRangeLabel(0, 20, 7)).toBe('1 – 7 sur 7');
    expect(intl.getRangeLabel(1, 20, 45)).toBe('21 – 40 sur 45');
    expect(intl.getRangeLabel(2, 20, 45)).toBe('41 – 45 sur 45');
  });

  it('dit « 0 sur 0 » pour une liste vide', () => {
    expect(intl.getRangeLabel(0, 20, 0)).toBe('0 sur 0');
  });

  it('borne une page au-delà de la fin au lieu d’annoncer une plage inversée', () => {
    expect(intl.getRangeLabel(3, 20, 45)).toBe('45 – 45 sur 45');
  });
});

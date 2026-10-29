import { EnumLabelPipe, OpenLabelPipe } from './enum-label.pipe';
import { enumLabel, knownEnumCodes } from './enum-labels';

describe('EnumLabelPipe', () => {
  const pipe = new EnumLabelPipe();

  it('traduit le mode d’un diagramme d’Ishikawa', () => {
    expect(pipe.transform('SIX_M', 'ishikawaMode')).toBe('6M');
    expect(pipe.transform('EIGHT_M', 'ishikawaMode')).toBe('8M');
  });

  it('traduit les modules, catégories et paliers du catalogue', () => {
    expect(pipe.transform('itsm', 'moduleName')).toBe('Connecteurs ITSM');
    expect(pipe.transform('integrations', 'moduleCategory')).toBe('Intégrations');
    expect(pipe.transform('FREE', 'billingTier')).toBe('Gratuit');
  });

  it('ne laisse jamais une valeur inconnue en capitales', () => {
    expect(enumLabel('NEW_VALUE', 'billingTier')).toBe('New value');
  });

  it('affiche un tiret quand la valeur manque', () => {
    expect(enumLabel(null, 'ishikawaMode')).toBe('—');
    expect(enumLabel(undefined, 'moduleName')).toBe('—');
  });

  it('expose les valeurs connues d’un domaine', () => {
    expect(knownEnumCodes('billingTier')).toEqual(['FREE', 'STANDARD', 'PRO', 'ENTERPRISE']);
  });
});

describe('OpenLabelPipe', () => {
  const pipe = new OpenLabelPipe();

  it('nomme l’objet ouvert', () => {
    expect(pipe.transform('NC-2026-0006')).toBe('Ouvrir NC-2026-0006');
  });

  it('retombe sur « Ouvrir » sans objet', () => {
    expect(pipe.transform('  ')).toBe('Ouvrir');
    expect(pipe.transform(null)).toBe('Ouvrir');
  });
});

import {
  LevelIndex, autonomousOn, categoriesOf, filterOperators, filterSkills, hasTrainerLevel, isAtRisk,
  levelLabel, nextLevel, normalize, operatorAverage, trainerShortfall, workshopFilterOptions
} from './competency-matrix.model';
import { MatrixOperator, MatrixSkill } from './competency-matrix.types';

/** Les règles de lecture de la matrice : échelle, couverture, moyennes, filtres. */
describe('competency-matrix.model', () => {
  const ops: MatrixOperator[] = [
    { id: 'o1', userId: 'u1', name: 'Martin Dubois', workshop: 'Usinage' },
    { id: 'o2', userId: 'u2', name: 'Inès Garcia', workshop: 'Assemblage' },
    { id: 'o3', userId: 'u3', name: 'Paul Girard', workshop: 'Peinture' }
  ];
  const sk = (skillId: string, category: string | null): MatrixSkill => ({
    skillId, name: skillId, category, trainerUserId: 't', trainerName: 'Karim', autonomous: 0, atRisk: true
  });
  const skills = [sk('s1', 'Usinage'), sk('s2', 'Qualité'), sk('s3', null), sk('s4', 'Usinage')];

  it('nomme chaque niveau, et un niveau inconnu par son nombre', () => {
    expect([0, 1, 2, 3, 4].map(levelLabel)).toEqual(['Non formé', 'Débutant', 'Autonome', 'Expert', 'Formateur']);
    expect(levelLabel(7)).toBe('7');
  });

  it('fait tourner le niveau de 0 à 4 puis revient à 0', () => {
    expect([0, 1, 2, 3, 4].map(nextLevel)).toEqual([1, 2, 3, 4, 0]);
  });

  it('indexe les niveaux posés ; une case absente vaut 0', () => {
    const index = new LevelIndex([{ userId: 'u1', skillId: 's1', level: 3 }]);
    expect(index.get('u1', 's1')).toBe(3);
    expect(index.get('u2', 's1')).toBe(0);
    index.set('u2', 's1', 4);
    expect(index.get('u2', 's1')).toBe(4);
    expect(new LevelIndex().get('u1', 's1')).toBe(0);
  });

  it('compte les autonomes et signale le risque sous deux personnes', () => {
    const index = new LevelIndex([
      { userId: 'u1', skillId: 's1', level: 2 },
      { userId: 'u2', skillId: 's1', level: 4 },
      { userId: 'u3', skillId: 's2', level: 3 },
      { userId: 'u1', skillId: 's2', level: 1 }
    ]);
    expect(autonomousOn(index, ops, 's1').map(o => o.name)).toEqual(['Martin Dubois', 'Inès Garcia']);
    expect(isAtRisk(index, ops, 's1')).toBeFalse();
    expect(isAtRisk(index, ops, 's2')).toBeTrue();
    expect(isAtRisk(index, ops, 's3')).toBeTrue();
    expect(hasTrainerLevel(index, ops, 's1')).toBeTrue();
    expect(hasTrainerLevel(index, ops, 's2')).toBeFalse();
  });

  it('fait la moyenne d\'une ligne sur les compétences affichées', () => {
    const index = new LevelIndex([{ userId: 'u1', skillId: 's1', level: 3 }, { userId: 'u1', skillId: 's2', level: 2 }]);
    expect(operatorAverage(index, 'u1', skills)).toBe('1.3');
    expect(operatorAverage(index, 'u1', [])).toBe('0.0');
  });

  it('dit le niveau du formateur désigné quand il est dans la matrice sans y être autonome', () => {
    const index = new LevelIndex([{ userId: 'u2', skillId: 's2', level: 1 }, { userId: 'u3', skillId: 's1', level: 2 }]);
    const de = (skillId: string, trainerUserId: string): MatrixSkill => ({ ...sk(skillId, null), trainerUserId });
    expect(trainerShortfall(index, ops, de('s2', 'u2'))).toBe(1);
    expect(trainerShortfall(index, ops, de('s1', 'u1'))).toBe(0);
    expect(trainerShortfall(index, ops, de('s1', 'u3'))).toBeNull();
    // Hors de la matrice : formé ailleurs, peut-être.
    expect(trainerShortfall(index, ops, de('s1', 'externe'))).toBeNull();
  });

  it('liste les catégories sans doublon ni vide', () => {
    expect(categoriesOf(skills)).toEqual(['Qualité', 'Usinage']);
  });

  it('propose au filtre les ateliers retirés encore portés', () => {
    expect(workshopFilterOptions(['Usinage', 'Assemblage'], ops)).toEqual(['Usinage', 'Assemblage', 'Peinture']);
  });

  it('filtre les opérateurs par atelier et par nom, sans égard aux accents', () => {
    expect(normalize('  Inès ')).toBe('ines');
    expect(filterOperators(ops, 'ines', '').map(o => o.id)).toEqual(['o2']);
    expect(filterOperators(ops, '', 'Usinage').map(o => o.id)).toEqual(['o1']);
    expect(filterOperators(ops, 'paul', 'Usinage')).toEqual([]);
    expect(filterOperators(ops, '', '').length).toBe(3);
  });

  it('filtre les compétences par catégorie', () => {
    expect(filterSkills(skills, 'Usinage').map(s => s.skillId)).toEqual(['s1', 's4']);
    expect(filterSkills(skills, '').length).toBe(4);
  });
});

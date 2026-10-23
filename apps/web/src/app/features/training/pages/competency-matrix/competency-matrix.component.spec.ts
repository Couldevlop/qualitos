import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CompetencyMatrixService } from '../../competency-matrix/competency-matrix.service';
import { CompetencyMatrixView, MatrixCell, MatrixSkill } from '../../competency-matrix/competency-matrix.types';
import { CompetencyOperatorDialogComponent } from '../competency-operator-dialog/competency-operator-dialog.component';
import { CompetencySkillDialogComponent } from '../competency-skill-dialog/competency-skill-dialog.component';
import { CompetencyMatrixComponent } from './competency-matrix.component';

/**
 * La matrice de compétences éditable (ADR 0098) : lecture, couverture par
 * colonne, cycle des niveaux (souris et clavier), écriture optimiste qui revient
 * sur un refus, ajout et retrait d'opérateurs et de compétences.
 */
describe('CompetencyMatrixComponent', () => {

  const skill = (skillId: string, name: string, category: string | null, trainerName = 'Formateur ' + name): MatrixSkill => ({
    skillId, name, category, trainerUserId: 't-' + skillId, trainerName, autonomous: 0, atRisk: true
  });

  const view = (over: Partial<CompetencyMatrixView> = {}): CompetencyMatrixView => ({
    workshops: ['Usinage', 'Assemblage', 'Qualité', 'Maintenance'],
    defaultWorkshops: true,
    operators: [
      { id: 'o-1', userId: 'u-1', name: 'Anna Dubois', workshop: 'Usinage' },
      { id: 'o-2', userId: 'u-2', name: 'Boris Lemaire', workshop: 'Usinage' },
      { id: 'o-3', userId: 'u-3', name: 'Chloé Martin', workshop: 'Assemblage' }
    ],
    skills: [
      skill('s-1', 'Tournage CN', 'Usinage'),
      skill('s-2', 'Soudure', 'Assemblage'),
      skill('s-3', 'Métrologie', 'Qualité')
    ],
    cells: [
      { userId: 'u-1', skillId: 's-1', level: 4 },
      { userId: 'u-2', skillId: 's-1', level: 2 },
      { userId: 'u-1', skillId: 's-2', level: 3 },
      { userId: 'u-3', skillId: 's-2', level: 1 }
    ],
    ...over
  });

  let fixture: ComponentFixture<CompetencyMatrixComponent>;
  let component: CompetencyMatrixComponent;
  let service: jasmine.SpyObj<CompetencyMatrixService>;
  let dialog: { open: jasmine.Spy };
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialogResult: unknown;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const tous = (sel: string) => Array.from(hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`));
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  async function setup(opts: { manage?: boolean; admin?: boolean; matrix?: Observable<CompetencyMatrixView> } = {}): Promise<void> {
    service = jasmine.createSpyObj<CompetencyMatrixService>('CompetencyMatrixService',
      ['matrix', 'addOperator', 'removeOperator', 'addSkill', 'removeSkill', 'setLevel', 'workshops', 'updateWorkshops']);
    service.matrix.and.returnValue(opts.matrix ?? of(view()));
    dialogResult = undefined;
    dialog = { open: jasmine.createSpy('open').and.callFake(() => ({ afterClosed: () => of(dialogResult) })) };
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const manage = opts.manage ?? true;
    const admin = opts.admin ?? false;
    const authz = { can: (code: string) => of(code === 'training.manage' ? manage : code === 'authz.manage' ? admin : false) };
    await TestBed.configureTestingModule({
      declarations: [CompetencyMatrixComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: CompetencyMatrixService, useValue: service },
        { provide: AuthzService, useValue: authz },
        { provide: MatSnackBar, useValue: snack }
      ]
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();
    fixture = TestBed.createComponent(CompetencyMatrixComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const cellule = (o: number, s: number) => component.level(component.operators[o], component.skills[s]);

  describe('lecture', () => {
    it('affiche les indicateurs, la grille et la moyenne par opérateur', async () => {
      await setup();
      expect(un('kpi-operateurs')?.textContent).toContain('3');
      expect(un('kpi-competences')?.textContent).toContain('3');
      // Tournage : 2 autonomes ; Soudure : 1 (Anna) ; Métrologie : 0 → 2 à risque.
      expect(un('kpi-risque')?.textContent).toContain('2');
      expect(un('kpi-formateur')?.textContent).toContain('1');
      expect(tous('ligne-operateur').length).toBe(3);
      expect(tous('entete-competence').length).toBe(3);
      expect(tous('moyenne').map(e => e.textContent?.trim())).toEqual(['2.3', '0.7', '0.3']);
    });

    it('nomme chaque case « Opérateur – Compétence : Niveau » et marque une case vide d\'un point', async () => {
      await setup();
      const cases = tous('case');
      expect(cases[0].getAttribute('aria-label')).toBe('Anna Dubois – Tournage CN : Formateur');
      expect(cases[2].getAttribute('aria-label')).toBe('Anna Dubois – Métrologie : Non formé');
      expect(cases[2].textContent?.trim()).toBe('·');
    });

    it('dit, dans le panneau des risques, qui tient seul la compétence — ou que personne ne la tient', async () => {
      await setup();
      const risques = tous('risque');
      expect(risques.length).toBe(2);
      expect(risques[0].textContent).toContain('Soudure');
      expect(risques[0].textContent).toContain('Seule personne autonome : Anna Dubois');
      expect(risques[0].textContent).toContain('Formateur : Formateur Soudure');
      expect(risques[1].textContent).toContain('Aucune personne autonome');
      expect(component.risks.map(r => r.soleAutonomous)).toEqual(['Anna Dubois', null]);
    });

    it('compte les personnes autonomes au pied de chaque colonne et signale les colonnes à risque', async () => {
      await setup();
      const pieds = tous('pied');
      expect(pieds.map(p => p.textContent?.replace(/\s+/g, '')))
        .toEqual(['2', '1⚠àrisque', '0⚠àrisque']);
      expect(pieds[0].classList).not.toContain('pied--risque');
      expect(pieds[1].classList).toContain('pied--risque');
    });

    it('n\'a plus aucun risque quand chaque compétence a deux autonomes', async () => {
      const cells: MatrixCell[] = ['s-1', 's-2', 's-3'].flatMap(s => [
        { userId: 'u-1', skillId: s, level: 2 }, { userId: 'u-2', skillId: s, level: 3 }
      ]);
      await setup({ matrix: of(view({ cells })) });
      expect(component.atRiskCount).toBe(0);
      expect(un('risques')?.textContent).toContain('Aucun risque');
      expect(un('kpi-risque')?.classList).toContain('kpi--ok');
    });

    it('dit que la matrice est vide quand il n\'y a ni opérateur ni compétence', async () => {
      await setup({ matrix: of(view({ operators: [], skills: [], cells: [] })) });
      expect(un('vide')).not.toBeNull();
      expect(un('matrice')).toBeNull();
      expect(un('risques')).toBeNull();
    });

    it('signale l\'absence d\'opérateur quand seules des compétences sont suivies', async () => {
      await setup({ matrix: of(view({ operators: [], cells: [] })) });
      expect(un('matrice')?.textContent).toContain('Aucun opérateur dans la matrice pour le moment.');
    });

    it('signale l\'échec de lecture et relit sur demande', async () => {
      await setup({ matrix: throwError(() => ({ status: 404 })) });
      expect(component.failed).toBeTrue();
      expect(snack.open).toHaveBeenCalledWith('Impossible de charger la matrice de compétences.', undefined, { duration: 5000 });
      service.matrix.and.returnValue(of(view()));
      component.charger();
      fixture.detectChanges();
      expect(component.failed).toBeFalse();
      expect(tous('ligne-operateur').length).toBe(3);
    });

    it('expose les libellés d\'action par nom, jamais par identifiant', async () => {
      await setup();
      expect(component.removeOperatorLabel(component.operators[0])).toBe('Retirer Anna Dubois de la matrice');
      expect(component.removeSkillLabel(component.skills[0])).toBe('Retirer la compétence Tournage CN de la matrice');
      expect(hote().textContent).not.toContain('u-1');
      expect(component.trackRisk(0, component.risks[0])).toBe('s-2');
      expect(component.trackOperator(0, component.operators[0])).toBe('o-1');
      expect(component.trackSkill(0, component.skills[0])).toBe('s-1');
    });
  });

  describe('filtres', () => {
    it('cherche un opérateur, filtre par atelier et par catégorie, puis efface tout', async () => {
      await setup();
      component.search = 'boris';
      fixture.detectChanges();
      expect(component.visibleOperators.map(o => o.name)).toEqual(['Boris Lemaire']);

      component.search = '';
      component.workshopFilter = 'Assemblage';
      component.categoryFilter = 'Qualité';
      fixture.detectChanges();
      expect(component.visibleOperators.map(o => o.name)).toEqual(['Chloé Martin']);
      expect(component.visibleSkills.map(s => s.name)).toEqual(['Métrologie']);
      expect(component.workshopOptions).toContain('Maintenance');
      expect(component.categoryOptions).toEqual(['Assemblage', 'Qualité', 'Usinage']);

      component.workshopFilter = 'Maintenance';
      fixture.detectChanges();
      expect(un('matrice')?.textContent).toContain('Aucun opérateur ne correspond aux filtres.');
      component.resetFilters();
      fixture.detectChanges();
      expect(component.visibleOperators.length).toBe(3);
      expect(component.visibleSkills.length).toBe(3);
    });

    it('estompe les colonnes couvertes quand on met les risques en évidence', async () => {
      await setup();
      un('mettre-en-evidence')!.click();
      fixture.detectChanges();
      expect(component.highlightRisks).toBeTrue();
      expect(un('mettre-en-evidence')?.getAttribute('aria-pressed')).toBe('true');
      const entetes = tous('entete-competence');
      expect(entetes[0].classList).toContain('estompe');
      expect(entetes[1].classList).not.toContain('estompe');
    });
  });

  describe('niveaux', () => {
    it('fait tourner le niveau d\'un clic, l\'enregistre, et repasse de 4 à 0', async () => {
      await setup();
      service.setLevel.and.callFake(req => of({ userId: req.userId, skillId: req.skillId, level: req.level }));
      tous('case')[0].click();
      fixture.detectChanges();
      expect(service.setLevel).toHaveBeenCalledWith({ userId: 'u-1', skillId: 's-1', level: 0 });
      expect(cellule(0, 0)).toBe(0);

      tous('case')[0].click();
      expect(service.setLevel).toHaveBeenCalledWith({ userId: 'u-1', skillId: 's-1', level: 1 });
      expect(cellule(0, 0)).toBe(1);
    });

    it('renvoie le dernier niveau voulu quand on reclique pendant l\'écriture', async () => {
      await setup();
      const reponses: Subject<MatrixCell>[] = [];
      service.setLevel.and.callFake(() => { const s = new Subject<MatrixCell>(); reponses.push(s); return s; });
      const o = component.operators[2];
      const s = component.skills[2];
      component.cycle(o, s);
      component.cycle(o, s);
      expect(service.setLevel).toHaveBeenCalledTimes(1);
      expect(component.level(o, s)).toBe(2);

      reponses[0].next({ userId: 'u-3', skillId: 's-3', level: 1 });
      expect(service.setLevel).toHaveBeenCalledTimes(2);
      expect(service.setLevel.calls.mostRecent().args[0]).toEqual({ userId: 'u-3', skillId: 's-3', level: 2 });
      reponses[1].next({ userId: 'u-3', skillId: 's-3', level: 2 });
      expect(service.setLevel).toHaveBeenCalledTimes(2);
      expect(component.level(o, s)).toBe(2);
    });

    it('rétablit la valeur du serveur quand l\'écriture est refusée', async () => {
      await setup();
      service.setLevel.and.returnValue(throwError(() => ({ status: 404 })));
      component.cycle(component.operators[1], component.skills[0]);
      expect(cellule(1, 0)).toBe(2);
      expect(snack.open).toHaveBeenCalledWith('Le niveau n\'a pas pu être enregistré.', undefined, { duration: 5000 });
    });

    it('ne fait rien tourner en lecture seule : les cases ne sont pas des boutons', async () => {
      await setup({ manage: false });
      expect(un('ajouter-operateur')).toBeNull();
      expect(un('ajouter-competence')).toBeNull();
      expect(un('retirer-operateur')).toBeNull();
      expect(hote().querySelector('button.case__bouton')).toBeNull();
      expect(tous('case')[0].getAttribute('role')).toBe('img');
      component.cycle(component.operators[0], component.skills[0]);
      expect(service.setLevel).not.toHaveBeenCalled();
    });

    it('déplace le focus aux flèches, à Début et à Fin, sans sortir de la grille', async () => {
      await setup();
      const evt = (key: string) => new KeyboardEvent('keydown', { key, cancelable: true });

      const droite = evt('ArrowRight');
      component.onCellKeydown(droite, 0, 0);
      expect(droite.defaultPrevented).toBeTrue();
      expect([component.activeRow, component.activeCol]).toEqual([0, 1]);
      expect(document.activeElement?.getAttribute('data-cell')).toBe('0-1');
      fixture.detectChanges();
      expect(hote().querySelector('[data-cell="0-1"]')?.getAttribute('tabindex')).toBe('0');
      expect(hote().querySelector('[data-cell="0-0"]')?.getAttribute('tabindex')).toBe('-1');

      component.onCellKeydown(evt('ArrowDown'), 0, 1);
      expect([component.activeRow, component.activeCol]).toEqual([1, 1]);
      component.onCellKeydown(evt('End'), 1, 1);
      expect([component.activeRow, component.activeCol]).toEqual([1, 2]);
      component.onCellKeydown(evt('Home'), 1, 2);
      expect([component.activeRow, component.activeCol]).toEqual([1, 0]);
      component.onCellKeydown(evt('ArrowLeft'), 1, 0);
      expect([component.activeRow, component.activeCol]).toEqual([1, 0]);
      component.onCellKeydown(evt('ArrowUp'), 0, 0);
      expect([component.activeRow, component.activeCol]).toEqual([1, 0]);

      const autre = evt('a');
      component.onCellKeydown(autre, 1, 0);
      expect(autre.defaultPrevented).toBeFalse();
    });

    it('fait tourner le niveau à Entrée, comme au clic (bouton natif)', async () => {
      await setup();
      service.setLevel.and.callFake(req => of({ userId: req.userId, skillId: req.skillId, level: req.level }));
      const bouton = tous('case')[1] as HTMLButtonElement;
      expect(bouton.tagName).toBe('BUTTON');
      expect(bouton.type).toBe('button');
      bouton.click();
      expect(service.setLevel).toHaveBeenCalledWith({ userId: 'u-1', skillId: 's-2', level: 4 });
    });

    it('garde la case active dans la grille quand un filtre la réduit', async () => {
      await setup();
      component.activeRow = 2;
      component.activeCol = 2;
      component.workshopFilter = 'Assemblage';
      expect(component.isActive(0, 2)).toBeTrue();
    });
  });

  describe('opérateurs et compétences', () => {
    it('ouvre l\'ajout d\'opérateur avec les ateliers, l\'atelier filtré et les comptes déjà inscrits, puis reprend la matrice', async () => {
      await setup();
      component.workshopFilter = 'Assemblage';
      const apres = view({ operators: [...view().operators, { id: 'o-4', userId: 'u-4', name: 'Dana Roy', workshop: 'Assemblage' }] });
      dialogResult = apres;
      un('ajouter-operateur')!.click();
      const [type, config] = dialog.open.calls.mostRecent().args;
      expect(type).toBe(CompetencyOperatorDialogComponent);
      expect(config.data).toEqual({ workshops: view().workshops, workshop: 'Assemblage', excludeIds: ['u-1', 'u-2', 'u-3'] });
      expect(config.panelClass).toBe('qos-dialog-panel');
      expect(component.operators.length).toBe(4);
    });

    it('n\'altère rien quand l\'ajout d\'opérateur est abandonné', async () => {
      await setup();
      un('ajouter-operateur')!.click();
      expect(dialog.open.calls.mostRecent().args[1].data.workshop).toBeUndefined();
      expect(component.operators.length).toBe(3);
    });

    it('ouvre l\'ajout de compétence avec les catégories et les ateliers, puis reprend la matrice', async () => {
      await setup();
      component.categoryFilter = 'Qualité';
      dialogResult = view({ skills: [...view().skills, skill('s-4', 'Peinture', 'Finition')] });
      un('ajouter-competence')!.click();
      const [type, config] = dialog.open.calls.mostRecent().args;
      expect(type).toBe(CompetencySkillDialogComponent);
      expect(config.data.category).toBe('Qualité');
      expect(config.data.categories).toEqual(['Assemblage', 'Qualité', 'Usinage', 'Maintenance']);
      expect(component.skills.length).toBe(4);

      dialogResult = undefined;
      component.categoryFilter = '';
      component.addSkill();
      expect(dialog.open.calls.mostRecent().args[1].data.category).toBeUndefined();
    });

    it('retire un opérateur après confirmation', async () => {
      await setup();
      dialogResult = true;
      service.removeOperator.and.returnValue(of(view({ operators: view().operators.slice(1) })));
      tous('retirer-operateur')[0].click();
      const [type, config] = dialog.open.calls.mostRecent().args;
      expect(type).toBe(ConfirmDialogComponent);
      expect(config.data.title).toBe('Retirer Anna Dubois de la matrice ?');
      expect(config.data.destructive).toBeTrue();
      expect(service.removeOperator).toHaveBeenCalledWith('o-1');
      expect(component.operators.length).toBe(2);
    });

    it('ne retire rien sans confirmation', async () => {
      await setup();
      dialogResult = false;
      component.removeOperator(component.operators[0]);
      component.removeSkill(component.skills[0]);
      expect(service.removeOperator).not.toHaveBeenCalled();
      expect(service.removeSkill).not.toHaveBeenCalled();
    });

    it('retire une compétence après confirmation, et oublie un filtre de catégorie devenu vide', async () => {
      await setup();
      dialogResult = true;
      component.categoryFilter = 'Qualité';
      service.removeSkill.and.returnValue(of(view({ skills: view().skills.slice(0, 2) })));
      component.removeSkill(component.skills[2]);
      expect(dialog.open.calls.mostRecent().args[1].data.title).toBe('Retirer « Métrologie » de la matrice ?');
      expect(service.removeSkill).toHaveBeenCalledWith('s-3');
      expect(component.skills.length).toBe(2);
      expect(component.categoryFilter).toBe('');
    });

    it('signale un retrait refusé', async () => {
      await setup();
      dialogResult = true;
      service.removeOperator.and.returnValue(throwError(() => ({ status: 404 })));
      service.removeSkill.and.returnValue(throwError(() => ({ status: 404 })));
      component.removeOperator(component.operators[0]);
      component.removeSkill(component.skills[0]);
      expect(snack.open).toHaveBeenCalledTimes(2);
      expect(snack.open.calls.mostRecent().args[0]).toBe('Le retrait n\'a pas pu être enregistré.');
      expect(component.operators.length).toBe(3);
    });

    it('montre le lien vers les ateliers au seul administrateur du client', async () => {
      await setup({ admin: true });
      expect(un('ateliers')?.getAttribute('href')).toBe('/admin/competences');
    });

    it('cache le lien vers les ateliers aux autres', async () => {
      await setup();
      expect(un('ateliers')).toBeNull();
    });
  });
});

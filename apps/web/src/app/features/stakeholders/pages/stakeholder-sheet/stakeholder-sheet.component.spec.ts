import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { StakeholdersService } from '../../stakeholders.service';
import {
  StakeholderRequirement,
  StakeholderSheet
} from '../../stakeholders.types';
import { StakeholderSheetComponent } from './stakeholder-sheet.component';

/**
 * La fiche d'une partie intéressée, et surtout sa dernière section : le
 * **tableau** des exigences.
 *
 * <p>Ce qui est vérifié ici n'est pas une mise en forme mais ce que le tableau
 * promet : une ligne par exigence, les colonnes toujours aux mêmes places, les
 * gestes sur la ligne qu'ils concernent, et rien de tout cela en lecture seule
 * — sauf les liens sortants, qui sont précisément ce qu'un auditeur vient
 * suivre.
 */
describe('StakeholderSheetComponent', () => {

  function exigence(over: Partial<StakeholderRequirement> = {}): StakeholderRequirement {
    return {
      id: 'ex-1',
      need: 'Respecter les délais de livraison',
      requirement: 'Respect du taux de service contractuel',
      origin: 'Contrat C-2026-114, art. 9.1',
      nature: 'CONTRACTUAL',
      risk: 'Retard de livraison',
      riskLevel: 'MEDIUM',
      control: 'Revue de contrat',
      indicator: 'Taux de livraison à l’heure',
      responsible: 'Directeur de production',
      evidence: 'Tableaux de bord',
      status: 'APPLICABLE',
      riskId: null, ncId: null, auditId: null, documentId: null, capaId: null,
      ...over
    };
  }

  function fiche(requirements: StakeholderRequirement[] = [exigence()]): StakeholderSheet {
    return {
      id: 'pi-1',
      name: 'Client A',
      category: 'CUSTOMERS',
      side: 'EXTERNAL',
      description: 'Donneur d’ordre aéronautique.',
      contact: null,
      sites: 'Tous les sites',
      needs: null,
      requirementsSummary: null,
      relevance: 'RELEVANT',
      relevanceJustification: 'Impact direct sur la conformité du produit.',
      risksAndOpportunities: null,
      qualityImpact: 'HIGH',
      regulatoryImpact: 'MEDIUM',
      criticality: 'HIGH',
      owner: null,
      controls: null,
      indicators: null,
      documents: null,
      actions: null,
      nextReviewOn: null,
      lastReviewedOn: null,
      reviewOverdue: false,
      requirements
    };
  }

  const MOI: AuthUser = {
    userId: 'u-1', tenantId: 't-1', displayName: 'Awa Diop', roles: ['QUALITY_MANAGER']
  };

  let fixture: ComponentFixture<StakeholderSheetComponent>;
  let component: StakeholderSheetComponent;
  let svc: jasmine.SpyObj<StakeholdersService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let auth: jasmine.SpyObj<AuthService>;

  /** Ce que la prochaine fenêtre ouverte rendra à sa fermeture. */
  function dialogRend(valeur: unknown): void {
    dialog.open.and.returnValue({ afterClosed: () => of(valeur) } as never);
  }

  async function build(canManage: boolean, sheet = fiche()): Promise<void> {
    svc = jasmine.createSpyObj<StakeholdersService>('StakeholdersService', [
      'update', 'markReviewed', 'addRequirement', 'setRequirementStatus',
      'linkRequirement', 'removeRequirement', 'openCapa', 'openRisk'
    ]);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['snapshot']);
    auth.snapshot.and.returnValue(MOI);
    dialogRend(undefined);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderSheetComponent],
      // FormsModule comme en production : la fiche pilote ses champs en
      // [(ngModel)], que SharedModule n'apporte pas. RouterModule pour les
      // liens sortants, qui sont de vrais routerLink.
      imports: [
        SharedModule, UiModule, UserPickerModule, FormsModule,
        RouterTestingModule, NoopAnimationsModule
      ],
      providers: [
        // Le sélecteur de personne (annuaire) injecte HttpClient : sans lui, le
        // gabarit ne se rend pas du tout (NG0201).
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: StakeholdersService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: auth }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(StakeholderSheetComponent);
    component = fixture.componentInstance;
    component.sheet = sheet;
    component.canManage = canManage;
    // Le brouillon naît dans ngOnChanges, qu'un banc doit appeler lui-même :
    // Angular ne le déclenche que pour une entrée liée par un gabarit parent.
    component.ngOnChanges();
    fixture.detectChanges();
  }

  function cellules(selecteur: string): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll(selecteur)) as HTMLElement[];
  }

  // ---------- le tableau des exigences ----------

  describe('en pilotage qualité', () => {

    beforeEach(async () => build(true));

    it('rend les exigences en TABLEAU, une ligne par exigence', () => {
      component.sheet = fiche([exigence(), exigence({ id: 'ex-2' })]);
      fixture.detectChanges();

      const lignes = cellules('.exigences__table tbody tr');
      expect(lignes.length).toBe(2);
      // Toutes les lignes ont les mêmes colonnes : c'est tout l'intérêt d'un
      // tableau sur une pile de cartes.
      expect(cellules('.exigences__table tbody [data-test="statut-exigence"]').length).toBe(2);
    });

    it('nomme ses colonnes, et l’exigence est l’en-tête de sa ligne', () => {
      const entetes = cellules('.exigences__table thead th').map(th => th.textContent!.trim());
      expect(entetes.length).toBe(5);
      expect(entetes[0]).toBe('Exigence');
      expect(entetes[3]).toBe('Liens');

      const sujet = fixture.nativeElement.querySelector('.exigences__sujet') as HTMLElement;
      expect(sujet.tagName).toBe('TH');
      expect(sujet.getAttribute('scope')).toBe('row');
      expect(sujet.textContent).toContain('Respect du taux de service contractuel');
      expect(sujet.textContent).toContain('Contrat C-2026-114, art. 9.1');
    });

    it('écrit le niveau de risque, jamais la seule couleur', () => {
      const risque = fixture.nativeElement.querySelector('.exigences__risque') as HTMLElement;
      expect(risque.textContent!.trim()).toBe('Moyen');
      expect(risque.querySelector('.chip--warn')).withContext('jeton de niveau').toBeTruthy();
    });

    it('porte les boutons de la maquette sur chaque ligne', () => {
      const attendus = ['lier-risque', 'lier-nc', 'lier-audit', 'lier-document', 'creer-action'];
      for (const test of attendus) {
        expect(cellules(`.exigences__liens [data-test="${test}"]`).length)
          .withContext(test).toBe(1);
      }
      expect(cellules('[data-test="retirer-exigence"]').length).toBe(1);
    });

    it('n’offre plus d’action quand la CAPA est déjà ouverte', () => {
      component.sheet = fiche([exigence({ capaId: 'capa-1' })]);
      fixture.detectChanges();

      expect(cellules('[data-test="creer-action"]').length).toBe(0);
      expect(cellules('[data-test="ouvrir-add_task"]').length).toBe(1);
    });

    it('marque le bouton d’un rattachement déjà posé', () => {
      component.sheet = fiche([exigence({ riskId: 'r-1' })]);
      fixture.detectChanges();

      const bouton = fixture.nativeElement.querySelector('[data-test="lier-risque"]') as HTMLElement;
      expect(bouton.classList).toContain('lie');
    });

    it('affiche l’exigence sans niveau de risque sans mentir sur la colonne', () => {
      component.sheet = fiche([exigence({ riskLevel: null, origin: null })]);
      fixture.detectChanges();

      const risque = fixture.nativeElement.querySelector('.exigences__risque') as HTMLElement;
      expect(risque.textContent!.trim()).toBe('—');
    });

    it('dit que la liste est vide plutôt que de montrer un tableau vide', () => {
      component.sheet = fiche([]);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('.exigences__table')).toBeNull();
      expect(fixture.nativeElement.textContent).toContain('Aucune exigence pour le moment');
    });
  });

  describe('en lecture seule', () => {

    beforeEach(async () => build(false, fiche([exigence({ riskId: 'r-1', documentId: 'd-1' })])));

    it('montre le statut sans permettre de le changer', () => {
      expect(fixture.nativeElement.querySelector('[data-test="statut-exigence"]')).toBeNull();
      const lu = fixture.nativeElement.querySelector('[data-test="statut-lecture"]') as HTMLElement;
      expect(lu.textContent!.trim()).toBe('Applicable');
    });

    it('laisse les liens SORTANTS ouvrables : c’est ce qu’un auditeur vient suivre', () => {
      expect(cellules('[data-test="liens-sortants"] a').length).toBe(2);
      expect(cellules('[data-test="lier-risque"]').length).toBe(0);
      expect(cellules('[data-test="retirer-exigence"]').length).toBe(0);
      // La colonne de retrait disparaît aussi de l'en-tête : pas de colonne vide.
      expect(cellules('.exigences__table thead th').length).toBe(4);
    });

    it('n’écrit rien, quel que soit le geste tenté', () => {
      component.save();
      component.markReviewed();
      component.addRequirement();
      component.setStatus(exigence(), 'CONTROLLED');
      component.link(exigence(), 'RISK');
      component.openCapa(exigence());
      component.openRisk(exigence());
      component.removeRequirement(exigence());

      expect(svc.update).not.toHaveBeenCalled();
      expect(svc.markReviewed).not.toHaveBeenCalled();
      expect(svc.addRequirement).not.toHaveBeenCalled();
      expect(svc.setRequirementStatus).not.toHaveBeenCalled();
      expect(svc.linkRequirement).not.toHaveBeenCalled();
      expect(svc.openCapa).not.toHaveBeenCalled();
      expect(svc.openRisk).not.toHaveBeenCalled();
      expect(svc.removeRequirement).not.toHaveBeenCalled();
      expect(dialog.open).not.toHaveBeenCalled();
    });
  });

  // ---------- les gestes d'une ligne ----------

  describe('les gestes d’une ligne', () => {

    beforeEach(async () => build(true));

    it('change le statut d’une ligne, et seulement de celle-là', async () => {
      await build(true, fiche([exigence(), exigence({ id: 'ex-2', status: 'TO_ANALYSE' })]));
      svc.setRequirementStatus.and.returnValue(of(exigence({ status: 'CONTROLLED' })));

      // Reposer le statut déjà en place n'appelle rien : un tableau se parcourt
      // au clavier, et traverser une colonne de listes ne doit rien écrire.
      component.setStatus(component.requirements[0], 'APPLICABLE');
      expect(svc.setRequirementStatus).not.toHaveBeenCalled();

      component.setStatus(component.requirements[0], 'CONTROLLED');

      expect(svc.setRequirementStatus).toHaveBeenCalledWith('pi-1', 'ex-1', 'CONTROLLED');
      expect(component.requirements[0].status).toBe('CONTROLLED');
      expect(component.requirements[1].status).withContext('la ligne voisine').toBe('TO_ANALYSE');
    });

    it('dit pourquoi un statut n’a pas pu changer', () => {
      svc.setRequirementStatus.and.returnValue(throwError(() => new Error('réseau')));

      component.setStatus(component.requirements[0], 'CONTROLLED');

      expect(snack.open).toHaveBeenCalled();
    });

    it('rattache l’exigence à l’objet choisi dans la fenêtre', () => {
      dialogRend('r-7');
      svc.linkRequirement.and.returnValue(of(exigence({ riskId: 'r-7' })));

      component.link(component.requirements[0], 'RISK');

      expect(svc.linkRequirement).toHaveBeenCalledWith('pi-1', 'ex-1', 'RISK', 'r-7');
      expect(component.requirements[0].riskId).toBe('r-7');
    });

    it('ne rattache rien quand la fenêtre est refermée sans choix', () => {
      dialogRend(undefined);

      component.link(component.requirements[0], 'RISK');

      expect(svc.linkRequirement).not.toHaveBeenCalled();
    });

    it('signale l’échec d’un rattachement', () => {
      dialogRend('r-7');
      svc.linkRequirement.and.returnValue(throwError(() => new Error('refus')));

      component.link(component.requirements[0], 'RISK');

      expect(snack.open).toHaveBeenCalled();
    });

    it('ouvre une CAPA à trente jours, pour soi par défaut', () => {
      svc.openCapa.and.returnValue(of(exigence({ capaId: 'capa-1', status: 'TO_REASSESS' })));

      component.openCapa(component.requirements[0]);

      const args = svc.openCapa.calls.mostRecent().args;
      expect(args[0]).toBe('pi-1');
      expect(args[1]).toBe('ex-1');
      expect(args[3]).toBe('u-1');
      expect(args[4]).toBe('Awa Diop');
      const echeance = new Date(args[5]);
      const jours = Math.round((echeance.getTime() - Date.now()) / 86400000);
      expect(jours).toBeGreaterThanOrEqual(29);
      expect(jours).toBeLessThanOrEqual(30);
      expect(component.requirements[0].capaId).toBe('capa-1');
    });

    it('n’ouvre pas deux CAPA pour la même exigence', async () => {
      await build(true, fiche([exigence({ capaId: 'capa-1' })]));

      component.openCapa(component.requirements[0]);

      expect(svc.openCapa).not.toHaveBeenCalled();
    });

    it('refuse d’ouvrir une CAPA sans compte identifiable', () => {
      auth.snapshot.and.returnValue(null);

      component.openCapa(component.requirements[0]);

      expect(svc.openCapa).not.toHaveBeenCalled();
      expect(snack.open).toHaveBeenCalled();
    });

    it('signale l’échec d’ouverture de CAPA', () => {
      svc.openCapa.and.returnValue(throwError(() => new Error('refus')));

      component.openCapa(component.requirements[0]);

      expect(snack.open).toHaveBeenCalled();
    });

    it('inscrit au registre le risque énoncé sur l’exigence', () => {
      dialogRend({
        type: 'QUALITY', process: 'Production', severity: 4, probability: 3,
        ownerId: 'u-1', ownerName: 'Awa Diop'
      });
      svc.openRisk.and.returnValue(of(exigence({ riskId: 'rsk-1' })));

      component.openRisk(component.requirements[0]);

      expect(svc.openRisk).toHaveBeenCalledWith('pi-1', 'ex-1', 'QUALITY', 'Production', 4, 3,
        'u-1', 'Awa Diop');
      expect(component.requirements[0].riskId).toBe('rsk-1');
      expect(snack.open).toHaveBeenCalled();
    });

    it('n’inscrit rien quand la fenêtre est refermée sans choix', () => {
      dialogRend(undefined);

      component.openRisk(component.requirements[0]);

      expect(svc.openRisk).not.toHaveBeenCalled();
    });

    it('n’inscrit pas deux fois le risque d’une même exigence', async () => {
      // La ligne existe déjà au registre : la réinscrire créerait deux fiches
      // du même risque, cotées séparément.
      await build(true, fiche([exigence({ riskId: 'rsk-1' })]));

      component.openRisk(component.requirements[0]);

      expect(dialog.open).not.toHaveBeenCalled();
      expect(svc.openRisk).not.toHaveBeenCalled();
    });

    it('n’ouvre pas la fenêtre pour une exigence dont le risque n’est pas énoncé', async () => {
      // Sans énoncé, il n'y a rien à inscrire : une ligne de registre vide ne
      // se cote ni ne se revoit.
      await build(true, fiche([exigence({ risk: null })]));

      component.openRisk(component.requirements[0]);

      expect(dialog.open).not.toHaveBeenCalled();
    });

    it('signale l’échec d’une inscription au registre', () => {
      dialogRend({
        type: 'QUALITY', process: 'Production', severity: 3, probability: 3,
        ownerId: 'u-1', ownerName: 'Awa Diop'
      });
      svc.openRisk.and.returnValue(throwError(() => new Error('refus')));

      component.openRisk(component.requirements[0]);

      expect(snack.open).toHaveBeenCalled();
    });

    it('retire une exigence seulement après confirmation', () => {
      dialogRend(false);
      component.removeRequirement(component.requirements[0]);
      expect(svc.removeRequirement).not.toHaveBeenCalled();

      dialogRend(true);
      svc.removeRequirement.and.returnValue(of(undefined));
      component.removeRequirement(component.requirements[0]);

      expect(svc.removeRequirement).toHaveBeenCalledWith('pi-1', 'ex-1');
      expect(component.requirements.length).toBe(0);
    });

    it('signale l’échec d’un retrait', () => {
      dialogRend(true);
      svc.removeRequirement.and.returnValue(throwError(() => new Error('refus')));

      component.removeRequirement(component.requirements[0]);

      expect(snack.open).toHaveBeenCalled();
    });

    it('ajoute une exigence depuis la fenêtre de saisie', () => {
      dialogRend({ requirement: 'Certificat matière 3.1' });
      svc.addRequirement.and.returnValue(of(exigence({ id: 'ex-2' })));

      component.addRequirement();

      expect(component.requirements.length).toBe(2);
      expect(snack.open).toHaveBeenCalled();
    });

    it('n’ajoute rien quand la fenêtre de saisie est annulée', () => {
      dialogRend(undefined);

      component.addRequirement();

      expect(svc.addRequirement).not.toHaveBeenCalled();
    });

    it('signale l’échec d’un ajout', () => {
      dialogRend({ requirement: 'Certificat matière 3.1' });
      svc.addRequirement.and.returnValue(throwError(() => new Error('refus')));

      component.addRequirement();

      expect(snack.open).toHaveBeenCalled();
    });

    it('suit les lignes par leur identifiant', () => {
      expect(component.trackByRequirement(0, component.requirements[0])).toBe('ex-1');
    });

    it('n’annonce un rechargement que si une exigence a bougé', () => {
      const vus: boolean[] = [];
      component.closed.subscribe(v => vus.push(v));

      component.close();
      svc.setRequirementStatus.and.returnValue(of(exigence({ status: 'CONTROLLED' })));
      component.setStatus(component.requirements[0], 'CONTROLLED');
      component.close();

      expect(vus).toEqual([false, true]);
    });
  });

  // ---------- le reste de la fiche ----------

  describe('la fiche elle-même', () => {

    beforeEach(async () => build(true));

    it('retient la plus élevée des deux cotations, et ne la fait pas saisir', () => {
      expect(component.criticality).toBe('HIGH');

      component.draft.qualityImpact = 'LOW';
      component.draft.regulatoryImpact = 'MEDIUM';
      expect(component.criticality).toBe('MEDIUM');

      component.draft.regulatoryImpact = 'LOW';
      expect(component.criticality).toBe('LOW');
    });

    it('ne se dit modifiée qu’avec un vrai changement, et sait revenir en arrière', () => {
      expect(component.dirty).toBeFalse();

      component.draft.name = 'Client A (groupe)';
      expect(component.dirty).toBeTrue();

      component.discard();
      expect(component.draft.name).toBe('Client A');
      expect(component.dirty).toBeFalse();
    });

    it('dit ce qui empêche d’enregistrer, plutôt que d’éteindre un bouton', () => {
      component.draft.name = '   ';
      expect(component.problems.length).toBe(1);

      component.draft.name = 'Client A';
      component.draft.relevanceJustification = '';
      expect(component.problems.length).toBe(1);

      // Une justification jamais saisie vaut une justification vide : trancher
      // la pertinence sans l'écrire est le même manque pour l'auditeur.
      component.draft.relevanceJustification = null;
      expect(component.problems.length).toBe(1);

      component.draft.relevance = 'TO_DETERMINE';
      expect(component.problems).toEqual([]);
    });

    it('n’enregistre pas une fiche qui ne passerait pas au serveur', () => {
      component.draft.name = '';

      component.save();

      expect(svc.update).not.toHaveBeenCalled();
    });

    it('enregistre la fiche et reprend ce que le serveur rend', () => {
      const rendue = fiche();
      rendue.name = 'Client A (groupe)';
      svc.update.and.returnValue(of(rendue));
      const vues: StakeholderSheet[] = [];
      component.saved.subscribe(s => vues.push(s));

      component.draft.name = 'Client A (groupe)';
      component.save();

      expect(svc.update).toHaveBeenCalled();
      expect(vues.length).toBe(1);
      expect(component.dirty).toBeFalse();
    });

    it('signale l’échec d’un enregistrement', () => {
      svc.update.and.returnValue(throwError(() => new Error('refus')));

      component.draft.name = 'Client A (groupe)';
      component.save();

      expect(snack.open).toHaveBeenCalled();
      expect(component.saving).toBeFalse();
    });

    it('enregistre une revue à la date du jour', () => {
      svc.markReviewed.and.returnValue(of(fiche()));

      component.markReviewed();

      expect(svc.markReviewed).toHaveBeenCalledWith('pi-1');
    });

    it('signale l’échec d’une revue', () => {
      svc.markReviewed.and.returnValue(throwError(() => new Error('refus')));

      component.markReviewed();

      expect(snack.open).toHaveBeenCalled();
    });

    it('retient le responsable choisi dans l’annuaire, et son effacement', () => {
      component.onOwner({ accountId: 'u-2', displayName: 'Jean Perez' } as never);
      expect(component.draft.owner).toEqual({ accountId: 'u-2', name: 'Jean Perez' });

      component.onOwner(null);
      expect(component.draft.owner).toBeNull();
    });

    it('énumère les rattachements établis, chacun avec sa route', () => {
      const liens = component.establishedLinks(exigence({
        riskId: 'r-1', ncId: 'nc-1', auditId: 'a-1', documentId: 'd-1', capaId: 'c-1'
      }));

      expect(liens.map(l => l.route)).toEqual([
        ['/risques', 'r-1'], ['/nc', 'nc-1'], ['/audits', 'a-1'],
        ['/documents', 'd-1'], ['/capa', 'c-1']
      ]);
    });

    it('n’énumère rien quand rien n’est rattaché', () => {
      expect(component.establishedLinks(exigence())).toEqual([]);
    });
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DirectoryService } from '../../../../core/directory/directory.service';
import { CapaActionDialogModule } from '../../capa-action-dialog.module';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaCaseResponse } from '../../capa.types';
import { CapaActionDialogComponent, CapaActionDialogData } from './capa-action-dialog.component';

function action(overrides: Partial<CapaActionResponse> = {}): CapaActionResponse {
  return {
    id: 'act-1',
    capaId: 'capa-1',
    title: 'Remplacer le joint du presse-étoupe',
    status: 'PENDING',
    actionType: 'CORRECTIVE',
    ...overrides
  };
}

/**
 * LA FENÊTRE D'UNE ACTION CAPA : ce qu'on va faire, qui en répond, pour quand —
 * et, pour une action corrective, À QUOI L'ON VERRA QUE ÇA A MARCHÉ.
 *
 * <p>Trois promesses se vérifient ici.
 *
 * <p>La <b>date de début</b> existe : le tableau ne montrait que l'échéance,
 * alors que le serveur porte les deux. Et leur <b>ordre</b> se signale sans
 * bloquer : une échéance avant le début est presque toujours une faute de
 * frappe sur l'une des deux dates, et refuser l'enregistrement ferait perdre le
 * reste du formulaire pour un chiffre.
 *
 * <p>Le <b>critère d'efficacité</b> se demande ICI, au moment où l'action
 * corrective s'écrit, parce que l'ADR 0108 le veut posé À L'AVANCE : demandé à
 * la vérification, il laisserait choisir après coup la mesure qui arrange.
 */
describe('CapaActionDialogComponent', () => {

  let fixture: ComponentFixture<CapaActionDialogComponent>;
  let composant: CapaActionDialogComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaActionDialogComponent, boolean>>;
  /** L'ordre réel des appels au service : il porte une promesse (le critère d'abord). */
  let ordre: string[];

  async function setup(data: Partial<CapaActionDialogData> = {}): Promise<void> {
    ordre = [];
    capa = jasmine.createSpyObj<CapaService>('CapaService', ['addAction', 'updateAction', 'updateCase']);
    capa.addAction.and.callFake(() => {
      ordre.push('addAction');
      return of(action());
    });
    capa.updateAction.and.callFake(() => {
      ordre.push('updateAction');
      return of(action());
    });
    capa.updateCase.and.callFake(() => {
      ordre.push('updateCase');
      return of({ id: 'capa-1' } as CapaCaseResponse);
    });
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaActionDialogComponent, boolean>>(
      'MatDialogRef', ['close']);
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    directory.getByAccount.and.returnValue(of({
      id: 'u-1', accountId: 'u-alice', displayName: 'Alice Durand', email: 'alice@acme.com', active: true
    }));

    await TestBed.configureTestingModule({
      imports: [CapaActionDialogModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: DirectoryService, useValue: directory },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        { provide: MAT_DIALOG_DATA, useValue: <CapaActionDialogData>{ caseId: 'capa-1', ...data } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaActionDialogComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const un = (test: string): HTMLElement | null =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-test="${test}"]`);

  describe('la date de début, et l’ordre des deux dates', () => {

    it('envoie la date de début saisie', async () => {
      await setup();
      composant.form.patchValue({ title: 'Recalibrer le cobot', decidedOn: '2026-11-02', dueDate: '2026-12-01' });

      composant.submit();

      expect(capa.addAction).toHaveBeenCalled();
      const corps = capa.addAction.calls.mostRecent().args[1];
      expect(corps.decidedOn).toBe('2026-11-02');
      expect(corps.dueDate).toBe('2026-12-01');
    });

    it('relit la date de début d’une action existante', async () => {
      await setup({ action: action({ decidedOn: '2026-10-05', dueDate: '2026-11-05' }) });
      expect(composant.form.controls.decidedOn.value).toBe('2026-10-05');
    });

    it('signale une échéance antérieure au début, et laisse tout de même enregistrer', async () => {
      await setup();
      composant.form.patchValue({ title: 'Recalibrer le cobot', decidedOn: '2026-12-01', dueDate: '2026-11-02' });
      fixture.detectChanges();

      expect(composant.echeanceAvantDebut).toBeTrue();
      expect(un('action-echeance-avant-debut')).not.toBeNull();
      // L'avertissement ne bloque pas : la règle n'invalide aucun champ requis.
      composant.submit();
      expect(capa.addAction).toHaveBeenCalled();
    });

    it('ne signale rien quand une seule des deux dates est connue', async () => {
      await setup();
      composant.form.patchValue({ title: 'Recalibrer le cobot', decidedOn: '2026-12-01', dueDate: '' });
      expect(composant.echeanceAvantDebut).toBeFalse();

      composant.form.patchValue({ decidedOn: '', dueDate: '2026-11-02' });
      expect(composant.echeanceAvantDebut).toBeFalse();
    });

    it('réévalue la règle quand c’est la DATE DE DÉBUT qui change', async () => {
      // La règle porte sur le groupe et non sur l'échéance : posée sur le champ
      // `dueDate`, elle ne serait pas rejouée en modifiant l'autre date.
      await setup();
      composant.form.patchValue({ title: 'Recalibrer', decidedOn: '2026-11-01', dueDate: '2026-11-10' });
      expect(composant.echeanceAvantDebut).toBeFalse();

      composant.form.patchValue({ decidedOn: '2026-11-20' });
      expect(composant.echeanceAvantDebut).toBeTrue();
    });
  });

  describe('l’évaluation d’efficacité, demandée POUR CHAQUE action (ADR 0131)', () => {

    it('la demande à l’ajout d’une action corrective', async () => {
      await setup();
      expect(composant.demandeCritere).toBeTrue();
      expect(un('action-efficacite-demande')).not.toBeNull();
    });

    /**
     * Elle se demandait une fois par DOSSIER : la deuxième action et les
     * suivantes s'écrivaient donc sans qu'on sache jamais si leur effet serait
     * vérifié. Deux actions d'un même dossier ne se mesurent pas à la même
     * chose — « recalibrer la presse » et « former les opérateurs de nuit » —
     * et ne se vérifient pas forcément par la même personne.
     */
    it('la demande même quand le dossier a déjà posé son critère, et le rappelle à côté',
      async () => {
        await setup({ effectivenessCriterion: 'Aucun rebut pendant 3 mois' });

        expect(composant.demandeCritere).toBeTrue();
        expect(un('action-efficacite-demande')).not.toBeNull();
        // Les deux coexistent : on écrit mieux la seconde en voyant la première.
        expect(un('action-efficacite-rappel')!.textContent).toContain('Aucun rebut pendant 3 mois');
      });

    it('l’envoie AVEC l’action, en une seule écriture, et non sur le dossier', async () => {
      await setup();
      composant.form.patchValue({
        title: 'Recalibrer le cobot',
        effectivenessCriterion: 'Aucun rebut sur ce défaut pendant 3 mois',
        verificationRequired: true,
        verificationAssigneeId: 'kc-amina'
      });

      composant.submit();

      // Une seule requête : deux écritures de suite laisseraient le critère
      // posé sans l'action qu'il mesure.
      expect(ordre).toEqual(['addAction']);
      expect(capa.updateCase).not.toHaveBeenCalled();
      const corps = capa.addAction.calls.mostRecent().args[1];
      expect(corps.effectivenessCriterion).toBe('Aucun rebut sur ce défaut pendant 3 mois');
      expect(corps.verificationRequired).toBeTrue();
      expect(corps.verificationAssigneeId).toBe('kc-amina');
    });

    it('n’emporte aucune clé quand on ne répond ni au critère ni à la question', async () => {
      await setup();
      composant.form.patchValue({ title: 'Recalibrer le cobot' });

      composant.submit();

      expect(capa.updateCase).not.toHaveBeenCalled();
      const corps = capa.addAction.calls.mostRecent().args[1];
      expect('effectivenessCriterion' in corps).toBeFalse();
      expect('verificationRequired' in corps).toBeFalse();
    });

    it('ne demande rien pour une action de confinement : elle ne prétend pas supprimer la cause',
      async () => {
        await setup();
        composant.form.patchValue({ actionType: 'CONTAINMENT' });
        fixture.detectChanges();

        expect(composant.demandeCritere).toBeFalse();
        expect(composant.demandeVerification).toBeFalse();
        expect(un('action-efficacite-demande')).toBeNull();
        expect(un('action-verification-demande')).toBeNull();
      });

    it('ne demande rien quand on CORRIGE une action : son critère est posé, et verrouillé',
      async () => {
        await setup({ action: action() });
        expect(composant.demandeCritere).toBeFalse();
        expect(composant.demandeVerification).toBeFalse();
      });

    it('ne demande rien sur un dossier en lecture seule', async () => {
      await setup({ readOnly: true });
      expect(composant.demandeCritere).toBeFalse();
      expect(composant.demandeVerification).toBeFalse();
    });
  });

  describe('la question « vérifiera-t-on l’efficacité ? »', () => {

    it('la pose à chaque action corrective ou préventive', async () => {
      await setup();
      expect(composant.demandeVerification).toBeTrue();
      expect(un('action-verification-demande')).not.toBeNull();
    });

    /**
     * Elle ne se reposait pas quand le dossier avait tranché — c'est-à-dire
     * presque toujours, la réponse étant donnée à l'ouverture. On écrivait donc
     * toutes les actions suivantes sans la question, et la plateforme paraissait
     * ne la poser nulle part.
     */
    it('la pose même quand le dossier a déjà répondu oui, et le rappelle', async () => {
      await setup({ verificationRequired: true });

      expect(composant.demandeVerification).toBeTrue();
      expect(un('action-verification-demande')).not.toBeNull();
      expect(un('action-verification-rappel-oui')).not.toBeNull();
    });

    it('la pose même quand le dossier a répondu non, et le rappelle', async () => {
      await setup({ verificationRequired: false });

      expect(composant.demandeVerification).toBeTrue();
      expect(un('action-verification-rappel-non')).not.toBeNull();
    });

    it('rappelle ce que CETTE action a répondu quand on la relit', async () => {
      await setup({
        action: { ...action(), effectivenessCriterion: 'Zero recidive sur 6 mois',
                  verificationRequired: true },
        // Le dossier dit autre chose : c'est l'action qu'on relit qui compte.
        effectivenessCriterion: 'Aucun rebut pendant 3 mois',
        verificationRequired: false
      });

      expect(composant.critereRappele).toBe('Zero recidive sur 6 mois');
      expect(composant.verificationDejaTranchee).toBeTrue();
    });
  });

  // --- une nature par discipline -------------------------------------------

  describe('la nature de l’action, imposée par la discipline qui l’ouvre', () => {

    /**
     * Le D3 endigue, le D5 corrige, le D7 prévient : la question était reposée
     * en trois choix sous un titre qui portait déjà la réponse. Un clic de trop,
     * et surtout une action classée sous la discipline voisine dès que la main
     * dérapait.
     */
    it('n’offre que la nature reçue, cochée, à l’ajout', async () => {
      await setup({ actionType: 'CONTAINMENT' });

      expect(composant.natures.map(n => n.value)).toEqual(['CONTAINMENT']);
      expect(composant.form.controls.actionType.value).toBe('CONTAINMENT');
      expect(un('action-nature')!.querySelectorAll('mat-radio-button').length).toBe(1);
    });

    it('propose les trois quand aucune n’est imposée', async () => {
      await setup();
      expect(composant.natures.map(n => n.value))
        .toEqual(['CONTAINMENT', 'CORRECTIVE', 'PREVENTIVE']);
    });

    /** On relit alors une action écrite : corriger sa nature est légitime. */
    it('propose les trois à l’édition, même si la discipline en impose une', async () => {
      await setup({ action: action(), actionType: 'PREVENTIVE' });
      expect(composant.natures.length).toBe(3);
    });
  });

  // --- l'efficacité suit la nature, pas la seule « corrective » -------------

  describe('l’évaluation d’efficacité d’une action préventive', () => {

    /**
     * « Plus aucune récidive sur les trois autres lignes pendant six mois » est
     * un critère d'efficacité au même titre qu'un autre. La prévention était
     * exclue de la demande : le D7 n'en demandait donc aucun.
     */
    it('demande le critère à l’ajout d’une action préventive', async () => {
      await setup({ actionType: 'PREVENTIVE' });

      expect(composant.demandeCritere).toBeTrue();
      expect(un('action-efficacite-demande')).not.toBeNull();
      expect(composant.demandeVerification).toBeTrue();
      expect(un('action-verification-demande')).not.toBeNull();
    });

    it('ne pose pas la question de la vérification à un endiguement', async () => {
      await setup({ actionType: 'CONTAINMENT' });

      expect(composant.demandeCritere).toBeFalse();
      expect(composant.demandeVerification).toBeFalse();
      expect(un('action-efficacite-demande')).toBeNull();
      expect(un('action-verification-demande')).toBeNull();
      expect(un('action-efficacite-rappel')).toBeNull();
    });

    /**
     * La décision appartient au dossier et ne se repose pas (ADR 0073) ; la
     * TAIRE laissait écrire une action corrective sans savoir si son effet
     * serait vérifié, et donnait à croire que la plateforme ne le demandait
     * nulle part.
     */
    it('rappelle la vérification déjà tranchée, en lecture', async () => {
      await setup({ actionType: 'CORRECTIVE', verificationRequired: true,
                    effectivenessCriterion: 'Aucun rebut pendant 3 mois' });

      expect(composant.verificationDejaTranchee).toBeTrue();
      expect(un('action-verification-rappel-oui')).not.toBeNull();
      expect(un('action-efficacite-rappel')!.textContent).toContain('Aucun rebut pendant 3 mois');
    });

    it('rappelle aussi le refus de vérification', async () => {
      await setup({ actionType: 'CORRECTIVE', verificationRequired: false });

      expect(composant.verificationDejaTranchee).toBeFalse();
      expect(un('action-verification-rappel-non')).not.toBeNull();
    });
  });


  describe('l’équipe de l’écart, proposée en tête du sélecteur', () => {

    it('passe l’équipe reçue au sélecteur de responsable', async () => {
      await setup({ team: [{ accountId: 'u-alice' }] });
      expect(composant.data.team).toEqual([{ accountId: 'u-alice' }]);
      expect(un('action-responsable')).not.toBeNull();
    });
  });
});

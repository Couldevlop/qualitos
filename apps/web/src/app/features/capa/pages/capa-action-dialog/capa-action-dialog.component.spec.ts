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

  describe('la demande d’évaluation d’efficacité (ADR 0108)', () => {

    it('la demande à l’ajout d’une action corrective, quand le dossier n’a pas de critère', async () => {
      await setup();
      expect(composant.demandeCritere).toBeTrue();
      expect(un('action-efficacite-demande')).not.toBeNull();
    });

    it('écrit le critère AVANT l’action : l’avance est le sens même de la règle', async () => {
      await setup();
      composant.form.patchValue({
        title: 'Recalibrer le cobot',
        effectivenessCriterion: 'Aucun rebut sur ce défaut pendant 3 mois'
      });

      composant.submit();

      expect(ordre).toEqual(['updateCase', 'addAction']);
      expect(capa.updateCase.calls.mostRecent().args[1].effectivenessCriterion)
        .toBe('Aucun rebut sur ce défaut pendant 3 mois');
    });

    it('n’écrit aucun critère quand on n’en a pas saisi', async () => {
      await setup();
      composant.form.patchValue({ title: 'Recalibrer le cobot' });

      composant.submit();

      expect(capa.updateCase).not.toHaveBeenCalled();
      expect(ordre).toEqual(['addAction']);
    });

    it('ne redemande pas un critère déjà posé : il se rappelle, en lecture', async () => {
      // Le redemander serait l'occasion de le réécrire pour qu'il colle au
      // résultat obtenu, ce que l'ADR 0108 veut précisément empêcher.
      await setup({ effectivenessCriterion: 'Aucun rebut pendant 3 mois' });

      expect(composant.demandeCritere).toBeFalse();
      expect(un('action-efficacite-demande')).toBeNull();
      expect(un('action-efficacite-rappel')!.textContent).toContain('Aucun rebut pendant 3 mois');
    });

    it('ne le demande pas pour une action de confinement : elle ne prétend pas supprimer la cause',
      async () => {
        await setup();
        composant.form.patchValue({ actionType: 'CONTAINMENT' });
        fixture.detectChanges();

        expect(composant.demandeCritere).toBeFalse();
        expect(un('action-efficacite-demande')).toBeNull();
      });

    it('ne le demande pas quand on CORRIGE une action : le dossier est déjà lancé', async () => {
      await setup({ action: action() });
      expect(composant.demandeCritere).toBeFalse();
    });

    it('ne le demande pas sur un dossier en lecture seule', async () => {
      await setup({ readOnly: true });
      expect(composant.demandeCritere).toBeFalse();
    });
  });

  describe('la question « vérifiera-t-on l’efficacité ? »', () => {

    it('la pose quand le dossier ne l’a pas tranchée, comme à l’ouverture d’un dossier', async () => {
      await setup();
      expect(composant.demandeVerification).toBeTrue();
      expect(un('action-verification-demande')).not.toBeNull();
    });

    it('ne la repose pas quand le dossier a déjà répondu oui', async () => {
      // Deux réponses contradictoires sur un même dossier, c'est une décision
      // qu'on ne sait plus lire.
      await setup({ verificationRequired: true });
      expect(composant.demandeVerification).toBeFalse();
      expect(un('action-verification-demande')).toBeNull();
    });

    it('ne la repose pas davantage quand le dossier a répondu non', async () => {
      // Non est une réponse, pas une absence de réponse.
      await setup({ verificationRequired: false });
      expect(composant.demandeVerification).toBeFalse();
      expect(un('action-verification-demande')).toBeNull();
    });

    it('ne la pose pas quand on CORRIGE une action : le dossier est déjà lancé', async () => {
      await setup({ action: action() });
      expect(composant.demandeVerification).toBeFalse();
    });

    it('ne la pose pas sur un dossier en lecture seule', async () => {
      await setup({ readOnly: true });
      expect(composant.demandeVerification).toBeFalse();
    });

    it('écrit la réponse sur le dossier AVANT l’action, en une seule mise à jour avec le critère',
      async () => {
        await setup();
        composant.form.patchValue({
          title: 'Recalibrer le cobot',
          effectivenessCriterion: 'Aucun rebut sur ce défaut pendant 3 mois',
          verificationRequired: true
        });

        composant.submit();

        expect(ordre).toEqual(['updateCase', 'addAction']);
        // Une seule requête : deux mises à jour de suite laisseraient le dossier
        // à moitié décidé si la seconde échouait.
        expect(capa.updateCase).toHaveBeenCalledTimes(1);
        const envoye = capa.updateCase.calls.mostRecent().args[1];
        expect(envoye.effectivenessCriterion).toBe('Aucun rebut sur ce défaut pendant 3 mois');
        expect(envoye.verificationRequired).toBeTrue();
      });

    it('n’écrit rien sur le dossier quand on ne répond ni au critère ni à la question',
      async () => {
        await setup();
        composant.form.patchValue({ title: 'Recalibrer le cobot' });

        composant.submit();

        expect(capa.updateCase).not.toHaveBeenCalled();
        expect(ordre).toEqual(['addAction']);
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

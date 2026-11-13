import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaCaseResponse } from '../../capa.types';
import { CapaActionsDialogComponent, CapaActionsDialogData } from './capa-actions-dialog.component';

/**
 * Ce qui reste OUVERT sur un écart, et où cela se traite.
 *
 * <p>La fenêtre portait le tableau complet des actions, que la fiche de l'écart
 * porte désormais elle-même : deux fois le même tableau sur le même écran, dont
 * un dans une fenêtre qui recouvre l'autre. Elle ne répond plus qu'à la question
 * que ce tableau ne pose pas — qu'est-ce qui reste à faire — et chaque ligne
 * mène au dossier où l'action s'écrit, s'avance et se prouve.
 */
describe('CapaActionsDialogComponent', () => {

  let capa: jasmine.SpyObj<CapaService>;
  let router: jasmine.SpyObj<Router>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaActionsDialogComponent, boolean>>;
  let fixture: ComponentFixture<CapaActionsDialogComponent>;
  let component: CapaActionsDialogComponent;

  const CASE_ID = 'capa-7';

  function action(partiel: Partial<CapaActionResponse> = {}): CapaActionResponse {
    return {
      id: 'act-1', capaId: CASE_ID, title: 'Reprendre le presse-étoupe',
      status: 'PENDING', actionType: 'CORRECTIVE', ...partiel
    };
  }

  function dossier(actions: CapaActionResponse[]): CapaCaseResponse {
    return {
      id: CASE_ID, tenantId: 't-1', title: 'Fuite', type: 'CORRECTIVE',
      criticity: 'HIGH', status: 'IN_PROGRESS', sourceType: 'NON_CONFORMITY',
      ownerId: 'u-1', createdAt: '', updatedAt: '', actions
    };
  }

  function monter(actions: CapaActionResponse[],
                  data: Partial<CapaActionsDialogData> = {}): void {
    capa = jasmine.createSpyObj<CapaService>('CapaService', ['getCase']);
    capa.getCase.and.returnValue(of(dossier(actions)));
    monterAvec(data);
  }

  function monterAvec(data: Partial<CapaActionsDialogData> = {}): void {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaActionsDialogComponent, boolean>>(
      'MatDialogRef', ['close']);

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [CapaActionsDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: Router, useValue: router },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { capaCaseId: CASE_ID, ...data } }
      ]
    });
    fixture = TestBed.createComponent(CapaActionsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function hote(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  // ---------- ce qui reste ouvert ----------

  /**
   * Une action FAITE n'a plus rien à ouvrir : la lister ici obligerait à trier
   * du regard ce que la fenêtre est censée trier elle-même. Elles se lisent en
   * bas de la fiche, dans le tableau qui les porte toutes.
   */
  it('ne liste que les actions non terminées', () => {
    monter([
      action({ id: 'a-1', title: 'À faire', status: 'PENDING' }),
      action({ id: 'a-2', title: 'En cours', status: 'IN_PROGRESS' }),
      action({ id: 'a-3', title: 'Déjà faite', status: 'DONE' })
    ]);

    const lignes = hote().querySelectorAll('[data-test="action-ouverte"]');
    expect(lignes.length).toBe(2);
    expect(hote().textContent).toContain('À faire');
    expect(hote().textContent).toContain('En cours');
    expect(hote().textContent).not.toContain('Déjà faite');
  });

  /** Le porteur et l'échéance absents s'affichent en tiret, jamais en vide. */
  it('montre le porteur et l’échéance, et un tiret à leur place quand ils manquent', () => {
    monter([action({ assigneeName: 'A. Durand', dueDate: '2026-12-01' }),
            action({ id: 'act-2', title: 'Sans porteur' })]);

    const lignes = hote().querySelectorAll('[data-test="action-ouverte"]');
    expect(lignes[0].textContent).toContain('A. Durand');
    expect(lignes[1].textContent).toContain('—');
  });

  it('dit la référence de l’écart d’où l’on vient', () => {
    monter([action()], { reference: 'NC-2026-0007' });

    expect(hote().textContent).toContain('NC-2026-0007');
  });

  /**
   * Aucune action ouverte n'est pas une panne : ou tout est fait, ou rien n'a
   * encore été écrit. Une fenêtre vide, elle, passe pour une panne.
   */
  it('dit qu’il ne reste rien d’ouvert plutôt que de ne rien montrer', () => {
    monter([action({ status: 'DONE' })]);

    expect(hote().querySelector('[data-test="actions-ouvertes"]')).toBeNull();
    expect(hote().querySelector('[data-test="actions-ouvertes-vide"]')).not.toBeNull();
  });

  // ---------- la ligne mène au dossier ----------

  /**
   * La fenêtre se ferme AVANT la navigation : rester ouverte par-dessus l'écran
   * d'arrivée masquerait exactement ce qu'on vient d'y chercher.
   */
  it('mène au dossier, et se ferme avant d’y aller', () => {
    monter([action()]);

    (hote().querySelector('[data-test="action-ouverte"]') as HTMLElement).click();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
    expect(router.navigate).toHaveBeenCalledWith(['/capa', CASE_ID]);
  });

  /** La ligne est atteignable au clavier : elle annonce un bouton, elle en est un. */
  it('ouvre le dossier au clavier comme à la souris', () => {
    monter([action()]);
    const ligne = hote().querySelector('[data-test="action-ouverte"]') as HTMLElement;

    expect(ligne.getAttribute('role')).toBe('button');
    expect(ligne.getAttribute('tabindex')).toBe('0');
    expect(ligne.getAttribute('aria-label')).toContain('Reprendre le presse-étoupe');

    ligne.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/capa', CASE_ID]);
  });

  it('offre la porte du dossier même quand rien n’est ouvert', () => {
    monter([]);

    (hote().querySelector('[data-test="actions-dialog-dossier"]') as HTMLElement).click();

    expect(router.navigate).toHaveBeenCalledWith(['/capa', CASE_ID]);
  });

  it('se ferme sans rien changer', () => {
    monter([action()]);

    (hote().querySelector('[data-test="actions-dialog-fermer"]') as HTMLElement).click();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  // ---------- la panne se dit ----------

  /**
   * Une fenêtre vide après un refus du serveur se lit « rien à faire » : c'est
   * le contraire de ce qu'on sait, et on refermerait en croyant l'écart traité.
   */
  it('dit la panne plutôt que de laisser croire qu’il ne reste rien', () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService', ['getCase']);
    capa.getCase.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500, statusText: 'Server Error'
    })));
    monterAvec();

    expect(hote().querySelector('[data-test="actions-dialog-erreur"]')).not.toBeNull();
    // Et surtout PAS le message « rien d'ouvert », qui dirait le contraire.
    expect(hote().querySelector('[data-test="actions-ouvertes-vide"]')).toBeNull();
  });

  // ---------- libellés ----------

  it('traduit la nature et le statut d’une action, et jamais son code brut', () => {
    monter([action()]);

    expect(component.actionTypeLabel('CONTAINMENT')).not.toContain('CONTAINMENT');
    expect(component.actionStatusLabel('PENDING')).toBe('À faire');
    expect(component.actionStatusLabel('IN_PROGRESS')).toBe('En cours');
    expect(component.actionStatusLabel('DONE')).toBe('Faite');
    expect(component.badge('IN_PROGRESS')).toBe('badge badge-in-progress');
    // Une action antérieure au type est corrective : elle ne pouvait être autre
    // chose, et la colonne ne doit pas rester vide pour autant.
    expect(component.actionTypeLabel(undefined as never))
      .toBe(component.actionTypeLabel('CORRECTIVE'));
  });

  it('suit une ligne par son identifiant, pas par sa position', () => {
    monter([action()]);

    expect(component.trackById(0, action({ id: 'act-9' }))).toBe('act-9');
  });

  /**
   * UNE seule lecture du dossier, quoi que le gabarit en fasse : il lit la
   * liste trois fois (le tableau, son état vide, le compteur), et trois
   * souscriptions enverraient trois requêtes pour une seule fenêtre.
   */
  it('ne lit le dossier qu’une fois', () => {
    monter([action()]);
    fixture.detectChanges();
    fixture.detectChanges();

    expect(capa.getCase).toHaveBeenCalledTimes(1);
  });
});

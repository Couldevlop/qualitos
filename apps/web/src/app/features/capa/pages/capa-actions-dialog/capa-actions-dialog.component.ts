import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay, startWith } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { actionTypeLabel } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import { CapaActionResponse, CapaActionStatus } from '../../capa.types';

/** Ce qu'il faut pour ouvrir la fenêtre : le dossier dont on lit les actions. */
export interface CapaActionsDialogData {
  capaCaseId: string;
  /** La référence de l'écart d'où l'on vient, pour que la fenêtre dise de quoi elle parle. */
  reference?: string;
}

/**
 * Ce que la fenêtre a à montrer, en UN SEUL objet.
 *
 * <p>Trois champs indépendants — un drapeau d'attente, une liste, un message
 * d'erreur — se contrediraient le temps d'une passe de détection : le drapeau
 * retombe pendant que le gabarit lit encore l'ancien, et Angular refuse en mode
 * développement une expression qui change après avoir été vérifiée (NG0100). Un
 * état indivisible n'a pas d'entre-deux.
 */
interface Etat {
  chargement: boolean;
  ouvertes: CapaActionResponse[];
  /** Ce qui empêche de lire le dossier, dit plutôt que laissé en blanc. */
  erreur: string | null;
}

/**
 * Ce qui reste OUVERT sur un écart, et où cela se traite.
 *
 * <p>La fenêtre portait le tableau complet des actions —
 * {@code qos-capa-action-panel} — alors que la fiche de l'écart le porte
 * désormais elle-même, sous l'analyse de cause. Deux fois le même tableau sur
 * le même écran, dont un dans une fenêtre qui recouvre l'autre : on ajoutait une
 * action par-dessus le tableau qui l'aurait montrée.
 *
 * <p>Elle répond donc à la seule question que le tableau de la fiche ne pose
 * pas : qu'est-ce qui reste à faire ? Les actions faites n'y sont pas — elles se
 * lisent en bas de la fiche — et une ligne mène au dossier qui la porte, là où
 * elle s'écrit, s'avance et se prouve.
 */
@Component({
  selector: 'qos-capa-actions-dialog',
  templateUrl: './capa-actions-dialog.component.html',
  styleUrls: ['./capa-actions-dialog.component.scss'],
  standalone: false
})
export class CapaActionsDialogComponent {

  /**
   * L'état de la fenêtre : l'attente, les actions ouvertes, ou la panne.
   *
   * <p>`shareReplay` et non une souscription par liaison : le gabarit lit cet
   * état plusieurs fois (le tableau, son état vide, le message d'attente), et
   * autant de souscriptions enverraient autant de requêtes pour une fenêtre.
   */
  readonly etat$: Observable<Etat> = this.capa.getCase(this.data.capaCaseId).pipe(
    map(dossier => <Etat>{
      chargement: false,
      // Une action FAITE n'a plus rien à ouvrir : la lister ici obligerait à
      // trier du regard ce que la fenêtre est censée trier elle-même.
      ouvertes: dossier.actions.filter(a => a.status !== 'DONE'),
      erreur: null
    }),
    catchError(err => {
      // eslint-disable-next-line no-console
      console.warn('[capa-actions-dialog] getCase failed', err?.status, err?.error?.title);
      return of(<Etat>{
        chargement: false,
        ouvertes: [],
        erreur: safeErrorMessage(
          err, $localize`:@@capa.actions-dialog.failed:Les actions du dossier n'ont pas pu être lues.`)
      });
    }),
    startWith(<Etat>{ chargement: true, ouvertes: [], erreur: null }),
    shareReplay({ bufferSize: 1, refCount: false })
  );

  constructor(
    private readonly dialogRef: MatDialogRef<CapaActionsDialogComponent, boolean>,
    private readonly capa: CapaService,
    private readonly router: Router,
    @Inject(MAT_DIALOG_DATA) readonly data: CapaActionsDialogData
  ) {}

  trackById(_: number, action: CapaActionResponse): string {
    return action.id;
  }

  actionTypeLabel(type: CapaActionResponse['actionType']): string {
    return actionTypeLabel(type || 'CORRECTIVE');
  }

  actionStatusLabel(status: CapaActionStatus): string {
    return status === 'PENDING'
      ? $localize`:@@capa.action-status.pending:À faire`
      : status === 'IN_PROGRESS'
        ? $localize`:@@capa.action-status.in-progress:En cours`
        : $localize`:@@capa.action-status.done:Faite`;
  }

  badge(status: CapaActionStatus): string {
    return 'badge badge-' + status.toLowerCase().replace('_', '-');
  }

  /** Libellé lisible par un lecteur d'écran : une ligne cliquable dit où elle mène. */
  openLabel(action: CapaActionResponse): string {
    return $localize`:@@capa.actions-dialog.open-aria:Ouvrir le dossier CAPA qui porte l'action : ${action.title}:titre:`;
  }

  /**
   * Mène au dossier qui porte l'action.
   *
   * <p>La fenêtre se ferme AVANT la navigation : rester ouverte par-dessus
   * l'écran d'arrivée masquerait exactement ce qu'on vient d'y chercher.
   */
  open(): void {
    this.dialogRef.close(false);
    this.router.navigate(['/capa', this.data.capaCaseId]);
  }

  close(): void {
    this.dialogRef.close(false);
  }
}

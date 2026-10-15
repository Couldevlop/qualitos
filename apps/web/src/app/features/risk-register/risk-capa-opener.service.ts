import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened
} from '../capa/pages/capa-create-dialog/capa-create-dialog.component';
import { RiskRegisterService } from './risk-register.service';
import { CapaLink, RiskView } from './risk-register.types';

/** Ce qu'il faut d'un risque pour lui ouvrir une CAPA. */
export type RiskForCapa = Pick<RiskView, 'id' | 'reference' | 'title' | 'owner' | 'grossLevel'>
  & Partial<Pick<RiskView, 'ownerUserId'>>;

/** Ce que la fenêtre rend quand la CAPA vient d'un risque : le lien, pour la fiche. */
interface RiskCapaOpened extends CapaOpened {
  link: CapaLink;
}

/**
 * « Créer une CAPA » depuis un risque, d'où qu'on le clique : la fiche du
 * risque, ou le panneau d'une case de la matrice du tableau de bord SMI.
 *
 * <p>LA fenêtre de création commune (ADR 0088), source verrouillée sur le
 * risque, criticité issue de sa cotation brute. Le propriétaire du risque est
 * proposé comme responsable : directement son compte quand il a été choisi
 * dans l'annuaire (ADR 0092), sinon s'il désigne exactement une personne de
 * l'annuaire en lecture restreinte (nom ou adresse — ADR 0091) ; le responsable
 * choisi pilote le dossier — et non plus celui qui clique.
 * Rend le lien du dossier ouvert, ou `null` si l'utilisateur a renoncé.
 */
@Injectable({ providedIn: 'root' })
export class RiskCapaOpener {

  constructor(
    private readonly dialog: MatDialog,
    private readonly service: RiskRegisterService
  ) {}

  open(risk: RiskForCapa): Observable<CapaLink | null> {
    const data: CapaCreateDialogData = {
      source: { type: 'RISK', reference: risk.reference },
      lockCriticity: true,
      prefill: {
        title: risk.title,
        type: 'PREVENTIVE',
        criticity: risk.grossLevel,
        ownerHint: risk.owner,
        ownerAccountId: risk.ownerUserId ?? undefined
      },
      open: request => this.service.openCapa(risk.id, {
        title: request.title,
        description: request.description ?? null,
        kind: request.type,
        ownerId: request.ownerId,
        ownerName: request.ownerName ?? null,
        dueDate: request.dueDate,
        verificationRequired: request.verificationRequired ?? null,
        verificationAssigneeId: request.verificationAssigneeId ?? null,
        verificationAssigneeName: request.verificationAssigneeName ?? null,
        verificationInstructions: request.verificationInstructions ?? null
      }).pipe(map((link): RiskCapaOpened => ({ capaId: link.id, link })))
    };
    return this.dialog.open<CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened>(
      CapaCreateDialogComponent, {
        data, panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
      }).afterClosed().pipe(
      map(opened => (opened ? (opened as RiskCapaOpened).link ?? null : null))
    );
  }
}

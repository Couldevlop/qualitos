import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import {
  ActionDialogComponent, ActionDialogData, ActionDialogResult
} from './pages/action-dialog/action-dialog.component';
import { RiskRegisterService } from './risk-register.service';
import { CapaLink } from './risk-register.types';

/** Ce qu'il faut d'un risque pour lui ouvrir une action CAPA. */
export interface RiskForCapa {
  id: string;
  reference: string;
  owner: string;
}

/**
 * « Créer une action CAPA » depuis un risque, d'où qu'on le clique : la fiche
 * du risque, ou le panneau d'une case de la matrice du tableau de bord SMI.
 *
 * <p>La fenêtre propose le propriétaire du risque comme responsable, et les
 * noms déjà employés au registre ; si ces suggestions ne viennent pas, la
 * fenêtre s'ouvre quand même — elles aident, elles ne conditionnent rien.
 * Rend le dossier ouvert, ou `null` si l'utilisateur a renoncé.
 */
@Injectable({ providedIn: 'root' })
export class RiskCapaOpener {

  constructor(
    private readonly dialog: MatDialog,
    private readonly service: RiskRegisterService
  ) {}

  open(risk: RiskForCapa): Observable<CapaLink | null> {
    return this.service.suggestions().pipe(
      map(s => s.owners),
      catchError(() => of([] as string[])),
      switchMap(assignees => this.dialog.open<ActionDialogComponent, ActionDialogData, ActionDialogResult>(
        ActionDialogComponent, {
          data: { mode: 'capa', reference: risk.reference, assignee: risk.owner, assignees },
          panelClass: 'qos-dialog-panel', restoreFocus: true
        }).afterClosed()),
      switchMap(result => result && result.kind && result.assignee && result.dueDate
        ? this.service.openCapa(risk.id, {
          title: result.title, description: result.description, kind: result.kind,
          assignee: result.assignee, dueDate: result.dueDate
        })
        : of(null))
    );
  }
}

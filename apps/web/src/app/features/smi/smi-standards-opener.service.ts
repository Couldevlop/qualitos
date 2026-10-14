import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { AuthService } from '../../core/auth/auth.service';
import { AddStandardDialogComponent } from './pages/add-standard-dialog/add-standard-dialog.component';
import { standardShortName } from './smi.labels';
import { StandardRef } from './smi.types';

/**
 * Les rôles du jeton qui valent « administrateur du client », alias compris
 * (ADR 0020) : exactement ceux qu'exige le serveur.
 */
export const TENANT_ADMIN_ROLES = ['admin_tenant', 'admin', 'tenant_admin'];

/**
 * Ouvre « Ajouter une norme au SMI » depuis le tableau de bord ou la matrice.
 *
 * <p>Adopter une norme est un paramétrage, réservé à l'administrateur du
 * client : l'écran ne propose l'action qu'à lui. Ce n'est qu'un confort — le
 * serveur reste seul juge et refuse les autres en 403.
 */
@Injectable({ providedIn: 'root' })
export class SmiStandardsOpener {

  constructor(
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  canAdd(): boolean {
    return this.auth.hasAnyRole(TENANT_ADMIN_ROLES);
  }

  /** Émet l'adoption créée, ou `undefined` si la fenêtre a été fermée sans rien ajouter. */
  open(): Observable<StandardRef | undefined> {
    return this.dialog.open<AddStandardDialogComponent, void, StandardRef>(AddStandardDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'dialog',
      restoreFocus: true
    }).afterClosed().pipe(
      tap(ref => {
        if (ref) {
          const nom = standardShortName(ref.name, ref.code);
          this.snack.open($localize`:@@smi.add.done:${nom}:standard: ajoutée au SMI — feuille de route de certification créée.`,
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      })
    );
  }
}

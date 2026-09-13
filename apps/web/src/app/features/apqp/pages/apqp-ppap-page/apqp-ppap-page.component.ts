import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ApqpService } from '../../apqp.service';
import { ApqpCycle, ApqpDeliverable, ApqpPhase } from '../../apqp.types';
import {
  ApqpDeliverableDetailDialogComponent, ApqpDeliverableDetailDialogData
} from '../apqp-deliverable-detail-dialog/apqp-deliverable-detail-dialog.component';
import { LignePpap } from '../apqp-ppap-summary/apqp-ppap-summary.component';

/** Qui peut écrire. Miroir exact du contrôle posé côté serveur. */
const ROLES_ECRITURE = ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/**
 * Le dossier PPAP, à lui seul.
 *
 * <p>Il vit déjà sous le schéma du cycle, où il donne son contexte : ce qui reste
 * à fournir, à côté des phases qui le produisent. Mais le dossier se travaille
 * aussi POUR LUI-MÊME — c'est lui qu'on remet au client, et c'est cette liste-là
 * qu'on parcourt à l'approche d'une soumission. D'où une entrée de menu et un
 * écran à part.
 *
 * <p>Le même composant rend les deux : une seconde implémentation aurait divergé
 * de la première au premier ajustement.
 */
@Component({
  selector: 'qos-apqp-ppap-page',
  templateUrl: './apqp-ppap-page.component.html',
  styleUrls: ['./apqp-ppap-page.component.scss'],
  standalone: false
})
export class ApqpPpapPageComponent implements OnInit {

  phases: ApqpPhase[] = [];
  ppapDone = 0;
  ppapTotal = 0;
  loading = false;

  readonly editable: boolean;

  constructor(
    private readonly service: ApqpService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_ECRITURE);
  }

  ngOnInit(): void {
    this.charger();
  }

  /** Le cycle compte-t-il seulement un livrable du dossier ? */
  get vide(): boolean {
    return !this.loading && this.ppapTotal === 0;
  }

  ouvrirLivrable(ligne: LignePpap): void {
    this.ouvrir(ligne.phase, ligne.deliverable);
  }

  private ouvrir(phase: ApqpPhase, deliverable: ApqpDeliverable): void {
    const ref = this.dialog.open(ApqpDeliverableDetailDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      width: '44rem',
      data: { phase, deliverable, editable: this.editable } as ApqpDeliverableDetailDialogData
    });
    ref.afterClosed().subscribe(cycle => {
      if (cycle) this.appliquer(cycle);
    });
  }

  private charger(): void {
    this.loading = true;
    this.service.cycle().subscribe({
      next: cycle => {
        this.appliquer(cycle);
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.snack.open(
          safeErrorMessage(err, $localize`:@@apqp.failed:Opération impossible sur le cycle APQP.`),
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  private appliquer(cycle: ApqpCycle): void {
    this.phases = cycle.phases;
    this.ppapDone = cycle.ppapDone;
    this.ppapTotal = cycle.ppapTotal;
  }
}

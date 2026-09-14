import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

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
 * Le dossier PPAP d'un PROJET, à lui seul.
 *
 * <p>Il n'a plus d'adresse globale : le dossier est ce qu'on remet au client d'un
 * projet donné, et deux projets n'ont pas le même. D'où une adresse nichée sous
 * le projet, et un retour vers lui.
 *
 * <p>Les livrables retenus sont ceux que l'UTILISATEUR a marqués « requis au
 * dossier PPAP ». Ce n'est plus l'astérisque d'un référentiel : c'est lui qui
 * sait ce que son client attend, et un projet peut en exiger plus ou moins.
 */
@Component({
  selector: 'qos-apqp-ppap-page',
  templateUrl: './apqp-ppap-page.component.html',
  styleUrls: ['./apqp-ppap-page.component.scss'],
  standalone: false
})
export class ApqpPpapPageComponent implements OnInit, OnDestroy {

  phases: ApqpPhase[] = [];
  ppapDone = 0;
  ppapTotal = 0;
  loading = false;

  projetId = '';
  projetNom = '';

  readonly editable: boolean;

  private readonly detruit$ = new Subject<void>();

  constructor(
    private readonly service: ApqpService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly route: ActivatedRoute,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_ECRITURE);
  }

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntil(this.detruit$))
      .subscribe(params => {
        this.projetId = params.get('projetId') ?? '';
        this.charger();
      });
  }

  ngOnDestroy(): void {
    this.detruit$.next();
    this.detruit$.complete();
  }

  /** Le projet compte-t-il seulement un livrable requis au dossier ? */
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
      data: {
        projectId: this.projetId, phase, deliverable, editable: this.editable
      } as ApqpDeliverableDetailDialogData
    });
    ref.afterClosed().subscribe(cycle => {
      if (cycle) this.appliquer(cycle);
    });
  }

  private charger(): void {
    if (!this.projetId) return;
    this.loading = true;
    this.service.cycle(this.projetId).subscribe({
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
    this.projetNom = cycle.projectName;
    this.ppapDone = cycle.ppapDone;
    this.ppapTotal = cycle.ppapTotal;
  }
}

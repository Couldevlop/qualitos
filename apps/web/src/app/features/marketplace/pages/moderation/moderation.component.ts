import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { MarketplaceService } from '../../marketplace.service';
import { MarketplacePackView } from '../../marketplace.types';
import {
  RejectDialogComponent, RejectDialogResult
} from './reject-dialog/reject-dialog.component';

/**
 * File de modération de l'éditeur (SUPER_ADMIN). Permet la prise en revue, la
 * publication, le rejet motivé et la dépréciation. Aucun pack n'est publié sans
 * cette validation humaine (CLAUDE.md §8.11).
 */
@Component({
  selector: 'qos-marketplace-moderation',
  templateUrl: './moderation.component.html',
  styleUrls: ['./moderation.component.scss'],
  standalone: false
})
export class ModerationComponent implements OnInit {

  packs: MarketplacePackView[] = [];
  loading = true;
  error = false;
  busy?: string;

  constructor(
    private readonly svc: MarketplaceService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = false;
    this.svc.moderationQueue().subscribe({
      next: packs => { this.packs = packs; this.loading = false; },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  takeForReview(p: MarketplacePackView): void {
    this.run(p, this.svc.takeForReview(p.id),
      $localize`:@@marketplace.mod.taken:Pack pris en revue`);
  }

  publish(p: MarketplacePackView): void {
    this.run(p, this.svc.publish(p.id),
      $localize`:@@marketplace.mod.published:Pack publié au catalogue`, true);
  }

  deprecate(p: MarketplacePackView): void {
    this.run(p, this.svc.deprecate(p.id),
      $localize`:@@marketplace.mod.deprecated:Pack déprécié`, true);
  }

  reject(p: MarketplacePackView): void {
    this.dialog.open(RejectDialogComponent, { panelClass: 'qos-dialog-panel' })
      .afterClosed().subscribe((res?: RejectDialogResult) => {
        if (!res || !res.reason) { return; }
        this.busy = p.id;
        this.svc.reject(p.id, res.reason).subscribe({
          next: () => {
            this.removeFromQueue(p.id);
            this.busy = undefined;
            this.snack.open(
              $localize`:@@marketplace.mod.rejected:Pack rejeté`,
              $localize`:@@common.ok:OK`, { duration: 3000 });
          },
          error: () => this.fail()
        });
      });
  }

  private run(p: MarketplacePackView, obs: ReturnType<MarketplaceService['publish']>,
              successMsg: string, removeFromQueue = false): void {
    this.busy = p.id;
    obs.subscribe({
      next: updated => {
        if (removeFromQueue) {
          this.removeFromQueue(p.id);
        } else {
          const idx = this.packs.findIndex(x => x.id === p.id);
          if (idx >= 0) { this.packs[idx] = updated; }
        }
        this.busy = undefined;
        this.snack.open(successMsg, $localize`:@@common.ok:OK`, { duration: 3000 });
      },
      error: () => this.fail()
    });
  }

  private removeFromQueue(id: string): void {
    this.packs = this.packs.filter(x => x.id !== id);
  }

  private fail(): void {
    this.busy = undefined;
    this.snack.open(
      $localize`:@@marketplace.mod.error:Action impossible dans cet état`,
      $localize`:@@common.close:Fermer`, { duration: 3500 });
  }
}

import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';

import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../../../core/offline/offline-queue.service';
import { QueuedOperation } from '../../../../core/offline/offline-queue.store';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';

/**
 * Page « File d'attente offline » (CLAUDE.md §15.2-15.3) : visualise les
 * écritures terrain en attente de synchronisation, permet de relancer le
 * rejeu manuellement ou d'abandonner une opération.
 *
 * La liste se rafraîchit sur chaque événement de la file (queued / replayed /
 * replay-failed / discarded) — pas de polling.
 */
@Component({
  selector: 'qos-offline-queue',
  templateUrl: './offline-queue.component.html',
  styleUrls: ['./offline-queue.component.scss'],
  standalone: false
})
export class OfflineQueueComponent implements OnInit, OnDestroy {

  readonly displayedColumns = ['label', 'method', 'queuedAt', 'actions'];

  readonly operations$ = new BehaviorSubject<QueuedOperation[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(false);
  online$!: Observable<boolean>;
  pendingCount$!: Observable<number>;
  replayRequested = false;

  private eventsSub?: Subscription;

  constructor(
    private readonly queue: OfflineQueueService,
    private readonly connectivity: ConnectivityService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.online$ = this.connectivity.online$;
    this.pendingCount$ = this.queue.pendingCount$;
    this.eventsSub = this.queue.events$.subscribe(() => void this.refresh());
    void this.refresh();
  }

  ngOnDestroy(): void {
    this.eventsSub?.unsubscribe();
  }

  async refresh(): Promise<void> {
    queueMicrotask(() => this.loading$.next(true));
    try {
      this.operations$.next(await this.queue.list());
    } finally {
      this.loading$.next(false);
    }
  }

  /** Relance le rejeu manuellement (utile après un retour réseau silencieux). */
  async syncNow(): Promise<void> {
    if (!this.connectivity.isOnline()) {
      this.snack.open(
        $localize`:@@offline.queue.still-offline:Toujours hors-ligne — la synchronisation reprendra au retour du réseau.`,
        $localize`:@@common.ok:OK`,
        { duration: 3500 }
      );
      return;
    }
    this.replayRequested = true;
    try {
      await this.queue.replay();
      await this.refresh();
      this.snack.open(
        $localize`:@@offline.queue.sync-done:Synchronisation terminée.`,
        $localize`:@@common.ok:OK`,
        { duration: 2500 }
      );
    } finally {
      this.replayRequested = false;
    }
  }

  discard(op: QueuedOperation): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@offline.queue.discard-title:Abandonner cette action ?`,
        message: $localize`:@@offline.queue.discard-message:« ${op.label}:label: » ne sera jamais synchronisée vers le serveur. Cette décision est définitive.`,
        confirmLabel: $localize`:@@offline.queue.discard-confirm:Abandonner`,
        cancelLabel: $localize`:@@offline.queue.discard-keep:Conserver`,
        destructive: true
      },
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        void this.queue.discard(op.id).then(() => {
          this.snack.open(
            $localize`:@@offline.queue.discarded:Action abandonnée.`,
            $localize`:@@common.ok:OK`,
            { duration: 2500 }
          );
        });
      }
    });
  }

  methodBadgeClass(method: QueuedOperation['method']): string {
    return 'method method-' + method.toLowerCase();
  }

  trackById(_: number, op: QueuedOperation): string {
    return op.id;
  }
}

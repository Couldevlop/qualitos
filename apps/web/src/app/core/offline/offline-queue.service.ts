import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, firstValueFrom, from, Observable, Subject } from 'rxjs';

import { ConnectivityService } from './connectivity.service';
import { OfflineQueueStore, QueuedOperation } from './offline-queue.store';

/** Événement de cycle de vie de la file — consommé par le shell (badge, snackbar). */
export interface OfflineQueueEvent {
  type: 'queued' | 'replayed' | 'replay-failed' | 'discarded';
  operation: QueuedOperation;
}

/**
 * File d'attente des écritures hors-ligne (CLAUDE.md §15.2-15.3) :
 * une action terrain saisie sans réseau est persistée localement (IndexedDB)
 * puis **rejouée automatiquement** au retour de la connexion, dans l'ordre.
 *
 * - Le rejeu passe par {@link HttpClient} : l'ApiInterceptor (re)pose un token
 *   frais au moment du rejeu — jamais de token stocké dans la file (OWASP A02).
 * - Une erreur réseau (status 0) STOPPE le rejeu (toujours offline) ; une
 *   erreur applicative (4xx/5xx) retire l'opération et émet `replay-failed`
 *   pour signalement — pas de boucle infinie sur une op invalide.
 * - Jamais de PII dans les logs : seuls `label` et l'URL sont journalisés.
 */
@Injectable({ providedIn: 'root' })
export class OfflineQueueService {

  private readonly pendingCount = new BehaviorSubject<number>(0);
  private readonly queueEvents = new Subject<OfflineQueueEvent>();
  private replaying = false;

  /** Nombre d'opérations en attente de synchronisation (badge UI). */
  readonly pendingCount$ = this.pendingCount.asObservable();
  readonly events$ = this.queueEvents.asObservable();

  constructor(
    private readonly http: HttpClient,
    private readonly connectivity: ConnectivityService,
    private readonly store: OfflineQueueStore
  ) {
    void this.refreshCount();
    this.connectivity.online$.subscribe(online => {
      if (online) {
        void this.replay();
      }
    });
  }

  /**
   * Met une écriture en attente. Retourne l'opération persistée — l'appelant
   * construit sa réponse optimiste à partir d'elle.
   */
  enqueue(
    method: QueuedOperation['method'],
    url: string,
    body: unknown,
    label: string
  ): Observable<QueuedOperation> {
    const op: QueuedOperation = {
      id: this.generateId(),
      method,
      url,
      body,
      label,
      queuedAt: new Date().toISOString()
    };
    return from(
      this.store.append(op).then(async () => {
        await this.refreshCount();
        this.queueEvents.next({ type: 'queued', operation: op });
        return op;
      })
    );
  }

  /** Opérations en attente, dans l'ordre de rejeu (page « File d'attente »). */
  list(): Promise<QueuedOperation[]> {
    return this.store.loadAll();
  }

  /**
   * Abandonne définitivement une opération en attente (décision utilisateur
   * depuis la page « File d'attente »). L'écriture ne sera jamais rejouée.
   */
  async discard(id: string): Promise<void> {
    const ops = await this.store.loadAll();
    const op = ops.find(o => o.id === id);
    if (!op) {
      return;
    }
    await this.store.remove(id);
    await this.refreshCount();
    this.queueEvents.next({ type: 'discarded', operation: op });
  }

  /** Rejoue la file dans l'ordre. Sans effet si déjà en cours ou hors-ligne. */
  async replay(): Promise<void> {
    if (this.replaying || !this.connectivity.isOnline()) {
      return;
    }
    this.replaying = true;
    try {
      const ops = await this.store.loadAll();
      for (const op of ops) {
        try {
          await firstValueFrom(this.http.request(op.method, op.url, { body: op.body }));
          await this.store.remove(op.id);
          this.queueEvents.next({ type: 'replayed', operation: op });
        } catch (err: unknown) {
          if (this.isNetworkError(err)) {
            // Toujours hors-ligne : on garde l'op et on retentera au prochain 'online'.
            return;
          }
          // Erreur applicative : l'op ne passera jamais — on la retire et on signale.
          await this.store.remove(op.id);
          this.queueEvents.next({ type: 'replay-failed', operation: op });
          console.warn(`[offline] rejeu refusé par l'API — « ${op.label} » (${op.url})`);
        }
      }
    } finally {
      this.replaying = false;
      await this.refreshCount();
    }
  }

  private async refreshCount(): Promise<void> {
    const ops = await this.store.loadAll();
    this.pendingCount.next(ops.length);
  }

  private isNetworkError(err: unknown): boolean {
    return typeof err === 'object' && err !== null && (err as { status?: number }).status === 0;
  }

  private generateId(): string {
    return typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : 'op-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10);
  }
}

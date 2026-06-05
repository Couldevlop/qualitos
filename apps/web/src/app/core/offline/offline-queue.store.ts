import { Injectable } from '@angular/core';

/**
 * Opération d'écriture mise en attente pendant une coupure réseau (audits 5S
 * terrain — CLAUDE.md §15.2-15.3 offline-first).
 *
 * ⚠ Confidentialité : `label` est affiché/journalisé — jamais de PII dedans
 * (zone, pilier, type d'action uniquement). Le `body` reste confiné au store
 * local du poste (IndexedDB) et n'est jamais journalisé.
 */
export interface QueuedOperation {
  id: string;
  method: 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  url: string;
  body: unknown;
  /** Description courte non sensible pour l'UI (« Score 5S Seiri — zone A »). */
  label: string;
  queuedAt: string;
}

/**
 * Port de persistance de la file offline. Deux adaptateurs : IndexedDB (prod)
 * et mémoire (tests, contextes sans IDB). Le service ne connaît que le port.
 * Auto-fourni au root via la factory (substituable dans les TestBed).
 */
@Injectable({ providedIn: 'root', useFactory: offlineQueueStoreFactory })
export abstract class OfflineQueueStore {
  abstract loadAll(): Promise<QueuedOperation[]>;
  abstract append(op: QueuedOperation): Promise<void>;
  abstract remove(id: string): Promise<void>;
}

/** Adaptateur mémoire — tests unitaires et environnements sans IndexedDB. */
@Injectable()
export class InMemoryQueueStore extends OfflineQueueStore {
  private readonly ops = new Map<string, QueuedOperation>();

  override async loadAll(): Promise<QueuedOperation[]> {
    return [...this.ops.values()].sort((a, b) => a.queuedAt.localeCompare(b.queuedAt));
  }

  override async append(op: QueuedOperation): Promise<void> {
    this.ops.set(op.id, op);
  }

  override async remove(id: string): Promise<void> {
    this.ops.delete(id);
  }
}

/**
 * Adaptateur IndexedDB — survit aux rechargements/fermetures de la PWA
 * (l'audit terrain saisi en zone blanche n'est jamais perdu).
 */
@Injectable()
export class IndexedDbQueueStore extends OfflineQueueStore {

  private static readonly DB_NAME = 'qualitos-offline';
  private static readonly STORE = 'pending-operations';
  private static readonly VERSION = 1;

  private dbPromise: Promise<IDBDatabase> | null = null;

  override loadAll(): Promise<QueuedOperation[]> {
    return this.withStore('readonly', store => store.getAll()) as Promise<QueuedOperation[]>;
  }

  override async append(op: QueuedOperation): Promise<void> {
    await this.withStore('readwrite', store => store.put(op));
  }

  override async remove(id: string): Promise<void> {
    await this.withStore('readwrite', store => store.delete(id));
  }

  private open(): Promise<IDBDatabase> {
    if (!this.dbPromise) {
      this.dbPromise = new Promise((resolve, reject) => {
        const req = indexedDB.open(IndexedDbQueueStore.DB_NAME, IndexedDbQueueStore.VERSION);
        req.onupgradeneeded = () => {
          if (!req.result.objectStoreNames.contains(IndexedDbQueueStore.STORE)) {
            req.result.createObjectStore(IndexedDbQueueStore.STORE, { keyPath: 'id' });
          }
        };
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
      });
    }
    return this.dbPromise;
  }

  private async withStore<T>(
    mode: IDBTransactionMode,
    action: (store: IDBObjectStore) => IDBRequest<T>
  ): Promise<T> {
    const db = await this.open();
    return new Promise<T>((resolve, reject) => {
      const tx = db.transaction(IndexedDbQueueStore.STORE, mode);
      const req = action(tx.objectStore(IndexedDbQueueStore.STORE));
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }
}

/** Choisit l'adaptateur selon les capacités du contexte d'exécution. */
export function offlineQueueStoreFactory(): OfflineQueueStore {
  return typeof indexedDB === 'undefined' ? new InMemoryQueueStore() : new IndexedDbQueueStore();
}

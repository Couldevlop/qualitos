import { Injectable } from '@angular/core';
import { fromEvent, map, merge, Observable, shareReplay, startWith } from 'rxjs';

/**
 * Abstraction de l'état réseau (navigator.onLine + événements online/offline).
 *
 * Service dédié plutôt qu'un accès direct à `navigator` : injectable et donc
 * substituable dans les tests (navigator.onLine est read-only), et point unique
 * si l'on raffine la détection plus tard (ping périodique, Network Information API).
 */
@Injectable({ providedIn: 'root' })
export class ConnectivityService {

  /** Flux de l'état de connexion — émet immédiatement l'état courant. */
  readonly online$: Observable<boolean> = merge(
    fromEvent(window, 'online').pipe(map(() => true)),
    fromEvent(window, 'offline').pipe(map(() => false))
  ).pipe(
    startWith(typeof navigator !== 'undefined' ? navigator.onLine : true),
    shareReplay({ bufferSize: 1, refCount: false })
  );

  isOnline(): boolean {
    return typeof navigator === 'undefined' ? true : navigator.onLine;
  }
}

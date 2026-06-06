import { BehaviorSubject, Observable, asyncScheduler } from 'rxjs';
import { distinctUntilChanged, observeOn } from 'rxjs/operators';

/**
 * Vue template d'un état piloté depuis les opérateurs d'un flux de données
 * (tap/finalize/catchError) : les émissions sont livrées en MACROTÂCHE
 * (asyncScheduler), donc jamais pendant la passe de détection courante —
 * éradique NG0100 sans changer l'ordre des transitions.
 */
export function deferredView<T>(state: BehaviorSubject<T>): Observable<T> {
  return state.pipe(observeOn(asyncScheduler), distinctUntilChanged());
}

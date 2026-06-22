import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Petit store réactif (wrapper minimal d'un BehaviorSubject) : une valeur
 * courante observable + lecture synchrone. Utilisé par le cross-filtering pour
 * rester testable sans dépendance à NgRx (non requis ici).
 */
export class ReplayLikeStore<T> {

  private readonly subject: BehaviorSubject<T>;

  constructor(initial: T) {
    this.subject = new BehaviorSubject<T>(initial);
  }

  /** Flux de la valeur courante (émet immédiatement la valeur présente). */
  get value$(): Observable<T> {
    return this.subject.asObservable();
  }

  /** Lecture synchrone de la valeur courante. */
  snapshot(): T {
    return this.subject.value;
  }

  /** Pose une nouvelle valeur. */
  set(value: T): void {
    this.subject.next(value);
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { NotificationView, UnreadCount } from './notification.types';

/**
 * Notifications in-app (§4 — journal d'événements qualité poussé à l'utilisateur).
 *
 * L'API serveur (`/api/v1/notifications`, 5 routes) existait sans aucun consommateur :
 * la cloche de la barre supérieure était un bouton décoratif. Ce service la branche.
 *
 * Le compteur de non-lues est exposé en flux : la cloche l'affiche en pastille et il est
 * réévalué après chaque lecture, sans re-télécharger la liste.
 */
@Injectable({ providedIn: 'root' })
export class NotificationsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/notifications`;

  /** Nombre de notifications non lues (0 tant que rien n'a été chargé). */
  private readonly unreadState$ = new BehaviorSubject<number>(0);
  readonly unread$ = this.unreadState$.asObservable();

  constructor(private readonly http: HttpClient) {}

  /**
   * Dernières notifications du tenant. Le tenant est dérivé du JWT côté serveur.
   * En cas d'indisponibilité, renvoie une liste vide : la barre supérieure ne doit
   * jamais casser à cause d'un service annexe.
   */
  recent(limit = 10): Observable<NotificationView[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<NotificationView[]>(this.endpoint, { params }).pipe(
      tap(list => this.unreadState$.next(list.filter(n => !n.read).length)),
      catchError(() => of([]))
    );
  }

  /** Rafraîchit la pastille sans charger la liste (appel léger). */
  refreshUnreadCount(): void {
    this.http.get<UnreadCount>(`${this.endpoint}/unread-count`).pipe(
      map(r => r.unread),
      catchError(() => of(0))
    ).subscribe(count => this.unreadState$.next(count));
  }

  /** Marque une notification comme lue et décrémente la pastille. */
  markRead(id: string): Observable<NotificationView | null> {
    return this.http.post<NotificationView>(`${this.endpoint}/${id}/read`, {}).pipe(
      tap(() => this.unreadState$.next(Math.max(0, this.unreadState$.value - 1))),
      catchError(() => of(null))
    );
  }

  /** Marque tout comme lu et remet la pastille à zéro. */
  markAllRead(): Observable<number> {
    return this.http.post<UnreadCount>(`${this.endpoint}/read-all`, {}).pipe(
      map(r => r.unread),
      catchError(() => of(0)),
      tap(unread => this.unreadState$.next(unread))
    );
  }
}

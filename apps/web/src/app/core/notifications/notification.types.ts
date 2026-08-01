/** Nature d'une notification in-app : pilote l'icône et la couleur (contrat serveur). */
export type NotificationType = 'INFO' | 'SUCCESS' | 'WARNING' | 'ALERT';

/** Notification telle qu'exposée par `GET /api/v1/notifications`. */
export interface NotificationView {
  id: string;
  type: NotificationType;
  title: string;
  body: string | null;
  /** Route interne à ouvrir au clic (ex. `/capa/{id}`), ou `null` si non actionnable. */
  link: string | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface UnreadCount {
  unread: number;
}

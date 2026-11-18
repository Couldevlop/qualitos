/** Comment la connexion au serveur sortant est protégée. */
export type MailSecurity = 'NONE' | 'STARTTLS' | 'TLS';

/**
 * Ce que le serveur rend.
 *
 * <p>Il n'y a **pas** de champ `password`, et ce n'est pas un oubli : l'API ne
 * renvoie jamais le mot de passe, ni même son cryptogramme. `hasPassword` dit
 * seulement qu'il y en a un.
 */
export interface MailSettings {
  /** Faux quand l'organisation n'a rien réglé : elle emprunte la messagerie de la plateforme. */
  configured: boolean;
  fromAddress: string | null;
  fromName: string | null;
  replyTo: string | null;
  host: string | null;
  port: number | null;
  security: MailSecurity | null;
  username: string | null;
  hasPassword: boolean;
  enabled: boolean;
  /** Dernier envoi d'essai concluant, ou `null` si la messagerie n'a jamais été éprouvée. */
  verifiedAt: string | null;
}

/**
 * Ce que l'écran envoie.
 *
 * <p>`password` vide signifie **« n'y touche pas »**, pas « efface-le » : le
 * formulaire ne peut pas réafficher un mot de passe qu'on ne lui rend jamais,
 * et traiter ce vide comme un effacement couperait la messagerie à chaque
 * correction de faute de frappe. L'effacer se demande par `clearPassword`.
 */
export interface MailSettingsRequest {
  fromAddress: string;
  fromName: string | null;
  replyTo: string | null;
  host: string;
  port: number;
  security: MailSecurity;
  username: string | null;
  password: string | null;
  clearPassword: boolean;
  enabled: boolean;
}

/** Le résultat d'un envoi d'essai. `detail` porte le motif quand il a échoué. */
export interface MailTestResult {
  success: boolean;
  detail: string | null;
  verifiedAt: string | null;
}

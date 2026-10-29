/**
 * Les rôles du jeton qui valent « administrateur du client », alias compris
 * (ADR 0020) : exactement ceux qu'exige le serveur pour tout paramétrage —
 * ajouter une norme au SMI, adopter, modifier, abandonner ou supprimer une
 * adoption dans le Standards Hub (ADR 0090). Ni le manager qualité ni le super
 * administrateur de l'éditeur n'en font partie.
 *
 * <p>À passer à {@link AuthService#hasAnyRole} : l'écran n'affiche l'action
 * qu'à ces rôles, le serveur refuse les autres en 403.
 */
export const TENANT_ADMIN_ROLES: readonly string[] = ['admin_tenant', 'admin', 'tenant_admin'];

/**
 * Les rôles qui ouvrent la console d'administration (`/admin`, §16) : ceux de
 * l'administrateur du client, plus le super administrateur de l'éditeur. La
 * barre de navigation n'affiche ses entrées qu'à ces rôles, et la garde de
 * route mène les autres à l'écran « accès réservé ».
 */
export const ADMIN_CONSOLE_ROLES: readonly string[] = ['ADMIN', 'ADMIN_TENANT', 'TENANT_ADMIN', 'SUPER_ADMIN'];

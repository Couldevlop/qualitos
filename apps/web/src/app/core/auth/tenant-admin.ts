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

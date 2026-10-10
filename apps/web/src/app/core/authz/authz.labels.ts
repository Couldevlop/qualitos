/**
 * Les mots des droits. Le serveur rend des codes (`capa.create`, `QUALITY_MANAGER`) ;
 * ils deviennent ici des mots dans la langue de l'écran. Un code inconnu — une
 * action ajoutée au catalogue avant que l'écran la connaisse — s'affiche tel quel.
 */

const PERMISSIONS: Record<string, string> = {
  'authz.manage': $localize`:@@authz.perm.authz-manage:Administrer les rôles et les droits`,
  'capa.view.all': $localize`:@@authz.perm.capa-view-all:Voir tous les dossiers (sinon : ceux qui le concernent)`,
  'capa.create': $localize`:@@authz.perm.capa-create:Ouvrir un dossier CAPA`,
  'capa.edit': $localize`:@@authz.perm.capa-edit:Modifier et démarrer un dossier`,
  'capa.resolve': $localize`:@@authz.perm.capa-resolve:Résoudre un dossier`,
  'capa.reject': $localize`:@@authz.perm.capa-reject:Rejeter un dossier`,
  'capa.verify': $localize`:@@authz.perm.capa-verify:Vérifier l'efficacité`,
  'capa.delete': $localize`:@@authz.perm.capa-delete:Supprimer un dossier`,
  'capa.action.manage': $localize`:@@authz.perm.capa-action-manage:Ajouter et retirer des actions`,
  'capa.action.update': $localize`:@@authz.perm.capa-action-update:Faire avancer une action`,
  'nc.view.all': $localize`:@@authz.perm.nc-view-all:Voir toutes les non-conformités (sinon : celles qu'il a déclarées)`,
  'nc.create': $localize`:@@authz.perm.nc-create:Déclarer une non-conformité`,
  'nc.edit': $localize`:@@authz.perm.nc-edit:Modifier une non-conformité`,
  'nc.photo': $localize`:@@authz.perm.nc-photo:Ajouter des photos`,
  'nc.process': $localize`:@@authz.perm.nc-process:Analyser et résoudre`,
  'nc.close': $localize`:@@authz.perm.nc-close:Clôturer`,
  'nc.reject': $localize`:@@authz.perm.nc-reject:Annuler ou rejeter`,
  'nc.escalate': $localize`:@@authz.perm.nc-escalate:Poser une action corrective`,
  'document.edit': $localize`:@@authz.perm.document-edit:Rédiger et archiver`,
  'document.submit': $localize`:@@authz.perm.document-submit:Soumettre à revue`,
  'document.approve': $localize`:@@authz.perm.document-approve:Approuver`,
  'document.publish': $localize`:@@authz.perm.document-publish:Publier`,
  'document.acknowledge': $localize`:@@authz.perm.document-acknowledge:Acquitter une lecture`,
  'risk.view.all': $localize`:@@authz.perm.risk-view-all:Voir tout le registre (sinon : ce qu'il a inscrit)`,
  'risk.manage': $localize`:@@authz.perm.risk-manage:Tenir le registre des risques`,
  'opportunity.manage': $localize`:@@authz.perm.opportunity-manage:Tenir le registre des opportunités`
};

const MODULES: Record<string, string> = {
  admin: $localize`:@@authz.module.admin:Administration`,
  capa: $localize`:@@authz.module.capa:CAPA`,
  nc: $localize`:@@authz.module.nc:Non-conformités`,
  document: $localize`:@@authz.module.document:Documents`,
  risk: $localize`:@@authz.module.risk:Risques et opportunités`
};

const SYSTEM_ROLES: Record<string, string> = {
  ADMIN_TENANT: $localize`:@@authz.role.admin-tenant:Administrateur`,
  QUALITY_DIRECTOR: $localize`:@@authz.role.quality-director:Directeur qualité`,
  QUALITY_MANAGER: $localize`:@@authz.role.quality-manager:Manager qualité`,
  AUDITOR: $localize`:@@authz.role.auditor:Auditeur`,
  USER: $localize`:@@authz.role.user:Utilisateur`,
  EXTERNAL_AUDITOR: $localize`:@@authz.role.external-auditor:Auditeur externe`
};

export const permissionLabel = (code: string): string => PERMISSIONS[code] ?? code;
export const moduleLabel = (code: string): string => MODULES[code] ?? code;

/** Le nom d'un rôle : celui que le client lui a donné, sinon celui de la plateforme. */
export function roleLabel(code: string, name?: string | null): string {
  return name || SYSTEM_ROLES[code] || code;
}

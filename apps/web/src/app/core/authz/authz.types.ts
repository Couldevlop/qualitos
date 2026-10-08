/** Les droits par client (ADR 0078), tels que `/api/v1/authz` les rend. */

/** Le code d'une action du catalogue, par exemple `capa.create`. */
export type PermissionCode = string;

export interface AuthzMe {
  userId: string | null;
  roles: string[];
  permissions: PermissionCode[];
}

export interface CatalogEntry {
  code: PermissionCode;
  module: string;
}

export interface RoleView {
  code: string;
  name: string | null;
  description: string | null;
  system: boolean;
  /** Faux pour un rôle système encore aux droits livrés par la plateforme. */
  customized: boolean;
  permissions: PermissionCode[];
}

export interface RoleCommand {
  code?: string;
  name?: string | null;
  description?: string | null;
  permissions: PermissionCode[];
}

/** Les rôles attribués dans l'application à un membre, en plus de ceux de son compte. */
export interface MemberRoles {
  userId: string;
  roles: string[];
}

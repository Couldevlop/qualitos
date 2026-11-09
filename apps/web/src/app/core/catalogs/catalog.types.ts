/**
 * Les listes déroulantes paramétrables par le client (§5.4).
 *
 * <p>Un nom de liste est celui du serveur (`CatalogId.slug`) : c'est la clé de
 * l'API et la seule chose que les écrans partagent avec lui.
 */
export type CatalogName =
  | 'nc-category'
  | 'audit-type'
  | 'supplier-type'
  | 'ehs-incident-type'
  | 'apqp-project-type'
  | 'capa-nature'
  | 'moc-change-type'
  | 'ishikawa-category'
  | 'kpi-frequency'
  | 'pokayoke-mechanism'
  | 'fives-zone'
  | 'stakeholder-sector';

/**
 * Jusqu'où le client peut aller.
 *
 * - `RENAME` : les codes sont ceux de la plateforme, il renomme et réordonne.
 * - `OPEN` : la liste entière lui appartient.
 */
export type CatalogMode = 'RENAME' | 'OPEN';

/** Une entrée : le code que la donnée porte, le mot que l'utilisateur lit. */
export interface CatalogEntry {
  code: string;
  label: string;
  active: boolean;
}

/**
 * Une liste rendue par le serveur.
 *
 * <p>`defaults` vrai signifie que le client n'a RIEN réglé : les écrans gardent
 * alors leurs propres libellés traduits en six langues. Faux, ce sont les mots
 * du client, dans sa langue — la plateforme ne les traduit pas.
 */
export interface Catalog {
  catalogue: CatalogName;
  mode: CatalogMode;
  defaults: boolean;
  entries: CatalogEntry[];
}

/** L'état d'une liste, pour l'écran de paramétrage. */
export interface CatalogSummary {
  catalogue: CatalogName;
  mode: CatalogMode;
  defaults: boolean;
  entryCount: number;
}

/** Une entrée telle qu'on l'envoie : `active` omis vaut actif. */
export interface CatalogEntryForm {
  code: string;
  label: string;
  active?: boolean;
}

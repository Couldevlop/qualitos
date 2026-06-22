/**
 * Types front du Marketplace de packs normatifs (CLAUDE.md §8.11).
 *
 * Le backend expose :
 *  - catalogue public : GET /api/v1/marketplace/packs (uniquement PUBLISHED) ;
 *  - soumission partenaire : POST /api/v1/marketplace/packs ;
 *  - modération éditeur : queue + transitions (take-review / publish / reject / deprecate) ;
 *  - installation tenant : install / uninstall / my / my history ;
 *  - notation : POST …/{id}/rate.
 */

export type MarketplacePackStatus =
  'SUBMITTED' | 'IN_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'DEPRECATED';

export type InstallationStatus = 'INSTALLED' | 'UNINSTALLED';

/** Vue d'un pack. Les champs de modération sont nuls dans le catalogue public. */
export interface MarketplacePackView {
  id: string;
  packId: string;
  version: string;
  publisher: string;
  title: string;
  description?: string | null;
  sector: string;
  norms: string[];
  priceCents: number;
  currency: string;
  status: MarketplacePackStatus;
  submittedBy?: string | null;
  submittedAt?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  reviewNotes?: string | null;
  signatureHash?: string | null;
  manifestUrl: string;
  ratingAvg: number;
  ratingCount: number;
  createdAt?: string;
  updatedAt?: string;
}

/** Vue d'une installation par tenant. */
export interface InstallationView {
  id: string;
  tenantId: string;
  marketplacePackId: string;
  packId: string;
  packVersion: string;
  status: InstallationStatus;
  installedBy?: string;
  installedAt?: string;
  uninstalledBy?: string | null;
  uninstalledAt?: string | null;
}

/** Commande de soumission partenaire. */
export interface SubmitRequest {
  packId: string;
  version: string;
  publisher: string;
  title: string;
  description?: string;
  sector: string;
  norms: string[];
  priceCents: number;
  currency: string;
  manifestUrl: string;
  manifestJson: string;
  signatureHash: string;
}

/** Vue composée pour le catalogue : pack + état d'installation tenant. */
export interface CatalogEntry {
  pack: MarketplacePackView;
  installation?: InstallationView;
}

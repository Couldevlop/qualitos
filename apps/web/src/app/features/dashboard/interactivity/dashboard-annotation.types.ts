/** Annotation collaborative posée sur un graphique de dashboard (§7.3). */
export interface DashboardAnnotation {
  id: string;
  tenantId: string;
  authorId: string;
  chartKey: string;
  anchorLabel?: string | null;
  body: string;
  createdAt: string;
  /** True si l'utilisateur courant peut supprimer (auteur ou admin). */
  deletable: boolean;
}

/** Payload de création — tenantId/authorId ne sont JAMAIS envoyés (JWT côté API). */
export interface CreateAnnotationRequest {
  chartKey: string;
  anchorLabel?: string | null;
  body: string;
}

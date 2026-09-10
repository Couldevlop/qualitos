/** Le tableau des idées, tel que le serveur le rend. */

export type IdeaStatus =
  'PROPOSED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'IMPLEMENTED' | 'MEASURED';

export interface Idea {
  id: string;
  title: string;
  description: string | null;
  status: IdeaStatus;
  authorId: string;
  /** Copié au dépôt, donc parfois absent (jeton sans nom, idée antérieure). */
  authorName: string | null;
  /** Calculé par le serveur : jamais stocké sur l'idée (cf. le service backend). */
  votes: number;
  votedByMe: boolean;
  /** Faux dès que l'idée est tranchée — le bouton disparaît alors. */
  voteOpen: boolean;
  circleId: string | null;
  rejectionReason: string | null;
  impactNote: string | null;
  createdAt: string;
}

export interface IdeaColumn {
  status: IdeaStatus;
  ideas: Idea[];
}

export interface IdeaBoard {
  columns: IdeaColumn[];
}

export interface SubmitIdeaRequest {
  title: string;
  description?: string;
  circleId?: string;
}

export interface RejectIdeaRequest { reason: string; }

export interface ImpactRequest { impactNote: string; }

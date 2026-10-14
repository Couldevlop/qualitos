/**
 * L'annuaire du client en lecture restreinte (ADR 0091), tel que
 * `/api/v1/directory/users` le rend : de quoi reconnaître et désigner une
 * personne, rien de ce qui sert à l'administrer.
 */
export interface DirectoryUser {
  id: string;
  /** Nom saisi à l'invitation, ou l'adresse quand aucun nom n'est enregistré. Jamais vide. */
  displayName: string;
  /** Adresse professionnelle : départage deux homonymes. */
  email: string;
  /** Faux pour un ancien membre, lu par identifiant (responsable d'un dossier passé). */
  active: boolean;
}

export interface DirectoryPage {
  content: DirectoryUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Bornes imposées par le serveur : au-delà, il répond 400. */
export const DIRECTORY_MAX_PAGE_SIZE = 50;
export const DIRECTORY_MAX_TERM_LENGTH = 100;

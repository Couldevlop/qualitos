import { SpringPage } from '../pdca/pdca.types';

/**
 * Une réclamation client, telle que la liste la rend (§4.9).
 *
 * <p>Vue de LECTURE, volontairement partielle : le registre des réclamations
 * vit côté serveur depuis longtemps, mais aucun écran ne le lisait. Ce type ne
 * déclare donc que ce dont la plateforme a besoin aujourd'hui — désigner une
 * réclamation sans la retaper. Il grandira avec les écrans qui l'utiliseront,
 * pas avant : un type qui annonce des champs que personne n'affiche laisse
 * croire qu'ils sont tenus à jour.
 */
export interface ComplaintSummary {
  id: string;
  /** Désignation citable — « REC-2026-0001 ». */
  code: string;
  subject: string;
  status: string;
  customerName?: string;
}

export type ComplaintsPage = SpringPage<ComplaintSummary>;

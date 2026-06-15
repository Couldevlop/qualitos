/**
 * Types du clustering de non-conformités (§4.3, §12.1). Le SPA envoie une liste de textes
 * de NC à l'engine (`POST /api/v1/ai/nc-clusters`) qui applique les garde-fous IA (OWASP
 * LLM04) et relaie vers ai-service (TF-IDF + DBSCAN densité, NumPy pur). Le tenant vient du
 * JWT côté serveur (jamais envoyé dans le body).
 */

export interface NcClusterRequest {
  texts: string[];
  /** Similarité cosinus minimale du voisinage (ε = 1 − threshold). */
  threshold?: number;
  /** Taille minimale du voisinage d'un point-cœur DBSCAN (densité). */
  minSamples?: number;
}

export interface NcCluster {
  clusterId: number;
  indices: number[];   // positions des NC membres dans la liste soumise
  size: number;
  topTerms: string[];  // termes représentatifs (explicabilité)
}

export interface NcClusterResponse {
  n: number;
  clusteredRatio: number;
  method: string;       // "dbscan"
  clusters: NcCluster[];
  noiseIndices: number[];
}

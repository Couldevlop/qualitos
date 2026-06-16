/**
 * Types de l'analyse NLP des réclamations (§4.9, §12.1). Le SPA envoie un lot de réclamations
 * à l'engine (`POST /api/v1/ai/complaints/analyze`) qui applique les garde-fous IA (OWASP
 * LLM04) et relaie vers ai-service (sentiment lexical + classification, pur). Le tenant vient
 * du JWT côté serveur (jamais envoyé dans le body).
 */

export interface ComplaintAnalyzeRequest {
  texts: string[];
  /** Taxonomie optionnelle {catégorie: [termes-graines]} ; défaut si absente. */
  categories?: Record<string, string[]>;
}

export interface ComplaintInsight {
  index: number;
  sentiment: number;        // polarité ∈ [-1, 1]
  sentimentLabel: string;   // "negative" | "neutral" | "positive"
  category: string;
  critical: boolean;
}

export interface ComplaintAnalyzeResponse {
  n: number;
  criticalCount: number;
  insights: ComplaintInsight[];
}

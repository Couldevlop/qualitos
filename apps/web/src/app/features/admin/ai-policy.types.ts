/** Fournisseur des appels LLM d'un tenant (ADR 0102). Miroir de `AiPolicyDto` côté engine. */
export type AiProviderName = 'ollama' | 'anthropic' | 'mistral';

export interface AiProviderOption {
  name: AiProviderName;
  /** Le modèle tourne chez le client : aucune donnée vers un tiers. */
  onPremise: boolean;
  /** `null` quand le service d'IA n'a pas répondu. */
  configured: boolean | null;
}

export interface AiPolicy {
  pinned: boolean;
  provider: AiProviderName | null;
  /** Celui qui répond réellement ; `null` si le service d'IA ne répond pas. */
  effectiveProvider: AiProviderName | null;
  platformDefault: AiProviderName | null;
  catalogAvailable: boolean;
  providers: AiProviderOption[];
  updatedAt: string | null;
  updatedBy: string | null;
}

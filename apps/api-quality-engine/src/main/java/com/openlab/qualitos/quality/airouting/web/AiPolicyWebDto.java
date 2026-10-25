package com.openlab.qualitos.quality.airouting.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Corps de {@code PUT /api/v1/admin/ai-policy} (ADR 0102). Ni tenant ni auteur :
 * ils viennent du jeton (§18.2). Le fournisseur est validé ici, puis au domaine.
 */
public record AiPolicyWebDto(
        @NotBlank
        @Pattern(regexp = "ollama|anthropic|mistral",
                message = "provider must be one of: ollama, anthropic, mistral")
        String provider) {
}

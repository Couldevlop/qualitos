package com.openlab.qualitos.quality.nlq;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint NLQ (§7.3). POST car l'appel a un effet de bord (inférence LLM + requête).
 * Authentifié (SecurityConfig {@code anyRequest().authenticated()}) ; le tenant est
 * dérivé du JWT côté passerelle, jamais du corps de requête.
 */
@RestController
@RequestMapping("/api/v1/ai/nlq")
public class NlqController {

    private final NlqService nlqService;

    public NlqController(NlqService nlqService) {
        this.nlqService = nlqService;
    }

    @PostMapping("/ask")
    public NlqDto.AskResponse ask(@Valid @RequestBody NlqDto.AskRequest request) {
        return nlqService.ask(request);
    }
}

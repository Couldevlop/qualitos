package com.openlab.qualitos.quality.complaintnlp;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint d'analyse NLP des réclamations (§4.9, §12.1). POST car l'appel a un effet de bord
 * (relais vers l'inférence d'{@code ai-service} : sentiment + classification). Authentifié
 * (SecurityConfig {@code anyRequest().authenticated()}, comme {@code SpcController}) ; le tenant
 * est dérivé du JWT côté passerelle, jamais du corps de requête (règle 18.2 #2).
 *
 * <p>Sous le préfixe {@code /api/v1/ai/} (analyse IA), distinct du module {@code complaints}
 * (gestion CRUD des réclamations).
 */
@RestController
@RequestMapping("/api/v1/ai/complaints")
public class ComplaintNlpController {

    private final ComplaintNlpService complaintNlpService;

    public ComplaintNlpController(ComplaintNlpService complaintNlpService) {
        this.complaintNlpService = complaintNlpService;
    }

    @PostMapping("/analyze")
    public ComplaintNlpDto.AnalyzeResponse analyze(
            @Valid @RequestBody ComplaintNlpDto.AnalyzeRequest request) {
        return complaintNlpService.analyze(request);
    }
}

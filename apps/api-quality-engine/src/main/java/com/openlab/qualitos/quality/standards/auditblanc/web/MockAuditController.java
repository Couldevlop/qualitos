package com.openlab.qualitos.quality.standards.auditblanc.web;

import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditDto;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — audit blanc IA avancé (Standards Hub §8.4 onglet 7). Simule un
 * audit de certification avant le réel : 30-100 questions ciblées sur les
 * clauses à risque, gap analysis et plan de remédiation auto-créé.
 *
 * <p>Sécurité : tenant + acteur issus du JWT (jamais du body, §18.2 #2/#5). Le
 * lancement (effet de bord : appel LLM + persistance + journalisation, d'où POST)
 * relève du pilotage qualité (Manager Qualité+). La relecture est ouverte à tout
 * authentifié du tenant (le service confine au tenant courant).
 */
@RestController
@RequestMapping("/api/v1/standards/adoptions/{adoptionId}/audit-blanc-ia")
public class MockAuditController {

    private final MockAuditService service;

    public MockAuditController(MockAuditService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('QUALITY_MANAGER','DIRECTOR_QUALITY','AUDITOR','ADMIN','ADMIN_TENANT','SUPER_ADMIN')")
    public MockAuditDto.Report run(@PathVariable UUID adoptionId) {
        return service.run(adoptionId);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MockAuditDto.Report> history(@PathVariable UUID adoptionId) {
        return service.history(adoptionId);
    }

    @GetMapping("/{runId}")
    @PreAuthorize("isAuthenticated()")
    public MockAuditDto.Report get(@PathVariable UUID adoptionId, @PathVariable UUID runId) {
        return service.get(runId);
    }
}

package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.revisionrequests.application.PfmeaPort;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.risk.FmeaProject;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import com.openlab.qualitos.quality.risk.FmeaStatus;
import com.openlab.qualitos.quality.risk.FmeaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Branche le moteur de propositions sur le module {@code risk}.
 *
 * <p>Chaque méthode revérifie le tenant sur l'entité chargée : les dépôts Spring
 * Data du module exposent des lectures par identifiant qui ne le font pas, et
 * s'en remettre à l'appelant serait exactement la faille qu'on cherche à fermer.
 */
@Component
public class PfmeaAdapter implements PfmeaPort {

    private final FmeaProjectRepository projects;
    private final FmeaItemRepository items;

    public PfmeaAdapter(FmeaProjectRepository projects, FmeaItemRepository items) {
        this.projects = projects;
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PfmeaItemSnapshot> item(UUID tenantId, UUID fmeaItemId) {
        if (fmeaItemId == null) return Optional.empty();
        return items.findByIdAndTenantId(fmeaItemId, tenantId)
                .flatMap(item -> projects.findById(item.getProjectId())
                        .filter(project -> tenantId.equals(project.getTenantId()))
                        .map(project -> snapshot(item, project)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> activeProjectOf(UUID tenantId, UUID productId) {
        return projects.findByTenantIdAndProductId(tenantId, productId).stream()
                .filter(project -> project.getType() == FmeaType.PROCESS_FMEA)
                .filter(project -> project.getStatus() == FmeaStatus.ACTIVE)
                .findFirst()
                .map(FmeaProject::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProjectActive(UUID tenantId, UUID projectId) {
        return projects.findById(projectId)
                .filter(project -> tenantId.equals(project.getTenantId()))
                .map(project -> project.getStatus() == FmeaStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional
    public void openRevision(UUID tenantId, UUID projectId) {
        FmeaProject project = requireProject(tenantId, projectId);
        if (project.getStatus() != FmeaStatus.ACTIVE) return;
        project.setStatus(FmeaStatus.DRAFT);
        project.setRevision(project.getRevision() + 1);
        projects.save(project);
    }

    @Override
    @Transactional
    public void updateRating(UUID tenantId, UUID fmeaItemId, String field, int value) {
        FmeaItem item = items.findByIdAndTenantId(fmeaItemId, tenantId)
                .orElseThrow(() -> new IllegalStateException("Unknown FMEA item: " + fmeaItemId));
        switch (field) {
            case "occurrence" -> item.setOccurrence(value);
            case "detection" -> item.setDetection(value);
            // Seules ces deux cotes se déduisent d'un fait observé. La sévérité
            // décrit la gravité de l'effet pour le client : ni une NC ni une CAPA
            // ne la changent, seule une revue de conception le peut.
            default -> throw new IllegalStateException("Unsupported FMEA rating: " + field);
        }
        item.recomputeRpn();
        items.save(item);
    }

    @Override
    @Transactional
    public UUID addItem(UUID tenantId, UUID projectId, String failureMode, String failureEffect) {
        FmeaProject project = requireProject(tenantId, projectId);
        FmeaItem item = new FmeaItem();
        item.setTenantId(project.getTenantId());
        item.setProjectId(project.getId());
        item.setSequenceNo(items.findMaxSequenceNo(project.getId()) + 1);
        item.setFailureMode(trim(failureMode, 500));
        item.setFailureEffect(trim(failureEffect, 500));
        // Non coté : l'analyse doit être faite par un humain, la ligne n'est qu'un
        // emplacement ouvert. Un item à 0 n'a ni RPN ni priorité d'action.
        return items.save(item).getId();
    }

    private FmeaProject requireProject(UUID tenantId, UUID projectId) {
        return projects.findById(projectId)
                .filter(project -> tenantId.equals(project.getTenantId()))
                .orElseThrow(() -> new IllegalStateException("Unknown FMEA project: " + projectId));
    }

    private static PfmeaItemSnapshot snapshot(FmeaItem item, FmeaProject project) {
        return new PfmeaItemSnapshot(item.getId(), project.getId(), project.getProductId(),
                item.getFailureMode(), item.getSeverity(), item.getOccurrence(), item.getDetection());
    }

    private static String trim(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}

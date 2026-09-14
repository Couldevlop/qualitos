package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les projets APQP d'un client.
 *
 * <p>Un projet est l'unité de travail : un programme, son cycle en V, et le
 * dossier PPAP qu'il produit. Une organisation en mène plusieurs de front, et
 * chacun se remet à son client séparément — d'où une liste, comme pour les
 * projets PFMEA, plutôt qu'un cycle unique par tenant (ADR 0072).
 */
@Service
public class ApqpProjectService {

    /** L'avancement d'un projet, tel que la requête groupée le rend. */
    private record Avancement(int total, int acquis, int ppapTotal, int ppapAcquis) {

        static final Avancement VIDE = new Avancement(0, 0, 0, 0);
    }

    private final ApqpProjectRepository projets;
    private final ApqpPhaseRepository phases;
    private final ApqpCycleSeeder seeder;

    public ApqpProjectService(ApqpProjectRepository projets,
                              ApqpPhaseRepository phases,
                              ApqpCycleSeeder seeder) {
        this.projets = projets;
        this.phases = phases;
        this.seeder = seeder;
    }

    /**
     * Les projets du client, avec leur avancement.
     *
     * <p>Sans pagination, et c'est délibéré : une organisation mène quelques
     * dizaines de programmes, pas des milliers, et l'écran doit pouvoir en dresser
     * le tableau d'ensemble — c'est précisément ce qu'on vient y chercher.
     */
    @Transactional(readOnly = true)
    public List<ApqpDto.ProjectResponse> lister() {
        UUID tenantId = requireTenantId();
        Map<UUID, Avancement> avancements = avancements(tenantId);
        List<ApqpDto.ProjectResponse> reponses = new ArrayList<>();
        for (ApqpProject projet : projets.findByTenantIdOrderByCreatedAtDesc(tenantId)) {
            reponses.add(enReponse(projet, avancements));
        }
        return reponses;
    }

    @Transactional(readOnly = true)
    public ApqpDto.ProjectResponse lire(UUID projectId) {
        UUID tenantId = requireTenantId();
        return enReponse(charger(projectId, tenantId), avancements(tenantId));
    }

    /**
     * Ouvre un projet, et lui donne aussitôt le cycle du référentiel.
     *
     * <p>Amorcé à la CRÉATION et non à la première lecture : le projet n'existe
     * que parce que quelqu'un vient de le demander, il n'y a donc plus rien à
     * économiser en différant, et un projet sans cycle serait un état de plus à
     * gérer dans chaque écran.
     */
    @Transactional
    public ApqpDto.ProjectResponse creer(ApqpDto.CreateProjectRequest requete, UUID acteur) {
        UUID tenantId = requireTenantId();

        ApqpProject projet = new ApqpProject();
        projet.setTenantId(tenantId);
        projet.setCreatedBy(acteur);
        appliquer(projet, requete.name(), requete.type(), requete.customer(),
                requete.reference(), requete.description());

        ApqpProject enregistre = projets.save(projet);
        seeder.amorcer(enregistre);
        return enReponse(enregistre, avancements(tenantId));
    }

    @Transactional
    public ApqpDto.ProjectResponse modifier(UUID projectId, ApqpDto.UpdateProjectRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpProject projet = charger(projectId, tenantId);
        appliquer(projet, requete.name(), requete.type(), requete.customer(),
                requete.reference(), requete.description());
        return enReponse(projets.save(projet), avancements(tenantId));
    }

    /**
     * Supprime un projet, son cycle, ses livrables et leurs preuves.
     *
     * <p>La cascade est portée par la base (V131) : un cycle sans projet n'a pas
     * d'existence propre. L'écran demande confirmation, le serveur ne la redemande
     * pas.
     */
    @Transactional
    public void supprimer(UUID projectId) {
        UUID tenantId = requireTenantId();
        projets.delete(charger(projectId, tenantId));
    }

    // ---------- interne ----------

    private void appliquer(ApqpProject projet, String nom, ApqpProjectType type,
                           String client, String reference, String description) {
        projet.setName(nom.trim());
        projet.setType(type);
        projet.setCustomer(nettoyer(client));
        projet.setReference(nettoyer(reference));
        projet.setDescription(nettoyer(description));
    }

    /**
     * L'avancement de tous les projets du client, en UNE requête.
     *
     * <p>La liste affiche quatre compteurs par ligne ; les demander projet par
     * projet ferait autant d'allers-retours que de programmes ouverts.
     */
    private Map<UUID, Avancement> avancements(UUID tenantId) {
        Map<UUID, Avancement> parProjet = new HashMap<>();
        for (Object[] ligne : phases.avancementParProjet(tenantId)) {
            parProjet.put((UUID) ligne[0], new Avancement(
                    entier(ligne[1]), entier(ligne[2]), entier(ligne[3]), entier(ligne[4])));
        }
        return parProjet;
    }

    /** {@code sum} rend {@code null} sur un groupe sans ligne retenue. */
    private static int entier(Object valeur) {
        return valeur == null ? 0 : ((Number) valeur).intValue();
    }

    private ApqpProject charger(UUID projectId, UUID tenantId) {
        return projets.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ApqpProjectNotFoundException(projectId));
    }

    private ApqpDto.ProjectResponse enReponse(ApqpProject projet,
                                              Map<UUID, Avancement> avancements) {
        Avancement a = avancements.getOrDefault(projet.getId(), Avancement.VIDE);
        return new ApqpDto.ProjectResponse(
                projet.getId(), projet.getName(), projet.getType(), projet.getCustomer(),
                projet.getReference(), projet.getDescription(),
                a.total(), a.acquis(), a.ppapTotal(), a.ppapAcquis(),
                projet.getCreatedAt(), projet.getUpdatedAt());
    }

    private String nettoyer(String valeur) {
        if (valeur == null) {
            return null;
        }
        String rogne = valeur.trim();
        return rogne.isEmpty() ? null : rogne;
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}

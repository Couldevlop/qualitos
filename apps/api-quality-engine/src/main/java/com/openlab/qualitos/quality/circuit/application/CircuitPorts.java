package com.openlab.qualitos.quality.circuit.application;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.circuit.domain.CircuitRun;
import com.openlab.qualitos.quality.circuit.domain.CircuitStep;
import com.openlab.qualitos.quality.circuit.domain.CircuitSubject;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Ce que les circuits lisent et écrivent. */
public final class CircuitPorts {

    private CircuitPorts() {}

    /** Le circuit réglé par chaque client, par type d'objet. */
    public interface Circuits {

        List<CircuitStep> find(UUID tenantId, CircuitSubject subject);

        void replace(UUID tenantId, CircuitSubject subject, List<CircuitStep> steps, UUID actor);
    }

    public interface Runs {

        Optional<CircuitRun> findOpen(UUID tenantId, CircuitSubject subject, UUID subjectId);

        Optional<CircuitRun> findLatest(UUID tenantId, CircuitSubject subject, UUID subjectId);

        void save(CircuitRun run);
    }

    /** Le client, l'acteur et ses rôles — tous du jeton ; les rôles connus du client. */
    public interface Context {

        UUID requireTenantId();

        UUID requireActorId();

        Set<String> actorRoles();

        Set<String> tenantRoles();

        /** Vrai si le rôle, tel que le client l'a réglé, accorde ce droit. */
        boolean roleGrants(String roleCode, Permission permission);
    }

    /** La trace opposable, sans texte libre (les commentaires restent au passage). */
    public interface Audit {

        void circuitReplaced(UUID tenantId, CircuitSubject subject, List<CircuitStep> steps, UUID actor);

        void decided(CircuitRun run, CircuitRun.Decision decision);
    }
}

package com.openlab.qualitos.quality.dmaic.resolution.application;

import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSheet;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetPhoto;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les ports de la fiche de résolution. Chaque lecture prend le tenant en
 * argument : l'isolation est dans la signature, pas dans la discipline.
 */
public final class ResolutionPorts {

    private ResolutionPorts() {}

    /** Le tenant et l'acteur, lus dans le jeton validé (§18.2), jamais dans le corps. */
    public interface Context {
        UUID requireTenantId();

        UUID requireActorId();
    }

    public interface Sheets {
        Optional<ResolutionSheet> findByProject(UUID tenantId, UUID projectId);

        ResolutionSheet save(ResolutionSheet sheet);
    }

    public interface Settings {
        Optional<ResolutionSettings> find(UUID tenantId);

        void save(UUID tenantId, ResolutionSettings settings, UUID actor, Instant now);

        /** Retire le réglage du client : la plateforme reprend ses valeurs. */
        void reset(UUID tenantId);
    }

    public interface Photos {
        List<SheetPhoto> list(UUID tenantId, UUID projectId);

        Optional<SheetPhoto> find(UUID tenantId, UUID projectId, int slot);

        SheetPhoto save(SheetPhoto photo);

        void delete(SheetPhoto photo);
    }

    /** Le binaire des photos. Indisponible quand le stockage objet est éteint (503). */
    public interface PhotoStorage {
        /** Faux quand le stockage objet est éteint : les photos ne se lisent ni ne s'écrivent. */
        boolean available();

        void put(String key, String contentType, byte[] content);

        URL presignGet(String key);

        void delete(String key);
    }

    /** Le projet DMAIC auquel la fiche se rattache, lu dans SON tenant. */
    public interface Projects {
        Optional<ProjectHeader> find(UUID tenantId, UUID projectId);
    }

    /** Ce que la fiche reprend du projet : son titre et son énoncé du problème. */
    public record ProjectHeader(UUID id, String title, String problemStatement) {
    }

    /** Le diagramme d'Ishikawa lié doit exister chez CE client. */
    public interface Ishikawas {
        boolean exists(UUID tenantId, UUID ishikawaId);

        /** Les causes du diagramme, pour le rapport client ; aucune quand il n'existe pas chez ce client. */
        default List<CauseLine> causes(UUID tenantId, UUID ishikawaId) {
            return List.of();
        }
    }

    /** Une cause d'Ishikawa : sa catégorie en clair et son libellé. */
    public record CauseLine(String category, String label) {
    }

    /**
     * Trace d'audit (OWASP A09) : qui a rempli la fiche, qui a changé le
     * catalogue, qui a versé ou retiré une photo.
     */
    public interface Audit {
        void sheetSaved(ResolutionSheet sheet, UUID actor);

        void settingsReplaced(UUID tenantId, ResolutionSettings settings, UUID actor);

        void settingsReset(UUID tenantId, UUID actor);

        void photoAdded(SheetPhoto photo, UUID actor);

        void photoRemoved(SheetPhoto photo, UUID actor);
    }

    /** Met le rapport d'action corrective client en page. */
    public interface ClaimReportRenderer {
        byte[] render(ClaimReport report);
    }
}

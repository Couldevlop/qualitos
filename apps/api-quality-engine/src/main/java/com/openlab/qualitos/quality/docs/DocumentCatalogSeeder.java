package com.openlab.qualitos.quality.docs;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Sème, une fois par client, l'arborescence et les processus de départ de la GED
 * (ADR 0094), tels que la maquette les présente. Le client les renomme, les
 * complète ou les retire ensuite à sa guise.
 *
 * <p>Transaction à part : deux premières lectures simultanées tentent chacune de
 * semer ; la seconde échoue sur la clé de {@code document_settings} et seule sa
 * propre transaction est annulée, pas la lecture qui l'a déclenchée.
 */
@Component
public class DocumentCatalogSeeder {

    /** Dossiers de premier niveau, et leurs sous-dossiers. */
    static final List<List<String>> DEFAULT_FOLDERS = List.of(
            List.of("Manuel qualité"),
            List.of("Procédures", "Processus de réalisation", "Processus support"),
            List.of("Instructions de travail"),
            List.of("Formulaires et modèles"),
            List.of("Enregistrements"),
            List.of("Documents externes"));

    static final List<String> DEFAULT_PROCESSES = List.of(
            "Management qualité", "Réalisation", "Support", "Achats",
            "Ressources humaines", "Mesure et amélioration");

    private final DocumentSettingsRowRepository settings;
    private final DocumentFolderRepository folders;
    private final DocumentProcessRepository processes;

    public DocumentCatalogSeeder(DocumentSettingsRowRepository settings,
                                 DocumentFolderRepository folders,
                                 DocumentProcessRepository processes) {
        this.settings = settings;
        this.folders = folders;
        this.processes = processes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seed(UUID tenantId) {
        if (settings.existsById(tenantId)) {
            return;
        }
        settings.saveAndFlush(DocumentSettingsRow.defaults(tenantId));
        int position = 0;
        for (List<String> branch : DEFAULT_FOLDERS) {
            DocumentFolder root = folders.save(folder(tenantId, null, branch.get(0), position++));
            int child = 0;
            for (String name : branch.subList(1, branch.size())) {
                folders.save(folder(tenantId, root.getId(), name, child++));
            }
        }
        int p = 0;
        for (String name : DEFAULT_PROCESSES) {
            DocumentProcess process = new DocumentProcess();
            process.setTenantId(tenantId);
            process.setName(name);
            process.setPosition(p++);
            processes.save(process);
        }
    }

    private static DocumentFolder folder(UUID tenantId, UUID parentId, String name, int position) {
        DocumentFolder f = new DocumentFolder();
        f.setTenantId(tenantId);
        f.setParentId(parentId);
        f.setName(name);
        f.setPosition(position);
        return f;
    }
}

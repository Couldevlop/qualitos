package com.openlab.qualitos.quality.derogation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance de la dérogation. Interface posée dans le domaine : c'est
 * lui qui dicte ce dont il a besoin, l'infrastructure s'y plie.
 *
 * <p>Aucune signature ne parle de pagination Spring ni de spécification JPA :
 * le domaine ne doit rien savoir du cadre qui le stocke. Le registre demande une
 * tranche et des critères, et reçoit une tranche.
 */
public interface DerogationRepository {

    /**
     * Enregistre le dossier et rend la version relue.
     *
     * @throws DerogationConflictException si un autre geste a écrit entre-temps
     */
    Derogation save(Derogation dossier);

    Optional<Derogation> findById(UUID tenantId, UUID id);

    /**
     * Le registre, filtré et paginé.
     *
     * <p>Les lignes rendues portent déjà la première ligne d'écart de chaque
     * dossier : la colonne « défaut » du registre en a besoin, et la demander
     * dossier par dossier ferait cinquante requêtes sur un écran qui en affiche
     * cinquante.
     */
    Slice<Derogation> search(DerogationQuery query);

    /** La file d'attente d'un manager : « Mes validations ». */
    Slice<Derogation> findPendingApprovals(UUID tenantId, UUID managerAccountId,
                                           int page, int size);

    /** L'alerte rouge et son nombre, au-dessus de « Mes validations ». */
    long countPendingApprovals(UUID tenantId, UUID managerAccountId);

    /**
     * Combien de dossiers portent déjà ce préfixe de référence, pour ce client.
     *
     * <p>Sert à numéroter la suivante. Le domaine ne compose pas la référence
     * lui-même : c'est le service qui tient la règle {@code DER-<année>-<rang>},
     * parce qu'elle dépend de la date du jour.
     */
    long countByReferencePrefix(UUID tenantId, String prefix);

    boolean existsByReference(UUID tenantId, String reference);

    /**
     * Une tranche de résultats, sans rien emprunter au cadre de persistance.
     *
     * @param total le nombre total de dossiers qui répondent aux critères, dont
     *              l'écran a besoin pour sa pagination
     */
    record Slice<T>(List<T> content, long total, int page, int size) {

        public Slice {
            content = List.copyOf(content);
        }

        public static <T> Slice<T> vide(int page, int size) {
            return new Slice<>(List.of(), 0L, page, size);
        }
    }
}

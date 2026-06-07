package com.openlab.qualitos.quality.nonconformity.storage;

import java.net.URL;
import java.time.Duration;

/**
 * Port de stockage binaire S3-compatible (MinIO/S3). Les adaptateurs ne
 * connaissent rien du domaine NC : ils manipulent des objets opaques (clé +
 * octets). La tenantisation est portée par la clé, construite en amont par le
 * service (jamais ici).
 *
 * <p>Deux implémentations : {@link S3ObjectStorage} (prod/dev MinIO, activée par
 * {@code qualitos.storage.s3.enabled=true}) et {@code InMemoryObjectStorage}
 * (tests). Quand aucune n'est active, le stockage est désactivé et les endpoints
 * photos répondent 503 (cf. {@link StorageDisabledException}).</p>
 */
public interface ObjectStorage {

    /**
     * Téléverse un objet. La taille est fournie explicitement (l'adaptateur S3
     * en a besoin pour le header Content-Length sans bufferiser entièrement).
     */
    void put(String key, String contentType, byte[] content);

    /**
     * Génère une URL pré-signée de lecture (GET) valable {@code ttl}.
     * L'objet n'est jamais exposé en accès public.
     */
    URL presignGet(String key, Duration ttl);

    /** Supprime l'objet. Idempotent : aucune erreur si la clé n'existe pas. */
    void delete(String key);
}

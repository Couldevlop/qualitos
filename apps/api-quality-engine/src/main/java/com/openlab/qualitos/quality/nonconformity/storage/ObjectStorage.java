package com.openlab.qualitos.quality.nonconformity.storage;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

    /**
     * Énumère les objets dont la clé commence par {@code prefix}, au plus
     * {@code limit}.
     *
     * <p>Sert exclusivement au balayage des binaires orphelins : un objet écrit
     * juste avant qu'une transaction n'échoue survit à la ligne qui le
     * désignait, et devient alors invisible pour l'application — et facturé
     * indéfiniment. Sans énumération, ces objets sont indétectables : rien, dans
     * la base, ne dit qu'ils existent.
     *
     * <p>{@code limit} est exigé plutôt que suggéré : un bucket de production
     * compte des dizaines de milliers d'objets, et un balayage sans borne
     * chargerait tout en mémoire pour n'en supprimer, presque toujours, aucun.
     */
    List<StoredObject> list(String prefix, int limit);

    /**
     * Un objet vu côté stockage, réduit à ce que le balayage exige.
     *
     * <p>{@code lastModified} n'est pas décoratif : c'est lui qui distingue un
     * orphelin d'un objet tout juste écrit par une transaction encore ouverte.
     * Sans cette date, le balayage supprimerait des preuves en cours de dépôt.
     */
    record StoredObject(String key, Instant lastModified, long sizeBytes) {}
}

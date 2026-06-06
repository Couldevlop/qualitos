package com.openlab.qualitos.quality.nonconformity.storage;

/**
 * Levée par les endpoints photos quand aucun {@link ObjectStorage} n'est actif
 * (propriété {@code qualitos.storage.s3.enabled} à false — comportement par
 * défaut). Mappée en 503 ProblemDetail par le GlobalExceptionHandler : pas de
 * NPE, message explicite côté client.
 */
public class StorageDisabledException extends RuntimeException {
    public StorageDisabledException() {
        super("Binary photo storage is disabled (qualitos.storage.s3.enabled=false)");
    }
}

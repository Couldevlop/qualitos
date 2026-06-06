package com.openlab.qualitos.quality.nonconformity.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration du stockage objet S3-compatible (§4.3).
 * Toutes les valeurs proviennent de variables d'environnement (cf. application.yml) :
 * AUCUN secret par défaut en dur (CLAUDE.md §18.2.3). Les credentials de dev
 * vivent uniquement dans docker-compose.dev.yml.
 */
@Component
@ConfigurationProperties(prefix = "qualitos.storage.s3")
public class StorageProperties {

    /** Active l'adaptateur S3 réel. OFF par défaut. */
    private boolean enabled = false;

    /** Endpoint S3/MinIO (ex. http://minio:9000). */
    private String endpoint;

    /** Bucket cible. */
    private String bucket;

    /** Région (MinIO ignore mais l'SDK l'exige ; us-east-1 par défaut). */
    private String region = "us-east-1";

    /** Clé d'accès (jamais en dur — ENV uniquement). */
    private String accessKey;

    /** Clé secrète (jamais en dur — ENV uniquement). */
    private String secretKey;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
}

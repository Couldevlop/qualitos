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

    /** Endpoint S3/MinIO joignable par le SERVEUR (ex. http://minio:9000). */
    private String endpoint;

    /**
     * Endpoint joignable par le NAVIGATEUR, utilisé pour signer les URLs de
     * lecture. Vide par défaut : les deux se confondent en développement.
     *
     * <p>En cluster ils diffèrent nécessairement. Le service interne
     * ({@code http://minio.<ns>.svc.cluster.local:9000}) ne se résout pas depuis
     * un poste : une URL présignée sur cet hôte serait signée correctement et
     * pourtant inouvrable — la pire des pannes, puisque le serveur ne voit passer
     * aucune erreur. La signature couvrant l'hôte, il ne suffit pas de réécrire
     * l'URL après coup : il faut signer pour l'hôte public dès l'origine.
     */
    private String publicEndpoint;

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

    public String getPublicEndpoint() { return publicEndpoint; }
    public void setPublicEndpoint(String publicEndpoint) { this.publicEndpoint = publicEndpoint; }

    /**
     * Endpoint à utiliser pour signer les lectures : le public s'il est déclaré,
     * l'interne sinon. Le repli est explicite plutôt que laissé au hasard d'une
     * variable vide, qui produirait une URI invalide au démarrage.
     */
    public String resolvePresignEndpoint() {
        return publicEndpoint == null || publicEndpoint.isBlank() ? endpoint : publicEndpoint;
    }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
}

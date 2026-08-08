package com.openlab.qualitos.quality.nonconformity.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'URL présignée est ouverte par le NAVIGATEUR, jamais par le serveur. Ces tests
 * tiennent l'invariant qui rend les pièces jointes réellement consultables : elle
 * est signée pour l'hôte public quand il existe.
 *
 * <p>La signature se calcule hors ligne — aucun appel réseau n'est fait ici.
 */
class S3ObjectStorageTest {

    private static StorageProperties props(String endpoint, String publicEndpoint) {
        StorageProperties p = new StorageProperties();
        p.setEnabled(true);
        p.setEndpoint(endpoint);
        p.setPublicEndpoint(publicEndpoint);
        p.setBucket("qualitos-evidence");
        p.setRegion("us-east-1");
        p.setAccessKey("test-access-key");
        p.setSecretKey("test-secret-key");
        return p;
    }

    @Test
    @DisplayName("signe la lecture pour l'hôte public, pas pour le service interne")
    void presignsAgainstPublicEndpoint() {
        S3ObjectStorage storage = new S3ObjectStorage(
                props("http://minio.qualitos-preprod.svc.cluster.local:9000",
                      "https://preprod.qualitos.example.com"));

        URL url = storage.presignGet("tenants/t1/capa/c1/file.pdf", Duration.ofMinutes(15));

        assertThat(url.getHost()).isEqualTo("preprod.qualitos.example.com");
        assertThat(url.getProtocol()).isEqualTo("https");
        // Path-style : le bucket est dans le chemin, ce qui permet de router la
        // lecture par simple préfixe d'ingress, sans réécriture — une réécriture
        // invaliderait la signature, qui couvre le chemin.
        assertThat(url.getPath()).isEqualTo("/qualitos-evidence/tenants/t1/capa/c1/file.pdf");
        assertThat(url.getQuery()).contains("X-Amz-Signature");
    }

    @Test
    @DisplayName("sans hôte public déclaré, retombe sur l'endpoint interne")
    void fallsBackToInternalEndpoint() {
        S3ObjectStorage storage = new S3ObjectStorage(props("http://minio:9000", null));

        URL url = storage.presignGet("tenants/t1/nc/n1/photo.jpg", Duration.ofMinutes(15));

        assertThat(url.getHost()).isEqualTo("minio");
        assertThat(url.getPort()).isEqualTo(9000);
    }

    @Test
    @DisplayName("un hôte public vide vaut absence, pas URI invalide")
    void blankPublicEndpointIsAbsence() {
        assertThat(props("http://minio:9000", "   ").resolvePresignEndpoint())
                .isEqualTo("http://minio:9000");
        assertThat(props("http://minio:9000", "").resolvePresignEndpoint())
                .isEqualTo("http://minio:9000");
    }

    @Test
    @DisplayName("la durée de validité demandée est celle qui est signée")
    void signsRequestedTtl() {
        S3ObjectStorage storage = new S3ObjectStorage(props("http://minio:9000", null));

        URL url = storage.presignGet("tenants/t1/capa/c1/file.pdf", Duration.ofMinutes(15));

        assertThat(url.getQuery()).contains("X-Amz-Expires=900");
    }
}

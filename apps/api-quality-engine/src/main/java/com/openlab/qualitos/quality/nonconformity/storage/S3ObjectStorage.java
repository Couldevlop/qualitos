package com.openlab.qualitos.quality.nonconformity.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptateur S3 (AWS SDK v2) compatible MinIO. Path-style obligatoire (MinIO ne
 * supporte pas le virtual-host bucket par défaut). Activé uniquement quand
 * {@code qualitos.storage.s3.enabled=true} — sinon aucun bean n'est créé et les
 * endpoints répondent 503 (StorageDisabledException).
 *
 * <p>Credentials/endpoint/bucket viennent exclusivement de {@link StorageProperties}
 * (variables d'environnement) : aucun secret par défaut en dur (§18.2.3).</p>
 *
 * <p>Deux endpoints, et non un seul : les écritures partent du serveur et visent
 * le service interne ; les lectures présignées sont ouvertes par le navigateur et
 * doivent donc porter l'hôte public (cf. {@link StorageProperties#resolvePresignEndpoint()}).</p>
 */
@Component
@ConditionalOnProperty(prefix = "qualitos.storage.s3", name = "enabled", havingValue = "true")
public class S3ObjectStorage implements ObjectStorage {

    /** Plafond imposé par S3 sur {@code maxKeys} : au-delà, la réponse est tronquée. */
    private static final int S3_MAX_KEYS_PER_PAGE = 1000;

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ObjectStorage(StorageProperties props) {
        this.bucket = props.getBucket();
        Region region = Region.of(props.getRegion());
        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
        URI endpoint = URI.create(props.getEndpoint());
        // L'URL présignée est ouverte par le NAVIGATEUR, pas par le serveur : elle
        // doit donc être signée pour l'hôte public. Signer pour le service interne
        // du cluster produirait une URL parfaitement valide et pourtant inouvrable,
        // sans qu'aucune erreur ne remonte côté serveur. La signature couvrant
        // l'hôte, on ne peut pas non plus réécrire l'URL après coup.
        URI presignEndpoint = URI.create(props.resolvePresignEndpoint());
        S3Configuration s3cfg = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        this.client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(creds)
                .serviceConfiguration(s3cfg)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(presignEndpoint)
                .region(region)
                .credentialsProvider(creds)
                .serviceConfiguration(s3cfg)
                .build();
    }

    @Override
    public void put(String key, String contentType, byte[] content) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();
        client.putObject(request, RequestBody.fromBytes(content));
    }

    @Override
    public URL presignGet(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(presign).url();
    }

    @Override
    public void delete(String key) {
        // S3 DELETE est idempotent : aucune erreur si la clé n'existe pas.
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /**
     * ListObjectsV2 paginé, arrêté dès que {@code limit} est atteint.
     *
     * <p>La pagination est déroulée à la main plutôt que par le paginateur du
     * SDK : celui-ci parcourt le bucket entier de façon paresseuse, et une
     * interruption au milieu laisserait une connexion HTTP ouverte. Ici, on sait
     * exactement combien de pages on demande, et on s'arrête net.
     *
     * <p>{@code maxKeys} est borné à 1000 par S3 ; demander davantage ne renvoie
     * pas plus. On demande donc le minimum entre ce plafond et ce qu'il reste
     * à collecter, pour ne pas rapatrier mille clés quand on en veut dix.
     */
    @Override
    public List<StoredObject> list(String prefix, String startAfter, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<StoredObject> out = new ArrayList<>();
        String continuationToken = null;
        do {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    // `startAfter` ne vaut que pour la PREMIÈRE page : au-delà,
                    // c'est le jeton de continuation qui situe la lecture, et S3
                    // ignore alors startAfter. Le passer quand même serait sans
                    // effet, mais le retirer explicitement dit où est l'autorité.
                    .startAfter(continuationToken == null ? startAfter : null)
                    .maxKeys(Math.min(S3_MAX_KEYS_PER_PAGE, limit - out.size()))
                    .continuationToken(continuationToken)
                    .build();
            ListObjectsV2Response response = client.listObjectsV2(request);
            for (S3Object o : response.contents()) {
                out.add(new StoredObject(o.key(), o.lastModified(), o.size()));
                if (out.size() >= limit) {
                    return List.copyOf(out);
                }
            }
            continuationToken = Boolean.TRUE.equals(response.isTruncated())
                    ? response.nextContinuationToken()
                    : null;
        } while (continuationToken != null);
        return List.copyOf(out);
    }
}

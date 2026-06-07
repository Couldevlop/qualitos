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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

/**
 * Adaptateur S3 (AWS SDK v2) compatible MinIO. Path-style obligatoire (MinIO ne
 * supporte pas le virtual-host bucket par défaut). Activé uniquement quand
 * {@code qualitos.storage.s3.enabled=true} — sinon aucun bean n'est créé et les
 * endpoints répondent 503 (StorageDisabledException).
 *
 * <p>Credentials/endpoint/bucket viennent exclusivement de {@link StorageProperties}
 * (variables d'environnement) : aucun secret par défaut en dur (§18.2.3).</p>
 */
@Component
@ConditionalOnProperty(prefix = "qualitos.storage.s3", name = "enabled", havingValue = "true")
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ObjectStorage(StorageProperties props) {
        this.bucket = props.getBucket();
        Region region = Region.of(props.getRegion());
        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
        URI endpoint = URI.create(props.getEndpoint());
        S3Configuration s3cfg = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        this.client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(creds)
                .serviceConfiguration(s3cfg)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
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
}

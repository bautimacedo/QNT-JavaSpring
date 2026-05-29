package com.gestion.qnt.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

/**
 * Genera presigned URLs para que el frontend acceda a archivos en S3 sin
 * exponer las credenciales AWS. Las URLs no se persisten — se generan
 * on-demand cuando alguien pide un archivo de inspección.
 *
 * Credenciales: si AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY están seteadas
 * (vía env var o JVM property -D), las usa explícitamente. Si no, cae al
 * DefaultCredentialsProvider del SDK (IAM role en EC2/ECS, ~/.aws/credentials).
 *
 * NOTA: la `-D` en JVM setea system properties con el nombre exacto que
 * uno escribe (ej. `-DAWS_ACCESS_KEY_ID=...` crea la prop `AWS_ACCESS_KEY_ID`).
 * El DefaultCredentialsProvider del SDK busca específicamente
 * `aws.accessKeyId` (camelCase) en sys props o `AWS_ACCESS_KEY_ID` en env
 * vars; por eso `@Value("${AWS_ACCESS_KEY_ID:}")` es lo que matchea con
 * el setenv.sh actual sin tener que renombrar nada.
 */
@Service
@Slf4j
public class AwsS3Service {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String defaultBucket;
    private final Duration ttl;

    public AwsS3Service(
            @Value("${aws.region}") String region,
            @Value("${aws.s3.results-bucket}") String defaultBucket,
            @Value("${aws.s3.presign-ttl-seconds:3600}") long ttlSeconds,
            @Value("${AWS_ACCESS_KEY_ID:}") String accessKeyId,
            @Value("${AWS_SECRET_ACCESS_KEY:}") String secretAccessKey) {
        this.defaultBucket = defaultBucket;
        this.ttl = Duration.ofSeconds(ttlSeconds);

        Region awsRegion = Region.of(region);
        boolean explicitCreds = !accessKeyId.isBlank() && !secretAccessKey.isBlank();

        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(awsRegion);
        S3ClientBuilder clientBuilder = S3Client.builder().region(awsRegion);

        if (explicitCreds) {
            StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
            presignerBuilder.credentialsProvider(creds);
            clientBuilder.credentialsProvider(creds);
            log.info("AwsS3Service: usando credenciales explícitas (access key id termina en ...{}).",
                    accessKeyId.substring(Math.max(0, accessKeyId.length() - 4)));
        } else {
            log.info("AwsS3Service: usando DefaultCredentialsProvider (IAM role / ~/.aws / etc.).");
        }

        this.presigner = presignerBuilder.build();
        this.s3Client  = clientBuilder.build();
        log.info("AwsS3Service inicializado: region={}, bucket={}, ttl={}s", region, defaultBucket, ttlSeconds);
    }

    /**
     * Genera una presigned URL GET para el objeto {bucket, key}. Si bucket es
     * null o blank, usa el bucket por defecto configurado.
     */
    public URL generatePresignedGetUrl(String bucket, String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key vacía");
        }
        String effectiveBucket = (bucket == null || bucket.isBlank()) ? defaultBucket : bucket;

        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(effectiveBucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getReq)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);
        return presigned.url();
    }

    /**
     * Descarga el objeto {bucket, key} de S3 y devuelve sus bytes.
     * Usar solo para archivos pequeños (imágenes). Para video usar presigned URL.
     */
    public byte[] getBytes(String bucket, String key) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key vacía");
        }
        String effectiveBucket = (bucket == null || bucket.isBlank()) ? defaultBucket : bucket;
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(effectiveBucket)
                .key(key)
                .build();
        try (var resp = s3Client.getObject(req)) {
            return resp.readAllBytes();
        }
    }

    @PreDestroy
    public void close() {
        try { presigner.close(); } catch (Exception e) { log.warn("Error cerrando S3Presigner: {}", e.getMessage()); }
        try { s3Client.close();  } catch (Exception e) { log.warn("Error cerrando S3Client: {}", e.getMessage()); }
    }
}

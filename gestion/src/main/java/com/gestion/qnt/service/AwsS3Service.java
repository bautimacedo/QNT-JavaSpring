package com.gestion.qnt.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

/**
 * Genera presigned URLs para que el frontend acceda a archivos en S3 sin
 * exponer las credenciales AWS. Las URLs no se persisten — se generan
 * on-demand cuando alguien pide un archivo de inspección.
 */
@Service
@Slf4j
public class AwsS3Service {

    private final S3Presigner presigner;
    private final String defaultBucket;
    private final Duration ttl;

    public AwsS3Service(
            @Value("${aws.region}") String region,
            @Value("${aws.s3.results-bucket}") String defaultBucket,
            @Value("${aws.s3.presign-ttl-seconds:3600}") long ttlSeconds) {
        this.defaultBucket = defaultBucket;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();
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

    @PreDestroy
    public void close() {
        try {
            presigner.close();
        } catch (Exception e) {
            log.warn("Error cerrando S3Presigner: {}", e.getMessage());
        }
    }
}

package com.adas.retrofit.service;

import com.adas.retrofit.entity.DamagePhoto;
import com.adas.retrofit.entity.Order;
import com.adas.retrofit.repository.DamagePhotoRepository;
import com.adas.retrofit.repository.OrderRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Фотофиксация повреждений: загрузка изображений в MinIO с привязкой к заявке
 * и отдача их обратно. Байты проксируются через приложение (см. {@link #openContent}),
 * чтобы браузер не обращался к MinIO напрямую (внутри docker-сети хост недоступен).
 */
@Service
public class PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorageService.class);

    private final MinioClient minioClient;
    private final DamagePhotoRepository photoRepository;
    private final OrderRepository orderRepository;
    private final String bucket;

    public PhotoStorageService(MinioClient minioClient,
                               DamagePhotoRepository photoRepository,
                               OrderRepository orderRepository,
                               @Value("${minio.bucket:adas-photos}") String bucket) {
        this.minioClient = minioClient;
        this.photoRepository = photoRepository;
        this.orderRepository = orderRepository;
        this.bucket = bucket;
    }

    /** Загружает один файл в MinIO и сохраняет метаданные. */
    @Transactional
    public DamagePhoto upload(UUID orderId, MultipartFile file) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        ensureBucket();

        String objectKey = orderId + "/" + UUID.randomUUID() + extension(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build());
        } catch (Exception e) {
            throw new PhotoStorageException("Не удалось загрузить файл в MinIO: " + e.getMessage(), e);
        }

        DamagePhoto photo = new DamagePhoto();
        photo.setOrder(order);
        photo.setObjectKey(objectKey);
        photo.setOriginalFilename(file.getOriginalFilename());
        photo.setContentType(file.getContentType());
        photo.setSize(file.getSize());
        DamagePhoto saved = photoRepository.save(photo);
        log.info("Фото {} заявки {} сохранено в MinIO ({})", saved.getId(), orderId, objectKey);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DamagePhoto> listPhotos(UUID orderId) {
        return photoRepository.findByOrderIdOrderByUploadedAtAsc(orderId);
    }

    @Transactional(readOnly = true)
    public DamagePhoto requirePhoto(UUID photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new PhotoStorageException("Фото не найдено: " + photoId, null));
    }

    /** Открывает поток байтов объекта из MinIO (вызывающий обязан закрыть). */
    public InputStream openContent(DamagePhoto photo) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(photo.getObjectKey())
                    .build());
            return response;
        } catch (Exception e) {
            throw new PhotoStorageException("Не удалось получить файл из MinIO: " + e.getMessage(), e);
        }
    }

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Создан бакет MinIO: {}", bucket);
            }
        } catch (Exception e) {
            throw new PhotoStorageException("MinIO недоступен: " + e.getMessage(), e);
        }
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
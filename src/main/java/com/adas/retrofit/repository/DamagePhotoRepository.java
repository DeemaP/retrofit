package com.adas.retrofit.repository;

import com.adas.retrofit.entity.DamagePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DamagePhotoRepository extends JpaRepository<DamagePhoto, UUID> {

    List<DamagePhoto> findByOrderIdOrderByUploadedAtAsc(UUID orderId);
}
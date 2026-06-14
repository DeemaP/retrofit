package com.adas.retrofit.repository;

import com.adas.retrofit.entity.PartSpec;
import com.adas.retrofit.entity.RetrofitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartSpecRepository extends JpaRepository<PartSpec, UUID> {

    List<PartSpec> findByModelAndProductionYearAndRetrofitType(String model, Integer productionYear, RetrofitType retrofitType);

    boolean existsByModelAndProductionYearAndRetrofitType(String model, Integer productionYear, RetrofitType retrofitType);

    void deleteByModelAndProductionYearAndRetrofitType(String model, Integer productionYear, RetrofitType retrofitType);
}
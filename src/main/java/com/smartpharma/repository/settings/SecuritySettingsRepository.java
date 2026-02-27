package com.smartpharma.repository.settings;

import com.smartpharma.entity.settings.SecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {

    @Query("SELECT ss FROM SecuritySettings ss WHERE ss.user.id = :userId")
    Optional<SecuritySettings> findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(@Param("userId") Long userId);
}
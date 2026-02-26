package com.smartpharma.repository.settings;

import com.smartpharma.entity.settings.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.user.id = :userId")
    Optional<NotificationSettings> findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(@Param("userId") Long userId);
}
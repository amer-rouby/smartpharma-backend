package com.smartpharma.repository;

import com.smartpharma.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPharmacyIdAndUsername(@Param("pharmacyId") Long pharmacyId, @Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.pharmacy.id = :pharmacyId AND u.deletedAt IS NULL ORDER BY u.fullName")
    List<User> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.pharmacy.id = :pharmacyId AND u.deletedAt IS NULL")
    Optional<User> findByIdAndPharmacyId(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);

    @Query("SELECT u FROM User u WHERE u.pharmacy.id = :pharmacyId AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) AND u.deletedAt IS NULL")
    List<User> searchByPharmacyIdAndUsername(@Param("pharmacyId") Long pharmacyId, @Param("query") String query);

    @Query("SELECT COUNT(u) FROM User u WHERE u.pharmacy.id = :pharmacyId AND u.deletedAt IS NULL")
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT u FROM User u WHERE u.pharmacy.id = :pharmacyId AND u.isActive = true AND u.deletedAt IS NULL")
    List<User> findActiveByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT u FROM User u WHERE u.pharmacy.id = :pharmacyId AND u.deletedAt IS NULL")
    List<User> findByPharmacyIdAndIsActiveTrue(@Param("pharmacyId") Long pharmacyId);
}
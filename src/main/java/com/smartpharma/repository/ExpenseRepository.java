package com.smartpharma.repository;

import com.smartpharma.entity.Expense;
import com.smartpharma.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ================================
    // ✅ Basic Queries
    // ================================

    Page<Expense> findByPharmacyIdAndDeletedAtIsNull(Long pharmacyId, Pageable pageable);

    Optional<Expense> findByIdAndPharmacyIdAndDeletedAtIsNull(Long id, Long pharmacyId);

    List<Expense> findByPharmacyIdAndExpenseDateBetweenAndDeletedAtIsNull(
            Long pharmacyId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    // ================================
    // ✅ Filter Queries
    // ================================

    Page<Expense> findByPharmacyIdAndCategoryAndDeletedAtIsNull(
            Long pharmacyId,
            ExpenseCategory category,
            Pageable pageable);

    Page<Expense> findByPharmacyIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
            Long pharmacyId,
            String title,
            Pageable pageable);

    // ================================
    // ✅ Aggregation Queries (للـ Reports)
    // ================================

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.pharmacy.id = :pharmacyId
        AND e.deletedAt IS NULL
    """)
    BigDecimal getTotalExpensesByPharmacy(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.pharmacy.id = :pharmacyId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
    """)
    BigDecimal getTotalExpensesByDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.pharmacy.id = :pharmacyId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
        GROUP BY e.category
    """)
    List<Object[]> getExpensesByCategory(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT FUNCTION('DATE', e.expenseDate), COALESCE(SUM(e.amount), 0), COUNT(e)
        FROM Expense e
        WHERE e.pharmacy.id = :pharmacyId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
        GROUP BY FUNCTION('DATE', e.expenseDate)
        ORDER BY FUNCTION('DATE', e.expenseDate)
    """)
    List<Object[]> getDailyExpenses(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.pharmacy.id = :pharmacyId
        AND e.deletedAt IS NULL
        ORDER BY e.expenseDate DESC
    """)
    List<Expense> findRecentExpenses(@Param("pharmacyId") Long pharmacyId, Pageable pageable);

    // ================================
    // ✅ Count Queries
    // ================================

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.pharmacy.id = :pharmacyId AND e.deletedAt IS NULL")
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COUNT(e) FROM Expense e
        WHERE e.pharmacy.id = :pharmacyId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
    """)
    Long countByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
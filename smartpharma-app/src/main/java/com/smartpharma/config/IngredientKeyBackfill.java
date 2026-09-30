package com.smartpharma.config;

import com.smartpharma.catalog.util.IngredientKey;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Product.ingredientKey is derived on every save, but products saved before the
 * column existed (or inserted by SQL seed scripts) have none, so the POS couldn't
 * offer them as alternatives. This runs at startup and:
 *  1. moves the free-text "activeIngredients" the product form used to keep in
 *     extra_attributes into the active_ingredient column (moved, not copied, so
 *     it happens once);
 *  2. derives the key for rows that have a scientific name or active ingredient
 *     but no key.
 * Both steps are no-ops on a database that is already up to date.
 */
@Component
@Order(5)
@Slf4j
public class IngredientKeyBackfill implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int moved = entityManager.createNativeQuery(
                "UPDATE smartpharma.products SET " +
                        "active_ingredient = COALESCE(active_ingredient, " +
                        "  LEFT(NULLIF(TRIM(extra_attributes->>'activeIngredients'), ''), 255)), " +
                        "ingredient_key = NULL, " +
                        "extra_attributes = extra_attributes - 'activeIngredients' " +
                        // jsonb_exists = the "?" operator, which a JPA query would read as a parameter
                        "WHERE jsonb_exists(extra_attributes, 'activeIngredients')"
        ).executeUpdate();
        if (moved > 0) {
            log.info("Moved the active ingredient of {} product(s) out of extra_attributes", moved);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT id, COALESCE(active_ingredient, scientific_name) FROM smartpharma.products " +
                                "WHERE ingredient_key IS NULL " +
                                "AND COALESCE(active_ingredient, scientific_name) IS NOT NULL")
                .getResultList();

        int updated = 0;
        for (Object[] row : rows) {
            String key = IngredientKey.of((String) row[1]);
            if (key == null) continue;
            updated += entityManager.createNativeQuery(
                            "UPDATE smartpharma.products SET ingredient_key = :key WHERE id = :id")
                    .setParameter("key", key)
                    .setParameter("id", ((Number) row[0]).longValue())
                    .executeUpdate();
        }
        if (updated > 0) {
            log.info("Derived the active-ingredient key for {} existing product(s)", updated);
        }
    }
}

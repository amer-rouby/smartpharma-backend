package com.smartpharma.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User.username is meant to be globally unique (it's what login looks up by, with no
 * pharmacy context to disambiguate) - see the comment on User.username. But on a
 * database that was never freshly created from the current entity mapping, Hibernate's
 * ddl-auto=update never retroactively adds a missing unique constraint to an existing
 * column, so a database can end up with only the older (pharmacy_id, username)
 * constraint from an earlier version of this app. That's the wrong scope: it lets two
 * different pharmacies register the same username, which would make
 * UserRepository.findByUsername() (used by login) throw on an ambiguous result for
 * both of them instead of failing predictably at registration time.
 *
 * This runs once at startup and adds the correct global constraint if it's missing -
 * but only if doing so wouldn't fail outright (i.e. no duplicate usernames already
 * exist across different pharmacies). If duplicates are found, it logs them clearly
 * instead of crashing the app or corrupting anything; an admin has to resolve those
 * manually (rename one of the conflicting accounts) before the constraint can be added.
 */
@Component
@Order(1) // run before StockBatchVersionBackfill/other backfills - order doesn't matter much, just keep them deterministic
@Slf4j
public class UsernameUniquenessBackfill implements ApplicationRunner {

    private static final String CONSTRAINT_NAME = "uk_users_username_global";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Tuple> duplicates = entityManager.createNativeQuery(
                        "SELECT username, COUNT(*) as cnt FROM smartpharma.users " +
                                "GROUP BY username HAVING COUNT(*) > 1", Tuple.class)
                .getResultList();

        if (!duplicates.isEmpty()) {
            log.error("Cannot enforce global username uniqueness: {} username(s) are currently shared across " +
                            "more than one pharmacy ({}). Registration and admin user creation already reject new " +
                            "duplicates, but these existing ones must be renamed manually before the database " +
                            "constraint can be added.",
                    duplicates.size(),
                    duplicates.stream().map(t -> t.get("username")).toList());
            return;
        }

        boolean alreadyExists = !entityManager.createNativeQuery(
                        "SELECT 1 FROM information_schema.table_constraints " +
                                "WHERE table_schema = 'smartpharma' AND table_name = 'users' AND constraint_name = :name")
                .setParameter("name", CONSTRAINT_NAME)
                .getResultList()
                .isEmpty();

        if (alreadyExists) {
            return;
        }

        entityManager.createNativeQuery(
                "ALTER TABLE smartpharma.users ADD CONSTRAINT " + CONSTRAINT_NAME + " UNIQUE (username)"
        ).executeUpdate();
        log.info("Added missing global unique constraint on users.username");
    }
}

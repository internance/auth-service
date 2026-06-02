package com.internance.auth.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.internance.auth.domain.model.User;

/**
 * Persistence access for {@link User}. Note the entity's {@code @SQLRestriction}
 * makes derived queries here skip soft-deleted rows.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Whether the username is taken by <em>any</em> row, including soft-deleted
     * ones. A native query is required to bypass the entity's
     * {@code @SQLRestriction}; this keeps the check aligned with the table's
     * full UNIQUE(username) constraint, so a deleted username is never reused.
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)", nativeQuery = true)
    boolean existsByUsernameIncludingDeleted(@Param("username") String username);
}

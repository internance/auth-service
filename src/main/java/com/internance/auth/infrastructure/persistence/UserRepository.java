package com.internance.auth.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.internance.auth.domain.model.User;

/**
 * Persistence access for {@link User}. Thanks to the entity's
 * {@code @SQLRestriction}, every query here transparently excludes
 * soft-deleted rows.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);
}

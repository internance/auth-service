package com.internance.auth.domain.model;

import com.internance.common.entity.BaseSoftDeleteEntity;
import com.internance.common.utils.IdGenerator;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at is null")
public class User extends BaseSoftDeleteEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id = IdGenerator.generate();

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;

    private User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Creates a new user. {@code password} must already be encoded by the
     * caller (e.g. a {@code PasswordEncoder}); the entity never stores raw
     * credentials.
     */
    public static User create(String username, String password, String role) {
        return new User(username, password, role);
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}

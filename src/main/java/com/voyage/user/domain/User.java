package com.voyage.user.domain;

import com.voyage.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    private static final String DEFAULT_CURRENCY = "KRW";
    private static final String DEFAULT_TIMEZONE = "Asia/Seoul";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash. Null for social-only accounts. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String passwordHash, String name,
                 String avatarUrl, String defaultCurrency, String timezone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.defaultCurrency = defaultCurrency;
        this.timezone = timezone;
    }

    /** Creates an email/password user, applying defaults for optional fields. */
    public static User createEmailUser(String email, String passwordHash, String name,
                                       String defaultCurrency, String timezone) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .name(name)
                .defaultCurrency(defaultCurrency != null ? defaultCurrency : DEFAULT_CURRENCY)
                .timezone(timezone != null ? timezone : DEFAULT_TIMEZONE)
                .build();
    }
}

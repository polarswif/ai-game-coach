package com.gamecoach.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One account. The password is never stored, only a BCrypt hash of it: a one-way scramble with a
 * random salt, so even someone who reads the database can't get the passwords back.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** As the person typed it, for display. */
    @Column(nullable = false, length = 20)
    private String username;

    /** Lower-case copy, unique, so "Alex" and "alex" can't both sign up. */
    @Column(name = "username_key", nullable = false, length = 20, unique = true)
    private String usernameKey;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** The invite code used to sign up; unique, which is what makes each code single-use. */
    @Column(name = "invite_code", length = 100, unique = true)
    private String inviteCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUser() {
        // for JPA
    }

    AppUser(String username, String passwordHash, String inviteCode, Instant createdAt) {
        this.username = username;
        this.usernameKey = key(username);
        this.passwordHash = passwordHash;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
    }

    static String key(String username) {
        return username.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public String getUsername() { return username; }
    String getPasswordHash() { return passwordHash; }
}

package com.gamecoach.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameKey(String usernameKey);

    boolean existsByUsernameKey(String usernameKey);

    boolean existsByInviteCode(String inviteCode);
}

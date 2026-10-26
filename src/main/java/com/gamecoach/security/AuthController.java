package com.gamecoach.security;

import com.gamecoach.controller.BadRequestException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.regex.Pattern;

/** Sign up with an invite code, log in, and check who you are. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");
    static final int MIN_PASSWORD = 8;

    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final AuthProperties properties;

    public AuthController(AppUserRepository users, PasswordEncoder passwords, TokenService tokens,
                          AuthProperties properties) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.properties = properties;
    }

    public record SignupRequest(String username, String password, String inviteCode) {
    }

    public record LoginRequest(String username, String password) {
    }

    /** What the website keeps after signing in: the token to send back, and who it belongs to. */
    public record AuthResponse(String token, String username, Instant expiresAt) {
    }

    public record Me(String username) {
    }

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        String username = request.username() == null ? "" : request.username().strip();
        String code = request.inviteCode() == null ? "" : request.inviteCode().strip();
        if (!USERNAME.matcher(username).matches()) {
            throw new BadRequestException("Usernames are 3 to 20 letters, numbers, - or _.");
        }
        if (request.password() == null || request.password().length() < MIN_PASSWORD) {
            throw new BadRequestException("Passwords need at least " + MIN_PASSWORD + " characters.");
        }
        if (!properties.inviteCodes().contains(code) || users.existsByInviteCode(code)) {
            throw new BadRequestException("That invite code isn't valid or has already been used.");
        }
        if (users.existsByUsernameKey(AppUser.key(username))) {
            throw new BadRequestException("That username is taken.");
        }
        users.save(new AppUser(username, passwords.encode(request.password()), code, Instant.now()));
        return ResponseEntity.status(HttpStatus.CREATED).body(respond(username));
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public AuthResponse login(@RequestBody LoginRequest request) {
        String username = request.username() == null ? "" : request.username().strip();
        AppUser user = users.findByUsernameKey(AppUser.key(username)).orElse(null);
        // The same message whether the name or the password was wrong, so it doesn't reveal which accounts exist.
        if (user == null || request.password() == null || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw new LoginFailedException("Wrong username or password.");
        }
        return respond(user.getUsername());
    }

    @GetMapping("/me")
    public Me me(Authentication authentication) {
        return new Me(authentication.getName());
    }

    private AuthResponse respond(String username) {
        TokenService.Token token = tokens.issue(username);
        return new AuthResponse(token.value(), username, token.expiresAt());
    }
}

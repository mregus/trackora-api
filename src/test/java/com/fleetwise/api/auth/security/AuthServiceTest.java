package com.fleetwise.api.auth.security;

import com.fleetwise.api.auth.dto.*;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.entity.UserRole;
import com.fleetwise.api.auth.repository.PasswordResetTokenRepository;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.notification.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthenticationManager authManager = mock(AuthenticationManager.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final EmailService emailService = mock(EmailService.class);

    @Value("${app.frontend-url}")
    private String frontendUrl;
    private AuthService service;

    @BeforeEach
    void setup() {
        service = new AuthService(userRepo, encoder, jwtService, authManager, passwordResetTokenRepository, emailService);
    }

    @Test
    void register_NewEmail_ShouldCreateUserAndReturnToken() {
        RegisterRequest req = new RegisterRequest("john@x.com", "pass", "John", "Doe", UserRole.OWNER);
        when(userRepo.existsByEmailIgnoreCase("john@x.com")).thenReturn(false);
        when(encoder.encode("pass")).thenReturn("hashed");
        when(userRepo.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse resp = service.register(req);

        assertEquals("jwt-token", resp.token());
        assertEquals("john@x.com", resp.email());
        verify(userRepo).save(any(User.class));
    }

    @Test
    void register_ExistingEmail_ShouldThrowException() {
        when(userRepo.existsByEmailIgnoreCase(any())).thenReturn(true);
        RegisterRequest req = new RegisterRequest("A","B","dup@x.com","p", UserRole.VIEWER);
        assertThrows(IllegalArgumentException.class, () -> service.register(req));
    }

    @Test
    void login_ValidCredentials_ShouldReturnToken() {
        LoginRequest req = new LoginRequest("me@x.com", "pass");
        User user = User.builder().id(UUID.randomUUID()).email(req.email()).build();
        when(userRepo.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt");
        AuthResponse resp = service.login(req);
        assertEquals("jwt", resp.token());
    }

    @Test
    void me_ExistingUser_ShouldReturnProfile() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id).email("mail@x.com").firstName("F").lastName("L").role(UserRole.MANAGER).build();
        UserPrincipal principal = new UserPrincipal(user);
        when(userRepo.findById(id)).thenReturn(Optional.of(user));

        MeResponse me = service.me(principal);
        assertEquals("mail@x.com", me.email());
    }
}

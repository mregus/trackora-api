package com.fleetwise.api.auth.security;

import com.fleetwise.api.auth.dto.*;
import com.fleetwise.api.auth.entity.PasswordResetToken;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.PasswordResetTokenRepository;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.notification.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role())
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        return toAuthResponse(saved, token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        String token = jwtService.generateToken(user);

        return toAuthResponse(user, token);
    }

    public MeResponse me(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        logger.debug("Forgot password request: {}", request);

        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            emailService.sendEmail(
                    user.getEmail(),
                    "Reset your Trackora password",
                    """
                    <h2>Reset your Trackora password</h2>
                    <p>Click the link below to reset your password.</p>
                    <p><a href="%s">Reset Password</a></p>
                    <p>This link expires in 1 hour.</p>
                    """.formatted(resetUrl)
            );
        });
        logger.info("Forgot password request has been sent");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token.");
        }

        var user = resetToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
}
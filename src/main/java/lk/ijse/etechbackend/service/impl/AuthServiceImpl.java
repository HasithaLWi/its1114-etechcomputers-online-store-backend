package lk.ijse.etechbackend.service.impl;

import lk.ijse.etechbackend.dto.ApiResponse;
import lk.ijse.etechbackend.dto.AuthDTO;
import lk.ijse.etechbackend.dto.AuthResponseDTO;
import lk.ijse.etechbackend.dto.ForgotPasswordDTO;
import lk.ijse.etechbackend.dto.UserDTO;
import lk.ijse.etechbackend.entity.User;
import lk.ijse.etechbackend.enumiration.UserRole;
import lk.ijse.etechbackend.exception.BadRequestException;
import lk.ijse.etechbackend.exception.ResourceNotFoundException;
import lk.ijse.etechbackend.exception.UnauthorizedException;
import lk.ijse.etechbackend.repository.UserRepository;
import lk.ijse.etechbackend.security.JwtUtil;
import lk.ijse.etechbackend.service.AuthService;
import lk.ijse.etechbackend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    // Thread-safe in-memory cache for OTP and password reset tokens
    private final Map<String, OtpState> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private static class OtpState {
        String otp;
        String resetToken;
        LocalDateTime createdAt;
        LocalDateTime expiresAt;
        int attempts;
        boolean verified;
        String userEmail;

        OtpState(String otp, String userEmail) {
            this.otp = otp;
            this.userEmail = userEmail;
            this.createdAt = LocalDateTime.now();
            this.expiresAt = LocalDateTime.now().plusMinutes(10);
            this.attempts = 0;
            this.verified = false;
        }
    }

    @Override
    public AuthResponseDTO login(AuthDTO request) {
        log.info("Login attempt for username: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Password mismatch for username: {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }

        UserDTO userDTO = mapToDTO(user, null);
        String token = jwtUtil.generateToken(userDTO);

        log.info("User {} successfully authenticated with role {}", user.getUsername(), user.getRole());
        return AuthResponseDTO.builder()
                .token(token)
                .user(userDTO)
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO register(UserDTO request) {
        log.info("Registering customer account with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .assignedBranch(null)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user ID: {} with role: {}", savedUser.getId(), savedUser.getRole());

        // Dispatch welcome email asynchronously
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName(), savedUser.getUsername());
        } catch (Exception e) {
            log.warn("Could not dispatch welcome email: {}", e.getMessage());
        }

        UserDTO userDTO = mapToDTO(savedUser, null);
        String token = jwtUtil.generateToken(userDTO);

        return AuthResponseDTO.builder()
                .token(token)
                .user(userDTO)
                .build();
    }

    @Override
    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return mapToDTO(user, null);
    }

    @Override
    public ApiResponse requestPasswordResetOtp(ForgotPasswordDTO.RequestOtp request) {
        String cleanIdentifier = (request.getIdentifier() != null) ? request.getIdentifier().trim().toLowerCase() : "";
        if (cleanIdentifier.isEmpty()) {
            throw new BadRequestException("Username or email address is required");
        }

        User user = userRepository.findByEmail(cleanIdentifier)
                .or(() -> userRepository.findByUsername(cleanIdentifier))
                .orElseThrow(() -> new BadRequestException("No registered account found with that email or username."));

        // Anti-spam cooldown check (60 seconds)
        OtpState existing = otpCache.get(user.getEmail().toLowerCase());
        if (existing != null && Duration.between(existing.createdAt, LocalDateTime.now()).getSeconds() < 60) {
            long remaining = 60 - Duration.between(existing.createdAt, LocalDateTime.now()).getSeconds();
            throw new BadRequestException("Please wait " + remaining + " seconds before requesting another code.");
        }

        // Generate 6-digit numeric OTP
        int otpNum = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpNum);

        log.info("Dispatching real Password Reset OTP [{}] via internet to: {} ({})", otp, user.getUsername(), user.getEmail());

        // Dispatch email over SMTP
        try {
            emailService.sendPasswordResetOtp(user.getEmail(), user.getName(), otp);
        } catch (Exception e) {
            log.error("Failed to send real OTP email to [{}]: {}", user.getEmail(), e.getMessage());
            throw new BadRequestException("Failed to send verification email to " + maskEmail(user.getEmail()) + ". Please verify SMTP server settings.");
        }

        // Store in cache only after email dispatch is executed
        OtpState state = new OtpState(otp, user.getEmail());
        otpCache.put(user.getEmail().toLowerCase(), state);
        otpCache.put(user.getUsername().toLowerCase(), state);

        String maskedEmail = maskEmail(user.getEmail());
        return ApiResponse.success("A 6-digit verification code has been sent to " + maskedEmail, Map.of(
                "maskedEmail", maskedEmail,
                "expiresInMinutes", 10
        ));
    }

    @Override
    public ApiResponse verifyPasswordResetOtp(ForgotPasswordDTO.VerifyOtp request) {
        String key = (request.getIdentifier() != null) ? request.getIdentifier().trim().toLowerCase() : "";
        OtpState state = otpCache.get(key);

        if (state == null) {
            throw new BadRequestException("No active reset request found. Please request a new verification code.");
        }

        if (LocalDateTime.now().isAfter(state.expiresAt)) {
            otpCache.remove(key);
            throw new BadRequestException("Verification code has expired. Please request a new code.");
        }

        if (state.attempts >= 5) {
            otpCache.remove(key);
            throw new BadRequestException("Too many invalid attempts. For your security, this code has been cancelled.");
        }

        if (!state.otp.equals(request.getOtp().trim())) {
            state.attempts++;
            int remaining = 5 - state.attempts;
            throw new BadRequestException("Invalid verification code. " + remaining + " attempts remaining.");
        }

        // Valid OTP
        state.verified = true;
        state.resetToken = UUID.randomUUID().toString();

        return ApiResponse.success("Verification code confirmed.", Map.of(
                "resetToken", state.resetToken
        ));
    }

    @Override
    @Transactional
    public ApiResponse resetPassword(ForgotPasswordDTO.ResetPassword request) {
        String key = (request.getIdentifier() != null) ? request.getIdentifier().trim().toLowerCase() : "";
        OtpState state = otpCache.get(key);

        if (state == null || !state.verified) {
            throw new BadRequestException("Session invalid or expired. Please verify your OTP code first.");
        }

        if (LocalDateTime.now().isAfter(state.expiresAt)) {
            otpCache.remove(key);
            throw new BadRequestException("Reset session expired. Please start over.");
        }

        // Verify token (or OTP as token)
        if (!request.getResetToken().equals(state.resetToken) && !request.getResetToken().equals(state.otp)) {
            throw new BadRequestException("Invalid reset token authorization.");
        }

        User user = userRepository.findByEmail(state.userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Purge OTP from cache
        otpCache.remove(user.getEmail().toLowerCase());
        otpCache.remove(user.getUsername().toLowerCase());

        // Send security alert
        try {
            emailService.sendPasswordChangedAlert(user.getEmail(), user.getName());
        } catch (Exception e) {
            log.warn("Could not dispatch password changed alert email: {}", e.getMessage());
        }

        log.info("Password successfully updated for user: {}", user.getUsername());
        return ApiResponse.success("Your password has been successfully updated! You can now log in with your new password.");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) return name.charAt(0) + "***" + domain;
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    private UserDTO mapToDTO(User user, Boolean canManage) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .assignedBranch(user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : null)
                .canManage(canManage)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
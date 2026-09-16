package lk.ijse.etechbackend.controller;

import jakarta.validation.Valid;
import lk.ijse.etechbackend.dto.ApiResponse;
import lk.ijse.etechbackend.dto.AuthDTO;
import lk.ijse.etechbackend.dto.AuthResponseDTO;
import lk.ijse.etechbackend.dto.ForgotPasswordDTO;
import lk.ijse.etechbackend.dto.UserDTO;
import lk.ijse.etechbackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthDTO request) {
        log.info("REST: Login request for user: {}", request.getUsername());
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody UserDTO request) {
        log.info("REST: Register request for user: {}", request.getUsername());
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("REST: Get current user profile for: {}", userDetails.getUsername());
        UserDTO userDTO = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<ApiResponse> requestPasswordResetOtp(@Valid @RequestBody ForgotPasswordDTO.RequestOtp request) {
        log.info("REST: Password reset OTP request for: {}", request.getIdentifier());
        ApiResponse response = authService.requestPasswordResetOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse> verifyPasswordResetOtp(@Valid @RequestBody ForgotPasswordDTO.VerifyOtp request) {
        log.info("REST: Password reset OTP verification for: {}", request.getIdentifier());
        ApiResponse response = authService.verifyPasswordResetOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ForgotPasswordDTO.ResetPassword request) {
        log.info("REST: Password reset completion for: {}", request.getIdentifier());
        ApiResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
